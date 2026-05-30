package pablog.selextrace.dto.auth;

import pablog.selextrace.model.auth.IdentityProvider;
import pablog.selextrace.model.auth.ResourceAccessLevel;
import pablog.selextrace.model.auth.SystemRole;

import java.time.Instant;
import java.util.List;

public class AuthDtos {

    public record SignUpRequest(
            String email,
            String username,
            String password
    ) {}

    public record SignInRequest(
            String email,
            String password
    ) {}

    public record ChangePasswordRequest(
            String currentPassword,
            String newPassword
    ) {}

    public record AuthUserResponse(
            String id,
            String email,
            String username,
            SystemRole systemRole,
            boolean mustChangePassword,
            List<IdentityProvider> linkedProviders,
            Instant createdAt
    ) {}

    public record CsrfResponse(
            String headerName,
            String parameterName,
            String token
    ) {}

    public record UserSummaryResponse(
            String id,
            String email,
            String username,
            SystemRole systemRole,
            boolean active,
            boolean mustChangePassword,
            List<IdentityProvider> linkedProviders,
            Instant createdAt
    ) {}

    public record UpdateUserRoleRequest(
            SystemRole systemRole
    ) {}

    public record UpdateUserActiveRequest(
            boolean active
    ) {}

    public record ResetPasswordRequest(
            String newPassword
    ) {}

    public record ProjectRequest(
            String name,
            String description
    ) {}

    public record ProjectMembershipRequest(
            String userId,
            String email,
            ResourceAccessLevel accessLevel
    ) {}

    public record ExperimentAccessGrantRequest(
            String userId,
            String email,
            ResourceAccessLevel accessLevel
    ) {}

    public record ExperimentProjectTransferRequest(
            Long projectId
    ) {}
}
