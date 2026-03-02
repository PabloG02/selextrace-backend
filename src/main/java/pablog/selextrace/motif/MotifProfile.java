package pablog.selextrace.motif;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/// Represents the statistical, structural, and alignment profile of an enriched sequence motif
/// discovered during a **SELEX** experiment.
///
/// Once significant `k`-mers are identified (via their structural context trace), overlapping
/// `k`-mers are clustered together into a unified motif. This class encapsulates that unified
/// entity. It utilizes a **Position Weight Matrix (PWM)** to define the motif's sequence preferences,
/// capturing the probability of observing each nucleotide (`A`, `C`, `G`, `T/U`) at each position
/// along the motif's length.
///
/// Furthermore, the class tracks how often this clustered motif falls into specific
/// secondary structures (e.g., **Hairpin**, **Bulge**, **Inner loop**, **Multi-loop**, **Dangling end**, **Paired**)
/// across selection rounds. It also provides built-in mechanisms to trim uninformative, low-entropy
/// flanking positions from the PWM using Shannon entropy thresholds, ensuring the final motif representation
/// is tightly focused on the core binding site.
public class MotifProfile {
    /// Length of the motif.
    private final int len;
    /// **Position Weight Matrix (PWM)**.
    /// The first dimension is the position in the motif.
    /// The second dimension holds nucleotide probabilities in the order: `A`, `C`, `G`, `T`.
    ///
    /// **Note:** Values are un-normalized sums until {@link #normalizeProfile()} is called.
    private final double[][] pwm;
    /// PWM accumulated exclusively from singleton aptamers (frequency ≤ threshold).
    ///
    /// **Note:** Values are un-normalized sums until {@link #normalizeProfile()} is called.
    private final double[][] singletonPwm;
    /// Consensus sequence derived from the PWM by majority-vote at each position.
    private String consensus;
    /// Inclusive start position of the trimmed PWM window (0-based).
    private int trims;
    /// Inclusive end position of the trimmed PWM window (0-based).
    private int trime;
    /// The seed k-mer used to initialize and align the motif.
    private String seed;
    /// Statistical significance of the motif (p-value).
    private double seedpValue;

    /// Total number of aptamers that contain at least one k-mer of this motif
    /// in the last sequencing round (weighted by copy count).
    private int totalOccs;
    /// Set of aptamer IDs that contain at least one k-mer of this motif in the
    /// last sequencing round. Used to avoid double-counting the same aptamer.
    private final IntOpenHashSet occSet = new IntOpenHashSet();
    /// All k-mers that compose this motif.
    private final ArrayList<String> kmerSet = new ArrayList<>();
    /// Maps each k-mer to its alignment string relative to the seed k-mer.
    private final HashMap<String, String> kmerAlignment = new HashMap<>();
    /// Fraction of all aptamers in the last sequencing round that contain this motif.
    private double proportion;
    /// Per-round sum of copy counts for aptamers whose frequency is ≤ singleton threshold.
    private final int[] singletonCount;
    /// Per-round sum of copy counts for aptamers whose frequency is > singleton threshold.
    private final int[] nonSingletonCount;
    /// Per-round sum of copy counts for **all** aptamers containing any k-mer of this motif.
    private final int[] totalCount;
    /// Number of structural contexts tracked, excluding the paired (stem) context.
    /// The paired probability is inferred as `1 - sum(other contexts)`.
    private final int numOfContexts = 5;

    /// Per-round, per-context structural probability matrix for **non-singleton** aptamers.
    /// Dimensions: `[numRounds][numOfContexts]`.
    ///
    /// Values are un-normalized sums until {@link #normalizeProfile()} is called.
    private final double[][] structProbArr;
    /// Per-round, per-context structural probability matrix for **singleton** aptamers.
    /// Dimensions: `[numRounds][numOfContexts]`.
    ///
    /// Values are un-normalized sums until {@link #normalizeProfile()} is called.
    private final double[][] singletonStructProbArr;

    public MotifProfile(int len, int numRounds) {
        this.len = len;
        this.pwm = new double[len][4];
        this.singletonPwm = new double[len][4];
        this.trims = 0;
        this.trime = len - 1;

        this.structProbArr = new double[numRounds][numOfContexts];
        this.singletonStructProbArr = new double[numRounds][numOfContexts];
        this.totalCount = new int[numRounds];
        this.singletonCount = new int[numRounds];
        this.nonSingletonCount = new int[numRounds];
    }

    public void addKmer(String kmer) {
        kmerSet.add(kmer);
    }

    public List<String> getKmers() {
        return kmerSet;
    }

    public void addKmerAlignment(String kmer, String alignment) {
        kmerAlignment.put(kmer, alignment);
    }

    public String getKmerAlignment(String kmer) {
        return kmerAlignment.get(kmer);
    }

    public String getSeed() {
        return seed;
    }

    public void setSeed(String s) {
        seed = s;
    }

    public double getSeedpValue() {
        return this.seedpValue;
    }

    public void setSeedpValue(double pValue) {
        this.seedpValue = pValue;
    }

    /// Registers an aptamer as containing this motif and accumulates its count into {@link #totalOccs}.
    /// Each aptamer is counted at most once — if `id` is already in {@link #occSet}, this is a no-op.
    ///
    /// @param id The internal integer identifier of the aptamer.
    /// @param num The copy count of the aptamer in the last round.
    public void addOccId(int id, int num) {
        if (!occSet.contains(id)) {
            occSet.add(id);
            totalOccs += num;
        }
    }

    public IntOpenHashSet getOccSet() {
        return occSet;
    }

    /// Adds all aptamer IDs from {@link #occSet} not already present in `another` to `another`,
    /// and returns the fraction of {@link #totalOccs} that was newly contributed.
    /// Used during motif filtering to greedily build a union of aptamer sets.
    ///
    /// @param another The accumulating union set to extend.
    /// @param id2Count Map from aptamer ID to its last-round count.
    /// @return Fraction of {@link #totalOccs} contributed by IDs not previously in `another`.
    public double addTo(IntOpenHashSet another, Int2IntOpenHashMap id2Count) {
        int totalno = 0;
        for (int k : occSet) {
            if (!another.contains(k)) {
                totalno += id2Count.get(k);
                another.add(k);
            }
        }
        return totalno / (totalOccs * 1.0);
    }

    public double getOverlapPercentage(IntOpenHashSet another, Int2IntOpenHashMap id2Count) {
        int totalO = 0;
        for (int k : occSet) {
            if (another.contains(k)) {
                totalO += id2Count.get(k);
            }
        }
        return totalO / (totalOccs * 1.0);
    }

    /// Accumulates weighted nucleotide observations from an aligned **non-singleton** aptamer
    /// sequence into the PWM.
    ///
    /// @param s Gapped alignment of the aptamer sequence relative to the motif (length must equal {@link #len}).
    /// @param count Frequency of the aptamer in the last selection round.
    public void addToPWM(String s, int count) {
        for (int i = 0; i < s.length(); i++) {
            pwm[i][toIndex(s.charAt(i))] += count;
        }
    }

    /// Accumulates weighted nucleotide observations from an aligned **singleton** aptamer
    /// sequence into the singleton PWM.
    ///
    /// @param s Gapped alignment of the aptamer sequence (length must equal {@link #len}).
    /// @param count Frequency of the aptamer in the last selection round.
    public void addToSingletonPWM(String s, int count) {
        for (int i = 0; i < s.length(); i++) {
            singletonPwm[i][toIndex(s.charAt(i))] += count;
        }
    }

    /// Accumulates the weighted structural context probability of this motif for a
    /// **non-singleton** aptamer occurrence in selection round `r`.
    ///
    /// @param r Zero-based selection round index.
    /// @param count Frequency of the aptamer in round `r`.
    /// @param avgContextProbArr Average per-context probability of the motif within this aptamer.
    public void addContextProb(int r, int count, double[] avgContextProbArr) {
        for (int c = 0; c < 5; c++) {
            structProbArr[r][c] += avgContextProbArr[c] * count;
        }
        nonSingletonCount[r] += count;
    }

    /// Accumulates the weighted structural context probability of this motif for a
    /// **singleton** aptamer occurrence in selection round `r`.
    ///
    /// @param r Zero-based selection round index.
    /// @param count Frequency of the aptamer in round `r`.
    /// @param avgContextProbArr Average per-context probability of the motif within this aptamer.
    public void addSingletonContextProb(int r, int count, double[] avgContextProbArr) {
        for (int c = 0; c < 5; c++) {
            singletonStructProbArr[r][c] += avgContextProbArr[c] * count;
        }
        singletonCount[r] += count;
    }

    public void addTotalCount(int r, int count) {
        totalCount[r] += count;
    }

    /// Computes and stores the fractional abundance of this motif in the last selection round.
    ///
    /// @param rc Per-round read counts; `rc[rc.length - 1]` is the total number of reads in the last round.
    public void calculateProportion(int[] rc) {
        int denominator = rc[rc.length - 1];
        proportion = totalCount[totalCount.length - 1] / (denominator * 1.0);
    }

    public double getProportion() {
        return proportion;
    }

    /// Normalizes the PWM and context-trace arrays after all per-aptamer accumulation is complete.
    ///
    /// For rounds where non-singleton counts fall below `100`, singleton data is merged into the
    /// non-singleton accumulators if their combined count is sufficient, to salvage a usable estimate.
    /// The same merge is applied to the PWM for the last round. After any such merges, each
    /// context-trace entry is divided by its round-level count, and each PWM row is normalized
    /// to a proper probability distribution summing to `1.0`.
    public void normalizeProfile() {
        for (int i = 0; i < nonSingletonCount.length; i++) {
            if (nonSingletonCount[i] < 100) {
                if ((singletonCount[i] + nonSingletonCount[i]) >= 100) {
                    nonSingletonCount[i] = singletonCount[i] + nonSingletonCount[i];
                    for (int c = 0; c < structProbArr[i].length; c++) {
                        structProbArr[i][c] += singletonStructProbArr[i][c];
                    }

                    if (i == (nonSingletonCount.length - 1)) {
                        for (int j = 0; j < pwm.length; j++) {
                            for (int k = 0; k < 4; k++) {
                                pwm[j][k] += singletonPwm[j][k];
                            }
                        }
                    }
                }
            }
        }

        // calculates the context trace for non-singleton counts
        for (int i = 0; i < nonSingletonCount.length; i++) {
            for (int c = 0; c < structProbArr[i].length; c++) {
                structProbArr[i][c] /= (nonSingletonCount[i] * 1.0);
                singletonStructProbArr[i][c] /= (singletonCount[i] * 1.0);
            }
        }

        for (int i = 0; i < pwm.length; i++) {
            double totalSum = 0.0;
            for (int j = 0; j < 4; j++) {
                totalSum += pwm[i][j];
            }
            if (totalSum > 0) {
                for (int j = 0; j < 4; j++) {
                    pwm[i][j] /= totalSum;
                }
            }
        }
    }

    /// Trims low-information-content columns from both ends of the PWM.
    ///
    /// A position is considered uninformative when its Shannon entropy exceeds `1.70` bits,
    /// which corresponds to a nearly uniform nucleotide distribution (max entropy ≈ 2 bits).
    /// Trimming advances {@link #trims} from the left and retreats {@link #trime} from the right
    /// until an informative position is reached or the two boundaries would cross.
    public void trim() {
        trims = 0;
        while (true) {
            double sum = 0.0;
            for (int i = 0; i < 4; i++) {
                if (pwm[trims][i] > 0.000001) {
                    sum = sum - pwm[trims][i] * Math.log(pwm[trims][i]) / Math.log(2.0);
                }
            }
            if ((sum < 1.70) || (trims == len - 1)) {
                break;
            }
            trims++;
        }

        trime = len - 1;
        while (true) {
            double sum = 0.0;
            for (int i = 0; i < 4; i++) {
                if (pwm[trime][i] > 0.000001) {
                    sum = sum - pwm[trime][i] * Math.log(pwm[trime][i]) / Math.log(2.0);
                }
            }
            if ((sum < 1.70) || (trime == trims)) {
                break;
            }
            trime--;
        }
    }

    public double[][] getPWM() {
        double[][] ret = new double[trime - trims + 1][4];
        for (int i = trims; i <= trime; i++) {
            ret[i - trims][0] = pwm[i][0];
            ret[i - trims][1] = pwm[i][3];
            ret[i - trims][2] = pwm[i][1];
            ret[i - trims][3] = pwm[i][2];
        }
        return ret;
    }

    public double[][] getTraceMatrix() {
        double[][] ret = new double[structProbArr.length][6];
        for (int r = 0; r < structProbArr.length; r++) {
            double sum = 0.0;
            for (int c = 0; c < 5; c++) {
                ret[r][c] = structProbArr[r][c];
                sum += structProbArr[r][c];
            }
            ret[r][5] = 1.0 - sum;
        }
        return ret;
    }

    private static int toIndex(char c) {
        return switch (c) {
            case 'A' -> 0;
            case 'G' -> 1;
            case 'T', 'U' -> 2;
            default -> 3;
        };
    }
}
