package pablog.selextrace.service;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import pablog.selextrace.cluster.HashAptaCluster;
import pablog.selextrace.config.AptaClusterConfiguration;
import pablog.selextrace.domain.cluster.ClusterContainer;
import pablog.selextrace.domain.cluster.InMemoryClusterContainer;
import pablog.selextrace.domain.experiment.Experiment;
import pablog.selextrace.model.ClusterAnalysis;
import pablog.selextrace.repository.ClusterAnalysisRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class AptaClusterService {

    private final ClusterAnalysisRepository analysisRepository;
    private final ExperimentPersistenceService experimentPersistenceService;

    public AptaClusterService(
            ClusterAnalysisRepository analysisRepository,
            ExperimentPersistenceService experimentPersistenceService
    ) {
        this.analysisRepository = analysisRepository;
        this.experimentPersistenceService = experimentPersistenceService;
    }

    public List<ClusterAnalysis> listAnalyses(String experimentId) {
        validateExperimentId(experimentId);
        ensureExperimentExists(experimentId);
        return analysisRepository.findByExperimentId(experimentId);
    }

    public ClusterAnalysis getAnalysis(String experimentId, String analysisId) {
        validateExperimentId(experimentId);
        if (!StringUtils.hasText(analysisId)) {
            throw new IllegalArgumentException("Analysis ID is required");
        }

        return analysisRepository.findByIdAndExperimentId(analysisId, experimentId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Clustering analysis not found"));
    }

    public ClusterAnalysis createAnalysis(String experimentId, AptaClusterConfiguration request) {
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
                experimentId,
                request,
                aptamerToCluster,
                durationMs
        );

        return analysisRepository.save(analysis);
    }

    @Transactional
    public void deleteAnalysis(String experimentId, String analysisId) {
        validateExperimentId(experimentId);
        if (!StringUtils.hasText(analysisId)) {
            throw new IllegalArgumentException("Analysis ID is required");
        }

        ensureExperimentExists(experimentId);
        long deleted = analysisRepository.deleteByIdAndExperimentId(analysisId, experimentId);
        if (deleted == 0) {
            throw new ResponseStatusException(NOT_FOUND, "Clustering analysis not found");
        }
    }

    private void validateExperimentId(String experimentId) {
        if (!StringUtils.hasText(experimentId)) {
            throw new IllegalArgumentException("Experiment id is required");
        }
    }

    private void ensureExperimentExists(String experimentId) {
        if (!experimentPersistenceService.existsExperiment(experimentId)) {
            throw new ResponseStatusException(NOT_FOUND, "Experiment not found");
        }
    }
}
