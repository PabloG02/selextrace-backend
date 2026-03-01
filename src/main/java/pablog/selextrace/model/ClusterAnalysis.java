package pablog.selextrace.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import pablog.selextrace.config.AptaClusterConfiguration;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "cluster_analyses")
public class ClusterAnalysis {

    @Id
    @Column(nullable = false, updatable = false, length = 36)
    private String id;

    @Column(nullable = false, length = 36)
    private String experimentId;

    @AttributeOverrides({
            @AttributeOverride(name = "randomizedRegionSize", column = @Column(name = "config_randomized_region_size", nullable = false)),
            @AttributeOverride(name = "lshDimension", column = @Column(name = "config_lsh_dimension", nullable = false)),
            @AttributeOverride(name = "lshIterations", column = @Column(name = "config_lsh_iterations", nullable = false)),
            @AttributeOverride(name = "editDistance", column = @Column(name = "config_edit_distance", nullable = false)),
            @AttributeOverride(name = "kmerSize", column = @Column(name = "config_kmer_size", nullable = false)),
            @AttributeOverride(name = "kmerCutoffIterations", column = @Column(name = "config_kmer_cutoff_iterations", nullable = false))
    })
    @Embedded
    private AptaClusterConfiguration requestConfig = new AptaClusterConfiguration();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "cluster_analysis_assignments", joinColumns = @JoinColumn(name = "id"))
    @MapKeyColumn(name = "aptamer_id")
    @Column(name = "cluster_id", nullable = false)
    private Map<Integer, Integer> aptamerToCluster = new HashMap<>();

    @Column(nullable = false)
    private long durationMs;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public ClusterAnalysis() {
    }

    public ClusterAnalysis(
            String experimentId,
            AptaClusterConfiguration requestConfig,
            Map<Integer, Integer> aptamerToCluster,
            long durationMs
    ) {
        this.experimentId = experimentId;
        this.requestConfig = requestConfig;
        this.aptamerToCluster = aptamerToCluster;
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

    public AptaClusterConfiguration getRequestConfig() {
        return requestConfig;
    }

    public void setRequestConfig(AptaClusterConfiguration requestConfig) {
        this.requestConfig = requestConfig;
    }

    public Map<Integer, Integer> getAptamerToCluster() {
        return aptamerToCluster;
    }

    public void setAptamerToCluster(Map<Integer, Integer> aptamerToCluster) {
        this.aptamerToCluster = aptamerToCluster == null ? new HashMap<>() : aptamerToCluster;
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
    }
}
