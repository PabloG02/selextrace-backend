package pablog.selextrace.cluster;

import org.eclipse.collections.api.iterator.MutableIntIterator;

/**
 * Wrapper class to pass required elements to consumers for parallel processing.
 *
 * @author Jan Hoinka
 */
public class LSHQueueItem {

    /**
     * The aptamer id of the seed sequence
     */
    public int aptamer_id;

    /**
     * the sequence of the seed id
     */
    public byte[] aptamer_sequence;

    /**
     * Iterator over the bucket the seed is contained in
     */
    public MutableIntIterator it;

    /**
     * The cluster id assigned to this seed
     */
    public int cluster_id;
}
