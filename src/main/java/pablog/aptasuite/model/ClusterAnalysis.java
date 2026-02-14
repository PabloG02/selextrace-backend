package pablog.aptasuite.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import pablog.aptasuite.config.AptaClusterConfiguration;

import java.time.Instant;
import java.util.Map;

@Document(collection = "clusterAnalyses")
public class ClusterAnalysis {

    @Id
    private String id;
    private String experimentId;
    private AptaClusterConfiguration requestConfig;
    private Map<Integer, Integer> aptamerToCluster;
    private long durationMs;

    @CreatedDate
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

    public String getExperimentId() {
        return experimentId;
    }

    public AptaClusterConfiguration getRequestConfig() {
        return requestConfig;
    }

    public Map<Integer, Integer> getAptamerToCluster() {
        return aptamerToCluster;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setExperimentId(String experimentId) {
        this.experimentId = experimentId;
    }

    public void setRequestConfig(AptaClusterConfiguration requestConfig) {
        this.requestConfig = requestConfig;
    }

    public void setAptamerToCluster(Map<Integer, Integer> aptamerToCluster) {
        this.aptamerToCluster = aptamerToCluster;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
