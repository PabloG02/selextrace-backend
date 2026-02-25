package pablog.aptasuite.dto.response;

import pablog.aptasuite.domain.pool.AptamerBounds;

import java.util.Map;

public record ExperimentPoolDTO(
        Map<Integer, String> idToAptamer,
        Map<Integer, AptamerBounds> idToBounds
) {}
