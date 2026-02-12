package pablog.aptasuite.domain.cluster;

import java.io.Serializable;
import java.util.Map.Entry;

/**
 * Interface for managing aptamer cluster memberships based on similarity measures.
 *
 * <p>This interface defines the contract for data structures that track which cluster
 * each aptamer belongs to. Aptamers are grouped into clusters based on some similarity
 * measure (e.g., sequence similarity, structural similarity).</p>
 *
 * <p><strong>Cluster ID Requirements:</strong></p>
 * <ul>
 *   <li>Cluster IDs must be assigned consecutively starting at 0</li>
 *   <li>Valid cluster IDs range from 0 to (numberOfClusters - 1)</li>
 * </ul>
 *
 * <p><strong>Core Functionality:</strong></p>
 * <ul>
 *   <li>Add aptamers to clusters</li>
 *   <li>Query cluster membership</li>
 *   <li>Iterate over aptamer-cluster assignments</li>
 * </ul>
 *
 * @author Jan Hoinka
 */
public interface ClusterContainer extends Serializable {

    /**
     * Adds an aptamer to the specified cluster.
     * <p>
     * If the aptamer was previously assigned to a different cluster,
     * the assignment will be updated to the new cluster.
     *
     * @param a the aptamer sequence as a String
     * @param cluster_id the target cluster ID (must be ≥ 0)
     * @return the cluster ID that the aptamer was assigned to
     */
    int addToCluster(String a, int cluster_id);

    /**
     * Adds an aptamer to the specified cluster.
     * <p>
     * If the aptamer was previously assigned to a different cluster,
     * the assignment will be updated to the new cluster.
     *
     * @param a the aptamer sequence as a byte array
     * @param cluster_id the target cluster ID (must be ≥ 0)
     * @return the cluster ID that the aptamer was assigned to
     */
    int addToCluster(byte[] a, int cluster_id);

    /**
     * Adds an aptamer to the specified cluster.
     * <p>
     * If the aptamer was previously assigned to a different cluster,
     * the assignment will be updated to the new cluster.
     *
     * @param a the aptamer identifier as an integer
     * @param cluster_id the target cluster ID (must be ≥ 0)
     * @return the cluster ID that the aptamer was assigned to
     */
    int addToCluster(int a, int cluster_id);

    /**
     * Reassigns an existing aptamer to a different cluster.
     * <p>
     * This method only works if the aptamer is already present in the container.
     * If the aptamer has not been previously added, no action is taken.
     *
     * @param a the aptamer identifier
     * @param cluster_id the new cluster ID to assign
     * @return the new cluster ID if successful, or -1 if the aptamer was not found
     */
    int reassignClusterId(int a, int cluster_id);

    /**
     * Checks whether an aptamer has been assigned to any cluster.
     *
     * @param a the aptamer sequence as a String
     * @return {@code true} if the aptamer is present in the container, {@code false} otherwise
     */
    boolean containsAptamer(String a);

    /**
     * Checks whether an aptamer has been assigned to any cluster.
     *
     * @param a the aptamer sequence as a byte array
     * @return {@code true} if the aptamer is present in the container, {@code false} otherwise
     * @see #containsAptamer(String)
     */
    boolean containsAptamer(byte[] a);

    /**
     * Checks whether an aptamer has been assigned to any cluster.
     *
     * @param a the aptamer identifier
     * @return {@code true} if the aptamer is present in the container, {@code false} otherwise
     * @see #containsAptamer(String)
     */
    boolean containsAptamer(int a);

    /**
     * Retrieves the cluster ID for the specified aptamer.
     *
     * @param a the aptamer identifier
     * @return the cluster ID (≥ 0) if the aptamer exists, or -1 if not found
     */
    int getClusterId(int a);

    /**
     * Returns the total number of aptamers that have been assigned to clusters.
     *
     * @return the count of aptamers in the container
     */
    int getSize();

    /**
     * Returns the total number of distinct clusters in this container.
     *
     * @return the count of clusters
     */
    int getNumberOfClusters();

    /**
     * Performs any additional logic on the data structure such as
     * optimizing once it is known no more items will be added
     * (i.e. upon completing the parsing).
     *
     * @implSpec Implementations are not required to provide this functionality.
     */
    void setReadOnly();

    /**
     * Sets the implementing class to read/write mode in case
     * persistent storage is used.
     *
     * @implSpec Implementations are not required to provide this functionality.
     */
    void setReadWrite();

    /**
     * Closes the underlying data structure, freeing any resources attached
     * to it.
     *
     * @implSpec Implementations are not required to provide this functionality.
     */
    void close();

    /**
     * Returns an iterator over all aptamer-cluster assignments.
     * <p>
     * Each entry contains an aptamer ID mapped to its cluster ID.
     * The iteration order is implementation-dependent.
     *
     * @return an {@code Iterable} of entries mapping aptamer IDs to cluster IDs
     */
    Iterable<Entry<Integer, Integer>> iterator();

    /**
     * Returns an iterator over all aptamer sequences and their cluster assignments.
     * <p>
     * Each entry contains an aptamer sequence (as a byte array) mapped to its cluster ID.
     * The iteration order is implementation-dependent.
     *
     * @return an {@code Iterable} of entries mapping aptamer sequences to cluster IDs
     */
    Iterable<Entry<byte[], Integer>> sequence_iterator();
}
