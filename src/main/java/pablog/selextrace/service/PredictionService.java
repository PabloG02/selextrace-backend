package pablog.selextrace.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import pablog.selextrace.dto.BppmResponseDTO;
import pablog.selextrace.dto.ContextProbabilityResponseDTO;
import pablog.selextrace.dto.MfeResponseDTO;
import pablog.selextrace.domain.experiment.Experiment;
import pablog.selextrace.domain.pool.InMemoryStructurePool;
import pablog.selextrace.domain.pool.StructurePool;
import pablog.selextrace.lib.capr.CapR;
import pablog.selextrace.lib.rnafold.Index;
import pablog.selextrace.lib.rnafold.MFEData;
import pablog.selextrace.lib.rnafold.RNAFoldAPI;
import pablog.selextrace.model.StructurePredictionAnalysis;
import pablog.selextrace.repository.StructurePredictionAnalysisRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class PredictionService {

    private static final Logger log = LoggerFactory.getLogger(PredictionService.class);

    private final StructurePredictionAnalysisRepository structurePredictionAnalysisRepository;
    private final ExperimentPersistenceService experimentPersistenceService;

    public PredictionService(
            StructurePredictionAnalysisRepository structurePredictionAnalysisRepository,
            ExperimentPersistenceService experimentPersistenceService
    ) {
        this.structurePredictionAnalysisRepository = structurePredictionAnalysisRepository;
        this.experimentPersistenceService = experimentPersistenceService;
    }

    public MfeResponseDTO computeMfe(String sequence) {
        byte[] stringBytes = sequence.getBytes();
        final RNAFoldAPI rnafoldapi = new RNAFoldAPI();
        MFEData result = rnafoldapi.getMFE(stringBytes);

        return new MfeResponseDTO(new String(result.structure), result.mfe);
    }

    public BppmResponseDTO computeBppm(String sequence) {
        int length = sequence.length();
        byte[] stringBytes = sequence.getBytes();

        final RNAFoldAPI rnafoldapi = new RNAFoldAPI();
        double[] result = rnafoldapi.getBppm(stringBytes);

        // Ragged upper-triangle matrix
        List<List<Double>> matrix = new ArrayList<>(length);

        for (int i = 0; i < length; i++) {
            List<Double> row = new ArrayList<>(length - i - 1);

            for (int j = i + 1; j < length; j++) {
                int flatIndex = Index.triu(i, j, length);
                row.add(result[flatIndex]);
            }

            matrix.add(row);
        }

        return new BppmResponseDTO(matrix);
    }

    public ContextProbabilityResponseDTO computeContextProbabilities(String sequence) {
        int length = sequence.length();
        byte[] stringBytes = sequence.getBytes();

        final CapR capr = new CapR();
        capr.ComputeStructuralProfile(stringBytes, length);

        double[] raw = capr.getStructuralProfile();

        List<Double> hairpin = new ArrayList<>(length);
        List<Double> bulge = new ArrayList<>(length);
        List<Double> internal = new ArrayList<>(length);
        List<Double> multi = new ArrayList<>(length);
        List<Double> dangling = new ArrayList<>(length);
        List<Double> paired = new ArrayList<>(length);

        for (int index = 0; index < length; index++) {
            double hairpinValue = raw[index];
            double bulgeValue = raw[length + index];
            double internalValue = raw[2 * length + index];
            double multiValue = raw[3 * length + index];
            double danglingValue = raw[4 * length + index];
            double pairedValue = 1 - hairpinValue - bulgeValue - internalValue - multiValue - danglingValue;

            hairpin.add(hairpinValue);
            bulge.add(bulgeValue);
            internal.add(internalValue);
            multi.add(multiValue);
            dangling.add(danglingValue);
            paired.add(pairedValue);
        }

        return new ContextProbabilityResponseDTO(hairpin, bulge, internal, multi, dangling, paired);
    }

    public StructurePool getStructurePool(String experimentId, boolean createIfMissing) {
        validateExperimentId(experimentId);

        Experiment experiment = experimentPersistenceService.findExperimentById(experimentId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Experiment not found"));

        StructurePredictionAnalysis analysis = structurePredictionAnalysisRepository.findById(experimentId)
                .orElseGet(() -> {
                    if (!createIfMissing) {
                        throw new ResponseStatusException(NOT_FOUND, "Structure analysis not found");
                    }
                    return createStructureAnalysis(experimentId, experiment);
                });

        InMemoryStructurePool structurePool = new InMemoryStructurePool(experiment.getPool());
        for (Map.Entry<Integer, double[]> profile : analysis.getProfiles().entrySet()) {
            structurePool.registerStructure(profile.getKey(), profile.getValue());
        }
        structurePool.setReadOnly();

        return structurePool;
    }

    private StructurePredictionAnalysis createStructureAnalysis(String experimentId, Experiment experiment) {
        long startTimeMs = System.currentTimeMillis();
        StructurePool structurePool = computeStructurePool(experiment);
        long elapsedTimeMs = System.currentTimeMillis() - startTimeMs;
        log.info("Computed structure pool in {} seconds", elapsedTimeMs / 1000.0);

        Map<Integer, double[]> aptamerToStructureProfile = StreamSupport.stream(structurePool.iterator().spliterator(), false)
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

        StructurePredictionAnalysis analysis = new StructurePredictionAnalysis(experimentId, aptamerToStructureProfile, elapsedTimeMs);
        return structurePredictionAnalysisRepository.save(analysis);
    }

    private StructurePool computeStructurePool(Experiment experiment) {
        StructurePool structurePool = new InMemoryStructurePool(experiment.getPool());
        ThreadLocal<CapR> localCapR = ThreadLocal.withInitial(CapR::new);

        StreamSupport.stream(experiment.getPool().inverse_view_iterator().spliterator(), true)
                .forEach(item -> {
                    CapR capr = localCapR.get();
                    int id = item.getKey();
                    byte[] sequence = item.getValue();

                    capr.ComputeStructuralProfile(sequence, sequence.length);
                    structurePool.registerStructure(id, capr.getStructuralProfile());
                });

        structurePool.setReadOnly();
        return structurePool;
    }

    private void validateExperimentId(String experimentId) {
        if (!StringUtils.hasText(experimentId)) {
            throw new IllegalArgumentException("Experiment id is required");
        }
    }
}