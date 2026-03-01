package pablog.selextrace.dto.response;

public record ExperimentImportStatsDTO(
        long totalProcessedReads,
        long totalAcceptedReads,
        long contigAssemblyFailure,
        long invalidAlphabet,
        long fivePrimeError,
        long threePrimeError,
        long invalidCycle,
        long totalPrimerOverlaps
) {}
