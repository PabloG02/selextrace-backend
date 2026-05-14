package pablog.selextrace.dto.response;

import pablog.selextrace.config.FsbcConfiguration;
import pablog.selextrace.model.FsbcAnalysis;
import pablog.selextrace.model.FsbcClusterSeed;
import pablog.selextrace.model.FsbcStringResult;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record FsbcAnalysisDTO(
        Long id,
        Long experimentId,
        FsbcConfiguration requestConfig,
        Map<Integer, Integer> aptamerToCluster,
        List<FsbcStringResult> rankedStrings,
        List<FsbcClusterSeed> clusterSeeds,
        int totalSequenceCount,
        int uniqueSequenceCount,
        int clusterCount,
        int stringCount,
        long durationMs,
        Instant createdAt
) {
    public static FsbcAnalysisDTO from(FsbcAnalysis analysis) {
        return new FsbcAnalysisDTO(
                analysis.getId(),
                analysis.getExperiment().getId(),
                analysis.getRequestConfig(),
                analysis.getAptamerToCluster(),
                analysis.getRankedStrings(),
                analysis.getClusterSeeds(),
                analysis.getTotalSequenceCount(),
                analysis.getUniqueSequenceCount(),
                analysis.getClusterCount(),
                analysis.getStringCount(),
                analysis.getDurationMs(),
                analysis.getCreatedAt()
        );
    }
}
