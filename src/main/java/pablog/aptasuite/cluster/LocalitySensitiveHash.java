package pablog.aptasuite.cluster;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pablog.aptasuite.domain.pool.AptamerBounds;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.Random;

/**
 * Implements the locality sensitive hashing family for AptaCluster.
 *
 * @author Jan Hoinka
 */
public class LocalitySensitiveHash {

    private static final Logger log = LoggerFactory.getLogger(LocalitySensitiveHash.class);

    /**
     * The randomized region size for which aptaCLUSTER should be applied to.
     * All aptamers with different randomized region sizes will be ignored for this instance.
     */
    private Integer randomizedRegionSize = null;
    /** The first index that was sampled for this instance */
    private Integer startIndex = null;
    /** The coprime selected to sample the remaining */
    private Integer coprime = null;
        /** The locality sensitive hashing dimension into which the data will be reduced to. */
    private Integer lshDimension = null;
    /** Random seed for this instance */
    private final Random rand = new Random();
    /**
     * List of instances of {@code LocalitySensitiveHash}. Used to make sure no two
     * equal hash functions are drawn.
     */
    private ArrayList<LocalitySensitiveHash> lhsInstances = null;
    private ArrayList<Integer> lshPositions = null;

    /**
     * Draws an LSH function from the LSH family.
     *
     * @param randomizedRegionSize the size of the nucleotide string for which the LSH is designed
     * @param lshDimension the dimension {@code x} of the reduced representation of the objects;
     *        must satisfy {@code x <= randomizedRegionSize}. The smaller {@code x} is,
     *        the less similar the objects can be while still being hashed into the same bucket
     */
    public LocalitySensitiveHash(int randomizedRegionSize, int lshDimension) {
        this.randomizedRegionSize = randomizedRegionSize;
        this.lshDimension = lshDimension;

        createHashPositions();
        logLSH();
    }

    /**
     * Draws a unique LSH function from the LSH family.
     *
     * @param randomizedRegionSize the size of the nucleotide string for which the LSH is designed
     * @param lshDimension the dimension {@code x} of the reduced representation of the objects;
     *        must satisfy {@code x <= randomizedRegionSize}. The smaller {@code x} is,
     *        the less similar the objects can be while still being hashed into the same bucket
     * @param lshInstances the list of already drawn LSH functions; the newly generated
     *        LSH function is guaranteed to be distinct from any instance in {@code lshInstances}
     */
    public LocalitySensitiveHash(int randomizedRegionSize, int lshDimension, ArrayList<LocalitySensitiveHash> lshInstances) {
        this.randomizedRegionSize = randomizedRegionSize;
        this.lshDimension = lshDimension;
        setLhsInstances(lshInstances);

        createHashPositions();
        logLSH();
    }

    /**
     * Generates all positive integers smaller than {@code x} that are coprime to {@code x}
     * and returns a randomly selected element from that set.
     * <p>
     * This method is used to initialize the hash functions in LSH
     * to help minimize overlaps between indices.
     *
     * @return a random integer smaller than {@code x} that is coprime to {@code x}
     */
    private int getRandomCoprimeNumber() {
        // Trivial case
        if (randomizedRegionSize == 1) {
            return 1;
        }

        // a = 2: Avoid the trivial case of 1, which is coprime with every number and would produce consecutive hash indices (not desirable).
        // a < x - 1: Consecutive numbers are always coprime, leading to the same issue as above (consecutive hash indices).
        ArrayList<Integer> allCoprimes = new ArrayList<>();
        for (int a = 2; a < (randomizedRegionSize - 1); ++a) {
            // a and x are coprime when their greatest common divisor (gcd) is 1
            if (BigInteger.valueOf(a).gcd(BigInteger.valueOf(randomizedRegionSize)).intValue() == 1) {
                allCoprimes.add(a);
            }
        }

        return allCoprimes.get(rand.nextInt(allCoprimes.size()));
    }

    /**
     * Ensures that if multiple LSH instances exist, no two share the same parameters.
     *
     * @param startIndex a unique start index between 0 and {@code randomizedRegionSize}
     * @param coprime a number coprime to {@code randomizedRegionSize} that is unique
     *        in combination with {@code startIndex}
     */
    private void setUniqueStartIndexAndCoprime() {
        // Generate random start index and coprime number
        startIndex = rand.nextInt(randomizedRegionSize);
        coprime = getRandomCoprimeNumber();

        // Make sure this combination has never been sampled before
        if (lhsInstances != null) {
            boolean found = true;
            while (found) {
                found = false;
                for (LocalitySensitiveHash lsh : lhsInstances) {
                    if (this.equals(lsh)) {
                        found = true;
                        break;
                    }
                }
                if (found) {
                    startIndex = rand.nextInt(randomizedRegionSize);
                    coprime = getRandomCoprimeNumber();
                }
            }
        }
    }

    /**
     * Generates a set of indices that define the reduced dimension of this LSH instance.
     */
    private void createHashPositions() {
        // Generate a unique start index and a coprime value
        setUniqueStartIndexAndCoprime();

        // Initialize the list of LSH positions with the start index
        lshPositions = new ArrayList<>();
        lshPositions.add(startIndex);

        // Compute the remaining indices using:
        // i_x = (i_(x-1) + k*n) mod m
        // where 'k' is any random number, 'n' is a fixed relative prime number (coprime) to 'm',
        // and 'm' is the seq_length.
        for (int v = 1; v < lshDimension - 1; ++v) {
            lshPositions.add((lshPositions.get(v - 1) + coprime) % randomizedRegionSize);
        }

        // Sort the indices to maintain a consistent order
        Collections.sort(lshPositions);
    }

    /**
     * Computes the string resulting from applying the LSH to the given sequence.
     *
     * @param sequence the input sequence; must have length {@code randomizedRegionSize}
     * @param bounds the bounds of the randomized region; primers will be ignored for the hash
     * @return a byte array representing the nucleotides from {@code sequence} at the indices
     *         specified in {@code lshPositions}
     */
    public byte[] getHash(byte[] sequence, AptamerBounds bounds) {
        return getHash(sequence, bounds.startIndex, bounds.endIndex);
    }

    /**
     * Computes the string resulting from applying the LSH to the given sequence.
     *
     * @param sequence the input sequence; must have length {@code randomizedRegionSize}
     * @param bounds an array containing the start and end indices of the randomized region;
     *        primers will be ignored for the hash
     * @return a byte array representing the nucleotides from {@code sequence} at the indices
     *         specified in {@code lshPositions}
     */
    public byte[] getHash(byte[] sequence, int[] bounds) {
        return getHash(sequence, bounds[0], bounds[1]);
    }

    /**
     * Computes the string resulting from applying the LSH to the given sequence
     *
     * @param sequence the input sequence; must have length {@code randomizedRegionSize}
     * @param lower the lower bound of the randomized region; primers will be ignored for the hash
     * @param upper the upper bound of the randomized region; primers will be ignored for the hash
     * @return a byte array containing the nucleotides from {@code sequence} at the indices
     *         specified in {@code lshPositions}
     */
    public byte[] getHash(byte[] sequence, int lower, int upper) {
        // Initiate array
        byte[] result = new byte[lshDimension];
        // Fill array
        int counter = 0;
        for (int index : lshPositions) {
            result[counter] = sequence[index + lower];
            counter++;
        }
        return result;
    }

    public ArrayList<LocalitySensitiveHash> getLhsInstances() {
        return lhsInstances;
    }

    public void setLhsInstances(ArrayList<LocalitySensitiveHash> lhsInstances) {
        this.lhsInstances = lhsInstances;
    }

    public Integer getStartIndex() {
        return startIndex;
    }

    public void setStartIndex(Integer startIndex) {
        this.startIndex = startIndex;
    }

    public Integer getCoprime() {
        return coprime;
    }

    public void setCoprime(Integer coprime) {
        this.coprime = coprime;
    }

    public Integer getRandomizedRegionSize() {
        return randomizedRegionSize;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null) {
            return false;
        }

        if (getClass() != o.getClass()) {
            return false;
        }

        LocalitySensitiveHash lsh = (LocalitySensitiveHash) o;
        return Objects.equals(startIndex, lsh.startIndex)
            && Objects.equals(coprime, lsh.coprime)
            && Objects.equals(lshDimension, lsh.lshDimension);
    }

    /**
     * Logs the parameters of this LSH instance and the indices that are hashed.
     */
    private void logLSH() {
        byte[] indices = new byte[this.randomizedRegionSize];
        byte[] sequence = new byte[this.randomizedRegionSize];

        for (int i = 0; i < this.randomizedRegionSize; i++) {
            sequence[i] = 'N';
            indices[i] = ' ';
        }
        for (int i : this.lshPositions) {
            indices[i] = '|';
        }
        indices[startIndex] = '*';

        String text = String.format(
            "Hash contains the following indices: initial position (*) at %s random coprime c (1< c < %s): %s",
            this.startIndex,
            this.randomizedRegionSize,
            this.coprime
        );

        StringBuilder sb = new StringBuilder();
        sb.append("            ").append(new String(indices)).append("            ").append('\n');
        sb.append("PRIMER5-----").append(new String(sequence)).append("-----PRIMER3").append('\n');
        sb.append(text);

        log.debug(sb.toString());
    }
}
