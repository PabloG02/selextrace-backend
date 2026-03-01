package pablog.selextrace.cluster;

import org.eclipse.collections.api.iterator.MutableIntIterator;
import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pablog.selextrace.domain.cluster.ClusterContainer;
import pablog.selextrace.domain.experiment.Experiment;
import pablog.selextrace.parsing.pipeline.PoisonPill;

import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Consumer implementation of LSH Clustering. The consumer takes items
 * from the queue, processes them and adds them to the structure database.
 *
 * @author Jan Hoinka
 */
public class LSHInitializationConsumer implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(LSHInitializationConsumer.class);

    /** The instance of ClusterContainer to store the data in */
    private final ClusterContainer clusters;
    /** The experiment instance */
    private final Experiment experiment;
    /** The queue to take items from */
    private final BlockingQueue<Object> queue;
    /** Unique cluster id. Writable to the consumers and thread-safe */
    private final AtomicInteger clusterId;
    /** Shares the number of processed buckets with the main thread */
    private final AtomicInteger progress;
    /** K-mer size used for the distance computation */
    private final Integer kmerSize;
    /**
     * The cutoff value in the k-mer distance space for which aptamers
     * which have a larger distance compared to the seed will be
     * excluded from a particular cluster
     */
    private final Double kmerCutoff;

    public LSHInitializationConsumer(
        BlockingQueue<Object> queue,
        AtomicInteger clusterId,
        ClusterContainer clusters,
        Integer kmerSize,
        Double kmerCutoff,
        AtomicInteger progress,
        Experiment experiment
    ) {
        this.queue = queue;
        this.clusterId = clusterId;
        this.clusters = clusters;
        this.kmerSize = kmerSize;
        this.kmerCutoff = kmerCutoff;
        this.progress = progress;
        this.experiment = experiment;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Object queueElement = queue.take();

                if (queueElement == PoisonPill.INSTANCE) {
                    log.debug("Encountered poison pill. Exiting thread.");
                    queue.put(PoisonPill.INSTANCE);
                    return;
                }

                @SuppressWarnings("unchecked")
                Entry<Integer, MutableIntList> bucket = (Entry<Integer, MutableIntList>) queueElement;

                // Iterate over the list of aptamers and form clusters
                int clusterAssignmentCounter = 0;
                while (clusterAssignmentCounter < bucket.getValue().size()) {
                    Integer currentSeed = null;
                    byte[] currentSeedSequence = null;
                    int currentClusterId = 0;

                    MutableIntIterator it = bucket.getValue().intIterator();
                    while (it.hasNext()) {
                        int aptamerId = it.next();

                        // Find the seed
                        if (currentSeed == null && !clusters.containsAptamer(aptamerId)) {
                            currentClusterId = this.clusterId.incrementAndGet();
                            currentSeed = aptamerId;
                            currentSeedSequence = experiment.getPool().getAptamer(currentSeed);
                            clusters.addToCluster(aptamerId, currentClusterId);
                            clusterAssignmentCounter++;
                            continue;
                        }
                        if (!clusters.containsAptamer(aptamerId)) {
                            double distance = Distances.KmerDistance(
                                experiment.getPool().getAptamer(aptamerId),
                                currentSeedSequence,
                                kmerSize
                            );

                            if (distance <= kmerCutoff) {
                                clusters.addToCluster(aptamerId, currentClusterId);
                                clusterAssignmentCounter++;
                            }
                        }
                    }
                }

                progress.incrementAndGet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.error("Error during LSH initialization consumer", e);
            }
        }
    }
}
