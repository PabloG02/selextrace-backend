package pablog.selextrace.service;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import pablog.selextrace.config.AptaTraceConfiguration;
import pablog.selextrace.domain.experiment.Experiment;
import pablog.selextrace.domain.pool.StructurePool;
import pablog.selextrace.model.MotifAnalysis;
import pablog.selextrace.motif.AptaTraceMotif;
import pablog.selextrace.motif.MotifAnalysisRun;
import pablog.selextrace.repository.MotifAnalysisRepository;

import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class MotifAnalysisService {

    private final MotifAnalysisRepository motifAnalysisRepository;
    private final ExperimentPersistenceService experimentPersistenceService;
    private final PredictionService predictionService;
    private final AptaTraceMotif aptaTraceMotif;

    public MotifAnalysisService(
            MotifAnalysisRepository motifAnalysisRepository,
            ExperimentPersistenceService experimentPersistenceService,
            PredictionService predictionService,
            AptaTraceMotif aptaTraceMotif
    ) {
        this.motifAnalysisRepository = motifAnalysisRepository;
        this.experimentPersistenceService = experimentPersistenceService;
        this.predictionService = predictionService;
        this.aptaTraceMotif = aptaTraceMotif;
    }

    public List<MotifAnalysis> listAnalyses(String experimentId) {
        validateExperimentId(experimentId);
        ensureExperimentExists(experimentId);
        return motifAnalysisRepository.findByExperimentId(experimentId);
    }

    public MotifAnalysis getAnalysis(String experimentId, String analysisId) {
        validateExperimentId(experimentId);
        if (!StringUtils.hasText(analysisId)) {
            throw new IllegalArgumentException("Analysis ID is required");
        }

        return motifAnalysisRepository.findByIdAndExperimentId(analysisId, experimentId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Motif analysis not found"));
    }

    public MotifAnalysis createAnalysis(String experimentId, AptaTraceConfiguration request) {
        validateExperimentId(experimentId);

        Experiment experiment = experimentPersistenceService.findExperimentById(experimentId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Experiment not found"));

        AptaTraceConfiguration config = AptaTraceConfiguration.mergeWithDefaults(request);
        StructurePool structurePool = predictionService.getStructurePool(experimentId, true);

        long startMs = System.currentTimeMillis();
        MotifAnalysisRun run = aptaTraceMotif.run(experiment, structurePool, config);
        long durationMs = System.currentTimeMillis() - startMs;

        MotifAnalysis analysis = new MotifAnalysis(
                experimentId,
                config,
                run.roundNames(),
                run.profiles(),
                run.significantKmerCount(),
                run.lastRoundCount(),
                durationMs
        );

        return motifAnalysisRepository.save(analysis);
    }

    @Transactional
    public void deleteAnalysis(String experimentId, String analysisId) {
        validateExperimentId(experimentId);
        if (!StringUtils.hasText(analysisId)) {
            throw new IllegalArgumentException("Analysis ID is required");
        }

        ensureExperimentExists(experimentId);
        long deleted = motifAnalysisRepository.deleteByIdAndExperimentId(analysisId, experimentId);
        if (deleted == 0) {
            throw new ResponseStatusException(NOT_FOUND, "Motif analysis not found");
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