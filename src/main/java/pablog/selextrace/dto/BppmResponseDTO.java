package pablog.selextrace.dto;

import java.util.List;

public record BppmResponseDTO (
        List<List<Double>> matrix
) {}
