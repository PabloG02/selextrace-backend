package pablog.selextrace.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import pablog.selextrace.config.AptaTraceConfiguration;
import pablog.selextrace.converter.CompressedMotifAnalysisProfileListConverter;
import pablog.selextrace.motif.MotifAnalysisProfile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "motif_analyses")
public class MotifAnalysis {

    @Id
    @Column(nullable = false, updatable = false, length = 36)
    private String id;

    @Column(nullable = false, length = 36)
    private String experimentId;

    @AttributeOverrides({
            @AttributeOverride(name = "kmerLength", column = @Column(name = "config_kmer_length", nullable = false)),
            @AttributeOverride(name = "filterClusters", column = @Column(name = "config_filter_clusters", nullable = false)),
            @AttributeOverride(name = "alpha", column = @Column(name = "config_alpha", nullable = false)),
    })
    @Embedded
    private AptaTraceConfiguration requestConfig = new AptaTraceConfiguration();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "motif_analysis_rounds", joinColumns = @JoinColumn(name = "analysis_id"))
    @OrderColumn(name = "round_index")
    @Column(name = "round_name", nullable = false)
    private List<String> roundNames = new ArrayList<>();

    @Convert(converter = CompressedMotifAnalysisProfileListConverter.class)
    @Column(nullable = false, columnDefinition = "BYTEA")
    private List<MotifAnalysisProfile> profiles = new ArrayList<>();

    @Column(nullable = false)
    private int significantKmerCount;

    @Column(nullable = false)
    private int motifCount;

    @Column(nullable = false)
    private int lastRoundCount;

    @Column(nullable = false)
    private long durationMs;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public MotifAnalysis() {
    }

    public MotifAnalysis(
            String experimentId,
            AptaTraceConfiguration requestConfig,
            List<String> roundNames,
            List<MotifAnalysisProfile> profiles,
            int significantKmerCount,
            int lastRoundCount,
            long durationMs
    ) {
        this.experimentId = experimentId;
        this.requestConfig = requestConfig;
        this.roundNames = roundNames == null ? new ArrayList<>() : new ArrayList<>(roundNames);
        this.profiles = profiles == null ? new ArrayList<>() : new ArrayList<>(profiles);
        this.significantKmerCount = significantKmerCount;
        this.motifCount = this.profiles.size();
        this.lastRoundCount = lastRoundCount;
        this.durationMs = durationMs;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getExperimentId() {
        return experimentId;
    }

    public void setExperimentId(String experimentId) {
        this.experimentId = experimentId;
    }

    public AptaTraceConfiguration getRequestConfig() {
        return requestConfig;
    }

    public void setRequestConfig(AptaTraceConfiguration requestConfig) {
        this.requestConfig = requestConfig;
    }

    public List<String> getRoundNames() {
        return roundNames;
    }

    public void setRoundNames(List<String> roundNames) {
        this.roundNames = roundNames == null ? new ArrayList<>() : new ArrayList<>(roundNames);
    }

    public List<MotifAnalysisProfile> getProfiles() {
        return profiles;
    }

    public void setProfiles(List<MotifAnalysisProfile> profiles) {
        this.profiles = profiles == null ? new ArrayList<>() : new ArrayList<>(profiles);
        this.motifCount = this.profiles.size();
    }

    public int getSignificantKmerCount() {
        return significantKmerCount;
    }

    public void setSignificantKmerCount(int significantKmerCount) {
        this.significantKmerCount = significantKmerCount;
    }

    public int getMotifCount() {
        return motifCount;
    }

    public void setMotifCount(int motifCount) {
        this.motifCount = motifCount;
    }

    public int getLastRoundCount() {
        return lastRoundCount;
    }

    public void setLastRoundCount(int lastRoundCount) {
        this.lastRoundCount = lastRoundCount;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @PrePersist
    public void assignIdIfMissing() {
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
        motifCount = profiles == null ? 0 : profiles.size();
    }
}