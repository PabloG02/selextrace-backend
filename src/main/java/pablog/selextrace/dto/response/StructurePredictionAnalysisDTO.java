package pablog.selextrace.dto.response;

import pablog.selextrace.model.StructurePredictionAnalysis;

import java.time.Instant;
import java.util.Map;

public record StructurePredictionAnalysisDTO(
        Long experimentId,
        Map<Integer, double[]> profiles,
        long durationMs,
        Instant createdAt
) {
    public static StructurePredictionAnalysisDTO from(StructurePredictionAnalysis analysis) {
        return new StructurePredictionAnalysisDTO(
                analysis.getExperimentId(),
                analysis.getProfiles(),
                analysis.getDurationMs(),
                analysis.getCreatedAt()
        );
    }
}
