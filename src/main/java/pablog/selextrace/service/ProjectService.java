package pablog.selextrace.service;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import pablog.selextrace.dto.auth.AuthDtos;
import pablog.selextrace.dto.project.ProjectDtos;
import pablog.selextrace.model.auth.ResourceAccessLevel;
import pablog.selextrace.model.persistence.AppUserRecord;
import pablog.selextrace.model.persistence.ProjectMembershipRecord;
import pablog.selextrace.model.persistence.ProjectRecord;
import pablog.selextrace.repository.AppUserRepository;
import pablog.selextrace.repository.ExperimentRecordRepository;
import pablog.selextrace.repository.ProjectMembershipRepository;
import pablog.selextrace.repository.ProjectRepository;

import java.util.List;
import java.util.Locale;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMembershipRepository projectMembershipRepository;
    private final ExperimentRecordRepository experimentRecordRepository;
    private final AppUserRepository userRepository;
    private final AuthorizationService authorizationService;

    public ProjectService(
            ProjectRepository projectRepository,
            ProjectMembershipRepository projectMembershipRepository,
            ExperimentRecordRepository experimentRecordRepository,
            AppUserRepository userRepository,
            AuthorizationService authorizationService
    ) {
        this.projectRepository = projectRepository;
        this.projectMembershipRepository = projectMembershipRepository;
        this.experimentRecordRepository = experimentRecordRepository;
        this.userRepository = userRepository;
        this.authorizationService = authorizationService;
    }

    public List<ProjectDtos.ProjectSummaryDTO> listProjects(AppUserRecord currentUser) {
        // If the user is an admin, fetch all projects and assign them MANAGER access
        if (authorizationService.isAdminUser(currentUser)) {
            return projectRepository.findAll()
                    .stream()
                    .map(project -> ProjectDtos.ProjectSummaryDTO.from(project, ResourceAccessLevel.MANAGER))
                    .toList();
        }

        // If it's a regular user, query their memberships directly.
        // This is extremely efficient because the database handles the filtering.
        return projectMembershipRepository.findAllByUser_Id(currentUser.getId())
                .stream()
                .map(membership -> ProjectDtos.ProjectSummaryDTO.from(membership.getProject(), membership.getAccessLevel()))
                .toList();
    }

    public ProjectDtos.ProjectDetailDTO getProject(AppUserRecord currentUser, Long projectId) {
        ProjectRecord project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Project not found"));

        ResourceAccessLevel accessLevel = authorizationService.getProjectAccessLevel(currentUser, projectId).orElse(ResourceAccessLevel.VIEWER);
        List<ProjectDtos.ProjectMembershipDTO> memberships = listMemberships(projectId);

        return ProjectDtos.ProjectDetailDTO.from(project, accessLevel, memberships);
    }

    @Transactional
    public ProjectDtos.ProjectDetailDTO createProject(AppUserRecord currentUser, AuthDtos.ProjectRequest request) {
        ProjectRecord project = new ProjectRecord();
        project.setName(requiredValue(request.name(), "Project name is required"));
        project.setDescription(blankToNull(request.description()));
        project.setCreatedByUser(userRepository.getReferenceById(currentUser.getId()));
        projectRepository.save(project);

        ProjectMembershipRecord membership = new ProjectMembershipRecord();
        membership.setProject(project);
        membership.setUser(userRepository.getReferenceById(currentUser.getId()));
        membership.setAccessLevel(ResourceAccessLevel.MANAGER);
        membership.setGrantedByUser(userRepository.getReferenceById(currentUser.getId()));
        projectMembershipRepository.save(membership);
        return getProject(currentUser, project.getId());
    }

    @Transactional
    public ProjectDtos.ProjectDetailDTO updateProject(
            AppUserRecord currentUser,
            Long projectId,
            AuthDtos.ProjectRequest request
    ) {
        ProjectRecord project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Project not found"));
        project.setName(requiredValue(request.name(), "Project name is required"));
        project.setDescription(blankToNull(request.description()));
        projectRepository.save(project);
        return getProject(currentUser, projectId);
    }

    @Transactional
    public void deleteProject(Long projectId) {
        ProjectRecord project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Project not found"));

        if (experimentRecordRepository.existsByProject_Id(projectId)) {
            throw new ResponseStatusException(CONFLICT, "Projects with experiments cannot be deleted");
        }

        projectMembershipRepository.deleteByProject_Id(projectId);
        projectRepository.delete(project);
    }

    @Transactional
    public ProjectDtos.ProjectDetailDTO upsertMembership(
            AppUserRecord currentUser,
            Long projectId,
            AuthDtos.ProjectMembershipRequest request
    ) {
        ProjectRecord project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Project not found"));

        AppUserRecord targetUser = resolveTargetUser(request.userId(), request.email());
        ResourceAccessLevel accessLevel = request.accessLevel() == null ? ResourceAccessLevel.VIEWER : request.accessLevel();

        ProjectMembershipRecord membership = projectMembershipRepository.findByProject_IdAndUser_Id(projectId, targetUser.getId())
                .orElseGet(ProjectMembershipRecord::new);
        membership.setProject(project);
        membership.setUser(targetUser);
        membership.setAccessLevel(accessLevel);
        membership.setGrantedByUser(userRepository.getReferenceById(currentUser.getId()));
        projectMembershipRepository.save(membership);
        return getProject(currentUser, projectId);
    }

    @Transactional
    public void removeMembership(Long projectId, String userId) {
        projectMembershipRepository.deleteByProject_IdAndUser_Id(projectId, userId);
    }

    public List<ProjectDtos.ProjectMembershipDTO> listMemberships(Long projectId) {
        return projectMembershipRepository.findAllByProject_Id(projectId)
                .stream()
                .map(ProjectDtos.ProjectMembershipDTO::from)
                .toList();
    }

    private String requiredValue(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, message);
        }
        return value.trim();
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private AppUserRecord resolveTargetUser(String userId, String email) {
        if (StringUtils.hasText(userId)) {
            return userRepository.findById(userId.trim())
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "User not found"));
        }

        if (StringUtils.hasText(email)) {
            return userRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "User not found"));
        }

        throw new ResponseStatusException(BAD_REQUEST, "User id or email is required");
    }
}
