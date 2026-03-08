package pablog.selextrace.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import pablog.selextrace.converter.CompressedIntegerDoubleArrayMapConverter;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "structure_prediction_analyses")
public class StructurePredictionAnalysis {

    @Id
    @Column(nullable = false, updatable = false, length = 36)
    private String experimentId;

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

    public StructurePredictionAnalysis(String experimentId, Map<Integer, double[]> profiles, long durationMs) {
        this.experimentId = experimentId;
        this.profiles = profiles;
        this.durationMs = durationMs;
    }

    public String getExperimentId() {
        return experimentId;
    }

    public void setExperimentId(String experimentId) {
        this.experimentId = experimentId;
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