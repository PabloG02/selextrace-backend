package pablog.aptasuite.dto.response;

import pablog.aptasuite.dto.SelectionCycleResponseDTO;

import java.time.Instant;
import java.util.List;

public record ExperimentDTO(
        String id,
        Instant createdAt,
        String name,
        String description,
        ExperimentSequencingDTO sequencing,
        ExperimentImportStatsDTO importStats,
        List<SelectionCycleResponseDTO> selectionCycles,
        ExperimentPoolDTO pool,
        ExperimentTechnicalDetailsResponseDTO technicalDetails
) {}
