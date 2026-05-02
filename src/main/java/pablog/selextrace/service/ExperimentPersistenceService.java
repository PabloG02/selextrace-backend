package pablog.selextrace.service;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import pablog.selextrace.domain.experiment.Experiment;
import pablog.selextrace.dto.ExperimentSummaryDTO;
import pablog.selextrace.dto.auth.AccessDtos;
import pablog.selextrace.dto.project.ProjectDtos;
import pablog.selextrace.dto.response.ExperimentDTO;
import pablog.selextrace.model.auth.ResourceAccessLevel;
import pablog.selextrace.model.persistence.AppUserRecord;
import pablog.selextrace.model.persistence.ExperimentPermissionRecord;
import pablog.selextrace.model.persistence.ExperimentMetadataRecord;
import pablog.selextrace.model.persistence.ExperimentRecord;
import pablog.selextrace.model.persistence.ProjectRecord;
import pablog.selextrace.model.persistence.SelectionCycleRecord;
import pablog.selextrace.repository.AptamerRecordRepository;
import pablog.selextrace.repository.ClusterAnalysisRepository;
import pablog.selextrace.repository.AppUserRepository;
import pablog.selextrace.repository.ExperimentPermissionRecordRepository;
import pablog.selextrace.repository.ExperimentMetadataRecordRepository;
import pablog.selextrace.repository.ExperimentRecordRepository;
import pablog.selextrace.repository.FsbcAnalysisRepository;
import pablog.selextrace.repository.MotifAnalysisRepository;
import pablog.selextrace.repository.ProjectRepository;
import pablog.selextrace.repository.SelectionCycleRecordRepository;
import pablog.selextrace.service.mapper.ExperimentRecordMapper;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ExperimentPersistenceService {

    private final ExperimentRecordRepository experimentRecordRepository;
    private final ExperimentMetadataRecordRepository experimentMetadataRecordRepository;
    private final SelectionCycleRecordRepository selectionCycleRecordRepository;
    private final AptamerRecordRepository aptamerRecordRepository;
    private final ClusterAnalysisRepository clusterAnalysisRepository;
    private final FsbcAnalysisRepository fsbcAnalysisRepository;
    private final MotifAnalysisRepository motifAnalysisRepository;
    private final ExperimentPermissionRecordRepository experimentPermissionRecordRepository;
    private final ProjectRepository projectRepository;
    private final ExperimentRecordMapper experimentRecordMapper;
    private final AuthorizationService authorizationService;
    private final ExperimentPermissionsService experimentPermissionsService;

    public ExperimentPersistenceService(
            ExperimentRecordRepository experimentRecordRepository,
            ExperimentMetadataRecordRepository experimentMetadataRecordRepository,
            SelectionCycleRecordRepository selectionCycleRecordRepository,
            AptamerRecordRepository aptamerRecordRepository,
            ClusterAnalysisRepository clusterAnalysisRepository,
            FsbcAnalysisRepository fsbcAnalysisRepository,
            MotifAnalysisRepository motifAnalysisRepository,
            ExperimentPermissionRecordRepository experimentPermissionRecordRepository,
            ProjectRepository projectRepository,
            ExperimentRecordMapper experimentRecordMapper,
            AuthorizationService authorizationService,
            ExperimentPermissionsService experimentPermissionsService
    ) {
        this.experimentRecordRepository = experimentRecordRepository;
        this.experimentMetadataRecordRepository = experimentMetadataRecordRepository;
        this.selectionCycleRecordRepository = selectionCycleRecordRepository;
        this.aptamerRecordRepository = aptamerRecordRepository;
        this.clusterAnalysisRepository = clusterAnalysisRepository;
        this.fsbcAnalysisRepository = fsbcAnalysisRepository;
        this.motifAnalysisRepository = motifAnalysisRepository;
        this.experimentPermissionRecordRepository = experimentPermissionRecordRepository;
        this.projectRepository = projectRepository;
        this.experimentRecordMapper = experimentRecordMapper;
        this.authorizationService = authorizationService;
        this.experimentPermissionsService = experimentPermissionsService;
    }

    @Transactional
    public ExperimentDTO persistExperiment(ExperimentRecord experimentRecord, AppUserRecord createdByUser, String requestedProjectId) {
        Objects.requireNonNull(experimentRecord, "experimentRecord is required");
        Objects.requireNonNull(createdByUser, "createdByUser is required");

        ProjectRecord project = resolveProject(experimentRecord, createdByUser, requestedProjectId);
        experimentRecord.setProject(project);

        String experimentId = experimentRecord.getId() == null || experimentRecord.getId().isBlank()
                ? UUID.randomUUID().toString()
                : experimentRecord.getId();

        experimentRecord.setId(experimentId);
        experimentRecord.setCreatedByUser(createdByUser);

        ExperimentMetadataRecord metadataRecord = experimentRecord.getMetadataRecord();
        metadataRecord.setExperimentId(experimentId);

        Set<SelectionCycleRecord> selectionCycles = experimentRecord.getSelectionCycleRecords();
        for (SelectionCycleRecord cycle : selectionCycles) {
            cycle.setExperimentId(experimentId);
        }

        // TODO: hack -> FIX IT
        experimentRecord.setMetadataRecord(null);
        experimentRecord.setSelectionCycleRecords(new LinkedHashSet<>());

        selectionCycleRecordRepository.deleteByIdExperimentId(experimentId);

        experimentRecordRepository.save(experimentRecord);
        experimentMetadataRecordRepository.save(metadataRecord);

        if (!selectionCycles.isEmpty()) {
            selectionCycleRecordRepository.saveAll(selectionCycles);
        }

        if (project == null) {
            experimentPermissionsService.grantAccess(experimentId, createdByUser.getId(), ResourceAccessLevel.MANAGER, createdByUser.getId());
        }

        return findExperimentResponseById(experimentId, createdByUser)
                .orElseThrow(() -> new IllegalStateException("Persisted experiment could not be reloaded"));
    }

    @Transactional
    public ExperimentDTO transferExperimentToProject(AppUserRecord currentUser, String experimentId, String targetProjectId) {
        String projectId = requireProjectId(targetProjectId);
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

    public Optional<ExperimentDTO> findExperimentResponseById(String experimentId, AppUserRecord currentUser) {
        return experimentRecordRepository.findById(experimentId)
                .filter(record -> authorizationService.canViewExperiment(currentUser, record.getId()))
                .map(record -> experimentRecordMapper.toExperimentDTO(
                        record,
                        toProjectReference(projectIdOf(record.getProject())),
                        authorizationService.getExperimentAccessLevel(currentUser, record).orElse(ResourceAccessLevel.VIEWER)
                ));
    }

    public Optional<Experiment> findExperimentById(String experimentId) {
        return experimentRecordRepository.findById(experimentId)
                .map(experimentRecordMapper::toExperiment);
    }

    public boolean existsExperiment(String experimentId) {
        return experimentRecordRepository.existsById(experimentId);
    }

    @Transactional
    public boolean deleteExperiment(String experimentId) {
        if (experimentId == null || experimentId.isBlank()) {
            return false;
        }
        if (!experimentRecordRepository.existsById(experimentId)) {
            return false;
        }

        // Remove dependent entities first to avoid foreign key violations.
        clusterAnalysisRepository.deleteByExperimentId(experimentId);
        fsbcAnalysisRepository.deleteByExperimentId(experimentId);
        motifAnalysisRepository.deleteByExperimentId(experimentId);

        selectionCycleRecordRepository.deleteByIdExperimentId(experimentId);
        aptamerRecordRepository.deleteByIdExperimentId(experimentId);
        experimentPermissionRecordRepository.deleteAllByExperiment_Id(experimentId);
        experimentMetadataRecordRepository.deleteById(experimentId);
        experimentRecordRepository.deleteById(experimentId);
        return true;
    }

    private ProjectDtos.ProjectReferenceDTO toProjectReference(String projectId) {
        if (projectId == null || projectId.isBlank()) {
            return null;
        }
        return projectRepository.findById(projectId)
                .map(ProjectDtos.ProjectReferenceDTO::from)
                .orElse(null);
    }

    private ProjectRecord resolveProject(ExperimentRecord experimentRecord, AppUserRecord createdByUser, String requestedProjectId) {
        String projectId = firstNonBlank(
                requestedProjectId,
                projectIdOf(experimentRecord.getProject())
        );
        if (projectId == null) {
            return null;
        }
        return loadProject(projectId);
    }

    private ProjectRecord loadProject(String projectId) {
        String normalizedProjectId = requireProjectId(projectId);
        return projectRepository.findById(normalizedProjectId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Project not found"));
    }

    private String projectIdOf(ProjectRecord project) {
        return project == null ? null : project.getId();
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary.trim();
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback.trim();
        }
        return null;
    }

    private String requireProjectId(String projectId) {
        if (projectId == null || projectId.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "A target project is required");
        }
        return projectId.trim();
    }

}
