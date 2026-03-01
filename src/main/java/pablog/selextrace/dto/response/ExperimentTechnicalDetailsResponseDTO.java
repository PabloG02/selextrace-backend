package pablog.selextrace.dto.response;

import java.util.Map;

public record ExperimentTechnicalDetailsResponseDTO(
        Map<String, Double> selectionCyclePercentages,
        ExperimentMetadataDTO metadata
) {}
