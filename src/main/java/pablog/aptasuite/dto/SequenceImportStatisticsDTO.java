package pablog.aptasuite.dto;

public record SequenceImportStatisticsDTO(
        long totalProcessedReads,
        long totalAcceptedReads,
        long contigAssemblyFailure,
        long invalidAlphabet,
        long fivePrimeError,
        long threePrimeError,
        long invalidCycle,
        long totalPrimerOverlaps
) {}
