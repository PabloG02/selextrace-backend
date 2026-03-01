package pablog.selextrace.cluster;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Implements a variety of distance metrics used for AptaCluster.
 *
 * @author Jan Hoinka
 */
public class Distances {

    /**
     * Computes the k-mer distance between two sequences.
     * <p>
     * Assumes that both sequences have the same length. The k-mer distance is calculated
     * by counting the frequency of each k-mer in both sequences and computing the sum of
     * squared differences in normalized frequencies.
     * <p>
     * Note: If k is larger than the length of either sequence, a maximal distance
     * of 2.0 is returned.
     *
     * @param a the first sequence as a byte array.
     * @param b the second sequence as a byte array. Must be of the same length as {@code a}.
     * @param kmerSize the length of k-mers to consider.
     * @return the k-mer distance between {@code a} and {@code b}, a non-negative double
     */
    public static double KmerDistance(byte[] a, byte[] b, int kmerSize) {
        // Map to store k-mer counts for both sequences.
        Map<String, int[]> kmerMap = new HashMap<>();

        // Count k-mers in both sequences
        for (int x = 0; x < a.length - kmerSize; x++) {
            String kmerA = new String(a, x, kmerSize);
            String kmerB = new String(b, x, kmerSize);

            kmerMap.computeIfAbsent(kmerA, key -> new int[]{0, 0})[0] += 1;
            kmerMap.computeIfAbsent(kmerB, key -> new int[]{0, 0})[1] += 1;
        }

        // If there are no or only one k-mer, return maximal distance
        if (kmerMap.size() <= 1) {
            return 2.0;
        }

        // Compute squared differences of normalized k-mer frequencies
        double distance = 0.0;
        for (Entry<String, int[]> kmer : kmerMap.entrySet()) {
            double countX = kmer.getValue()[0] / (double) (a.length - kmerSize + 1);
            double countY = kmer.getValue()[1] / (double) (b.length - kmerSize + 1);
            distance += Math.pow(Math.abs(countX - countY), 2);
        }

        return distance;
    }
}
