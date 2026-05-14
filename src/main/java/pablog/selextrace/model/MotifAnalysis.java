package pablog.selextrace.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import pablog.selextrace.config.AptaTraceConfiguration;
import pablog.selextrace.converter.CompressedMotifAnalysisProfileListConverter;
import pablog.selextrace.model.persistence.ExperimentRecord;
import pablog.selextrace.motif.MotifAnalysisProfile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "motif_analyses")
public class MotifAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "experiment_id",
        nullable = false,
        updatable = false,
        foreignKey = @ForeignKey(name = "fk_motif_analysis_experiment")
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ExperimentRecord experiment;

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
    @OnDelete(action = OnDeleteAction.CASCADE)
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
            ExperimentRecord experiment,
            AptaTraceConfiguration requestConfig,
            List<String> roundNames,
            List<MotifAnalysisProfile> profiles,
            int significantKmerCount,
            int lastRoundCount,
            long durationMs
    ) {
        this.experiment = experiment;
        this.requestConfig = requestConfig;
        this.roundNames = roundNames == null ? new ArrayList<>() : new ArrayList<>(roundNames);
        this.profiles = profiles == null ? new ArrayList<>() : new ArrayList<>(profiles);
        this.significantKmerCount = significantKmerCount;
        this.motifCount = this.profiles.size();
        this.lastRoundCount = lastRoundCount;
        this.durationMs = durationMs;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ExperimentRecord getExperiment() {
        return experiment;
    }

    public void setExperiment(ExperimentRecord experiment) {
        this.experiment = experiment;
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
    public void calculateMotifCount() {
        motifCount = profiles == null ? 0 : profiles.size();
    }
}