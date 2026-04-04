package pablog.selextrace.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;
import pablog.selextrace.config.FsbcConfiguration;
import pablog.selextrace.domain.metadata.Metadata;
import pablog.selextrace.model.FsbcAnalysis;
import pablog.selextrace.model.persistence.AptamerRecord;
import pablog.selextrace.model.persistence.ExperimentMetadataRecord;
import pablog.selextrace.model.persistence.ExperimentRecord;
import pablog.selextrace.model.persistence.SelectionCycleRecord;
import pablog.selextrace.repository.FsbcAnalysisRepository;
import pablog.selextrace.service.ExperimentPersistenceService;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class FsbcControllerTest {

    @Autowired
    private FsbcController fsbcController;

    @Autowired
    private ExperimentPersistenceService experimentPersistenceService;

    @Autowired
    private FsbcAnalysisRepository fsbcAnalysisRepository;

    private String experimentId;

    @BeforeEach
    void setUp() {
        fsbcAnalysisRepository.deleteAll();
        experimentId = UUID.randomUUID().toString();
        experimentPersistenceService.persistExperiment(createExperimentRecord(experimentId));
    }

    @Test
    void supportsCreateListGetDeleteAndExperimentCleanup() {
        FsbcAnalysis created = fsbcController.createAnalysis(experimentId, new FsbcConfiguration(2, 2, 3, false, 1));
        assertEquals(2, created.getRequestConfig().selectionCycleRound());
        assertTrue(created.getRankedStrings().size() > 0);

        assertEquals(1, fsbcController.listAnalyses(experimentId).size());
        assertEquals(created.getId(), fsbcController.getAnalysis(experimentId, created.getId()).getId());

        fsbcController.deleteAnalysis(experimentId, created.getId());
        ResponseStatusException missingAfterDelete = assertThrows(
                ResponseStatusException.class,
                () -> fsbcController.getAnalysis(experimentId, created.getId())
        );
        assertEquals(404, missingAfterDelete.getStatusCode().value());

        FsbcAnalysis recreated = fsbcController.createAnalysis(experimentId, new FsbcConfiguration(2, 2, 3, false, 1));
        experimentPersistenceService.deleteExperiment(experimentId);

        ResponseStatusException missingExperiment = assertThrows(
                ResponseStatusException.class,
                () -> fsbcController.getAnalysis(experimentId, recreated.getId())
        );
        assertEquals(404, missingExperiment.getStatusCode().value());
    }

    @Test
    void returnsNotFoundForMissingAnalysis() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> fsbcController.getAnalysis(experimentId, "missing")
        );
        assertEquals(404, exception.getStatusCode().value());
    }

    private ExperimentRecord createExperimentRecord(String id) {
        ExperimentRecord record = new ExperimentRecord();
        record.setId(id);
        record.setName("Controller Test");
        record.setDescription("Synthetic experiment");
        record.setAptamerSize(5);
        record.setFivePrimePrimer("GG");
        record.setThreePrimePrimer("CC");
        record.setTotalProcessedReads(65);
        record.setTotalAcceptedReads(65);
        record.setContigAssemblyFailure(0);
        record.setInvalidAlphabet(0);
        record.setFivePrimeError(0);
        record.setThreePrimeError(0);
        record.setInvalidCycle(0);
        record.setTotalPrimerOverlaps(0);

        ExperimentMetadataRecord metadataRecord = new ExperimentMetadataRecord();
        metadataRecord.setMetadata(new Metadata());
        record.setMetadataRecord(metadataRecord);

        Set<AptamerRecord> aptamerRecords = new LinkedHashSet<>();
        aptamerRecords.add(createAptamerRecord(id, 1, "GGAAAACC", 2, 6));
        aptamerRecords.add(createAptamerRecord(id, 2, "GGAAATCC", 2, 6));
        aptamerRecords.add(createAptamerRecord(id, 3, "GGTTTTCC", 2, 6));
        aptamerRecords.add(createAptamerRecord(id, 4, "GGTTTACT", 2, 6));
        record.setAptamerRecords(aptamerRecords);

        Set<SelectionCycleRecord> cycles = new LinkedHashSet<>();
        cycles.add(createSelectionCycleRecord("Round 1", 1, Map.of(1, 20, 2, 10)));
        cycles.add(createSelectionCycleRecord("Round 2", 2, Map.of(1, 12, 2, 8, 3, 9, 4, 6)));
        record.setSelectionCycleRecords(cycles);
        return record;
    }

    private AptamerRecord createAptamerRecord(String experimentId, int aptamerId, String sequence, int startIndex, int endIndex) {
        AptamerRecord record = new AptamerRecord();
        record.setExperimentId(experimentId);
        record.setAptamerNumericId(aptamerId);
        record.setSequence(sequence);
        record.setStartIndex(startIndex);
        record.setEndIndex(endIndex);
        return record;
    }

    private SelectionCycleRecord createSelectionCycleRecord(String name, int round, Map<Integer, Integer> counts) {
        SelectionCycleRecord record = new SelectionCycleRecord();
        record.setName(name);
        record.setRound(round);
        record.setControlSelection(false);
        record.setCounterSelection(false);
        record.setTotalSize(counts.values().stream().mapToInt(Integer::intValue).sum());
        record.setUniqueSize(counts.size());
        record.setCounts(new LinkedHashMap<>(counts));
        return record;
    }
}
