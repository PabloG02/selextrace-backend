package pablog.selextrace.dto.response;

import pablog.selextrace.config.AptaTraceConfiguration;
import pablog.selextrace.model.MotifAnalysis;
import pablog.selextrace.motif.MotifAnalysisProfile;

import java.time.Instant;
import java.util.List;

public record MotifAnalysisDTO(
        Long id,
        Long experimentId,
        AptaTraceConfiguration requestConfig,
        List<String> roundNames,
        List<MotifAnalysisProfile> profiles,
        int significantKmerCount,
        int motifCount,
        int lastRoundCount,
        long durationMs,
        Instant createdAt
) {
    public static MotifAnalysisDTO from(MotifAnalysis analysis) {
        return new MotifAnalysisDTO(
                analysis.getId(),
                analysis.getExperiment().getId(),
                analysis.getRequestConfig(),
                analysis.getRoundNames(),
                analysis.getProfiles(),
                analysis.getSignificantKmerCount(),
                analysis.getMotifCount(),
                analysis.getLastRoundCount(),
                analysis.getDurationMs(),
                analysis.getCreatedAt()
        );
    }
}
