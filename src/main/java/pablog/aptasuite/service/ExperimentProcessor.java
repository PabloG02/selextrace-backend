package pablog.aptasuite.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pablog.aptasuite.config.ExperimentConfiguration;
import pablog.aptasuite.domain.experiment.Experiment;
import pablog.aptasuite.domain.experiment.ExperimentFactory;
import pablog.aptasuite.domain.experiment.SelectionCycle;
import pablog.aptasuite.domain.metadata.AptaPlexProgress;
import pablog.aptasuite.domain.pool.AptamerBounds;
import pablog.aptasuite.dto.*;
import pablog.aptasuite.parsing.AptaPlexParser;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class ExperimentProcessor {

    private static final Logger log = LoggerFactory.getLogger(ExperimentProcessor.class);

    private Experiment experiment;
    private AptaPlexParser parser;

    public Experiment getExperiment() {
        return experiment;
    }

    public AptaPlexParser getParser() {
        return parser;
    }

    public AptaPlexProgress getProgress() {
        if (parser != null) {
            return (AptaPlexProgress) parser.Progress();
        }
        return null;
    }

    /**
     * Process an experiment using the given ExperimentConfiguration.
     *
     * @param config Pre-filled ExperimentConfiguration from REST API
     * @return ExperimentOverviewDTO containing experiment overview with details and statistics
     * @throws Exception if processing fails
     */
    public ExperimentOverviewDTO processData(ExperimentConfiguration config) throws Exception {

        log.info("Initializing experiment: {}", config.Experiment.name);

        // Use ExperimentFactory to create the Experiment (delegates construction + validation).
        ExperimentFactory factory = new ExperimentFactory();
        experiment = factory.createExperiment(config);

        log.info("Experiment initialized successfully: {}", experiment.getName());

        log.info("Initializing parser: {}", config.AptaplexParser.backend.getName());

        // Initialize parser
        parser = new AptaPlexParser(config, experiment);

        // Optional: assign forward/reverse files if configured
        // parser.forwardFiles = config.AptaplexParser.forwardFiles;
        // parser.reverseFiles = config.AptaplexParser.reverseFiles;

        log.info("Parser initialized: {}", config.AptaplexParser.backend.getName());

        // Run parser
        log.info("Starting AptaPlex processing...");
        long startTime = System.currentTimeMillis();

        parser.run();

        double elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000.0;
        log.info("Parsing completed in {} seconds", elapsedSeconds);

        // Build structured response
        return buildExperimentOverview(config);
    }

    /**
     * Builds a structured experiment overview response.
     *
     * @param config The experiment configuration
     * @return ExperimentOverviewDTO containing the experiment overview
     */
    private ExperimentOverviewDTO buildExperimentOverview(ExperimentConfiguration config) {
        // General Information
        GeneralInformationDTO generalInfo = new GeneralInformationDTO(
            experiment.getName(),
            experiment.getDescription(),
            config.Experiment.randomizedRegionSize,
            String.valueOf(config.Experiment.primer5),
            String.valueOf(config.Experiment.primer3)
        );

        // Sequence Import Statistics
        AptaPlexProgress progress = (AptaPlexProgress) parser.Progress();
        assert progress != null;
        SequenceImportStatisticsDTO importStats = new SequenceImportStatisticsDTO(
            progress.totalProcessedReads.get(),
            progress.totalAcceptedReads.get(),
            progress.totalContigAssemblyFails.get(),
            progress.totalInvalidContigs.get(),
            progress.totalUnmatchablePrimer5.get(),
            progress.totalUnmatchablePrimer3.get(),
            progress.totalInvalidCycle.get(),
            progress.totalPrimerOverlaps.get()
        );

        // Selection Cycle Percentages
        Map<String, Double> cyclePercentages = new HashMap<>();
        for (SelectionCycle cycle : experiment.getSelectionCycles()) {
            if (progress.totalAcceptedReads.get() > 0) {
                double percentage = (cycle.getSize() * 100.0) / progress.totalAcceptedReads.get();
                cyclePercentages.put(cycle.getName(), percentage);
            }
        }

        ExperimentDetailsDTO experimentDetails = new ExperimentDetailsDTO(
            generalInfo,
            importStats,
            cyclePercentages
        );

        SelectionCycleCompositionSetDTO selectionCycleComposition = new SelectionCycleCompositionSetDTO(
            1,
            mapCycles(experiment.getSelectionCycles(), 1),
            null,
            null
        );

        SelectionCycleResponseDTO selectionCycleResponse = new SelectionCycleResponseDTO(
            experiment.getSelectionCycles().getFirst().getName(),
            experiment.getSelectionCycles().getFirst().getRound(),
            experiment.getSelectionCycles().getFirst().isControlSelection(),
            experiment.getSelectionCycles().getFirst().isCounterSelection(),
            Optional.ofNullable(experiment.getSelectionCycles().getFirst().getBarcodeFivePrime())
                    .map(String::new)
                    .orElse(null),
            Optional.ofNullable(experiment.getSelectionCycles().getFirst().getBarcodeThreePrime())
                    .map(String::new)
                    .orElse(null),
            experiment.getSelectionCycles().getFirst().getSize(),
            experiment.getSelectionCycles().getFirst().getUniqueSize(),
                StreamSupport.stream(
                                experiment.getSelectionCycles().getFirst().iterator().spliterator(),
                                false
                        )
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue
                        ))
        );

        Map<Integer, String> idToAptamer = StreamSupport.stream(
                    experiment.getPool().inverse_view_iterator().spliterator(),
                        false
                )
                .collect(Collectors.toMap(Map.Entry::getKey,e -> new String(e.getValue(), StandardCharsets.UTF_8)));

        Map<Integer, AptamerBounds> idToBounds = StreamSupport.stream(
                        experiment.getPool().bounds_iterator().spliterator(),
                        false
                )
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> new AptamerBounds(e.getValue())
                ));

        ExperimentOverviewDTO overview = new ExperimentOverviewDTO(
            experimentDetails,
            getRegionSizes(), // ExperimentOverview-RandomizedRegionSizeDistribution
            selectionCycleComposition, // ExperimentOverview-SelectionCycleComposition
            // Testing purposes
            selectionCycleResponse,
            experiment.getMetadata(),
            idToAptamer,
            idToBounds
        );

        return overview;
    }

    public Map<String, Object> getRegionSizes() {
        Map<Integer, Integer> totals = new HashMap<>();

        // Loop through all selection cycles
        var allCycles = experiment.getMetadata().nucleotideDistributionAccepted;
        for (var cycleEntry : allCycles.entrySet()) {
            var cycle = cycleEntry.getValue();
            for (var sizeEntry : cycle.entrySet()) {
                int size = sizeEntry.getKey();
                var positions = sizeEntry.getValue();

                // Only sum counts from position 0 (to match original behavior)
                int sum = positions.get(0)
                        .values()
                        .stream()
                        .mapToInt(Integer::intValue)
                        .sum();

                totals.merge(size, sum, Integer::sum);
            }
        }

        // Prepare data list
        List<Map<String, Integer>> data = new ArrayList<>();
        int total = 0;
        for (var e : totals.entrySet()) {
            data.add(Map.of("size", e.getKey(), "count", e.getValue()));
            total += e.getValue();
        }

        // Response JSON
        return Map.of(
                "data", totals,
                "total", total
        );
    }

    private static SelectionCycleCompositionDTO mapCycles(List<SelectionCycle> cycles, int cutoff) {
        for (var cycle : cycles) {
            if (cycle == null) continue;

            var label = "Round %d (%s)".formatted(cycle.getRound(), cycle.getName());

            double uniqueFraction = (cycle.getUniqueSize() / (double) cycle.getSize()) * 100;
            int singletonCount = 0;
            int enrichedCount = 0;

            for (var aptamer : cycle.iterator()) {
                if (aptamer.getValue() > cutoff) enrichedCount++;
                else singletonCount++;
            }

            double singletonFreq = (singletonCount / (double) cycle.getUniqueSize()) * 100;
            double enrichedFreq  = (enrichedCount / (double) cycle.getUniqueSize()) * 100;

            return new SelectionCycleCompositionDTO(singletonFreq, enrichedFreq, uniqueFraction);
        }
        return null;
    }
}