package pablog.selextrace.dto;

import pablog.selextrace.dto.project.ProjectDtos;
import pablog.selextrace.model.auth.ResourceAccessLevel;

import java.time.Instant;

/**
 * A lightweight summary of an experiment for listing purposes.
 * Contains only essential fields: id, name, description, and creation date.
 */
public record ExperimentSummaryDTO(
        Long id,
        String name,
        String description,
        Instant createdAt,
        ProjectDtos.ProjectReferenceDTO project,
        ResourceAccessLevel accessLevel
) {}
