package pablog.aptasuite.dto;

import pablog.aptasuite.domain.metadata.Metadata;
import pablog.aptasuite.domain.pool.AptamerBounds;

import java.util.List;
import java.util.Map;

public record ExperimentOverviewDTO(
        ExperimentDetailsDTO experimentDetails,
        Map<String, Object> randomizedRegionSizeDistribution,
        // Testing purposes
        List<SelectionCycleResponseDTO> selectionCycleResponse,
        Metadata metadata,
        Map<Integer, String> idToAptamer,
        Map<Integer, AptamerBounds> idToBounds
) {
}
