package pablog.selextrace.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pablog.selextrace.config.ExperimentConfiguration;
import pablog.selextrace.domain.experiment.Experiment;
import pablog.selextrace.domain.experiment.ExperimentFactory;
import pablog.selextrace.domain.experiment.SelectionCycle;
import pablog.selextrace.domain.metadata.AptaPlexProgress;
import pablog.selextrace.domain.pool.AptamerBounds;
import pablog.selextrace.model.persistence.AptamerRecord;
import pablog.selextrace.model.persistence.ExperimentRecord;
import pablog.selextrace.model.persistence.ExperimentMetadataRecord;
import pablog.selextrace.model.persistence.SelectionCycleRecord;
import pablog.selextrace.parsing.AptaPlexParser;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class ExperimentProcessor {

    private static final Logger log = LoggerFactory.getLogger(ExperimentProcessor.class);

    private final ExperimentConfiguration config;
    private Experiment experiment;
    private AptaPlexParser parser;

    /// @param config Pre-filled ExperimentConfiguration from REST API
    public ExperimentProcessor(ExperimentConfiguration config) {
        this.config = Objects.requireNonNull(config, "config must not be null");
    }

    /// Process an experiment using the given ExperimentConfiguration.
    public void processData() throws Exception {

        log.info("Initializing experiment: {}", config.Experiment.name);

        ExperimentFactory factory = new ExperimentFactory();
        experiment = factory.createExperiment(config);

        log.info("Experiment initialized successfully: {}", experiment.getName());
        log.info("Initializing parser: {}", config.AptaplexParser.backend.getName());

        parser = new AptaPlexParser(config, experiment);

        // Optional: assign forward/reverse files if configured
        // parser.forwardFiles = config.AptaplexParser.forwardFiles;
        // parser.reverseFiles = config.AptaplexParser.reverseFiles;

        log.info("Parser initialized: {}", config.AptaplexParser.backend.getName());
        log.info("Starting AptaPlex processing...");

        long startTime = System.currentTimeMillis();
        parser.run();

        double elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000.0;
        log.info("Parsing completed in {} seconds", elapsedSeconds);
    }

    /// Builds an ExperimentRecord suitable for persistence, based on the processed experiment data.
    public ExperimentRecord buildExperimentRecord() {
        validateProcessedState();

        AptaPlexProgress progress = (AptaPlexProgress) parser.Progress();
        assert progress != null;

        ExperimentRecord record = new ExperimentRecord();
        record.setName(experiment.getName());
        record.setDescription(experiment.getDescription());
        record.setAptamerSize(config.Experiment.randomizedRegionSize);
        record.setFivePrimePrimer(String.valueOf(config.Experiment.primer5));
        record.setThreePrimePrimer(String.valueOf(config.Experiment.primer3));
        record.setTotalProcessedReads(progress.totalProcessedReads.get());
        record.setTotalAcceptedReads(progress.totalAcceptedReads.get());
        record.setContigAssemblyFailure(progress.totalContigAssemblyFails.get());
        record.setInvalidAlphabet(progress.totalInvalidContigs.get());
        record.setFivePrimeError(progress.totalUnmatchablePrimer5.get());
        record.setThreePrimeError(progress.totalUnmatchablePrimer3.get());
        record.setInvalidCycle(progress.totalInvalidCycle.get());
        record.setTotalPrimerOverlaps(progress.totalPrimerOverlaps.get());

        ExperimentMetadataRecord metadataRecord = new ExperimentMetadataRecord();
        metadataRecord.setMetadata(experiment.getMetadata());
        record.setMetadataRecord(metadataRecord);

        for (SelectionCycle cycle : experiment.getSelectionCycles()) {
            if (cycle == null) {
                continue;
            }
            SelectionCycleRecord cycleRecord = new SelectionCycleRecord();
            cycleRecord.setName(cycle.getName());
            cycleRecord.setRound(cycle.getRound());
            cycleRecord.setControlSelection(cycle.isControlSelection());
            cycleRecord.setCounterSelection(cycle.isCounterSelection());
            cycleRecord.setBarcode5Prime(Optional.ofNullable(cycle.getBarcodeFivePrime()).map(String::new).orElse(null));
            cycleRecord.setBarcode3Prime(Optional.ofNullable(cycle.getBarcodeThreePrime()).map(String::new).orElse(null));
            cycleRecord.setTotalSize(cycle.getSize());
            cycleRecord.setUniqueSize(cycle.getUniqueSize());
            cycleRecord.setCounts(StreamSupport.stream(cycle.iterator().spliterator(), false)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
            record.addSelectionCycle(cycleRecord);
        }

        Set<AptamerRecord> aptamerRecords = new LinkedHashSet<>();
        Map<Integer, String> idToAptamer = StreamSupport.stream(
                        experiment.getPool().inverse_view_iterator().spliterator(), false)
                .collect(Collectors.toMap(Map.Entry::getKey, e -> new String(e.getValue(), StandardCharsets.UTF_8)));
        Map<Integer, AptamerBounds> idToBounds = StreamSupport.stream(
                        experiment.getPool().bounds_iterator().spliterator(), false)
                .collect(Collectors.toMap(Map.Entry::getKey, e -> new AptamerBounds(e.getValue())));

        for (Map.Entry<Integer, String> entry : idToAptamer.entrySet()) {
            Integer aptamerId = entry.getKey();
            String sequence = entry.getValue();
            AptamerBounds bounds = idToBounds.get(aptamerId);

            if (aptamerId == null || sequence == null || bounds == null) {
                continue;
            }

            AptamerRecord aptamerRecord = new AptamerRecord();
            aptamerRecord.setAptamerNumericId(aptamerId);
            aptamerRecord.setSequence(sequence);
            aptamerRecord.setStartIndex(bounds.startIndex);
            aptamerRecord.setEndIndex(bounds.endIndex);
            aptamerRecords.add(aptamerRecord);
        }
        record.setAptamerRecords(aptamerRecords);

        return record;
    }

    private void validateProcessedState() {
        if (experiment == null || parser == null) {
            throw new IllegalStateException("processData() must be called before building experiment outputs");
        }
    }
}