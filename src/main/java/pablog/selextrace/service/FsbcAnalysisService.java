package pablog.selextrace.service;

import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import pablog.selextrace.config.FsbcConfiguration;
import pablog.selextrace.domain.experiment.Experiment;
import pablog.selextrace.domain.experiment.SelectionCycle;
import pablog.selextrace.fsbc.FsbcEngine;
import pablog.selextrace.dto.response.FsbcAnalysisDTO;
import pablog.selextrace.model.FsbcAnalysis;
import pablog.selextrace.repository.ExperimentRecordRepository;
import pablog.selextrace.repository.FsbcAnalysisRepository;

import java.util.Comparator;
import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class FsbcAnalysisService {

    private final FsbcAnalysisRepository fsbcAnalysisRepository;
    private final ExperimentRecordRepository experimentRecordRepository;
    private final ExperimentPersistenceService experimentPersistenceService;
    private final FsbcEngine fsbcEngine;

    public FsbcAnalysisService(
            FsbcAnalysisRepository fsbcAnalysisRepository,
            ExperimentRecordRepository experimentRecordRepository,
            ExperimentPersistenceService experimentPersistenceService,
            FsbcEngine fsbcEngine
    ) {
        this.fsbcAnalysisRepository = fsbcAnalysisRepository;
        this.experimentRecordRepository = experimentRecordRepository;
        this.experimentPersistenceService = experimentPersistenceService;
        this.fsbcEngine = fsbcEngine;
    }

    public List<FsbcAnalysisDTO> listAnalyses(Long experimentId) {
        validateExperimentId(experimentId);
        ensureExperimentExists(experimentId);
        return fsbcAnalysisRepository.findByExperimentId(experimentId).stream()
                .map(FsbcAnalysisDTO::from)
                .toList();
    }

    public FsbcAnalysisDTO getAnalysis(Long experimentId, Long analysisId) {
        validateExperimentId(experimentId);
        if (analysisId == null) {
            throw new IllegalArgumentException("Analysis ID is required");
        }

        return fsbcAnalysisRepository.findByIdAndExperimentId(analysisId, experimentId)
                .map(FsbcAnalysisDTO::from)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "FSBC analysis not found"));
    }

    public FsbcAnalysisDTO createAnalysis(Long experimentId, FsbcConfiguration request) {
        validateExperimentId(experimentId);

        Experiment experiment = experimentPersistenceService.findExperimentById(experimentId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Experiment not found"));

        FsbcConfiguration merged = FsbcConfiguration.mergeWithDefaults(request);
        validateConfiguration(merged);

        SelectionCycle selectionCycle = resolveSelectionCycle(experiment, merged.selectionCycleRound());
        FsbcConfiguration requestConfig = new FsbcConfiguration(
                selectionCycle.getRound(),
                merged.minLength(),
                merged.maxLength(),
                merged.rnaSequence()
        );

        long startMs = System.currentTimeMillis();
        FsbcEngine.FsbcRunResult result = fsbcEngine.run(experiment, selectionCycle, requestConfig);
        long durationMs = System.currentTimeMillis() - startMs;

        FsbcAnalysis analysis = new FsbcAnalysis(
                experimentRecordRepository.getReferenceById(experimentId),
                requestConfig,
                result.aptamerToCluster(),
                result.rankedStrings(),
                result.clusterSeeds(),
                result.totalSequenceCount(),
                result.uniqueSequenceCount(),
                durationMs
        );

        return FsbcAnalysisDTO.from(fsbcAnalysisRepository.save(analysis));
    }

    @Transactional
    public void deleteAnalysis(Long experimentId, Long analysisId) {
        validateExperimentId(experimentId);
        if (analysisId == null) {
            throw new IllegalArgumentException("Analysis ID is required");
        }

        ensureExperimentExists(experimentId);
        long deleted = fsbcAnalysisRepository.deleteByIdAndExperimentId(analysisId, experimentId);
        if (deleted == 0) {
            throw new ResponseStatusException(NOT_FOUND, "FSBC analysis not found");
        }
    }

    private SelectionCycle resolveSelectionCycle(Experiment experiment, Integer requestedRound) {
        List<SelectionCycle> positiveCycles = experiment.getSelectionCycles().stream()
                .filter(cycle -> !cycle.isControlSelection() && !cycle.isCounterSelection())
                .sorted(Comparator.comparingInt(SelectionCycle::getRound))
                .toList();

        if (positiveCycles.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Experiment has no positive selection cycles");
        }

        int targetRound = requestedRound != null
                ? requestedRound
                : positiveCycles.get(positiveCycles.size() - 1).getRound();

        return positiveCycles.stream()
                .filter(cycle -> cycle.getRound() == targetRound)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Selection cycle round " + targetRound + " is not available for FSBC"
                ));
    }

    private void validateConfiguration(FsbcConfiguration config) {
        if (config.minLength() == null || config.minLength() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minLength must be greater than 0");
        }
        if (config.maxLength() == null || config.maxLength() < config.minLength()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "maxLength must be greater than or equal to minLength");
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
