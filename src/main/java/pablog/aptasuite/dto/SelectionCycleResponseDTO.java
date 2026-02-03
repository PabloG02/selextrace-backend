package pablog.aptasuite.dto;

import java.util.Map;

public record SelectionCycleResponseDTO (
        String name,
        int round,
        boolean isControlSelection,
        boolean isCounterSelection,
        String barcode5Prime,
        String barcode3Prime,
        int totalSize,
        int uniqueSize,
        Map<Integer, Integer> counts
) {}
