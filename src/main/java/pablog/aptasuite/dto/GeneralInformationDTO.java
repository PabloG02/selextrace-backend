package pablog.aptasuite.dto;

public record GeneralInformationDTO(
        String name,
        String description,
        int aptamerSize,
        String fivePrimePrimer,
        String threePrimePrimer
) {}
