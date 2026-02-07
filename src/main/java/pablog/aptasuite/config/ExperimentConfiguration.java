package pablog.aptasuite.config;

import pablog.aptasuite.parsing.AptaPlexParser;
import pablog.aptasuite.parsing.Parser;
import pablog.aptasuite.parsing.io.FastqReader;
import pablog.aptasuite.parsing.io.Reader;

public class ExperimentConfiguration {

    // --- public nested typed sections (defaults set here) ---
    public final ExperimentConfig Experiment = new ExperimentConfig();
    public final AptamerPoolConfig AptamerPool = new AptamerPoolConfig();
    public final MapDBAptamerPoolConfig MapDBAptamerPool = new MapDBAptamerPoolConfig();
    public final SelectionCycleConfig SelectionCycle = new SelectionCycleConfig();
    public final MapDBSelectionCycleConfig MapDBSelectionCycle = new MapDBSelectionCycleConfig();
    public final StructurePoolConfig StructurePool = new StructurePoolConfig();
    public final MapDBStructurePoolConfig MapDBStructurePool = new MapDBStructurePoolConfig();
    public final ClusterContainerConfig ClusterContainer = new ClusterContainerConfig();
    public final AptaClusterConfig AptaCluster = new AptaClusterConfig();
    public final ParserConfig Parser = new ParserConfig();
    public final AptaplexParserConfig AptaplexParser = new AptaplexParserConfig();
    public final AptaSimConfig AptaSim = new AptaSimConfig();
    public final AptaTraceConfig AptaTrace = new AptaTraceConfig();
    public final ExportConfig Export = new ExportConfig();
    public final PerformanceConfig Performance = new PerformanceConfig();
    public final MapDBConfig MapDB = new MapDBConfig();

    // --- nested config types ---
    public static class ExperimentConfig {
        public String name = null;
        public String description = null;
        public String projectPath = null;
        public String primer5 = null;
        public String primer3 = null;
        public Integer randomizedRegionSize = null;
    }

    public static class AptamerPoolConfig {
//        public Class<? extends AptamerPool> backend = MapDBAptamerPool.class;
    }

    public static class MapDBAptamerPoolConfig {
        public int bloomFilterCapacity = 250000000;
        public double bloomFilterCollisionProbability = 0.001;
        public int maxTreeMapCapacity = 1000000;
    }

    public static class SelectionCycleConfig {
        //        public Class<? extends SelectionCycle> backend = MapDBSelectionCycle.class;
        public Integer round = null;
        public String name = null;
        public Boolean isControlSelection = null;
        public Boolean isCounterSelection = null;
    }

    public static class MapDBSelectionCycleConfig {
        public double bloomFilterCollisionProbability = 0.001;
    }

    public static class StructurePoolConfig {
//        public Class<? extends StructurePool> backend = MapDBStructurePool.class;
    }

    public static class MapDBStructurePoolConfig {
        public double bloomFilterCollisionProbability = 0.001;
        public int maxTreeMapCapacity = 500000;
        public int maxTreeMapCapacityBppm = 150000;
    }

    public static class ClusterContainerConfig {
//        public Class<? extends ClusterContainer> backend = MapDBClusterContainer.class;
    }

    public static class AptaClusterConfig {
        public int EditDistance = 5;
        public int LSHIterations = 5;
        public int KmerSize = 3;
        public int KmerCutoffIterations = 10000;
    }

    public static class ParserConfig {
        public Class<? extends Parser> backend = AptaPlexParser.class;
    }

    public static class AptaplexParserConfig {
        public Class<? extends Reader> backend = FastqReader.class;
        public Integer randomizedRegionSizeLowerBound = null;
        public Integer randomizedRegionSizeUpperBound = null;
        public boolean isPerFile = true;
        public int BlockingQueueSize = 5000;
        public int PairedEndMinOverlap = 15;
        public int PairedEndMaxMutations = 5;
        public int PairedEndMaxScoreValue = 55;
        public int BarcodeTolerance = 1;
        public int PrimerTolerance = 3;
        public boolean StoreReverseComplement = false;
        public boolean CheckReverseComplement = false;
        public boolean OnlyRandomizedRegionInData = false;
        public boolean UndeterminedToFile = false;
        public boolean BatchMode = false;

        public String barcodes5Prime = null;
        public String barcodes3Prime = null;

        public String[] forwardFiles = null;
        public String[] reverseFiles = null;
    }

    public static class AptaSimConfig {
        public int HmmDegree = 2;
        public int RandomizedRegionSize = 40;
        public int NumberOfSequences = 1000000;
        public int NumberOfSeeds = 100;
        public int MinSeedAffinity = 80;
        public int MaxSequenceCount = 10;
        public int MaxSequenceAffinity = 25;
        public String[] NucleotideDistribution = {"0.25", "0.25", "0.25", "0.25"};
        public double SelectionPercentage = 0.20;
        public String[] BaseMutationRates = {"0.25", "0.25", "0.25", "0.25"};
        public double MutationProbability = 0.05;
        public double AmplificationEfficiency = 0.995;
    }

    public static class AptaTraceConfig {
        public int KmerLength = 6;
        public boolean FilterClusters = true;
        public boolean OutputClusters = true;
        public int Alpha = 10;
    }

    public static class ExportConfig {
        public boolean compress = true;
        public String SequenceFormat = "fastq";
        public boolean IncludePrimerRegions = true;
        public int MinimalClusterSize = 1;
        public String ClusterFilterCriteria = "ClusterSize";
        public String PoolCardinalityFormat = "frequencies";
    }

    public static class PerformanceConfig {
        public int maxNumberOfCores = Runtime.getRuntime().availableProcessors() - 1;
    }

    public static class MapDBConfig {
        public long AllocateStartSize = 1L * 1024 * 1024 * 1024;
        public long AllocateIncrement = 100L * 1024 * 1024;
    }

    public ExperimentConfiguration() {
        // defaults already set in field initializers
    }

    public static ExperimentConfiguration defaults() {
        return new ExperimentConfiguration();
    }
}