package pablog.selextrace.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pablog.selextrace.dto.auth.AccessDtos;
import pablog.selextrace.dto.auth.AuthDtos;
import pablog.selextrace.security.CurrentUserService;
import pablog.selextrace.service.ExperimentPermissionsService;

import java.util.List;

@RestController
@RequestMapping("/api/experiments/{experimentId}/permissions")
public class ExperimentPermissionsController {

    private final ExperimentPermissionsService experimentPermissionsService;
    private final CurrentUserService currentUserService;

    public ExperimentPermissionsController(
            ExperimentPermissionsService experimentPermissionsService,
            CurrentUserService currentUserService
    ) {
        this.experimentPermissionsService = experimentPermissionsService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<AccessDtos.ExperimentAccessGrantDTO> listAccess(@PathVariable Long experimentId) {
        return experimentPermissionsService.listAccess(currentUserService.requireUser(), experimentId);
    }

    @PostMapping
    public List<AccessDtos.ExperimentAccessGrantDTO> upsertAccess(
            @PathVariable Long experimentId,
            @RequestBody AuthDtos.ExperimentAccessGrantRequest request
    ) {
        return experimentPermissionsService.upsertAccess(currentUserService.requireUser(), experimentId, request);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> removeAccess(
            @PathVariable Long experimentId,
            @PathVariable String userId
    ) {
        experimentPermissionsService.removeAccess(currentUserService.requireUser(), experimentId, userId);
        return ResponseEntity.noContent().build();
    }
}
