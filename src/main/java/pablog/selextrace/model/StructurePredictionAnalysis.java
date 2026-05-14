package pablog.selextrace.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import pablog.selextrace.converter.CompressedIntegerDoubleArrayMapConverter;
import pablog.selextrace.model.persistence.ExperimentRecord;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "structure_prediction_analyses")
public class StructurePredictionAnalysis {

    @Id
    @Column(nullable = false, updatable = false)
    private Long experimentId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(
        name = "experiment_id",
        nullable = false,
        updatable = false,
        foreignKey = @ForeignKey(name = "fk_structure_prediction_experiment")
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ExperimentRecord experiment;

    @Convert(converter = CompressedIntegerDoubleArrayMapConverter.class)
    @Column(nullable = false, columnDefinition = "BYTEA")
    private Map<Integer, double[]> profiles = new HashMap<>();

    @Column(nullable = false)
    private long durationMs;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public StructurePredictionAnalysis() {
    }

    public StructurePredictionAnalysis(ExperimentRecord experiment, Map<Integer, double[]> profiles, long durationMs) {
        this.experiment = experiment;
        this.profiles = profiles;
        this.durationMs = durationMs;
    }

    public Long getExperimentId() {
        return experimentId;
    }

    public ExperimentRecord getExperiment() {
        return experiment;
    }

    public void setExperiment(ExperimentRecord experiment) {
        this.experiment = experiment;
        if (experiment != null) {
            this.experimentId = experiment.getId();
        }
    }

    public Map<Integer, double[]> getProfiles() {
        return profiles;
    }

    public void setProfiles(Map<Integer, double[]> profiles) {
        this.profiles = profiles == null ? new HashMap<>() : profiles;
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