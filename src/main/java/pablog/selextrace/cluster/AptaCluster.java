package pablog.selextrace.cluster;

/**
 * Implements the logic of AptaCluster as described in Res Comput Mol Biol. 2014;8394:115-128.
 *
 * @author Jan Hoinka
 */
public interface AptaCluster {

    /**
     * Performs locality sensitive hashing on the data and assigns each aptamer to one cluster
     */
    void performLSH();
}
