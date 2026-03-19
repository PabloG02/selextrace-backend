package pablog.selextrace.cluster;

import org.eclipse.collections.impl.factory.primitive.IntLists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import orestes.bloomfilter.BloomFilter;
import orestes.bloomfilter.FilterBuilder;
import pablog.selextrace.config.AptaClusterConfiguration;
import pablog.selextrace.domain.cluster.ClusterContainer;
import pablog.selextrace.domain.experiment.Experiment;
import pablog.selextrace.domain.experiment.SelectionCycle;
import pablog.selextrace.domain.pool.AptamerBounds;
import pablog.selextrace.lib.rnafold.Index;
import pablog.selextrace.util.QSComparator;
import pablog.selextrace.util.Quicksort;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementation of AptaCluster in Java.
 *
 * @author Jan Hoinka
 */
public class HashAptaCluster implements AptaCluster {

    private static final Logger log = LoggerFactory.getLogger(HashAptaCluster.class);

    /** Stores all the LSH instances according to {@link #lshIterations} */
    private final ArrayList<LocalitySensitiveHash> lsh = new ArrayList<>();
    /** Number of LSH iterations to perform */
    private final Integer lshIterations;
    /**
     * Length of the randomized region in the aptamers.
     * Only aptamers matching this length will be considered; sequences of other lengths are ignored.
     */
    private final Integer randomizedRegionSize;
    /** The LSH dimension */
    private final Integer localityHashDimension;
    /** Experiment reference */
    private final Experiment experiment;
    /** The kmer size to use for distance calculations */
    private final Integer kmerSize;
    /**
     * Maximum number of mutations allowed relative to the seed sequence
     * for an aptamer to be included in the cluster.
     */
    private final Integer editDistance;
    /** Number of iterations */
    private final Integer kmerCutoffIterations;
    /** The edit distance converted into the kmer distance space */
    private Double kmerCutoff = null;
    /** Thread-safe counter for generating unique cluster IDs. */
    private final AtomicInteger clusterId = new AtomicInteger(-1);
    /**
     * Array of aptamer IDs sorted in descending order
     * by their cumulative cardinality across all selection cycles.
     */
    private int[] aptamersBySize = null;
    /**
     * Stores cluster information for aptamers.
     * key = aptamer ID, value = cluster ID
     */
    private final ClusterContainer clusters;
    private final int maxNumberOfCores;

    public HashAptaCluster(Experiment experiment, AptaClusterConfiguration config, ClusterContainer clusters) {
        if (experiment == null) {
            throw new IllegalArgumentException("Experiment must not be null");
        }
        if (config == null) {
            throw new IllegalArgumentException("AptaClusterConfiguration must not be null");
        }
        if (clusters == null) {
            throw new IllegalArgumentException("Cluster container must not be null");
        }

        this.randomizedRegionSize = config.randomizedRegionSize();
        this.localityHashDimension = config.lshDimension();
        this.lshIterations = config.lshIterations();
        this.editDistance = config.editDistance();
        this.kmerSize = config.kmerSize();
        this.kmerCutoffIterations = config.kmerCutoffIterations();
        this.maxNumberOfCores = 12; // TODO: MAKE THIS A PARAMETER
        this.experiment = experiment;
        this.clusters = clusters;

        for (int x = 0; x < lshIterations; x++) {
            lsh.add(new LocalitySensitiveHash(randomizedRegionSize, localityHashDimension, lsh));
        }
    }

    @Override
    public void performLSH() {
        log.info("Starting AptaCluster");

        initializeAptamersBySize();
        computeKmerCutoff();

        for (int x = 0; x < this.lshIterations; x++) {
            lshIteration(x + 1, lsh.get(x));
        }

        adjustClusterIds();
        clusters.setReadOnly();
    }

    private void lshIteration(int iterationNumber, LocalitySensitiveHash lshInstance) {
        log.info("Starting LSH Iteration {}", iterationNumber);
        long startTimeMs = System.currentTimeMillis();

        // Create an LSH hashing representation of the data
        Buckets buckets = generateBuckets(lshInstance);

        log.info("Assigning clusters based on kmer distance");
        if (iterationNumber == 1) {
            // Process the buckets in parallel
            AtomicInteger progress = new AtomicInteger(0);
            // Creating shared object
            BlockingQueue<Object> sharedQueue = new ArrayBlockingQueue<>(500); // TODO MAKE THIS A PARAMETER

            int numThreads = Math.min(Runtime.getRuntime().availableProcessors(), maxNumberOfCores);

            // Create producer and consumer threads
            Thread prodThread = new Thread(new LSHInitializationProducer(buckets, sharedQueue), "LSHInitialization Producer");
            ArrayList<Thread> consumers = new ArrayList<>();

            // We need at least one consumer
            consumers.add(new Thread(new LSHInitializationConsumer(
                sharedQueue,
                clusterId,
                clusters,
                kmerSize,
                kmerCutoff,
                progress,
                experiment
            ), "LSHInitialization Consumer 1"));

            // Add remaining consumers
            for (int x = 1; x < numThreads - 1; x++) {
                consumers.add(new Thread(new LSHInitializationConsumer(
                    sharedQueue,
                    clusterId,
                    clusters,
                    kmerSize,
                    kmerCutoff,
                    progress,
                    experiment
                ), "LSHInitialization Consumer " + (x + 1)));
            }

            // Start threads
            for (Thread consumer : consumers) {
                consumer.start();
            }
            prodThread.start();

            // Monitor progress
            while (prodThread.isAlive() && !prodThread.isInterrupted()) {
                try {
                    log.info("Processed {}/{} buckets", progress.get(), buckets.size());
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // Wait for threads to finish
            try {
                for (Thread consumer : consumers) {
                    consumer.join();
                }
                prodThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            log.info("Processed {}/{} buckets", progress.get(), buckets.size());
            log.info("Clustering completed, found {} clusters", clusterId.get());
        }
        // Consecutive iterations need to be handled slightly different
        else {
            AtomicInteger processed = new AtomicInteger(0);
            AtomicInteger reassignmentCounter = new AtomicInteger(0);

            // We need to know if the corresponding aptamer has already been processed in this iteration,
            // use aptamer id as index for a bitset
            BloomFilter<Integer> visited = new FilterBuilder(aptamersBySize.length, 0.001).buildBloomFilter();
            // Creating shared object
            BlockingQueue<Object> sharedQueue = new ArrayBlockingQueue<>(1000); // TODO MAKE THIS A PARAMETER

            int numThreads = Math.min(Runtime.getRuntime().availableProcessors(), maxNumberOfCores);

            // Create producer and consumer threads
            Thread prodThread = new Thread(new LSHProducer(
                sharedQueue,
                aptamersBySize,
                processed,
                buckets,
                randomizedRegionSize,
                visited,
                lshInstance,
                clusters,
                experiment
            ), "LSH Producer");
            ArrayList<Thread> consumers = new ArrayList<>();

            // We need at least one consumer
            consumers.add(new Thread(new LSHConsumer(
                sharedQueue,
                clusters,
                kmerSize,
                kmerCutoff,
                processed,
                visited,
                reassignmentCounter,
                experiment
            ), "LSH Consumer 1"));

            // Add remaining consumers
            for (int x = 1; x < numThreads - 1; x++) {
                consumers.add(new Thread(new LSHConsumer(
                    sharedQueue,
                    clusters,
                    kmerSize,
                    kmerCutoff,
                    processed,
                    visited,
                    reassignmentCounter,
                    experiment
                ), "LSH Consumer " + (x + 1)));
            }

            // Start threads
            for (Thread consumer : consumers) {
                consumer.start();
            }
            prodThread.start();

            // Monitor progress
            while (prodThread.isAlive() && !prodThread.isInterrupted()) {
                try {
                    log.info("Processed {}/{} items", processed.get(), aptamersBySize.length);
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // Wait for threads to finish
            try {
                for (Thread consumer : consumers) {
                    consumer.join();
                }
                prodThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            log.info("Processed {}/{} items", processed.get(), aptamersBySize.length);
            log.info("Clustering completed, reassigned {} items", reassignmentCounter.get());
        }

        log.info("Finished LSH iteration {} in {} seconds", iterationNumber, ((System.currentTimeMillis() - startTimeMs) / 1000.0));
    }

    /**
     * Creates a partitioning of the data using the specified locality-sensitive hash function.
     * The resulting clusters are sorted in descending order by overall aptamer size.
     *
     * @param lshInstance the locality-sensitive hash function used for partitioning
     * @return the partitioned pool
     */
    private Buckets generateBuckets(LocalitySensitiveHash lshInstance) {
        log.info("Generating LSH Buckets");
        long startTimeMs = System.currentTimeMillis();

        // Create buckets and fill them with their corresponding hash
        Buckets buckets = Buckets.withExpectedSize(experiment.getPool().size());

        int counter = 0;
        for (int itemId : this.aptamersBySize) {
            counter++;
            if (counter % 1000 == 0) {
                log.info("Processed {}/{} items", counter, this.aptamersBySize.length);
            }

            byte[] item = experiment.getPool().getAptamer(itemId);
            AptamerBounds itemBounds = experiment.getPool().getAptamerBounds(itemId);

            // Skip all aptamers which do not have the appropriate size
            if ((itemBounds.endIndex - itemBounds.startIndex) != this.randomizedRegionSize) {
                continue;
            }

            int hash = Arrays.hashCode(lshInstance.getHash(item, itemBounds));

            if (!buckets.contains(hash)) {
                buckets.justPut(hash, IntLists.mutable.of(itemId));
            } else {
                buckets.get(hash).add(itemId);
            }
        }
        log.info("Processed {}/{} items", counter, this.aptamersBySize.length);

        log.info("Finished generating {} buckets in {} seconds", buckets.size(), ((System.currentTimeMillis() - startTimeMs) / 1000.0));
        return buckets;
    }

    /**
     * Creates a list of aptamers sorted in descending order by their
     * cumulative size across all selection cycles.
     */
    private void initializeAptamersBySize() {
        log.info("Initializing auxiliary data structures");
        long startTimeMs = System.currentTimeMillis();

        // Create an array of aptamer IDs for all pools
        this.aptamersBySize = new int[experiment.getPool().size()];

        int counter = 0;
        for (Integer item : this.experiment.getPool().id_iterator()) {
            this.aptamersBySize[counter] = item;
            counter++;
        }

        Quicksort.sort(this.aptamersBySize);

        // Create a temporary array containing the cumulative sum of aptamers in all selection cycles
        int[] aptamerSums = new int[experiment.getPool().size()];
        for (SelectionCycle cycle : experiment.getSelectionCycles()) {
            counter = 0;
            // IMPORTANT: This loop relies on `cycle` keys being sorted in ascending order.
            for (Entry<Integer, Integer> cycleIt : cycle.iterator()) {
                // This is guaranteed to terminate since cycle IDs are a subset of the aptamer pool IDs
                while (this.aptamersBySize[counter] != cycleIt.getKey()) {
                    counter++;
                }
                aptamerSums[counter] += cycleIt.getValue();
            }
        }

        // Sort the ID list according to the sizes
        class AptamerSizeQSComparator implements QSComparator {
            @Override
            public int compare(int a, int b) {
                return -Integer.compare(a, b);
            }
        }
        Quicksort.sort(this.aptamersBySize, aptamerSums, new AptamerSizeQSComparator());

        log.info("Finished initialization in {} seconds", ((System.currentTimeMillis() - startTimeMs) / 1000.0));
    }

    /**
     * Estimates an optimal cutoff value for considering two aptamers similar
     * based on the k-mer distance.
     * <p>
     * The cutoff is determined by randomly selecting a set of sequences and
     * introducing {@code user_similarity} point mutations. Each mutated sequence
     * is then compared to its original counterpart, and the k-mer distance is
     * computed. The average of these distances is used as the cutoff value.
     *
     * @param user_similarity the maximum number of point mutations introduced
     *        into each sequence
     * @return the estimated optimal cutoff value
     */
    private double computeKmerCutoff() {
        log.info("Computing kmer cutoff for cluster generation");
        long startTimeMs = System.currentTimeMillis();

        Random rand = new Random();
        double cutoff = 0.0;
        int counter = 0;
        byte[] nucleotides = {'A', 'C', 'G', 'T'};

        for (int x = 0; x < this.kmerCutoffIterations; ++x) {
            // Randomly select a sample sequence from the pool
            byte[] parentSequence = experiment.getPool().getAptamer(this.aptamersBySize[rand.nextInt(this.aptamersBySize.length)]);
            for (int y = 0; y < 100; ++y) {
                // Randomly mutate the sequence
                byte[] mutantSequence = Arrays.copyOf(parentSequence, parentSequence.length);
                for (int z = 0; z < editDistance; ++z) {
                    mutantSequence[rand.nextInt(mutantSequence.length)] = nucleotides[rand.nextInt(4)];
                }
                cutoff += Distances.KmerDistance(parentSequence, mutantSequence, kmerSize);
                counter++;
            }
        }

        // Return the average distance as the cutoff
        kmerCutoff = cutoff / counter;
        log.info("Finished cutoff computation in {} seconds. Using cutoff {}", ((System.currentTimeMillis() - startTimeMs) / 1000.0), kmerCutoff);

        return kmerCutoff;
    }

    /**
     * Renumbers cluster IDs to ensure they form a contiguous sequence starting from 0.
     * <p>
     * During LSH iterations, some clusters may become empty, leaving gaps in the cluster ID sequence.
     * For example, we might have clusters [0, 2, 5, 7] instead of [0, 1, 2, 3].
     * AptaSuite requires cluster IDs to be consecutive integers with no gaps.
     * This function compacts the ID space by shifting each cluster ID down to fill any gaps.
     */
    private void adjustClusterIds() {
        // Maps old cluster ID (array index) to the adjustment needed to make IDs contiguous.
        // cluster_adjustment_map[i] contains:
        //   1 = cluster ID 'i' doesn't exist (sentinel value representing "unassigned")
        //   2 = cluster ID 'i' exists (temporary marker during initialization)
        //   negative value = the amount to shift cluster ID 'i' downward
        int[] clusterAdjustmentMap = new int[clusterId.get() + 1];
        // Initialize all positions as "unassigned" (using 1 as sentinel)
        Arrays.fill(clusterAdjustmentMap, 1);

        // Mark positions that correspond to existing cluster IDs with sentinel value 2
        clusters.iterator().forEach(entry -> clusterAdjustmentMap[entry.getValue()] = 2);

        // Find the first two existing cluster IDs to begin computing shifts
        int idx1 = Index.indexOf(clusterAdjustmentMap, 2);
        int idx2 = Index.indexOf(clusterAdjustmentMap, 2, idx1 + 1);
        int prevIdx = idx1;
        int prevValue = -idx1; // shift needed for first cluster (to move it to position 0)

        // Walk through all existing cluster IDs and compute the shift needed for each.
        // The shift equals the number of gaps (missing IDs) before this position.
        while (idx2 < clusterAdjustmentMap.length && idx2 != -1) {
            // Store the computed shift for the previous cluster ID
            clusterAdjustmentMap[prevIdx] = prevValue;
            // Compute shift for current position: it's the previous shift, plus 1 (for continuing
            // the sequence), minus the gap between this ID and the previous ID
            prevValue = -(idx2 - idx1) + prevValue + 1;
            // Advance to the next existing cluster ID
            prevIdx = idx2;
            idx1 = idx2;
            idx2 = Index.indexOf(clusterAdjustmentMap, 2, idx2 + 1);
        }

        // Store the shift for the last existing cluster ID
        if (prevIdx != -1) {
            clusterAdjustmentMap[prevIdx] = prevValue;
        }

        // Apply the computed shifts to renumber all clusters to contiguous IDs
        clusters.iterator().forEach(entry ->
            clusters.reassignClusterId(entry.getKey(), entry.getValue() + clusterAdjustmentMap[entry.getValue()])
        );
    }
}
