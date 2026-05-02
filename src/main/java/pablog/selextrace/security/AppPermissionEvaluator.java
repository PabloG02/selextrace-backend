package pablog.selextrace.security;

import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import pablog.selextrace.model.persistence.AppUserRecord;
import pablog.selextrace.repository.AppUserRepository;
import pablog.selextrace.service.AuthorizationService;

import java.io.Serializable;

@Component
public class AppPermissionEvaluator implements PermissionEvaluator {

    private final AuthorizationService authorizationService;
    private final AppUserRepository appUserRepository;

    public AppPermissionEvaluator(AuthorizationService authorizationService, AppUserRepository appUserRepository) {
        this.authorizationService = authorizationService;
        this.appUserRepository = appUserRepository;
    }

    // Used when the whole object is passed: hasPermission(#experiment, 'VIEW')
    @Override
    public boolean hasPermission(Authentication auth, Object targetDomainObject, Object permission) {
        // You can implement this later if you start passing full entity objects to @PreAuthorize
        return false;
    }

    // Used when the ID and Type are passed: hasPermission(#experimentId, 'Experiment', 'VIEW')
    @Override
    public boolean hasPermission(Authentication auth, Serializable targetId, String targetType, Object permission) {
        if (!(auth.getPrincipal() instanceof AuthenticatedUser principal) || !(targetId instanceof String id) || !(permission instanceof String)) {
            return false;
        }

        AppUserRecord user = appUserRepository.findById(principal.getUserId()).orElse(null);
        if (user == null) return false;

        return switch ((targetType + ":" + permission).toLowerCase()) {
            case "experiment:view" -> authorizationService.canViewExperiment(user, id);
            case "experiment:manage" -> authorizationService.canManageExperiment(user, id);
            case "project:view" -> authorizationService.canViewProject(user, id);
            case "project:manage" -> authorizationService.canManageProject(user, id);
            default -> false;
        };
    }
}