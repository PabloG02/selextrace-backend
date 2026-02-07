package pablog.aptasuite.parsing.pipeline;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.milaboratory.core.PairedEndReadsLayout;
import com.milaboratory.core.io.sequence.PairedRead;
import com.milaboratory.core.io.sequence.SingleReadImpl;
import com.milaboratory.core.merger.MismatchOnlyPairedReadMerger;
import com.milaboratory.core.merger.PairedReadMergingResult;
import com.milaboratory.core.merger.QualityMergingAlgorithm;
import com.milaboratory.core.sequence.NSequenceWithQuality;

import pablog.aptasuite.config.ExperimentConfiguration;
import pablog.aptasuite.domain.experiment.SelectionCycle;
import pablog.aptasuite.domain.metadata.Accumulator;
import pablog.aptasuite.domain.metadata.AptaPlexProgress;
import pablog.aptasuite.domain.metadata.Metadata;
import pablog.aptasuite.parsing.distance.BitapDistance;
import pablog.aptasuite.parsing.distance.Distance;
import pablog.aptasuite.parsing.distance.EditDistance;
import pablog.aptasuite.parsing.distance.Result;
import pablog.aptasuite.parsing.io.Read;
import pablog.aptasuite.util.ArrayUtils;

/**
 * Consumer component of the AptaPlex parser pipeline.
 * <p>
 * This class implements the consumer pattern for processing sequencing reads.
 * It takes reads from a blocking queue, processes them according to the specified
 * configuration (demultiplexing, primer/barcode matching, randomized region extraction),
 * and adds valid sequences to the aptamer pool.
 * </p>
 * <p>
 * The consumer supports multiple processing modes:
 * <ul>
 *   <li>Batch mode: Pre-processed randomized regions only</li>
 *   <li>Randomized region only: Sequences without primers/barcodes</li>
 *   <li>Full parsing: Complete demultiplexing and extraction</li>
 * </ul>
 * </p>
 *
 * @author Jan Hoinka
 */
public class AptaPlexConsumer implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(AptaPlexConsumer.class);

    /**
     * The blocking queue from which reads are consumed
     */
    private final BlockingQueue<Object> queue;

    /**
     * Thread-safe progress tracker for parser statistics
     */
    private final AptaPlexProgress progress;

    /**
     * Experiment configuration containing all parsing parameters
     */
    private final ExperimentConfiguration config;

    /**
     * Metadata storage for nucleotide distributions and quality scores
     */
    private final Metadata metadata;

    /**
     * List of all selection cycles in the experiment
     */
    private final List<SelectionCycle> cycles;

    // --- Configuration flags ---

    /**
     * Batch mode: assumes reads are preprocessed randomized regions only.
     * No primer or barcode checks are performed.
     */
    private final boolean batchMode;

    /**
     * Indicates whether data has been previously demultiplexed (per-file mode)
     */
    private final boolean isPerFile;

    /**
     * Indicates whether sequences contain only randomized regions
     * (primers will be added synthetically)
     */
    private final boolean onlyRandomizedRegionInData;

    /**
     * If true, stores the reverse complement of sequences
     */
    private final boolean storeReverseComplement;

    /**
     * If true, checks the reverse complement if initial parsing fails
     */
    private final boolean checkReverseComplement;

    // --- Primer and barcode data ---

    /**
     * 5' primer sequence
     */
    private final byte[] primer5;

    /**
     * 5' primer in reverse (for bitap algorithm)
     */
    private final byte[] primer5Reverse;

    /**
     * 3' primer sequence (may be empty in batch mode)
     */
    private final byte[] primer3;

    /**
     * List of 5' barcodes (empty if isPerFile is true)
     */
    private final List<byte[]> barcodes5;

    /**
     * List of 3' barcodes (empty if isPerFile is true)
     */
    private final List<byte[]> barcodes3;

    // --- Randomized region size constraints ---

    /**
     * Exact expected size of randomized region (takes precedence over bounds)
     */
    private final Integer randomizedRegionSizeExactBound;

    /**
     * Minimum acceptable size of randomized region
     */
    private final Integer randomizedRegionSizeLowerBound;

    /**
     * Maximum acceptable size of randomized region
     */
    private final Integer randomizedRegionSizeUpperBound;

    // --- Distance algorithms ---

    /**
     * Bitap algorithm for fast approximate matching (primers ≤32 bp)
     */
    private final Distance bitapDistance;

    /**
     * Edit distance algorithm for longer primers (>32 bp)
     */
    private final Distance editDistance;

    /**
     * Maximum allowed mismatches for primer matching
     */
    private final int primerTolerance;

    /**
     * Maximum allowed mismatches for barcode matching
     */
    private final int barcodeTolerance;

    // --- Paired-end merging ---

    /**
     * MiTools merger for creating contigs from paired-end reads
     */
    private final MismatchOnlyPairedReadMerger merger;

    // --- Current read being processed ---

    /**
     * The read currently being processed by this consumer
     */
    private Read currentRead;

    /**
     * Constructs a new AptaPlexConsumer.
     *
     * @param queue the blocking queue to consume reads from
     * @param progress the progress tracker for parser statistics
     * @param config the experiment configuration
     * @param metadata the metadata storage for distributions and quality scores
     * @param cycles the list of selection cycles
     * @throws IllegalArgumentException if any required parameter is null
     * @throws IllegalStateException if configuration is invalid
     */
    public AptaPlexConsumer(
            BlockingQueue<Object> queue,
            AptaPlexProgress progress,
            ExperimentConfiguration config,
            Metadata metadata,
            List<SelectionCycle> cycles) {

        // Validate parameters
        if (queue == null) {
            throw new IllegalArgumentException("Queue cannot be null");
        }
        if (progress == null) {
            throw new IllegalArgumentException("Progress tracker cannot be null");
        }
        if (config == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }
        if (metadata == null) {
            throw new IllegalArgumentException("Metadata cannot be null");
        }
        if (cycles == null) {
            throw new IllegalArgumentException("Selection cycles cannot be null");
        }

        this.queue = queue;
        this.progress = progress;
        this.config = config;
        this.metadata = metadata;
        this.cycles = cycles;

        // Initialize configuration flags
        this.batchMode = config.AptaplexParser.BatchMode;
        this.isPerFile = config.AptaplexParser.isPerFile;
        this.onlyRandomizedRegionInData = config.AptaplexParser.OnlyRandomizedRegionInData;
        this.storeReverseComplement = config.AptaplexParser.StoreReverseComplement;
        this.checkReverseComplement = config.AptaplexParser.CheckReverseComplement;

        // Initialize primers
        this.primer5 = config.Experiment.primer5.getBytes();
        this.primer5Reverse = config.Experiment.primer5.getBytes();
        ArrayUtils.reverse(this.primer5Reverse); // Reverse for bitap algorithm

        this.primer3 = batchMode ? new byte[0] : config.Experiment.primer3.getBytes();

        // Initialize barcodes
        this.barcodes5 = new ArrayList<>();
        this.barcodes3 = new ArrayList<>();

        if (config.AptaplexParser.barcodes5Prime != null) {
            for (String barcode : config.AptaplexParser.barcodes5Prime.split(",")) {
                barcodes5.add(barcode.trim().getBytes());
            }
        }

        if (config.AptaplexParser.barcodes3Prime != null) {
            for (String barcode : config.AptaplexParser.barcodes3Prime.split(",")) {
                barcodes3.add(barcode.trim().getBytes());
            }
        }

        // Initialize randomized region size constraints
        this.randomizedRegionSizeExactBound = config.Experiment.randomizedRegionSize;
        this.randomizedRegionSizeLowerBound = config.AptaplexParser.randomizedRegionSizeLowerBound;
        this.randomizedRegionSizeUpperBound = config.AptaplexParser.randomizedRegionSizeUpperBound;

        // Validate configuration
        validateConfiguration();

        // Initialize distance algorithms
        this.bitapDistance = new BitapDistance();
        this.editDistance = new EditDistance();
        this.primerTolerance = config.AptaplexParser.PrimerTolerance;
        this.barcodeTolerance = config.AptaplexParser.BarcodeTolerance;

        // Initialize paired-end merger
        this.merger = new MismatchOnlyPairedReadMerger(
                config.AptaplexParser.PairedEndMinOverlap,
                0,// TODO: minMatchQualitySum (was added in recent versions of the library) (I'm trying to replicate the old behaviour based on the comparison of the source code between the versions)
                1.0 - 1.0 * config.AptaplexParser.PairedEndMaxMutations / config.AptaplexParser.PairedEndMinOverlap,
                config.AptaplexParser.PairedEndMaxScoreValue,
                QualityMergingAlgorithm.SumSubtraction,
                PairedEndReadsLayout.Unknown
        );

        log.info("AptaPlexConsumer initialized: batchMode={}, isPerFile={}, " +
                        "onlyRandomizedRegionInData={}, storeReverseComplement={}, checkReverseComplement={}",
                batchMode, isPerFile, onlyRandomizedRegionInData, storeReverseComplement, checkReverseComplement);
    }

    /**
     * Validates the configuration for consistency.
     *
     * @throws IllegalStateException if configuration is invalid
     */
    private void validateConfiguration() {
        // Ensure we have either a 3' primer or a randomized region size
        if (primer3.length == 0 && randomizedRegionSizeExactBound == null) {
            throw new IllegalStateException(
                    "Neither 3' primer nor randomized region size specified. " +
                            "At least one is required for sequence extraction."
            );
        }

        // Validate randomized region size bounds
        if (randomizedRegionSizeExactBound == null) {
            // If using bounds, both must be specified
            if ((randomizedRegionSizeLowerBound != null && randomizedRegionSizeUpperBound == null) ||
                    (randomizedRegionSizeLowerBound == null && randomizedRegionSizeUpperBound != null)) {
                throw new IllegalStateException(
                        "Both lower and upper bounds must be specified for randomized region size range"
                );
            }

            // Lower bound must be less than upper bound
            if (randomizedRegionSizeLowerBound != null && randomizedRegionSizeUpperBound != null &&
                    randomizedRegionSizeLowerBound >= randomizedRegionSizeUpperBound) {
                throw new IllegalStateException(
                        "Lower bound must be less than upper bound for randomized region size"
                );
            }
        }

        log.debug("Configuration validated successfully");
    }

    /**
     * Main execution method that consumes and processes reads from the queue.
     * <p>
     * This method continuously polls the queue for reads, processes them according
     * to the configured mode, and updates progress statistics. It terminates when
     * it encounters the poison pill.
     * </p>
     */
    @Override
    public void run() {
        log.info("Consumer thread started");

        byte[] contig = null;

        try {
            while (true) {
                // Take next element from queue (blocks if empty)
                Object queueElement = queue.take();

                // Check for poison pill
                if (queueElement instanceof PoisonPill) {
                    log.info("Encountered poison pill. Terminating consumer thread.");
                    queue.put(PoisonPill.INSTANCE); // Pass poison pill to other consumers
                    return;
                }

                try {
                    // Extract contig from queue element
                    contig = extractContig(queueElement);

                    // Validate contig
                    if (contig == null) {
                        progress.totalInvalidContigs.incrementAndGet();
                        continue;
                    }

                    // Process according to mode
                    if (batchMode) {
                        processBatchMode(contig);
                    } else if (onlyRandomizedRegionInData) {
                        processRandomizedRegionOnly(contig);
                    } else {
                        processFullRead(contig);
                    }

                    // Update progress
                    progress.totalProcessedReads.incrementAndGet();

                } catch (Exception e) {
                    log.error("Error processing read: contig={}",
                            contig != null ? new String(contig) : "null", e);
                }
            }

        } catch (InterruptedException e) {
            log.warn("Consumer thread interrupted", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Fatal error in consumer thread", e);
            throw new RuntimeException("Consumer thread failed", e);
        }
    }

    /**
     * Extracts and validates a contig from a queue element.
     *
     * @param queueElement the element from the queue
     * @return the contig byte array, or null if invalid
     */
    private byte[] extractContig(Object queueElement) {
        currentRead = (Read) queueElement;

        byte[] contig;

        // Handle paired-end vs single-end
        if (currentRead.reverse_read != null) {
            // Paired-end: compute transcribed reverse and create contig
            computeTranscribedReverse();
            contig = computeContig();
        } else {
            // Single-end: use forward read directly
            contig = currentRead.forward_read;
        }

        // Validate sequence (only A, C, G, T allowed)
        if (!isValidSequence(contig)) {
            return null;
        }

        return contig;
    }

    /**
     * Processes a read in batch mode (pre-processed randomized regions).
     *
     * @param contig the contig to process
     * @return the error counter that was incremented, or null if successful
     */
    private AtomicInteger processBatchMode(byte[] contig) {
        // Validate contig size
        if (randomizedRegionSizeExactBound != null) {
            if (contig.length != randomizedRegionSizeExactBound) {
                progress.totalContigAssemblyFails.incrementAndGet();
                return progress.totalContigAssemblyFails;
            }
        } else {
            // Check bounds
            if (randomizedRegionSizeLowerBound != null && randomizedRegionSizeUpperBound != null) {
                if (contig.length < randomizedRegionSizeLowerBound ||
                        contig.length > randomizedRegionSizeUpperBound) {
                    progress.totalContigAssemblyFails.incrementAndGet();
                    return progress.totalContigAssemblyFails;
                }
            }
        }

        // Store sequence (with reverse complement if requested)
        if (storeReverseComplement) {
            contig = computeReverseComplement(contig);
        }

        currentRead.selection_cycle.addToSelectionCycle(contig, 0, contig.length);

        // Add metadata
        addAcceptedNucleotideDistributions(currentRead.selection_cycle, contig, 0, contig.length);
        addNucleotideDistributions();
        addQualityScores();

        progress.totalAcceptedReads.incrementAndGet();
        return null;
    }

    /**
     * Processes a read containing only the randomized region.
     * Primers are added synthetically.
     *
     * @param contigRR the randomized region contig
     * @return the error counter that was incremented, or null if successful
     */
    private AtomicInteger processRandomizedRegionOnly(byte[] contigRR) {
        // Synthetically add primers
        byte[] contig = new byte[primer5.length + contigRR.length + primer3.length];
        System.arraycopy(primer5, 0, contig, 0, primer5.length);
        System.arraycopy(contigRR, 0, contig, primer5.length, contigRR.length);
        System.arraycopy(primer3, 0, contig, primer5.length + contigRR.length, primer3.length);

        // Extract boundaries
        int start = primer5.length;
        int end = primer5.length + contigRR.length;

        // Store sequence (with reverse complement if requested)
        if (storeReverseComplement) {
            contig = computeReverseComplement(contig);
            // Recompute boundaries for reverse complement
            start = contig.length - (primer5.length + contigRR.length);
            end = contig.length - primer5.length;
        }

        currentRead.selection_cycle.addToSelectionCycle(contig, start, end);

        // Add metadata
        addAcceptedNucleotideDistributions(currentRead.selection_cycle, contig, start, end);
        addNucleotideDistributions();
        addQualityScores();

        progress.totalAcceptedReads.incrementAndGet();
        return null;
    }

    /**
     * Processes a full read with demultiplexing and extraction.
     * Attempts both forward and reverse complement if configured.
     *
     * @param contig the contig to process
     */
    private void processFullRead(byte[] contig) {
        // Try processing the read as-is
        AtomicInteger returnCode = processRead(contig, false, null);

        // If failed and reverse complement checking is enabled, try reverse complement
        if (returnCode != null && checkReverseComplement) {
            contig = computeReverseComplement(contig);
            returnCode = processRead(contig, true, returnCode);
        }

        // If still failed, log the failure
        if (returnCode != null) {
            log.debug("Failed to process read after all attempts");
        }
    }

    /**
     * Processes a read by matching primers/barcodes and extracting the randomized region.
     *
     * @param contig the contig to process
     * @param reverseComplement true if this is a reverse complement attempt
     * @param previousReturnCode result from previous attempt, or null
     * @return the error counter that was incremented, or null if successful
     */
    private AtomicInteger processRead(byte[] contig, boolean reverseComplement, AtomicInteger previousReturnCode) {
        // Match 5' primer (search from 3' end using reversed sequences)
        byte[] contigReverse = contig.clone();
        ArrayUtils.reverse(contigReverse);

        Result primer5Match = matchPrimer(contigReverse, primer5Reverse);
        if (primer5Match != null) {
            // Convert index back to original orientation
            primer5Match.index = contig.length - primer5Match.index - primer5Reverse.length;
        }

        if (primer5Match == null) {
            progress.totalUnmatchablePrimer5.incrementAndGet();
            decrementIfNotNull(previousReturnCode);
            return progress.totalUnmatchablePrimer5;
        }

        // Match 3' primer if present
        Result primer3Match = null;
        if (primer3.length > 0) {
            primer3Match = matchPrimer(contig, primer3);
            if (primer3Match == null) {
                progress.totalUnmatchablePrimer3.incrementAndGet();
                decrementIfNotNull(previousReturnCode);
                return progress.totalUnmatchablePrimer3;
            }
        }

        // Identify selection cycle (if not per-file mode)
        if (!isPerFile) {
            currentRead.selection_cycle = matchBarcodes(contig, primer5Match, primer3Match);
        }

        if (currentRead.selection_cycle == null) {
            progress.totalInvalidCycle.incrementAndGet();
            decrementIfNotNull(previousReturnCode);
            return progress.totalInvalidCycle;
        }

        // Check for primer overlap
        if (primer3Match != null && isOverlapped(primer5Match, primer5, primer3Match, primer3)) {
            progress.totalPrimerOverlaps.incrementAndGet();
            decrementIfNotNull(previousReturnCode);
            return progress.totalPrimerOverlaps;
        }

        // Extract randomized region
        int rrStart = primer5Match.index + primer5.length;
        int rrEnd = (primer3Match == null)
                ? rrStart + randomizedRegionSizeExactBound
                : primer3Match.index;

        // Validate extraction
        if (!isValidExtraction(contig, rrStart, rrEnd)) {
            // Determine which primer caused the failure
            if (rrStart - primer5.length < 0) {
                progress.totalUnmatchablePrimer5.incrementAndGet();
                decrementIfNotNull(previousReturnCode);
                return progress.totalUnmatchablePrimer5;
            } else if (rrEnd + primer3.length > contig.length) {
                progress.totalUnmatchablePrimer3.incrementAndGet();
                decrementIfNotNull(previousReturnCode);
                return progress.totalUnmatchablePrimer3;
            }
            return null;
        }

        // Extract sequence with primers
        byte[] extractedSequence = Arrays.copyOfRange(
                contig,
                rrStart - primer5.length,
                rrEnd + primer3.length
        );

        // Apply reverse complement if requested
        if (storeReverseComplement) {
            extractedSequence = computeReverseComplement(extractedSequence);
            // Recompute boundaries
            int start = extractedSequence.length - (primer5.length + (rrEnd - rrStart));
            int end = extractedSequence.length - primer5.length;

            currentRead.selection_cycle.addToSelectionCycle(extractedSequence, start, end);
            addAcceptedNucleotideDistributions(currentRead.selection_cycle, extractedSequence, start, end);
        } else {
            currentRead.selection_cycle.addToSelectionCycle(
                    extractedSequence,
                    primer5.length,
                    primer5.length + (rrEnd - rrStart)
            );
            addAcceptedNucleotideDistributions(currentRead.selection_cycle, contig, rrStart, rrEnd);
        }

        // Add metadata
        addNucleotideDistributions();
        addQualityScores();

        progress.totalAcceptedReads.incrementAndGet();
        decrementIfNotNull(previousReturnCode);

        return null;
    }

    /**
     * Validates whether the extracted randomized region is valid.
     *
     * @param contig the full contig
     * @param rrStart start index of randomized region
     * @param rrEnd end index of randomized region
     * @return true if valid, false otherwise
     */
    private boolean isValidExtraction(byte[] contig, int rrStart, int rrEnd) {
        // Primers must be in correct order
        if (rrStart >= rrEnd || rrEnd > contig.length) {
            return false;
        }

        // 5' primer must not overshoot left
        if (rrStart - primer5.length < 0) {
            return false;
        }

        // 3' primer must not overshoot right
        if (rrEnd + primer3.length > contig.length) {
            return false;
        }

        int rrSize = rrEnd - rrStart;

        // Check exact size if specified
        if (randomizedRegionSizeExactBound != null && rrSize != randomizedRegionSizeExactBound) {
            return false;
        }

        // Check bounds if specified
        if (randomizedRegionSizeLowerBound != null && randomizedRegionSizeUpperBound != null) {
            if (rrSize < randomizedRegionSizeLowerBound || rrSize > randomizedRegionSizeUpperBound) {
                return false;
            }
        }

        return true;
    }

    /**
     * Decrements an AtomicInteger if it's not null.
     *
     * @param counter the counter to decrement
     */
    private void decrementIfNotNull(AtomicInteger counter) {
        if (counter != null) {
            counter.decrementAndGet();
        }
    }

    /**
     * Computes the transcribed reverse (reverse complement) of the reverse read.
     * Also reverses the quality scores.
     */
    private void computeTranscribedReverse() {
        // Complement each nucleotide
        for (int x = 0; x < currentRead.reverse_read.length; x++) {
            currentRead.reverse_read[x] = complement(currentRead.reverse_read[x]);
        }

        // Reverse both sequence and quality scores
        reverseArray(currentRead.reverse_read);
        reverseArray(currentRead.reverse_quality);
    }

    /**
     * Returns the complement of a nucleotide.
     *
     * @param nucleotide the nucleotide byte (ASCII)
     * @return the complement nucleotide byte
     */
    private byte complement(byte nucleotide) {
        return switch (nucleotide) {
            case 65 -> 84; // A -> T
            case 67 -> 71; // C -> G
            case 71 -> 67; // G -> C
            case 84 -> 65; // T -> A
            default -> nucleotide;
        };
    }

    /**
     * Reverses an array in place.
     *
     * @param array the array to reverse
     */
    private void reverseArray(byte[] array) {
        for (int i = 0; i < array.length / 2; i++) {
            byte temp = array[i];
            array[i] = array[array.length - i - 1];
            array[array.length - i - 1] = temp;
        }
    }

    /**
     * Computes the reverse complement of a sequence.
     *
     * @param sequence the original sequence
     * @return the reverse complement
     */
    private byte[] computeReverseComplement(byte[] sequence) {
        byte[] result = sequence.clone();

        // Complement
        for (int x = 0; x < result.length; x++) {
            result[x] = complement(result[x]);
        }

        // Reverse
        reverseArray(result);

        return result;
    }

    /**
     * Creates a contig from paired-end reads using the MiTools merger.
     *
     * @return the merged contig, or null if merging failed
     */
    private byte[] computeContig() {
        // Create paired read object
        PairedRead pairedRead = new PairedRead(
                new SingleReadImpl(
                        0,
                        new NSequenceWithQuality(
                                new String(currentRead.forward_read),
                                new String(currentRead.forward_quality)
                        ),
                        "forward"
                ),
                new SingleReadImpl(
                        0,
                        new NSequenceWithQuality(
                                new String(currentRead.reverse_read),
                                new String(currentRead.reverse_quality)
                        ),
                        "reverse"
                )
        );

        // Merge reads
        PairedReadMergingResult result = merger.process(pairedRead.toTuple());

        // Check if merging was successful
        if (!result.isSuccessful()) {
            return null;
        }

        return result.getOverlappedSequence().getSequence().toString().getBytes();
    }

    /**
     * Validates that a contig contains only valid nucleotides (A, C, G, T).
     *
     * @param contig the contig to validate
     * @return true if valid, false otherwise
     */
    private boolean isValidSequence(byte[] contig) {
        if (contig == null) {
            return false;
        }

        for (byte nucleotide : contig) {
            if (nucleotide != 65 && nucleotide != 67 && nucleotide != 71 && nucleotide != 84) {
                return false;
            }
        }

        return true;
    }

    /**
     * Finds the best fuzzy match for a primer using approximate string matching.
     * Uses bitap for short primers (≤32 bp) and edit distance for longer ones.
     *
     * @param contig the contig to search in
     * @param primer the primer to match
     * @return Result containing match position and error count, or null if no match
     */
    private Result matchPrimer(byte[] contig, byte[] primer) {
        // Use edit distance for long primers
        if (primer.length > 32) {
            return editDistance.indexOf(contig, primer, primerTolerance, 0, contig.length);
        }

        // Use fast bitap algorithm for short primers
        Result bestMatch = bitapDistance.indexOf(contig, primer, primerTolerance, 0, contig.length);

        if (bestMatch == null) {
            return null;
        }

        // Perfect match - no need for refinement
        if (bestMatch.errors == 0) {
            return bestMatch;
        }

        // Refine match position by computing actual mismatches
        return refinePrimerMatch(contig, primer, bestMatch);
    }

    /**
     * Refines a primer match by checking neighboring positions.
     *
     * @param contig the contig
     * @param primer the primer
     * @param initialMatch the initial match from bitap
     * @return refined match result
     */
    private Result refinePrimerMatch(byte[] contig, byte[] primer, Result initialMatch) {
        // Count mismatches at initial position
        int mismatches = countMismatches(contig, primer, initialMatch.index);

        if (initialMatch.errors == mismatches) {
            return initialMatch;
        }

        // Search neighboring positions for better match
        int bestMismatches = mismatches;
        int bestIndex = initialMatch.index;

        for (int offset = 1; offset < primerTolerance; offset++) {
            int currentMismatches = countMismatches(contig, primer, initialMatch.index - offset);

            if (currentMismatches < bestMismatches) {
                bestMismatches = currentMismatches;
                bestIndex = initialMatch.index - offset;
            }
        }

        // Return best match if within tolerance
        if (bestMismatches <= primerTolerance) {
            initialMatch.index = bestIndex;
            initialMatch.errors = bestMismatches;
            return initialMatch;
        }

        return null;
    }

    /**
     * Counts mismatches between a primer and contig at a specific position.
     * Treats array out-of-bounds as mismatches.
     *
     * @param contig the contig
     * @param primer the primer
     * @param position the starting position in the contig
     * @return the number of mismatches
     */
    private int countMismatches(byte[] contig, byte[] primer, int position) {
        int mismatches = 0;

        for (int i = 0; i < primer.length; i++) {
            try {
                if (contig[position + i] != primer[i]) {
                    mismatches++;
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                // Primer extends beyond contig - count as mismatch
                mismatches++;
            }
        }

        return mismatches;
    }

    /**
     * Matches barcodes to a contig and assigns the read to a selection cycle.
     * <p>
     * Searches for the best matching barcode on both 5' and 3' sides. If both
     * are present, they must match the same cycle.
     * </p>
     *
     * @param contig the contig to search
     * @param primer5Match the matched 5' primer position
     * @param primer3Match the matched 3' primer position (may be null)
     * @return the matching SelectionCycle, or null if no match found
     */
    private SelectionCycle matchBarcodes(byte[] contig, Result primer5Match, Result primer3Match) {
        // Match 5' barcode if present
        Result barcodeMatch5 = null;
        Integer barcodeIndex5 = null;

        if (!barcodes5.isEmpty()) {
            for (int i = 0; i < barcodes5.size(); i++) {
                // Search left of 5' primer
                Result currentMatch = bitapDistance.indexOf(
                        contig,
                        barcodes5.get(i),
                        barcodeTolerance,
                        0,
                        primer5Match.index
                );

                // Keep best match within tolerance
                if (currentMatch != null &&
                        currentMatch.errors <= barcodeTolerance &&
                        (barcodeMatch5 == null || currentMatch.errors < barcodeMatch5.errors)) {
                    barcodeMatch5 = currentMatch;
                    barcodeIndex5 = i;
                }
            }
        }

        // Match 3' barcode if present
        Result barcodeMatch3 = null;
        Integer barcodeIndex3 = null;

        if (!barcodes3.isEmpty() && primer3Match != null) {
            for (int i = 0; i < barcodes3.size(); i++) {
                // Search right of 3' primer
                Result currentMatch = bitapDistance.indexOf(
                        contig,
                        barcodes3.get(i),
                        barcodeTolerance,
                        primer3Match.index + primer3.length,
                        contig.length
                );

                // Keep best match within tolerance
                if (currentMatch != null &&
                        currentMatch.errors <= barcodeTolerance &&
                        (barcodeMatch3 == null || currentMatch.errors < barcodeMatch3.errors)) {
                    barcodeMatch3 = currentMatch;
                    barcodeIndex3 = i;
                }
            }
        }

        // Determine selection cycle based on barcode matches

        // Both barcodes present - must match same cycle
        if (!barcodes5.isEmpty() && !barcodes3.isEmpty()) {
            if (barcodeIndex5 != null && barcodeIndex5.equals(barcodeIndex3)) {
                return cycles.get(barcodeIndex5);
            }
            return null;
        }

        // Only 5' barcode present
        if (!barcodes5.isEmpty() && barcodeIndex5 != null) {
            return cycles.get(barcodeIndex5);
        }

        // Only 3' barcode present
        if (!barcodes3.isEmpty() && barcodeIndex3 != null) {
            return cycles.get(barcodeIndex3);
        }

        // No barcodes matched
        return null;
    }

    /**
     * Checks if two matched sequences overlap on the contig.
     *
     * @param match1 first match result
     * @param seq1 first sequence
     * @param match2 second match result
     * @param seq2 second sequence
     * @return true if sequences overlap, false otherwise
     */
    private boolean isOverlapped(Result match1, byte[] seq1, Result match2, byte[] seq2) {
        // Define boundaries
        int s1Start = match1.index;
        int s1End = s1Start + seq1.length - 1;

        int s2Start = match2.index;
        int s2End = s2Start + seq2.length - 1;

        // Check for overlap
        return (s1End >= s2Start && s1End >= s2End) || (s1Start <= s2End && s1Start >= s2Start);
    }

    /**
     * Adds quality scores from the current read to the metadata accumulators.
     * Assumes all reads are of the same length within a selection cycle.
     */
    private void addQualityScores() {
        String cycleName = currentRead.selection_cycle.getName();

        // Add forward read quality scores
        if (currentRead.forward_quality != null) {
            ConcurrentHashMap<Integer, Accumulator> forward =
                    metadata.qualityScoresForward.get(cycleName);

            if (forward != null) {
                for (int i = 0; i < currentRead.forward_quality.length; i++) {
                    forward.computeIfAbsent(i, k -> new Accumulator())
                            .addDataValue(currentRead.forward_quality[i] - 33); // Convert from Phred+33
                }
            }
        }

        // Add reverse read quality scores
        if (currentRead.reverse_quality != null) {
            ConcurrentHashMap<Integer, Accumulator> reverse =
                    metadata.qualityScoresReverse.get(cycleName);

            if (reverse != null) {
                for (int i = 0; i < currentRead.reverse_quality.length; i++) {
                    reverse.computeIfAbsent(i, k -> new Accumulator())
                            .addDataValue(currentRead.reverse_quality[i] - 33); // Convert from Phred+33
                }
            }
        }
    }

    /**
     * Adds nucleotide distributions from the current read to the metadata.
     * Tracks the frequency of each nucleotide at each position.
     */
    private void addNucleotideDistributions() {
        String cycleName = currentRead.selection_cycle.getName();

        // Add forward read nucleotide distribution
        if (currentRead.forward_read != null) {
            ConcurrentHashMap<Integer, ConcurrentHashMap<Byte, Integer>> forward =
                    metadata.nucleotideDistributionForward.get(cycleName);

            if (forward != null) {
                for (int i = 0; i < currentRead.forward_read.length; i++) {
                    ConcurrentHashMap<Byte, Integer> nucleotideCounts =
                            forward.computeIfAbsent(i, k -> createNucleotideCountMap());

                    byte nucleotide = currentRead.forward_read[i];
                    nucleotideCounts.merge(nucleotide, 1, Integer::sum);
                }
            }
        }

        // Add reverse read nucleotide distribution
        if (currentRead.reverse_read != null) {
            ConcurrentHashMap<Integer, ConcurrentHashMap<Byte, Integer>> reverse =
                    metadata.nucleotideDistributionReverse.get(cycleName);

            if (reverse != null) {
                for (int i = 0; i < currentRead.reverse_read.length; i++) {
                    ConcurrentHashMap<Byte, Integer> nucleotideCounts =
                            reverse.computeIfAbsent(i, k -> createNucleotideCountMap());

                    byte nucleotide = currentRead.reverse_read[i];
                    nucleotideCounts.merge(nucleotide, 1, Integer::sum);
                }
            }
        }
    }

    /**
     * Adds nucleotide distribution of the accepted randomized region to metadata.
     * Categorizes distributions by the length of the randomized region.
     *
     * @param cycle the selection cycle for this read
     * @param contig the full contig sequence
     * @param rrStart start index of randomized region
     * @param rrEnd end index of randomized region
     */
    private void addAcceptedNucleotideDistributions(
            SelectionCycle cycle,
            byte[] contig,
            int rrStart,
            int rrEnd) {

        int rrSize = rrEnd - rrStart;
        String cycleName = cycle.getName();

        // Get or create the size-specific distribution map
        ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, ConcurrentHashMap<Byte, Integer>>> acceptedDistributions =
                metadata.nucleotideDistributionAccepted.get(cycleName);

        if (acceptedDistributions == null) {
            log.warn("No accepted distribution map found for cycle: {}", cycleName);
            return;
        }

        ConcurrentHashMap<Integer, ConcurrentHashMap<Byte, Integer>> sizeMap =
                acceptedDistributions.computeIfAbsent(rrSize, k -> new ConcurrentHashMap<>());

        // Add nucleotide counts for each position in the randomized region
        int position = 0;
        for (int i = rrStart; i < rrEnd; i++) {
            ConcurrentHashMap<Byte, Integer> nucleotideCounts =
                    sizeMap.computeIfAbsent(position, k -> createNucleotideCountMap());

            byte nucleotide = contig[i];
            nucleotideCounts.merge(nucleotide, 1, Integer::sum);

            position++;
        }
    }

    /**
     * Creates a new nucleotide count map initialized with zeros.
     *
     * @return a map with entries for A, C, G, T, and N
     */
    private ConcurrentHashMap<Byte, Integer> createNucleotideCountMap() {
        ConcurrentHashMap<Byte, Integer> map = new ConcurrentHashMap<>(5);
        map.put((byte) 'A', 0);
        map.put((byte) 'C', 0);
        map.put((byte) 'G', 0);
        map.put((byte) 'T', 0);
        map.put((byte) 'N', 0);
        return map;
    }
}