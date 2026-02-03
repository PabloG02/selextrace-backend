package pablog.aptasuite.dto;

import java.time.Instant;

/**
 * A lightweight summary of an experiment for listing purposes.
 * Contains only essential fields: id, name, description, and creation date.
 */
public record ExperimentSummaryDTO(
                String id,
                String name,
                String description,
                Instant createdAt) {
}
