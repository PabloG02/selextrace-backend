package pablog.aptasuite.dto;

import java.util.List;

public record ContextProbabilityResponseDTO(
        List<Double> hairpin,
        List<Double> bulge,
        List<Double> internal,
        List<Double> multi,
        List<Double> dangling,
        List<Double> paired
) {}
