package pablog.aptasuite.dto.response;

public record ExperimentSequencingDTO(
        Integer aptamerSize,
        String fivePrimePrimer,
        String threePrimePrimer
) {}
