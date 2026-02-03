package pablog.aptasuite.dto;

import java.util.Map;

public record ExperimentDetailsDTO(
        GeneralInformationDTO generalInformation,
        SequenceImportStatisticsDTO sequenceImportStatistics,
        Map<String, Double> selectionCyclePercentages
) {}
