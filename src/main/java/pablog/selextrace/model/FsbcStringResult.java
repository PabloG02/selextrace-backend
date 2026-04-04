package pablog.selextrace.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;

@Embeddable
public class FsbcStringResult implements Serializable {

    @Column(name = "string_rank", nullable = false)
    private int rank;

    @Column(name = "subsequence", nullable = false, length = 128)
    private String subsequence;

    @Column(name = "subsequence_length", nullable = false)
    private int length;

    @Column(name = "observed_count", nullable = false)
    private int observedCount;

    @Column(name = "expected_fraction", nullable = false)
    private double expectedFraction;

    @Column(name = "z_score", nullable = false)
    private double zScore;

    @Column(name = "normalized_z_score", nullable = false)
    private double normalizedZScore;

    public FsbcStringResult() {
    }

    public FsbcStringResult(
            int rank,
            String subsequence,
            int length,
            int observedCount,
            double expectedFraction,
            double zScore,
            double normalizedZScore
    ) {
        this.rank = rank;
        this.subsequence = subsequence;
        this.length = length;
        this.observedCount = observedCount;
        this.expectedFraction = expectedFraction;
        this.zScore = zScore;
        this.normalizedZScore = normalizedZScore;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public String getSubsequence() {
        return subsequence;
    }

    public void setSubsequence(String subsequence) {
        this.subsequence = subsequence;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getObservedCount() {
        return observedCount;
    }

    public void setObservedCount(int observedCount) {
        this.observedCount = observedCount;
    }

    public double getExpectedFraction() {
        return expectedFraction;
    }

    public void setExpectedFraction(double expectedFraction) {
        this.expectedFraction = expectedFraction;
    }

    public double getZScore() {
        return zScore;
    }

    public void setZScore(double zScore) {
        this.zScore = zScore;
    }

    public double getNormalizedZScore() {
        return normalizedZScore;
    }

    public void setNormalizedZScore(double normalizedZScore) {
        this.normalizedZScore = normalizedZScore;
    }
}
