package pablog.aptasuite.domain.experiment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pablog.aptasuite.config.ExperimentConfiguration;
import pablog.aptasuite.domain.metadata.Metadata;
import pablog.aptasuite.domain.pool.AptamerPool;
import pablog.aptasuite.domain.pool.InMemoryAptamerPool;

import java.util.ArrayList;
import java.util.List;

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

}
