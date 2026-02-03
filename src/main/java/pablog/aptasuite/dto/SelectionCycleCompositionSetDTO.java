package pablog.aptasuite.dto;

public record SelectionCycleCompositionSetDTO(
        int singletonCount,
        SelectionCycleCompositionDTO positiveSelectionCycles,
        SelectionCycleCompositionDTO negativeSelectionCycles,
        SelectionCycleCompositionDTO controlSelectionCycles
) {}
