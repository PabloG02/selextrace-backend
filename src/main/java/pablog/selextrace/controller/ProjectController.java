package pablog.selextrace.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pablog.selextrace.dto.auth.AuthDtos;
import pablog.selextrace.dto.project.ProjectDtos;
import pablog.selextrace.security.CurrentUserService;
import pablog.selextrace.service.ProjectService;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@PreAuthorize("isAuthenticated()")
public class ProjectController {

    private final ProjectService projectService;
    private final CurrentUserService currentUserService;

    public ProjectController(ProjectService projectService, CurrentUserService currentUserService) {
        this.projectService = projectService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<ProjectDtos.ProjectSummaryDTO> listProjects() {
        return projectService.listProjects(currentUserService.requireUser());
    }

    @GetMapping("/{projectId}")
    @PreAuthorize("hasPermission(#projectId, 'project', 'view')")
    public ProjectDtos.ProjectDetailDTO getProject(@PathVariable Long projectId) {
        return projectService.getProject(currentUserService.requireUser(), projectId);
    }

    @PostMapping
    public ProjectDtos.ProjectDetailDTO createProject(@RequestBody AuthDtos.ProjectRequest request) {
        return projectService.createProject(currentUserService.requireUser(), request);
    }

    @PatchMapping("/{projectId}")
    @PreAuthorize("hasPermission(#projectId, 'project', 'manage')")
    public ProjectDtos.ProjectDetailDTO updateProject(
            @PathVariable Long projectId,
            @RequestBody AuthDtos.ProjectRequest request) {
        return projectService.updateProject(currentUserService.requireUser(), projectId, request);
    }

    @PostMapping("/{projectId}/members")
    @PreAuthorize("hasPermission(#projectId, 'project', 'manage')")
    public ProjectDtos.ProjectDetailDTO upsertMembership(
            @PathVariable Long projectId,
            @RequestBody AuthDtos.ProjectMembershipRequest request) {
        return projectService.upsertMembership(currentUserService.requireUser(), projectId, request);
    }

    @DeleteMapping("/{projectId}/members/{userId}")
    @PreAuthorize("hasPermission(#projectId, 'project', 'manage')")
    public ResponseEntity<Void> removeMembership(
            @PathVariable Long projectId,
            @PathVariable String userId) {
        projectService.removeMembership(projectId, userId);
        return ResponseEntity.noContent().build();
    }
}
