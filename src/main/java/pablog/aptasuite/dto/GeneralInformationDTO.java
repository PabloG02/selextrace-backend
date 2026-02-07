package pablog.aptasuite.dto;

public record GeneralInformationDTO(
        String name,
        String description,
        Integer aptamerSize,
        String fivePrimePrimer,
        String threePrimePrimer
) {}
