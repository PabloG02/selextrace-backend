package pablog.selextrace.service.mapper;

import org.springframework.stereotype.Component;
import pablog.selextrace.domain.experiment.Experiment;
import pablog.selextrace.domain.experiment.InMemorySelectionCycle;
import pablog.selextrace.domain.experiment.SelectionCycle;
import pablog.selextrace.domain.metadata.Metadata;
import pablog.selextrace.domain.pool.AptamerBounds;
import pablog.selextrace.domain.pool.AptamerPool;
import pablog.selextrace.domain.pool.InMemoryAptamerPool;
import pablog.selextrace.dto.ExperimentSummaryDTO;
import pablog.selextrace.dto.SelectionCycleResponseDTO;
import pablog.selextrace.dto.response.ExperimentDTO;
import pablog.selextrace.dto.response.ExperimentImportStatsDTO;
import pablog.selextrace.dto.response.ExperimentMetadataDTO;
import pablog.selextrace.dto.response.ExperimentPoolDTO;
import pablog.selextrace.dto.response.ExperimentSequencingDTO;
import pablog.selextrace.dto.response.ExperimentTechnicalDetailsResponseDTO;
import pablog.selextrace.model.persistence.AptamerRecord;
import pablog.selextrace.model.persistence.ExperimentRecord;
import pablog.selextrace.model.persistence.SelectionCycleRecord;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class ExperimentRecordMapper {

    public ExperimentSummaryDTO toSummaryDTO(ExperimentRecord record) {
        Objects.requireNonNull(record, "record is required");
        return new ExperimentSummaryDTO(
                record.getId(),
                record.getName(),
                record.getDescription(),
                record.getCreatedAt()
        );
    }

    public ExperimentDTO toExperimentDTO(ExperimentRecord record) {
        Objects.requireNonNull(record, "record is required");

        List<SelectionCycleRecord> safeCycles = record.getSelectionCycleRecords() == null
                ? List.of()
                : new ArrayList<>(record.getSelectionCycleRecords());

        ExperimentSequencingDTO sequencing = new ExperimentSequencingDTO(
                record.getAptamerSize(),
                record.getFivePrimePrimer(),
                record.getThreePrimePrimer()
        );

        ExperimentImportStatsDTO importStats = new ExperimentImportStatsDTO(
                record.getTotalProcessedReads(),
                record.getTotalAcceptedReads(),
                record.getContigAssemblyFailure(),
                record.getInvalidAlphabet(),
                record.getFivePrimeError(),
                record.getThreePrimeError(),
                record.getInvalidCycle(),
                record.getTotalPrimerOverlaps()
        );

        Map<Integer, String> idToAptamer = new HashMap<>();
        Map<Integer, AptamerBounds> idToBounds = new HashMap<>();
        if (record.getAptamerRecords() != null) {
            for (AptamerRecord aptamer : record.getAptamerRecords()) {
                idToAptamer.put(aptamer.getAptamerNumericId(), aptamer.getSequence());
                idToBounds.put(aptamer.getAptamerNumericId(), new AptamerBounds(aptamer.getStartIndex(), aptamer.getEndIndex()));
            }
        }

        ExperimentPoolDTO pool = new ExperimentPoolDTO(idToAptamer, idToBounds);

        ExperimentTechnicalDetailsResponseDTO technicalDetails = new ExperimentTechnicalDetailsResponseDTO(
                record.getMetadataRecord() == null
                        ? null
                        : ExperimentMetadataDTO.from(record.getMetadataRecord().getMetadata())
        );

        List<SelectionCycleResponseDTO> cycleResponses = safeCycles
                .stream()
                .sorted(Comparator.comparingInt(SelectionCycleRecord::getRound).thenComparing(SelectionCycleRecord::getName))
                .map(cycle -> new SelectionCycleResponseDTO(
                        cycle.getName(),
                        cycle.getRound(),
                        cycle.isControlSelection(),
                        cycle.isCounterSelection(),
                        cycle.getBarcode5Prime(),
                        cycle.getBarcode3Prime(),
                        cycle.getTotalSize(),
                        cycle.getUniqueSize(),
                        cycle.getCounts() == null ? Map.of() : cycle.getCounts()
                ))
                .toList();

        return new ExperimentDTO(
                record.getId(),
                record.getCreatedAt(),
                record.getName(),
                record.getDescription(),
                sequencing,
                importStats,
                cycleResponses,
                pool,
                technicalDetails
        );
    }

    public Experiment toExperiment(ExperimentRecord record) {
        Objects.requireNonNull(record, "record is required");

        AptamerPool pool = new InMemoryAptamerPool();
        List<SelectionCycle> cycles = createSelectionCycles(record, pool);
        preloadAptamers(record, pool);
        hydrateCycleMemberships(record, cycles);

        Metadata metadata = record.getMetadataRecord() != null && record.getMetadataRecord().getMetadata() != null
                ? record.getMetadataRecord().getMetadata()
                : new Metadata(cycles);

        return new Experiment(
                record.getName(),
                record.getDescription(),
                cycles,
                metadata,
                pool
        );
    }

    private List<SelectionCycle> createSelectionCycles(ExperimentRecord record, AptamerPool pool) {
        List<SelectionCycleRecord> cycleRecords = record.getSelectionCycleRecords() == null
                ? List.of()
                : record.getSelectionCycleRecords()
                .stream()
                .sorted(Comparator.comparingInt(SelectionCycleRecord::getRound).thenComparing(SelectionCycleRecord::getName))
                .toList();

        List<SelectionCycle> cycles = new ArrayList<>();
        for (SelectionCycleRecord cycleRecord : cycleRecords) {
            InMemorySelectionCycle cycle = new InMemorySelectionCycle(
                    cycleRecord.getName(),
                    cycleRecord.getRound(),
                    cycleRecord.isControlSelection(),
                    cycleRecord.isCounterSelection(),
                    pool,
                    cycles
            );

            if (cycleRecord.getBarcode5Prime() != null) {
                cycle.setBarcodeFivePrime(cycleRecord.getBarcode5Prime().getBytes(StandardCharsets.UTF_8));
            }
            if (cycleRecord.getBarcode3Prime() != null) {
                cycle.setBarcodeThreePrime(cycleRecord.getBarcode3Prime().getBytes(StandardCharsets.UTF_8));
            }

            cycles.add(cycle);
        }

        return cycles;
    }

    private void preloadAptamers(ExperimentRecord record, AptamerPool pool) {
        if (record.getAptamerRecords() == null) {
            return;
        }

        record.getAptamerRecords()
                .stream()
                .sorted(Comparator.comparingInt(AptamerRecord::getAptamerNumericId))
                .forEach(aptamer -> pool.registerAptamer(aptamer.getSequence(), aptamer.getStartIndex(), aptamer.getEndIndex()));
    }

    private void hydrateCycleMemberships(ExperimentRecord record, List<SelectionCycle> cycles) {
        if (record.getSelectionCycleRecords() == null || record.getSelectionCycleRecords().isEmpty()) {
            return;
        }

        Map<Integer, AptamerRecord> aptamersById = new HashMap<>();
        if (record.getAptamerRecords() != null) {
            for (AptamerRecord aptamer : record.getAptamerRecords()) {
                aptamersById.put(aptamer.getAptamerNumericId(), aptamer);
            }
        }

        Map<String, SelectionCycle> cyclesByName = new HashMap<>();
        for (SelectionCycle cycle : cycles) {
            cyclesByName.put(cycle.getName(), cycle);
        }

        for (SelectionCycleRecord cycleRecord : record.getSelectionCycleRecords()) {
            SelectionCycle cycle = cyclesByName.get(cycleRecord.getName());
            if (cycle == null || cycleRecord.getCounts() == null || cycleRecord.getCounts().isEmpty()) {
                continue;
            }

            for (Map.Entry<Integer, Integer> countEntry : cycleRecord.getCounts().entrySet()) {
                Integer aptamerId = countEntry.getKey();
                Integer count = countEntry.getValue();
                AptamerRecord aptamer = aptamersById.get(aptamerId);

                if (aptamer == null || count == null || count <= 0) {
                    continue;
                }

                cycle.addToSelectionCycle(
                        aptamer.getSequence(),
                        aptamer.getStartIndex(),
                        aptamer.getEndIndex(),
                        count
                );
            }
        }
    }
}
