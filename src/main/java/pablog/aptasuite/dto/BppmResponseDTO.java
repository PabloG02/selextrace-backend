package pablog.aptasuite.dto;

import java.util.List;

public record BppmResponseDTO (
        List<List<Double>> matrix
) {}
