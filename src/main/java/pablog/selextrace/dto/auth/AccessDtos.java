package pablog.selextrace.dto.auth;

import pablog.selextrace.model.auth.ResourceAccessLevel;

public class AccessDtos {

    public record ExperimentAccessGrantDTO(
            String userId,
            String email,
            String username,
            ResourceAccessLevel accessLevel
    ) {}
}