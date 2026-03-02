package pablog.selextrace.motif;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.util.MathUtils;

/// Tracks the evolutionary trajectory and structural context shifts of a specific `k`-mer
/// across multiple **SELEX** rounds.
///
/// In a SELEX experiment, RNA/DNA pool sequences undergo iterative rounds of selection for binding affinity.
/// This class helps identify sequence motifs (represented by `k`-mers) that not only survive but
/// actively shift their secondary structural distribution—for example, moving from a random coil to a highly
/// stable **Hairpin** or **Bulge**—as selection pressure increases.
///
/// To separate true evolutionary selection from random sequencing noise, this class maintains two parallel
/// probability tracks. The **singleton** track (sequences with counts ≤ `singletonThres`) acts as a
/// background null model representing random structural drift. The **non-singleton** track (sequences with
/// counts > `singletonThres`) represents the actively enriched pool. By computing the **Kullback-Leibler (KL) divergence**
/// between rounds for the non-singleton pool and comparing it against the null model, the algorithm isolates
/// `k`-mers exhibiting statistically significant, directed structural evolution.
public class KContextTrace implements Comparable<KContextTrace> {

    /// Minimum required aggregate `k`-mer count in a specific selection round
    /// for its **singleton** occurrences to be included in the **background** KL divergence
    /// (context shifting) calculation.
    private static final int MINIMAL_SINGLETON_COUNT = 100;
    /// Minimum required aggregate `k`-mer count in a specific selection round
    /// for its **non-singleton** occurrences to be included in the **primary** KL divergence
    /// (context shifting) calculation.
    private static final int MINIMAL_COUNT = 100;

    /// The nucleotide sequence of the `k`-mer being tracked.
    /// E.g., for the DNA sequence `ATGCATG` and `k = 3`, a `k`-mer could be `ATG`.
    private final String kmer;
    /// The total number of selection rounds in the SELEX experiment.
    private final int numOfRounds;
    /// The number of distinct secondary structural contexts being evaluated (e.g., Hairpin, Bulge,
    /// Inner loop, Multi-loop, Dangling end).
    private final int numOfContexts;
    /// A matrix `[round][context]` holding the average structural probability profile of the k-mer
    /// in each round, specifically for aptamers with frequencies > `singletonThres`.
    /// These "non-singletons" represent sequences actively undergoing evolutionary selection.
    ///
    /// **Note:** Values are un-normalized sums until {@link #normalizeProfile()} is called.
    private final double[][] structProbArr;
    /// A matrix `[round][context]` holding the average structural probability profile of the k-mer
    /// in each round, specifically for aptamers with frequencies ≤ `singletonThres`.
    /// These "singletons" represent background noise or unselected sequences, serving as the null distribution.
    ///
    /// **Note:** Values are un-normalized sums until {@link #normalizeProfile()} is called.
    private final double[][] singletonStructProbArr;
    /// Array `[round]` storing the sum of frequencies for all aptamers containing this k-mer,
    /// strictly limited to aptamers with frequencies ≤ `singletonThres`.
    /// In the SELEX context, these low-frequency "singleton" occurrences are treated as background noise
    /// or unselected sequences, tracked here to build a statistical null distribution.
    private final int[] singletonCount;
    /// Array `[round]` storing the sum of frequencies for all aptamers containing this k-mer,
    /// strictly limited to aptamers with frequencies > `singletonThres`.
    /// These high-frequency, "non-singleton" occurrences represent the sequences that are actively
    /// undergoing evolutionary enrichment and selection for target binding.
    private final int[] nonSingletonCount;
    /// Array `[round]` storing the absolute total frequency of **all** aptamers containing this k-mer
    private final int[] totalCount;

    /// The context shifting score (calculated via KL divergence) evaluated only for actively selected
    /// aptamers with frequencies > `singletonThres`.
    /// This score mathematically quantifies how strongly the k-mer's structural probability distribution
    /// shifts across SELEX rounds, indicating an evolutionary push toward a specific folded binding state.
    private double contextShiftingScore;
    /// The context shifting score (calculated via KL divergence) evaluated specifically for background
    /// aptamers with small frequencies (≤ `singletonThres`).
    /// This metric acts as a baseline/null model representing random structural drift, which is used
    /// to determine if the non-singleton `contextShiftingScore` is statistically significant.
    private double singletonContextShiftingScore;
    /// The integer ID of the specific structural state (e.g., `0` for Hairpin, `1` for Bulge)
    /// that this `k`-mer is overwhelmingly shifting towards as selection progresses.
    private int selectionContext;
    /// Flag indicating if the number of aptamers containing the `k`-mer is more than `1%` in the last round.
    private boolean strongPresence;
    /// Decimal proportion (percentage) of the aptamers containing the motif in the last selection round.
    private double proportion;
    /// `P-value` of the context shifting score of the `k`-mer.
    private double pvalue;
    /// Flag indicating whether the `k`-mer has at least {@link #MINIMAL_COUNT} occurrences
    /// in every round up to the round of interest to be considered a reliable data point.
    private boolean enoughOccurrences = true;

    /// Constructs a new trace for a specific `k`-mer.
    ///
    /// @param kmer The nucleotide sequence of the `k`-mer.
    /// @param numOfRounds Total selection cycles in the SELEX experiment.
    /// @param numOfContexts Number of distinct secondary structure contexts analyzed.
    public KContextTrace(String kmer, int numOfRounds, int numOfContexts) {
        this.numOfRounds = numOfRounds;
        this.kmer = kmer;
        this.numOfContexts = numOfContexts;
        this.structProbArr = new double[numOfRounds][numOfContexts];
        this.singletonStructProbArr = new double[numOfRounds][numOfContexts];
        this.singletonCount = new int[numOfRounds];
        this.nonSingletonCount = new int[numOfRounds];
        this.totalCount = new int[numOfRounds];
    }

    public String getKmer() {
        return kmer;
    }

    public int getSelectionContext() {
        return selectionContext;
    }

    public double getPValue() {
        return pvalue;
    }

    public double getProportion() {
        return this.proportion;
    }

    /// Flags whether this `k`-mer occupies a significant proportion (≥ 1%) of the pool in the final round.
    ///
    /// @param lastRoundCount The absolute total number of aptamers analyzed in the final SELEX round.
    public void setStrongPresence(int lastRoundCount) {
        strongPresence = totalCount[totalCount.length - 1] >= 0.01 * lastRoundCount;
        proportion = totalCount[totalCount.length - 1] / (lastRoundCount * 1.0);
    }

    public boolean hasStrongPresence() {
        return strongPresence;
    }

    /// Accumulates the weighted structural context probability of this `k`-mer for a
    /// **non-singleton** aptamer occurrence in selection round `r`.
    ///
    /// @param r Zero-based selection round index.
    /// @param count Frequency of the aptamer in round `r`.
    /// @param avgContextProbArr Average per-context probability of the k-mer within this aptamer.
    public void addContextProb(int r, int count, double[] avgContextProbArr) {
        for (int c = 0; c < 5; c++) {
            structProbArr[r][c] += avgContextProbArr[c] * count;
        }
        nonSingletonCount[r] += count;
    }

    /// Accumulates the weighted structural context probability of this `k`-mer for a
    /// **singleton** aptamer occurrence in selection round `r`.
    ///
    /// @param r Zero-based selection round index.
    /// @param count Frequency of the aptamer in round `r`.
    /// @param avgContextProbArr Average per-context probability of the k-mer within this aptamer.
    public void addSingletonContextProb(int r, int count, double[] avgContextProbArr) {
        for (int c = 0; c < 5; c++) {
            singletonStructProbArr[r][c] += avgContextProbArr[c] * count;
        }
        singletonCount[r] += count;
    }

    public void addTotalCount(int r, int count) {
        totalCount[r] += count;
    }

    /// Sets {@link #enoughOccurrences} to `false` if any round falls below {@link #MINIMAL_COUNT}.
    /// K-mers that fail this check are excluded from the context-trace pipeline.
    public void checkEnoughOccurrences() {
        enoughOccurrences = true;
        for (int value : totalCount) {
            if (value < MINIMAL_COUNT) {
                enoughOccurrences = false;
                break;
            }
        }
    }

    public boolean hasEnoughOccurrences() {
        return enoughOccurrences;
    }

    /// Normalizes the context-trace matrices after all per-aptamer accumulation is complete,
    /// converting raw weighted sums into weighted-mean probability distributions.
    ///
    /// For rounds where non-singleton counts fall below {@link #MINIMAL_COUNT} (and for round `0`,
    /// which always serves as a baseline), singleton data is merged into the non-singleton
    /// accumulators if the combined count is sufficient, to salvage a usable estimate.
    /// After any such merges, each entry is divided by its corresponding round-level count.
    public void normalizeProfile() {
        for (int i = 0; i < nonSingletonCount.length; i++) {
            if (nonSingletonCount[i] < MINIMAL_COUNT || i == 0) {
                if ((singletonCount[i] + nonSingletonCount[i]) >= MINIMAL_COUNT) {
                    nonSingletonCount[i] = singletonCount[i] + nonSingletonCount[i];
                    for (int c = 0; c < structProbArr[i].length; c++) {
                        structProbArr[i][c] += singletonStructProbArr[i][c];
                    }
                }
            }
        }

        for (int i = 0; i < nonSingletonCount.length; i++) {
            if (totalCount[i] >= MINIMAL_COUNT) {
                for (int c = 0; c < structProbArr[i].length; c++) {
                    structProbArr[i][c] = structProbArr[i][c] / nonSingletonCount[i];
                    singletonStructProbArr[i][c] /= (singletonCount[i] * 1.0);
                }
            }
        }
    }

    /// Computes the KL divergence (relative entropy) of distribution `p` from
    /// distribution `q` over the six structural contexts.
    ///
    /// The paired (P) context probability for each distribution is inferred as
    /// `1 - sum(p)` and `1 - sum(q)` respectively, and included in the sum when
    /// both values are positive.
    ///
    /// ```
    /// KL(p || q) = Σ p[i] · log(p[i] / q[i])   (for all i where p[i] > 0 and q[i] > 0)
    /// ```
    ///
    /// @param p The probability distribution of the shifted/later round (length = numOfContexts).
    /// @param q The reference/baseline probability distribution from an earlier round (length = numOfContexts).
    /// @return The computed KL divergence score (≥ 0.0, where 0.0 means the distributions are identical).
    private double calculateRelativeEntropy(double[] p, double[] q) {
        double sump = 0.0;
        double sumq = 0.0;
        double sum = 0.0;

        for (int i = 0; i < p.length; i++) {
            sump += p[i];
            sumq += q[i];
            if (p[i] > 0.0 && q[i] > 0.0) {
                sum += p[i] * Math.log(p[i] / q[i]);
            }
        }

        if ((1 - sump) > 0.0 && (1 - sumq) > 0.0) {
            sum += (1 - sump) * Math.log((1 - sump) / (1 - sumq));
        }

        return sum;
    }

    /// Computes the background context-shifting score from **singleton** aptamers.
    ///
    /// Averages the pairwise KL divergence across all round pairs `(j, k)` where both rounds
    /// meet {@link #MINIMAL_SINGLETON_COUNT}. If not all round pairs contribute, the score is
    /// set to `0.0` to indicate insufficient data.
    ///
    /// @param rc Per-round total read counts (unused, kept for API symmetry with {@link #calculateKLScore}).
    public void calculateSingletonKLScore(int[] rc) {
        singletonContextShiftingScore = 0.0;
        double times = 0.0;

        for (int k = 1; k < singletonStructProbArr.length; k++) {
            for (int j = 0; j < k; j++) {
                if (singletonStructProbArr[j][0] <= 1.0 && singletonStructProbArr[k][0] <= 1.0
                        && singletonCount[j] >= MINIMAL_SINGLETON_COUNT
                        && singletonCount[k] >= MINIMAL_SINGLETON_COUNT) {
                    times += 1.0;
                    singletonContextShiftingScore += calculateRelativeEntropy(singletonStructProbArr[k], singletonStructProbArr[j]);
                }
            }
        }

        double threshold = (singletonStructProbArr.length - 1) * singletonStructProbArr.length / 2.0;
        if (times > threshold - 0.01)
            singletonContextShiftingScore /= times;
        else
            singletonContextShiftingScore = 0.0;
    }

    /// Computes the primary context-shifting score from **non-singleton** aptamers.
    ///
    /// Averages the pairwise KL divergence across all round pairs `(j, k)` where both rounds
    /// meet {@link #MINIMAL_COUNT}. If not all round pairs contribute, the score is set to `0.0`
    /// to indicate insufficient data.
    ///
    /// @param rc Per-round total read counts (unused, kept for API symmetry with {@link #calculateSingletonKLScore}).
    public void calculateKLScore(int[] rc) {
        contextShiftingScore = 0.0;
        double times = 0.0;

        for (int k = 1; k < structProbArr.length; k++) {
            for (int j = 0; j < k; j++) {
                if (structProbArr[j][0] <= 1.0 && structProbArr[k][0] <= 1.0
                        && nonSingletonCount[j] >= MINIMAL_COUNT
                        && nonSingletonCount[k] >= MINIMAL_COUNT) {
                    times += 1.0;
                    contextShiftingScore += calculateRelativeEntropy(structProbArr[k], structProbArr[j]);
                }
            }
        }

        double threshold = (structProbArr.length - 1) * structProbArr.length / 2.0;
        if (times > threshold - 0.01)
            contextShiftingScore /= times;
        else
            contextShiftingScore = 0.0;
    }

    /// Identifies which structural context (e.g., **Hairpin**, **Bulge**, **Paired**)
    /// this `k`-mer is being driven towards as selection progresses.
    /// It calculates this by finding the structural context that exhibits the highest cumulative
    /// positive shift in probability across all pairwise round comparisons.
    public void deriveSelectionContext() {
        int savec = -1;
        double maxSum = -1_000_000.0;

        double[] pProb = new double[numOfRounds];
        for (int i = 0; i < numOfRounds; i++) {
            double sum = 0;
            for (int c = 0; c < numOfContexts; c++) {
                sum += structProbArr[i][c];
            }
            pProb[i] = 1 - sum;
        }

        for (int c = 0; c <= numOfContexts; c++) {
            double sum = 0;
            if (c < numOfContexts) {
                for (int i = 1; i < numOfRounds; i++) {
                    for (int j = 0; j < i; j++) {
                        sum += structProbArr[i][c] - structProbArr[j][c];
                    }
                }
            } else {
                for (int i = 1; i < numOfRounds; i++) {
                    for (int j = 0; j < i; j++) {
                        sum += pProb[i] - pProb[j];
                    }
                }
            }
            if (sum > maxSum) {
                maxSum = sum;
                savec = c;
            }
        }

        this.selectionContext = savec;
    }

    /// Approximates the right-tail p-value of a z-score under the standard normal distribution.
    ///
    /// Uses the asymptotic formula `P(Z > z) ≈ 1 / (z · √(2π) · exp(z² / 2))`,
    /// which is valid for large positive `z`.
    ///
    /// @param z A positive z-score.
    /// @return Approximate right-tail probability.
    public static double computePValue(double z) {
        return 1.0 / (z * Math.sqrt(MathUtils.TWO_PI) * Math.exp(z * z * 0.5));
    }

    /// Tests whether the context-shifting score of this `k`-mer is statistically significant
    /// relative to the singleton background distribution.
    ///
    /// @param singletonKL Log-transformed singleton KL scores for all k-mers that passed
    ///                    the occurrence threshold; these form the empirical null distribution.
    /// @param topValue Minimum log-KL score required (typically the 90th percentile of all scores).
    /// @param p Significance threshold for the p-value (e.g., `0.01` or `0.05`).
    /// @return `true` if the `k`-mer shows significant, non-random structural evolution.
    public boolean isSignificant(double[] singletonKL, double topValue, double p) {
        DescriptiveStatistics ds = new DescriptiveStatistics(singletonKL);
        double mean = ds.getMean();
        double std = ds.getStandardDeviation();
        double z = (Math.log(contextShiftingScore) - mean) / std;
        // Compute z value of the scores based on the null distribution of scores of kmers computed from aptamers with small counts
        pvalue = z > 0.0 ? computePValue(z) : 1.0;
        return pvalue <= p && Math.log(contextShiftingScore) >= topValue;
    }

    public double getKLScore() {
        return contextShiftingScore;
    }

    public double getSingletonKLScore() {
        return singletonContextShiftingScore;
    }

    /// Compares this `KContextTrace` to another based strictly on their {@link #proportion}.
    ///
    /// Two instances are considered equal if their proportions differ by less
    /// than `1e-6`.
    ///
    /// @param mkc The other `KContextTrace` to compare against.
    /// @return `-1` if this proportion is smaller, `1` if larger, `0` if equal
    @Override
    public int compareTo(KContextTrace mkc) {
        if (Math.abs(this.proportion - mkc.getProportion()) < 0.000001)
            return 0;
        else if (this.proportion < mkc.getProportion())
            return -1;
        else
            return 1;
    }
}
