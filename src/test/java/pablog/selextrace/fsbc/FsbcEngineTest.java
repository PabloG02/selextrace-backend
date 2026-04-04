package pablog.selextrace.fsbc;

import org.junit.jupiter.api.Test;
import pablog.selextrace.config.FsbcConfiguration;
import pablog.selextrace.domain.experiment.Experiment;
import pablog.selextrace.domain.experiment.InMemorySelectionCycle;
import pablog.selextrace.domain.experiment.SelectionCycle;
import pablog.selextrace.domain.metadata.Metadata;
import pablog.selextrace.domain.pool.InMemoryAptamerPool;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FsbcEngineTest {

    private final FsbcEngine engine = new FsbcEngine();

    @Test
    void extractsRandomizedRegionForRequestedRoundOnly() {
        Experiment experiment = buildExperiment(List.of(
                new CycleSpec("Round 1", 1, List.of(
                        new SequenceSpec("GGAAAACC", 2, 6, 5),
                        new SequenceSpec("GGAAATCC", 2, 6, 3)
                )),
                new CycleSpec("Round 2", 2, List.of(
                        new SequenceSpec("GGTTTTCC", 2, 6, 6),
                        new SequenceSpec("GGTTTACT", 2, 6, 4)
                ))
        ));

        SelectionCycle roundOne = experiment.getSelectionCycles().get(0);
        FsbcEngine.InputData inputData = engine.extractInputData(experiment, roundOne, false);

        assertEquals(Map.of("AAAA", 5, "AAAT", 3), inputData.sequenceCounts());
        assertEquals(8, inputData.totalSequenceCount());
    }

    @Test
    void computesWeightedBaseRatiosFromSlicedSequences() {
        Map<Character, Double> ratios = engine.computeBaseRatios(Map.of(
                "AAA", 3,
                "ATA", 1
        ));

        assertEquals(11d / 12d, ratios.get('A'), 1.0e-9);
        assertEquals(1d / 12d, ratios.get('T'), 1.0e-9);
    }

    @Test
    void clustersAptamersDeterministicallyAroundOverrepresentedSeed() {
        Experiment experiment = buildExperiment(List.of(
                new CycleSpec("Round 2", 2, List.of(
                        new SequenceSpec("AAAAG", 0, 5, 20),
                        new SequenceSpec("CAAAT", 0, 5, 15),
                        new SequenceSpec("GAAAC", 0, 5, 10),
                        new SequenceSpec("TTTTT", 0, 5, 1)
                ))
        ));

        SelectionCycle roundTwo = experiment.getSelectionCycles().get(0);
        FsbcEngine.FsbcRunResult result = engine.run(experiment, roundTwo, new FsbcConfiguration(2, 2, 3, false, 1));

        assertTrue(result.rankedStrings().stream().anyMatch(stringResult -> stringResult.getSubsequence().contains("AA")));
        assertEquals(45, result.clusterSeeds().get(0).getTotalCount());
        assertEquals(3, result.clusterSeeds().get(0).getMemberCount());

        Integer firstCluster = result.aptamerToCluster().get(1);
        assertEquals(firstCluster, result.aptamerToCluster().get(2));
        assertEquals(firstCluster, result.aptamerToCluster().get(3));
        assertTrue(!result.aptamerToCluster().containsKey(4) || !result.aptamerToCluster().get(4).equals(firstCluster));
    }

    private Experiment buildExperiment(List<CycleSpec> cycleSpecs) {
        InMemoryAptamerPool pool = new InMemoryAptamerPool();
        List<SelectionCycle> cycles = new ArrayList<>();

        for (CycleSpec cycleSpec : cycleSpecs) {
            InMemorySelectionCycle cycle = new InMemorySelectionCycle(
                    cycleSpec.name(),
                    cycleSpec.round(),
                    false,
                    false,
                    pool,
                    cycles
            );
            cycles.add(cycle);

            for (SequenceSpec sequenceSpec : cycleSpec.sequences()) {
                cycle.addToSelectionCycle(
                        sequenceSpec.sequence(),
                        sequenceSpec.startIndex(),
                        sequenceSpec.endIndex(),
                        sequenceSpec.count()
                );
            }
        }

        return new Experiment("FSBC Test", "Synthetic", cycles, new Metadata(cycles), pool);
    }

    private record CycleSpec(String name, int round, List<SequenceSpec> sequences) {
    }

    private record SequenceSpec(String sequence, int startIndex, int endIndex, int count) {
    }
}
