package pablog.aptasuite.dto.response;

import pablog.aptasuite.domain.metadata.Accumulator;
import pablog.aptasuite.domain.metadata.Metadata;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public record ExperimentMetadataDTO (
        Map<String, Map<Integer, AccumulatorDTO>> qualityScoresForward,
        Map<String, Map<Integer, AccumulatorDTO>> qualityScoresReverse,
        Map<String, ConcurrentHashMap<Integer, ConcurrentHashMap<Byte, Integer>>> nucleotideDistributionForward,
        Map<String, ConcurrentHashMap<Integer, ConcurrentHashMap<Byte, Integer>>> nucleotideDistributionReverse,
        Map<String, ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, ConcurrentHashMap<Byte, Integer>>>> nucleotideDistributionAccepted
) {
    public static ExperimentMetadataDTO from(Metadata metadata) {
        if (metadata == null) {
            return null;
        }

        return new ExperimentMetadataDTO(
                mapQualityScores(metadata.qualityScoresForward),
                mapQualityScores(metadata.qualityScoresReverse),
                metadata.nucleotideDistributionForward,
                metadata.nucleotideDistributionReverse,
                metadata.nucleotideDistributionAccepted
        );
    }

    private static Map<String, Map<Integer, AccumulatorDTO>> mapQualityScores(
            Map<String, ConcurrentHashMap<Integer, Accumulator>> source
    ) {
        Map<String, Map<Integer, AccumulatorDTO>> mapped = new HashMap<>();
        for (Map.Entry<String, ConcurrentHashMap<Integer, Accumulator>> entry : source.entrySet()) {
            Map<Integer, AccumulatorDTO> values = new HashMap<>();
            if (entry.getValue() != null) {
                for (Map.Entry<Integer, Accumulator> accumulatorEntry : entry.getValue().entrySet()) {
                    if (accumulatorEntry.getValue() != null) {
                        values.put(accumulatorEntry.getKey(), AccumulatorDTO.from(accumulatorEntry.getValue()));
                    }
                }
            }
            mapped.put(entry.getKey(), values);
        }

        return mapped;
    }
}
