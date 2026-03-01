package pablog.selextrace.service;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import pablog.selextrace.domain.experiment.Experiment;
import pablog.selextrace.dto.ExperimentSummaryDTO;
import pablog.selextrace.dto.response.ExperimentDTO;
import pablog.selextrace.model.persistence.ExperimentMetadataRecord;
import pablog.selextrace.model.persistence.ExperimentRecord;
import pablog.selextrace.model.persistence.SelectionCycleRecord;
import pablog.selextrace.repository.ExperimentMetadataRecordRepository;
import pablog.selextrace.repository.ExperimentRecordRepository;
import pablog.selextrace.repository.SelectionCycleRecordRepository;
import pablog.selextrace.service.mapper.ExperimentRecordMapper;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class ExperimentPersistenceService {

    private final ExperimentRecordRepository experimentRecordRepository;
    private final ExperimentMetadataRecordRepository experimentMetadataRecordRepository;
    private final SelectionCycleRecordRepository selectionCycleRecordRepository;
    private final ExperimentRecordMapper experimentRecordMapper;

    public ExperimentPersistenceService(
            ExperimentRecordRepository experimentRecordRepository,
            ExperimentMetadataRecordRepository experimentMetadataRecordRepository,
            SelectionCycleRecordRepository selectionCycleRecordRepository,
            ExperimentRecordMapper experimentRecordMapper
    ) {
        this.experimentRecordRepository = experimentRecordRepository;
        this.experimentMetadataRecordRepository = experimentMetadataRecordRepository;
        this.selectionCycleRecordRepository = selectionCycleRecordRepository;
        this.experimentRecordMapper = experimentRecordMapper;
    }

    @Transactional
    public ExperimentDTO persistExperiment(ExperimentRecord experimentRecord) {
        Objects.requireNonNull(experimentRecord, "experimentRecord is required");

        String experimentId = experimentRecord.getId() == null || experimentRecord.getId().isBlank()
                ? UUID.randomUUID().toString()
                : experimentRecord.getId();

        experimentRecord.setId(experimentId);

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
        return findExperimentResponseById(experimentId)
                .orElseThrow(() -> new IllegalStateException("Persisted experiment could not be reloaded"));
    }

    public List<ExperimentSummaryDTO> findExperimentSummaries() {
        return experimentRecordRepository.findAll()
                .stream()
                .map(experimentRecordMapper::toSummaryDTO)
                .toList();
    }

    public Optional<ExperimentDTO> findExperimentResponseById(String experimentId) {
        return experimentRecordRepository.findById(experimentId)
            .map(experimentRecordMapper::toExperimentDTO);
    }

    public Optional<Experiment> findExperimentById(String experimentId) {
        return experimentRecordRepository.findById(experimentId)
                .map(experimentRecordMapper::toExperiment);
    }

    public boolean existsExperiment(String experimentId) {
        return experimentRecordRepository.existsById(experimentId);
    }
}
