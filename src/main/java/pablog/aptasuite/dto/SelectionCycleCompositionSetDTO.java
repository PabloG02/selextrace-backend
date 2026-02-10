package pablog.aptasuite.dto;

import java.util.Map;

public record SelectionCycleCompositionSetDTO(
        int singletonCount,
        Map<String, SelectionCycleCompositionDTO> positiveSelectionCycles,
        Map<String, SelectionCycleCompositionDTO> negativeSelectionCycles,
        Map<String, SelectionCycleCompositionDTO> controlSelectionCycles
) {}
