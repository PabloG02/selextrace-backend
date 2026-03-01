package pablog.selextrace.cluster;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import orestes.bloomfilter.BloomFilter;
import pablog.selextrace.domain.cluster.ClusterContainer;
import pablog.selextrace.domain.experiment.Experiment;
import pablog.selextrace.parsing.pipeline.PoisonPill;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Consumer implementation of LSH Clustering. The consumer takes items
 * from the queue, processes them and adds them to the structure database.
 *
 * @author Jan Hoinka
 */
public class LSHConsumer implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(LSHConsumer.class);

    /** The instance of ClusterContainer to store the data in */
    private final ClusterContainer clusters;
    /** The experiment instance */
    private final Experiment experiment;
    /** The queue to take items from */
    private final BlockingQueue<Object> queue;
    /** Shares the number of processed items with the main thread */
    private final AtomicInteger processed;
    private final AtomicInteger reassignmentCounter;
    private final BloomFilter<Integer> visited;
    /** K-mer size used for the distance computation */
    private final Integer kmerSize;
    /**
     * The cutoff value in the k-mer distance space for which aptamers
     * which have a larger distance compared to the seed will be
     * excluded from a particular cluster
     */
    private final Double kmerCutoff;

    public LSHConsumer(
        BlockingQueue<Object> queue,
        ClusterContainer clusters,
        Integer kmerSize,
        Double kmerCutoff,
        AtomicInteger processed,
        BloomFilter<Integer> visited,
        AtomicInteger reassignmentCounter,
        Experiment experiment
    ) {
        this.queue = queue;
        this.clusters = clusters;
        this.kmerSize = kmerSize;
        this.kmerCutoff = kmerCutoff;
        this.processed = processed;
        this.visited = visited;
        this.reassignmentCounter = reassignmentCounter;
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

                LSHQueueItem item = (LSHQueueItem) queueElement;

                // Mark the current item as processed
                processed.incrementAndGet();

                // Iterate over the bucket and reassign cluster members if appropriate
                while (item.it.hasNext()) {
                    int itemId = item.it.next();
                    // Only recompute distances between items that are not already in the same cluster
                    if (!visited.contains(itemId) && item.cluster_id != clusters.getClusterId(itemId)) {
                        double distance = Distances.KmerDistance(
                            experiment.getPool().getAptamer(itemId),
                            item.aptamer_sequence,
                            kmerSize
                        );

                        if (distance <= kmerCutoff) {
                            clusters.addToCluster(itemId, item.cluster_id);
                            reassignmentCounter.incrementAndGet();
                            visited.add(itemId);
                            processed.incrementAndGet();
                        }
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.error("Error during LSH consumer", e);
            }
        }
    }
}
