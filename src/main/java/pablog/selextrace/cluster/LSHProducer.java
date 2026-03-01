package pablog.selextrace.cluster;

import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import orestes.bloomfilter.BloomFilter;
import orestes.bloomfilter.FilterBuilder;
import pablog.selextrace.domain.cluster.ClusterContainer;
import pablog.selextrace.domain.experiment.Experiment;
import pablog.selextrace.domain.pool.AptamerBounds;
import pablog.selextrace.parsing.pipeline.PoisonPill;

import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Producer for subsequent LSH iterations.
 *
 * @author Jan Hoinka
 */
public class LSHProducer implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(LSHProducer.class);

    /** The queue to fill */
    private final BlockingQueue<Object> queue;
    /** The data to be put into the queue */
    private final Buckets buckets;
    private final AtomicInteger processed;
    private final int[] aptamersBySize;
    private final Experiment experiment;
    private final int randomizedRegionSize;
    private final BloomFilter<Integer> visited;
    private final LocalitySensitiveHash lsh;
    private final ClusterContainer clusters;

    public LSHProducer(
        BlockingQueue<Object> queue,
        int[] aptamersBySize,
        AtomicInteger processed,
        Buckets buckets,
        int randomizedRegionSize,
        BloomFilter<Integer> visited,
        LocalitySensitiveHash lsh,
        ClusterContainer clusters,
        Experiment experiment
    ) {
        this.queue = queue;
        this.aptamersBySize = aptamersBySize;
        this.processed = processed;
        this.buckets = buckets;
        this.randomizedRegionSize = randomizedRegionSize;
        this.visited = visited;
        this.lsh = lsh;
        this.clusters = clusters;
        this.experiment = experiment;
    }

    @Override
    public void run() {
        // Iterate over the data and put it in the queue
        BloomFilter<Integer> lockedBuckets = new FilterBuilder(buckets.size(), 0.001).buildBloomFilter();

        for (int aptamerId : this.aptamersBySize) {
            byte[] aptamerSequence = experiment.getPool().getAptamer(aptamerId);
            AptamerBounds aptamerBounds = experiment.getPool().getAptamerBounds(aptamerId);

            // Skip all aptamers which do not have the appropriate size
            if ((aptamerBounds.endIndex - aptamerBounds.startIndex) != this.randomizedRegionSize) {
                continue;
            }

            int currentHash = Arrays.hashCode(lsh.getHash(aptamerSequence, aptamerBounds));

            // Check if bucket is already assigned
            if (!lockedBuckets.contains(currentHash) && !visited.contains(aptamerId)) {
                MutableIntList bucket = buckets.get(currentHash);
                if (bucket == null) {
                    continue;
                }

                // Singleton clusters will not require any processing downstream, so we can ignore them in this LSH iteration
                if (bucket.size() == 1) {
                    visited.add(aptamerId);
                    lockedBuckets.add(currentHash);
                    processed.incrementAndGet();
                    continue;
                }

                // Add aptamer for processing
                lockedBuckets.add(currentHash);
                visited.add(aptamerId);

                try {
                    LSHQueueItem item = new LSHQueueItem();
                    item.aptamer_id = aptamerId;
                    item.aptamer_sequence = aptamerSequence;
                    item.it = bucket.intIterator();
                    item.cluster_id = clusters.getClusterId(aptamerId);

                    queue.put(item);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }

        // Add poison pill to signal consumers that processing is done
        log.debug("Added poison pill to LSH queue");
        try {
            queue.put(PoisonPill.INSTANCE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
