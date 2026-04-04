package pablog.selextrace.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import pablog.selextrace.config.FsbcConfiguration;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "fsbc_analyses")
public class FsbcAnalysis {

    @Id
    @Column(nullable = false, updatable = false, length = 36)
    private String id;

    @Column(nullable = false, length = 36)
    private String experimentId;

    @AttributeOverrides({
            @AttributeOverride(name = "selectionCycleRound", column = @Column(name = "config_selection_cycle_round", nullable = false)),
            @AttributeOverride(name = "minLength", column = @Column(name = "config_min_length", nullable = false)),
            @AttributeOverride(name = "maxLength", column = @Column(name = "config_max_length", nullable = false)),
            @AttributeOverride(name = "rnaSequence", column = @Column(name = "config_rna_sequence", nullable = false)),
            @AttributeOverride(name = "threadCount", column = @Column(name = "config_thread_count", nullable = false))
    })
    @Embedded
    private FsbcConfiguration requestConfig = new FsbcConfiguration();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "fsbc_analysis_assignments", joinColumns = @JoinColumn(name = "analysis_id"))
    @MapKeyColumn(name = "aptamer_id")
    @Column(name = "cluster_id", nullable = false)
    private Map<Integer, Integer> aptamerToCluster = new HashMap<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "fsbc_analysis_strings", joinColumns = @JoinColumn(name = "analysis_id"))
    @OrderColumn(name = "string_index")
    private List<FsbcStringResult> rankedStrings = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "fsbc_analysis_cluster_seeds", joinColumns = @JoinColumn(name = "analysis_id"))
    @OrderColumn(name = "cluster_index")
    private List<FsbcClusterSeed> clusterSeeds = new ArrayList<>();

    @Column(nullable = false)
    private int totalSequenceCount;

    @Column(nullable = false)
    private int uniqueSequenceCount;

    @Column(nullable = false)
    private int clusterCount;

    @Column(nullable = false)
    private int stringCount;

    @Column(nullable = false)
    private long durationMs;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public FsbcAnalysis() {
    }

    public FsbcAnalysis(
            String experimentId,
            FsbcConfiguration requestConfig,
            Map<Integer, Integer> aptamerToCluster,
            List<FsbcStringResult> rankedStrings,
            List<FsbcClusterSeed> clusterSeeds,
            int totalSequenceCount,
            int uniqueSequenceCount,
            long durationMs
    ) {
        this.experimentId = experimentId;
        this.requestConfig = requestConfig;
        this.aptamerToCluster = aptamerToCluster == null ? new HashMap<>() : new HashMap<>(aptamerToCluster);
        setRankedStrings(rankedStrings);
        setClusterSeeds(clusterSeeds);
        this.totalSequenceCount = totalSequenceCount;
        this.uniqueSequenceCount = uniqueSequenceCount;
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

    public FsbcConfiguration getRequestConfig() {
        return requestConfig;
    }

    public void setRequestConfig(FsbcConfiguration requestConfig) {
        this.requestConfig = requestConfig;
    }

    public Map<Integer, Integer> getAptamerToCluster() {
        return aptamerToCluster;
    }

    public void setAptamerToCluster(Map<Integer, Integer> aptamerToCluster) {
        this.aptamerToCluster = aptamerToCluster == null ? new HashMap<>() : new HashMap<>(aptamerToCluster);
    }

    public List<FsbcStringResult> getRankedStrings() {
        return rankedStrings;
    }

    public void setRankedStrings(List<FsbcStringResult> rankedStrings) {
        this.rankedStrings = rankedStrings == null ? new ArrayList<>() : new ArrayList<>(rankedStrings);
        this.stringCount = this.rankedStrings.size();
    }

    public List<FsbcClusterSeed> getClusterSeeds() {
        return clusterSeeds;
    }

    public void setClusterSeeds(List<FsbcClusterSeed> clusterSeeds) {
        this.clusterSeeds = clusterSeeds == null ? new ArrayList<>() : new ArrayList<>(clusterSeeds);
        this.clusterCount = this.clusterSeeds.size();
    }

    public int getTotalSequenceCount() {
        return totalSequenceCount;
    }

    public void setTotalSequenceCount(int totalSequenceCount) {
        this.totalSequenceCount = totalSequenceCount;
    }

    public int getUniqueSequenceCount() {
        return uniqueSequenceCount;
    }

    public void setUniqueSequenceCount(int uniqueSequenceCount) {
        this.uniqueSequenceCount = uniqueSequenceCount;
    }

    public int getClusterCount() {
        return clusterCount;
    }

    public void setClusterCount(int clusterCount) {
        this.clusterCount = clusterCount;
    }

    public int getStringCount() {
        return stringCount;
    }

    public void setStringCount(int stringCount) {
        this.stringCount = stringCount;
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
        clusterCount = clusterSeeds == null ? 0 : clusterSeeds.size();
        stringCount = rankedStrings == null ? 0 : rankedStrings.size();
    }
}
