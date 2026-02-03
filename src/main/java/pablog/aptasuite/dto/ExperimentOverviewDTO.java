package pablog.aptasuite.dto;

import pablog.aptasuite.domain.metadata.Metadata;

import java.util.Map;

public record ExperimentOverviewDTO(
        ExperimentDetailsDTO experimentDetails,
        Map<String, Object> randomizedRegionSizeDistribution,
        SelectionCycleCompositionSetDTO selectionCycleComposition,
        // Testing purposes
        SelectionCycleResponseDTO selectionCycleResponse,
        Metadata metadata,
        Map<Integer, String> idToAptamer
) {
}
