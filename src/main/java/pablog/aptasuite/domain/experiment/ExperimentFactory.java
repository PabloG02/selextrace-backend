package pablog.aptasuite.domain.experiment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pablog.aptasuite.config.ExperimentConfiguration;
import pablog.aptasuite.domain.metadata.Metadata;
import pablog.aptasuite.domain.pool.AptamerBounds;
import pablog.aptasuite.domain.pool.AptamerPool;
import pablog.aptasuite.domain.pool.InMemoryAptamerPool;
import pablog.aptasuite.dto.ExperimentOverviewDTO;
import pablog.aptasuite.dto.GeneralInformationDTO;
import pablog.aptasuite.dto.SelectionCycleResponseDTO;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ExperimentFactory {

    private static final Logger log = LoggerFactory.getLogger(ExperimentFactory.class);

    public ExperimentFactory() {}

    public Experiment createExperiment(ExperimentConfiguration conf) {
        long startTime = System.currentTimeMillis();

        try {
            String name = conf.Experiment.name;
            String description = conf.Experiment.description;

            AptamerPool pool = new InMemoryAptamerPool();
            List<SelectionCycle> selectionCycles = initializeSelectionCycles(conf, pool);
            Metadata metadata = new Metadata(selectionCycles);

            Experiment experiment = new Experiment(name, description, selectionCycles, metadata, pool);

            log.info("Experiment loaded in {} milliseconds", System.currentTimeMillis() - startTime);

            return experiment;

        } catch (Exception e) {
            log.error("Error creating experiment: {}", e.getMessage());
            throw new RuntimeException("Failed to create experiment", e);
        }
    }

    public Experiment createExperiment(ExperimentOverviewDTO overview) {
        long startTime = System.currentTimeMillis();

        try {
            if (overview == null) {
                throw new IllegalArgumentException("Experiment overview is required");
            }

            GeneralInformationDTO generalInfo = overview.experimentDetails() == null
                    ? null
                    : overview.experimentDetails().generalInformation();

            if (generalInfo == null || generalInfo.name() == null || generalInfo.name().isBlank()) {
                throw new IllegalArgumentException("Experiment name is required");
            }

            String name = generalInfo.name();
            String description = generalInfo.description();

            AptamerPool pool = new InMemoryAptamerPool(overview);
            List<SelectionCycle> selectionCycles = initializeSelectionCycles(overview, pool);
            Metadata metadata = overview.metadata() != null ? overview.metadata() : new Metadata(selectionCycles);
            Experiment experiment = new Experiment(name, description, selectionCycles, metadata, pool);

            log.info("Experiment loaded from overview in {} milliseconds", System.currentTimeMillis() - startTime);

            return experiment;

        } catch (Exception e) {
            log.error("Error creating experiment from overview: {}", e.getMessage());
            throw new RuntimeException("Failed to create experiment from overview", e);
        }
    }

    private List<SelectionCycle> initializeSelectionCycles(ExperimentConfiguration conf, AptamerPool pool) {
        List<SelectionCycle> selectionCycles = new ArrayList<>();

        for (ExperimentConfiguration.SelectionCycleConfig cycleConfig : conf.SelectionCycles) {
            Integer round = cycleConfig.round;
            String name = cycleConfig.name;

            if (round == null || round < 0) {
                throw new IllegalArgumentException("SelectionCycle round must be a non-negative integer.");
            }
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("SelectionCycle name is required.");
            }

            boolean isControl = Boolean.TRUE.equals(cycleConfig.isControlSelection);
            boolean isCounter = Boolean.TRUE.equals(cycleConfig.isCounterSelection);

            SelectionCycle cycle = new InMemorySelectionCycle(name, round, isControl, isCounter, pool, selectionCycles);
            selectionCycles.add(cycle);
        }

        return selectionCycles;
    }

    private List<SelectionCycle> initializeSelectionCycles(ExperimentOverviewDTO overview, AptamerPool pool) {
        List<SelectionCycleResponseDTO> responses = overview.selectionCycleResponse();
        if (responses == null || responses.isEmpty()) {
            return Collections.emptyList();
        }

        List<SelectionCycle> selectionCycles = new ArrayList<>(responses.size());

        for (SelectionCycleResponseDTO response : responses) {
            String name = response.name();
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("SelectionCycle name is required");
            }

            int round = response.round();
            if (round < 0) {
                throw new IllegalArgumentException("SelectionCycle round must be a non-negative integer.");
            }

            InMemorySelectionCycle cycle = new InMemorySelectionCycle(
                    name,
                    round,
                    response.isControlSelection(),
                    response.isCounterSelection(),
                    pool,
                    selectionCycles
            );

            if (response.barcode5Prime() != null) {
                cycle.setBarcodeFivePrime(response.barcode5Prime().getBytes(StandardCharsets.UTF_8));
            }
            if (response.barcode3Prime() != null) {
                cycle.setBarcodeThreePrime(response.barcode3Prime().getBytes(StandardCharsets.UTF_8));
            }

            selectionCycles.add(cycle);
        }

        for (int i = 0; i < responses.size(); i++) {
            SelectionCycleResponseDTO response = responses.get(i);
            SelectionCycle cycle = selectionCycles.get(i);

            Map<Integer, Integer> counts = response.counts();
            if (counts == null) {
                continue;
            }

            for (Map.Entry<Integer, Integer> entry : counts.entrySet()) {
                Integer id = entry.getKey();
                Integer count = entry.getValue();
                if (id == null || count == null) {
                    throw new IllegalArgumentException("Selection cycle counts must not contain null keys or values");
                }

                byte[] sequence = pool.getAptamer(id);
                AptamerBounds bounds = pool.getAptamerBounds(id);
                if (sequence == null || bounds == null) {
                    throw new IllegalArgumentException("Missing aptamer data for id " + id);
                }

                cycle.addToSelectionCycle(sequence, bounds.startIndex, bounds.endIndex, count);
            }
        }

        return selectionCycles;
    }
}
