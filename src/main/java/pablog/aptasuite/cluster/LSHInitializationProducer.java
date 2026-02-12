package pablog.aptasuite.cluster;

import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pablog.aptasuite.parsing.pipeline.PoisonPill;

import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;

/**
 * Producer for the initial LSH clustering iteration.
 *
 * @author Jan Hoinka
 */
public class LSHInitializationProducer implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(LSHInitializationProducer.class);

    /** The queue to fill */
    private final BlockingQueue<Object> queue;
    /** The data to be put into the queue */
    private final Buckets items;

    /**
     * The constructor expects an Iterable over either an aptamer pool or a selection
     * cycle. The key contains the randomized region of the aptamer and the value its
     * corresponding unique id.
     */
    public LSHInitializationProducer(Buckets items, BlockingQueue<Object> queue) {
        this.queue = queue;
        this.items = items;
    }

    @Override
    public void run() {
        // Iterate over the data and put it in the queue
        for (Entry<Integer, MutableIntList> item : items.entrySet()) {
            try {
                queue.put(item);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }

        // Add poison pill to signal consumers that processing is done
        log.debug("Added poison pill to LSHInitialization queue");
        try {
            queue.put(PoisonPill.INSTANCE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
