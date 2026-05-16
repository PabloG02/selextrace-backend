package pablog.selextrace.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pablog.selextrace.domain.experiment.Experiment;
import pablog.selextrace.dto.ExperimentSummaryDTO;
import pablog.selextrace.dto.project.ProjectDtos;
import pablog.selextrace.dto.response.ExperimentDTO;
import pablog.selextrace.model.auth.ResourceAccessLevel;
import pablog.selextrace.model.persistence.AppUserRecord;
import pablog.selextrace.model.persistence.AptamerRecord;
import pablog.selextrace.model.persistence.ExperimentRecord;
import pablog.selextrace.model.persistence.ProjectRecord;
import pablog.selextrace.repository.AptamerRecordRepository;
import pablog.selextrace.repository.ExperimentRecordRepository;
import pablog.selextrace.repository.ProjectRepository;
import pablog.selextrace.service.mapper.ExperimentRecordMapper;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ExperimentPersistenceService {

    private final ExperimentRecordRepository experimentRecordRepository;
    private final AptamerRecordRepository aptamerRecordRepository;
    private final ProjectRepository projectRepository;
    private final ExperimentRecordMapper experimentRecordMapper;
    private final AuthorizationService authorizationService;
    private final ExperimentPermissionsService experimentPermissionsService;

    public ExperimentPersistenceService(
            ExperimentRecordRepository experimentRecordRepository,
            AptamerRecordRepository aptamerRecordRepository,
            ProjectRepository projectRepository,
            ExperimentRecordMapper experimentRecordMapper,
            AuthorizationService authorizationService,
            ExperimentPermissionsService experimentPermissionsService
    ) {
        this.experimentRecordRepository = experimentRecordRepository;
        this.aptamerRecordRepository = aptamerRecordRepository;
        this.projectRepository = projectRepository;
        this.experimentRecordMapper = experimentRecordMapper;
        this.authorizationService = authorizationService;
        this.experimentPermissionsService = experimentPermissionsService;
    }

    @Transactional
    public ExperimentDTO persistExperiment(ExperimentRecord experimentRecord, AppUserRecord createdByUser, Long requestedProjectId) {
        Objects.requireNonNull(experimentRecord, "experimentRecord is required");
        Objects.requireNonNull(createdByUser, "createdByUser is required");

        experimentRecord.setCreatedByUser(createdByUser);
        experimentRecord.setProject(resolveProject(experimentRecord, createdByUser, requestedProjectId));

        ExperimentRecord saved = experimentRecordRepository.save(experimentRecord);

        if (experimentRecord.getProject() == null) {
            experimentPermissionsService.grantAccess(saved.getId(), createdByUser.getId(), ResourceAccessLevel.MANAGER, createdByUser.getId());
        }

        return findExperimentResponseById(saved.getId(), createdByUser)
                .orElseThrow(() -> new IllegalStateException("Persisted experiment could not be reloaded"));
    }

    @Transactional
    public ExperimentDTO transferExperimentToProject(AppUserRecord currentUser, Long experimentId, Long targetProjectId) {
        Long projectId = requireProjectId(targetProjectId);
        ProjectRecord project = loadProject(projectId);

        ExperimentRecord experiment = experimentRecordRepository.findById(experimentId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Experiment not found"));

        authorizationService.assertCanManageExperiment(currentUser, experimentId);
        authorizationService.assertCanManageProject(currentUser, projectId);

        experiment.setProject(project);
        experimentRecordRepository.save(experiment);

        return findExperimentResponseById(experimentId, currentUser)
                .orElseThrow(() -> new IllegalStateException("Transferred experiment could not be reloaded"));
    }

    public List<ExperimentSummaryDTO> findExperimentSummaries(AppUserRecord currentUser) {
        return experimentRecordRepository.findAll()
                .stream()
                .filter(record -> authorizationService.canViewExperiment(currentUser, record.getId()))
                .map(record -> experimentRecordMapper.toSummaryDTO(
                        record,
                        toProjectReference(projectIdOf(record.getProject())),
                        authorizationService.getExperimentAccessLevel(currentUser, record).orElse(ResourceAccessLevel.VIEWER)
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<ExperimentDTO> findExperimentResponseById(Long experimentId, AppUserRecord currentUser) {
        return experimentRecordRepository.findDetailedById(experimentId)
                .filter(record -> authorizationService.canViewExperiment(currentUser, record.getId()))
                .map(record -> {
                    Stream<AptamerRecord> aptamerStream = aptamerRecordRepository.streamByExperimentId(experimentId);
                    return experimentRecordMapper.toExperimentDTO(
                            record,
                            aptamerStream,
                            toProjectReference(projectIdOf(record.getProject())),
                            authorizationService.getExperimentAccessLevel(currentUser, record).orElse(ResourceAccessLevel.VIEWER)
                    );
                });
    }

    @Transactional(readOnly = true)
    public Optional<Experiment> findExperimentById(Long experimentId) {
        return experimentRecordRepository.findDetailedById(experimentId)
                .map(record -> {
                    Stream<AptamerRecord> aptamerStream = aptamerRecordRepository.streamByExperimentId(experimentId);
                    return experimentRecordMapper.toExperiment(record, aptamerStream);
                });
    }

    public boolean existsExperiment(Long experimentId) {
        return experimentRecordRepository.existsById(experimentId);
    }

    /// Deletes an experiment and all related data.
    ///
    /// Uses a native SQL DELETE so that the database's `ON DELETE CASCADE`
    /// constraints (declared via `@OnDelete`) handle all child tables
    /// in a single pass, avoiding Hibernate's row-by-row cascade removal.
    @Transactional
    public boolean deleteExperiment(Long experimentId) {
        if (experimentId == null) {
            return false;
        }
        if (!experimentRecordRepository.existsById(experimentId)) {
            return false;
        }

        experimentRecordRepository.bulkDeleteById(experimentId);

        return true;
    }

    private ProjectDtos.ProjectReferenceDTO toProjectReference(Long projectId) {
        if (projectId == null) {
            return null;
        }
        return projectRepository.findById(projectId)
                .map(ProjectDtos.ProjectReferenceDTO::from)
                .orElse(null);
    }

    private ProjectRecord resolveProject(ExperimentRecord experimentRecord, AppUserRecord createdByUser, Long requestedProjectId) {
        Long projectId = firstNonNull(
                requestedProjectId,
                projectIdOf(experimentRecord.getProject())
        );
        if (projectId == null) {
            return null;
        }
        return loadProject(projectId);
    }

    private ProjectRecord loadProject(Long projectId) {
        return projectRepository.findById(requireProjectId(projectId))
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Project not found"));
    }

    private Long projectIdOf(ProjectRecord project) {
        return project == null ? null : project.getId();
    }

    private Long firstNonNull(Long primary, Long fallback) {
        return primary != null ? primary : fallback;
    }

    private Long requireProjectId(Long projectId) {
        if (projectId == null) {
            throw new ResponseStatusException(BAD_REQUEST, "A target project is required");
        }
        return projectId;
    }

}
