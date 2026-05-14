package pablog.selextrace.dto.response;

import pablog.selextrace.config.AptaClusterConfiguration;
import pablog.selextrace.model.ClusterAnalysis;

import java.time.Instant;
import java.util.Map;

public record ClusterAnalysisDTO(
        Long id,
        Long experimentId,
        AptaClusterConfiguration requestConfig,
        Map<Integer, Integer> aptamerToCluster,
        long durationMs,
        Instant createdAt
) {
    public static ClusterAnalysisDTO from(ClusterAnalysis analysis) {
        return new ClusterAnalysisDTO(
                analysis.getId(),
                analysis.getExperiment().getId(),
                analysis.getRequestConfig(),
                analysis.getAptamerToCluster(),
                analysis.getDurationMs(),
                analysis.getCreatedAt()
        );
    }
}
