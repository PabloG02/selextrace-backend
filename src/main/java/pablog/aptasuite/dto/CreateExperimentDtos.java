package pablog.aptasuite.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import pablog.aptasuite.model.ReaderType;

import java.util.List;

/**
 * Full DTO model for experiment creation.
 * Uses Java 16+ records for conciseness and immutability.
 */
public class CreateExperimentDtos {

    // =====================
    // Root DTO
    // =====================
    public record CreateExperimentDto(
            String name,
            String description,
            ExperimentSequencing sequencing,
            List<SelectionCycle> selectionCycles
    ) {}

    // =====================
    // Sequencing configuration
    // =====================
    public record ExperimentSequencing(
            boolean isDemultiplexed,
            String readType,
            ReaderType fileFormat,
            Primers primers,
            RandomizedRegion randomizedRegion
    ) {}

    // =====================
    // Primers
    // =====================
    public record Primers(
            String fivePrime,
            String threePrime
    ) {}

    // =====================
    // Randomized region (either exact or range)
    // =====================
    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.PROPERTY,
            property = "type"
    )
    @JsonSubTypes({
            @JsonSubTypes.Type(value = ExactLengthRandomizedRegion.class, name = "exact"),
            @JsonSubTypes.Type(value = RangeRandomizedRegion.class, name = "range")
    })
    public sealed interface RandomizedRegion permits ExactLengthRandomizedRegion, RangeRandomizedRegion {}

    public record ExactLengthRandomizedRegion(
            String type,  // "exact"
            int exactLength
    ) implements RandomizedRegion {}

    public record RangeRandomizedRegion(
            String type,  // "range"
            int min,
            int max
    ) implements RandomizedRegion {}

    // =====================
    // Selection cycle
    // =====================
    public record SelectionCycle(
            int roundNumber,
            String roundName,
            boolean isControl,
            boolean isCounterSelection
            // Files not included — handled separately as @RequestPart files
    ) {}
}
