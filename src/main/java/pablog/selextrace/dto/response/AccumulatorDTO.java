package pablog.selextrace.dto.response;

import pablog.selextrace.domain.metadata.Accumulator;

public record AccumulatorDTO(
        double mean,
        double variance,
        double stddev,
        int count
) {
    public static AccumulatorDTO from(Accumulator accumulator) {
        return new AccumulatorDTO(
                accumulator.mean(),
                accumulator.var(),
                accumulator.stddev(),
                accumulator.count()
        );
    }
}