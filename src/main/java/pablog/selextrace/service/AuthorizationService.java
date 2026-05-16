package pablog.selextrace.service;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import pablog.selextrace.model.auth.ResourceAccessLevel;
import pablog.selextrace.model.auth.SystemRole;
import pablog.selextrace.model.persistence.AppUserRecord;
import pablog.selextrace.model.persistence.ExperimentPermissionRecord;
import pablog.selextrace.model.persistence.ExperimentRecord;
import pablog.selextrace.model.persistence.ProjectMembershipRecord;
import pablog.selextrace.repository.ExperimentPermissionRecordRepository;
import pablog.selextrace.repository.ExperimentRecordRepository;
import pablog.selextrace.repository.ProjectMembershipRepository;

import java.util.Optional;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class AuthorizationService {

    private final ProjectMembershipRepository projectMembershipRepository;
    private final ExperimentPermissionRecordRepository experimentPermissionRecordRepository;
    private final ExperimentRecordRepository experimentRecordRepository;

    public AuthorizationService(
            ProjectMembershipRepository projectMembershipRepository,
            ExperimentPermissionRecordRepository experimentPermissionRecordRepository,
            ExperimentRecordRepository experimentRecordRepository
    ) {
        this.projectMembershipRepository = projectMembershipRepository;
        this.experimentPermissionRecordRepository = experimentPermissionRecordRepository;
        this.experimentRecordRepository = experimentRecordRepository;
    }

    public boolean isAdminUser(AppUserRecord user) {
        return user.getSystemRole() == SystemRole.ADMIN;
    }

    public boolean canViewProject(AppUserRecord user, Long projectId) {
        return isAdminUser(user)
                || projectMembershipRepository.findByProject_IdAndUser_Id(projectId, user.getId()).isPresent();
    }

    public boolean canManageProject(AppUserRecord user, Long projectId) {
        return isAdminUser(user)
                || projectMembershipRepository.findByProject_IdAndUser_Id(projectId, user.getId())
                .map(ProjectMembershipRecord::getAccessLevel)
                .map(ResourceAccessLevel::allowsManagement)
                .orElse(false);
    }

    public boolean canViewExperiment(AppUserRecord user, Long experimentId) {
        if (isAdminUser(user)) {
            return true;
        }

        Optional<ExperimentRecord> experiment = experimentRecordRepository.findById(experimentId);
        if (experiment.isEmpty()) {
            return false;
        }

        ExperimentRecord record = experiment.get();
        if (record.getProject() != null && canViewProject(user, record.getProject().getId())) {
            return true;
        }

        return experimentPermissionRecordRepository.findByExperiment_IdAndUser_Id(experimentId, user.getId()).isPresent();
    }

    public boolean canManageExperiment(AppUserRecord user, Long experimentId) {
        if (isAdminUser(user)) {
            return true;
        }

        Optional<ExperimentRecord> experiment = experimentRecordRepository.findById(experimentId);
        if (experiment.isEmpty()) {
            return false;
        }

        ExperimentRecord record = experiment.get();
        if (record.getProject() != null && canManageProject(user, record.getProject().getId())) {
            return true;
        }

        return experimentPermissionRecordRepository.findByExperiment_IdAndUser_Id(experimentId, user.getId())
                .map(ExperimentPermissionRecord::getAccessLevel)
                .map(ResourceAccessLevel::allowsManagement)
                .orElse(false);
    }

    public Optional<ResourceAccessLevel> getProjectAccessLevel(AppUserRecord user, Long projectId) {
        if (isAdminUser(user)) {
            return Optional.of(ResourceAccessLevel.MANAGER);
        }
        return projectMembershipRepository.findByProject_IdAndUser_Id(projectId, user.getId())
                .map(ProjectMembershipRecord::getAccessLevel);
    }

    public Optional<ResourceAccessLevel> getExperimentAccessLevel(AppUserRecord user, ExperimentRecord experiment) {
        if (isAdminUser(user)) {
            return Optional.of(ResourceAccessLevel.MANAGER);
        }
        if (experiment.getProject() != null) {
            Optional<ResourceAccessLevel> inherited = getProjectAccessLevel(user, experiment.getProject().getId());
            if (inherited.isPresent()) {
                return inherited;
            }
        }
        return experimentPermissionRecordRepository.findByExperiment_IdAndUser_Id(experiment.getId(), user.getId())
                .map(ExperimentPermissionRecord::getAccessLevel);
    }


    public void assertCanManageProject(AppUserRecord user, Long projectId) {
        if (!canManageProject(user, projectId)) {
            throw new ResponseStatusException(FORBIDDEN, "You do not have permission to manage this project");
        }
    }

    public void assertCanManageExperiment(AppUserRecord user, Long experimentId) {
        if (!experimentRecordRepository.existsById(experimentId)) {
            throw new ResponseStatusException(NOT_FOUND, "Experiment not found");
        }
        if (!canManageExperiment(user, experimentId)) {
            throw new ResponseStatusException(FORBIDDEN, "You do not have permission to manage this experiment");
        }
    }
}
