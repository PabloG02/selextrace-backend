package pablog.selextrace.dto.response;

import pablog.selextrace.domain.pool.AptamerBounds;

import java.util.Map;

public record ExperimentPoolDTO(
        Map<Integer, String> idToAptamer,
        Map<Integer, AptamerBounds> idToBounds
) {}
