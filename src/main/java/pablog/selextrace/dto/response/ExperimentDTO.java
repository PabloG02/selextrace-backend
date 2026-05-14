package pablog.selextrace.dto.response;

import pablog.selextrace.dto.SelectionCycleResponseDTO;
import pablog.selextrace.dto.project.ProjectDtos;
import pablog.selextrace.model.auth.ResourceAccessLevel;

import java.time.Instant;
import java.util.List;

public record ExperimentDTO(
        Long id,
        String name,
        String description,
        Instant createdAt,
        ProjectDtos.ProjectReferenceDTO project,
        ResourceAccessLevel accessLevel,
        ExperimentSequencingDTO sequencing,
        ExperimentImportStatsDTO importStats,
        List<SelectionCycleResponseDTO> selectionCycles,
        ExperimentPoolDTO pool,
        ExperimentTechnicalDetailsResponseDTO technicalDetails
) {}
