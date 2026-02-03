package pablog.aptasuite.domain.metadata;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import pablog.aptasuite.domain.experiment.SelectionCycle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jan Hoinka
 * Contains additional information regarding the experiment
 * that is not required strictly required for any routines but
 * might still be of interest to the user.
 * 
 * Examples of these data might be the nucleotide frequency 
 * distributions of the aptmers, the quality scroes of the reads
 * etc.
 * 
 */
public class Metadata {

    private static final Logger logger = LoggerFactory.getLogger(Metadata.class);

    /**
     * Stores the quality scores for each nucleotide position averaged over all the
     * reads per selection cycle
     * Key: SelectionCycle name
     * Value: <Nucleotide Position, Averaged Quality Score>
     */
    public Map<String, ConcurrentHashMap<Integer, Accumulator>> qualityScoresForward = new HashMap<>();

    /**
     * Stores the quality scores for each nucleotide position averaged over all the
     * reads per selection cycle
     * Key: SelectionCycle name
     * Value: <Nucleotide Position, Averaged Quality Score>
     */
    public Map<String, ConcurrentHashMap<Integer, Accumulator>> qualityScoresReverse = new HashMap<>();

    /**
     * The nucleotide distribution at each position of the forward read per
     * selection cycle
     * Key: SelectionCycle name
     * Value: <Nucleotide Position, <Nucleotide, Count>>
     */
    public Map<String, ConcurrentHashMap<Integer, ConcurrentHashMap<Byte, Integer>>> nucleotideDistributionForward = new HashMap<>();

    /**
     * The nucleotide distribution at each position of the reverse read per
     * selection cycle
     * Key: SelectionCycle name
     * Value: <Nucleotide Position, <Nucleotide, Count>>
     */
    public Map<String, ConcurrentHashMap<Integer, ConcurrentHashMap<Byte, Integer>>> nucleotideDistributionReverse = new HashMap<>();

    /**
     * The nucleotide distribution at each position of the accepted reads per length
     * and per selection cycle
     * Key: SelectionCycle name
     * Value: <Randomized Region Size, <Nucleotide Position, <Nucleotide, Count>>>
     */
    public Map<String, ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, ConcurrentHashMap<Byte, Integer>>>> nucleotideDistributionAccepted = new HashMap<>();

    /**
     * Contains the parsing statistics after all the data has been processed.
     * Currently, the following
     * keys are expected to exist:
     * 1) processed_reads
     * 2) accepted_reads
     * 3) contig_assembly_fails
     * 4) invalid_alphabet
     * 5) 5_prime_error
     * 6) 3_prime_error
     * 7) invalid_cycle
     * 8) total_primer_overlaps
     */
    public Map<ParserStat, Integer> parserStatistics = new EnumMap<>(ParserStat.class);

    /**
     * Default constructor required by Spring Data MongoDB for deserialization.
     */
    public Metadata() {}

    /**
     * Constructor.
     * Initializes all metadata structures in memory.
     */
    public Metadata(List<SelectionCycle> selectionCycles) {
        logger.info("Creating new Metadata instance.");

        // Instantiate the selection cycle
        for (SelectionCycle sc : selectionCycles) {
            qualityScoresForward.put(sc.getName(), new ConcurrentHashMap<>());
            qualityScoresReverse.put(sc.getName(), new ConcurrentHashMap<>());

            nucleotideDistributionForward.put(sc.getName(), new ConcurrentHashMap<>());
            nucleotideDistributionReverse.put(sc.getName(), new ConcurrentHashMap<>());

            nucleotideDistributionAccepted.put(sc.getName(), new ConcurrentHashMap<>());
        }
    }

}
