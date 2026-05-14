package pablog.selextrace.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import pablog.selextrace.config.AptaClusterConfiguration;
import pablog.selextrace.model.persistence.ExperimentRecord;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "cluster_analyses")
public class ClusterAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "experiment_id",
        nullable = false,
        updatable = false,
        foreignKey = @ForeignKey(name = "fk_cluster_analysis_experiment")
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ExperimentRecord experiment;

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
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Map<Integer, Integer> aptamerToCluster = new HashMap<>();

    @Column(nullable = false)
    private long durationMs;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public ClusterAnalysis() {
    }

    public ClusterAnalysis(
            ExperimentRecord experiment,
            AptaClusterConfiguration requestConfig,
            Map<Integer, Integer> aptamerToCluster,
            long durationMs
    ) {
        this.experiment = experiment;
        this.requestConfig = requestConfig;
        this.aptamerToCluster = aptamerToCluster;
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


}
