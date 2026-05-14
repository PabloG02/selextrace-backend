package pablog.selextrace.dto.project;

import pablog.selextrace.model.auth.ResourceAccessLevel;
import pablog.selextrace.model.persistence.ProjectMembershipRecord;
import pablog.selextrace.model.persistence.ProjectRecord;

import java.time.Instant;
import java.util.List;

public class ProjectDtos {

    public record ProjectReferenceDTO(
            Long id,
            String name
    ) {
        public static ProjectReferenceDTO from(ProjectRecord project) {
            if (project == null) return null;
            return new ProjectReferenceDTO(project.getId(), project.getName());
        }
    }

    public record ProjectSummaryDTO(
            Long id,
            String name,
            String description,
            Instant createdAt,
            ResourceAccessLevel accessLevel
    ) {
        public static ProjectSummaryDTO from(ProjectRecord project, ResourceAccessLevel accessLevel) {
            if (project == null) return null;
            return new ProjectSummaryDTO(
                    project.getId(),
                    project.getName(),
                    project.getDescription(),
                    project.getCreatedAt(),
                    accessLevel
            );
        }
    }

    public record ProjectDetailDTO(
            Long id,
            String name,
            String description,
            Instant createdAt,
            ResourceAccessLevel accessLevel,
            List<ProjectMembershipDTO> memberships
    ) {
        public static ProjectDetailDTO from(ProjectRecord project, ResourceAccessLevel accessLevel, List<ProjectMembershipDTO> memberships) {
            if (project == null) return null;
            return new ProjectDetailDTO(
                    project.getId(),
                    project.getName(),
                    project.getDescription(),
                    project.getCreatedAt(),
                    accessLevel,
                    memberships
            );
        }
    }

    public record ProjectMembershipDTO(
            String userId,
            String email,
            String username,
            ResourceAccessLevel accessLevel
    ) {
        public static ProjectMembershipDTO from(ProjectMembershipRecord membership) {
            if (membership == null || membership.getUser() == null) return null;
            return new ProjectMembershipDTO(
                    membership.getUser().getId(),
                    membership.getUser().getEmail(),
                    membership.getUser().getUsername(),
                    membership.getAccessLevel()
            );
        }
    }
}