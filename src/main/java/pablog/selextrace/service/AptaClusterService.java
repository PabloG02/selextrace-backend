package pablog.selextrace.service;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import pablog.selextrace.cluster.HashAptaCluster;
import pablog.selextrace.config.AptaClusterConfiguration;
import pablog.selextrace.domain.cluster.ClusterContainer;
import pablog.selextrace.domain.cluster.InMemoryClusterContainer;
import pablog.selextrace.domain.experiment.Experiment;
import pablog.selextrace.dto.response.ClusterAnalysisDTO;
import pablog.selextrace.model.ClusterAnalysis;
import pablog.selextrace.repository.ClusterAnalysisRepository;
import pablog.selextrace.repository.ExperimentRecordRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class AptaClusterService {

    private final ClusterAnalysisRepository analysisRepository;
    private final ExperimentRecordRepository experimentRecordRepository;
    private final ExperimentPersistenceService experimentPersistenceService;

    public AptaClusterService(
            ClusterAnalysisRepository analysisRepository,
            ExperimentRecordRepository experimentRecordRepository,
            ExperimentPersistenceService experimentPersistenceService
    ) {
        this.analysisRepository = analysisRepository;
        this.experimentRecordRepository = experimentRecordRepository;
        this.experimentPersistenceService = experimentPersistenceService;
    }

    public List<ClusterAnalysisDTO> listAnalyses(Long experimentId) {
        validateExperimentId(experimentId);
        ensureExperimentExists(experimentId);
        return analysisRepository.findByExperimentId(experimentId).stream()
                .map(ClusterAnalysisDTO::from)
                .toList();
    }

    public ClusterAnalysisDTO getAnalysis(Long experimentId, Long analysisId) {
        validateExperimentId(experimentId);
        if (analysisId == null) {
            throw new IllegalArgumentException("Analysis ID is required");
        }

        return analysisRepository.findByIdAndExperimentId(analysisId, experimentId)
                .map(ClusterAnalysisDTO::from)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Clustering analysis not found"));
    }

    public ClusterAnalysisDTO createAnalysis(Long experimentId, AptaClusterConfiguration request) {
        validateExperimentId(experimentId);

        Experiment experiment = experimentPersistenceService.findExperimentById(experimentId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Experiment not found"));

        AptaClusterConfiguration config = AptaClusterConfiguration.mergeWithDefaults(request);

        ClusterContainer clusterContainer = new InMemoryClusterContainer(experiment.getPool());
        HashAptaCluster aptaCluster = new HashAptaCluster(experiment, config, clusterContainer);

        long startMs = System.currentTimeMillis();
        aptaCluster.performLSH();
        long durationMs = System.currentTimeMillis() - startMs;

        Map<Integer, Integer> aptamerToCluster = StreamSupport.stream(clusterContainer.iterator().spliterator(), false)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        ClusterAnalysis analysis = new ClusterAnalysis(
                experimentRecordRepository.getReferenceById(experimentId),
                request,
                aptamerToCluster,
                durationMs
        );

        return ClusterAnalysisDTO.from(analysisRepository.save(analysis));
    }

    @Transactional
    public void deleteAnalysis(Long experimentId, Long analysisId) {
        validateExperimentId(experimentId);
        if (analysisId == null) {
            throw new IllegalArgumentException("Analysis ID is required");
        }

        ensureExperimentExists(experimentId);
        long deleted = analysisRepository.deleteByIdAndExperimentId(analysisId, experimentId);
        if (deleted == 0) {
            throw new ResponseStatusException(NOT_FOUND, "Clustering analysis not found");
        }
    }

    private void validateExperimentId(Long experimentId) {
        if (experimentId == null) {
            throw new IllegalArgumentException("Experiment id is required");
        }
    }

    private void ensureExperimentExists(Long experimentId) {
        if (!experimentPersistenceService.existsExperiment(experimentId)) {
            throw new ResponseStatusException(NOT_FOUND, "Experiment not found");
        }
    }
}
