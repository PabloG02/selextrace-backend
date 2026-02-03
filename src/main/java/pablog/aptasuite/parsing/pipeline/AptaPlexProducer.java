package pablog.aptasuite.parsing.pipeline;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pablog.aptasuite.config.ExperimentConfiguration;
import pablog.aptasuite.domain.experiment.SelectionCycle;
import pablog.aptasuite.parsing.io.Read;
import pablog.aptasuite.parsing.io.Reader;

/**
 * Producer component of the AptaPlex parser pipeline.
 * <p>
 * This class implements the producer pattern for parsing sequencing data files.
 * It reads forward and optionally reverse sequencing files, extracts relevant
 * information, and adds read objects to a blocking queue for consumer threads
 * to process.
 * </p>
 * <p>
 * The producer supports both single-end and paired-end sequencing, as well as
 * multiplexed and demultiplexed data formats.
 * </p>
 *
 * @author Jan Hoinka
 */
public class AptaPlexProducer implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(AptaPlexProducer.class);

    /**
     * The blocking queue where parsed reads are placed for consumer processing
     */
    private final BlockingQueue<Object> queue;

    /**
     * Configuration object containing all experiment and parsing parameters
     */
    private final ExperimentConfiguration config;

    /**
     * List of selection cycles from the experiment
     */
    private final List<SelectionCycle> selectionCycles;

    /**
     * Indicates whether data is already demultiplexed (per-file mode)
     * or needs to be multiplexed during parsing
     */
    private final boolean isPerFile;

    /**
     * Total number of reads processed by this producer instance
     */
    private int totalProcessedReads = 0;

    /**
     * Constructs a new AptaPlexProducer.
     *
     * @param queue the blocking queue to populate with parsed reads
     * @param config the experiment configuration containing parsing parameters
     * @param selectionCycles the list of selection cycles from the experiment
     * @throws IllegalArgumentException if queue, config, or selectionCycles is null
     */
    public AptaPlexProducer(BlockingQueue<Object> queue, ExperimentConfiguration config, List<SelectionCycle> selectionCycles) {
        if (queue == null) {
            throw new IllegalArgumentException("Queue cannot be null");
        }
        if (config == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }
        if (selectionCycles == null) {
            throw new IllegalArgumentException("Selection cycles cannot be null");
        }

        this.queue = queue;
        this.config = config;
        this.selectionCycles = selectionCycles;
        this.isPerFile = config.AptaplexParser.isPerFile;

        log.info("AptaPlexProducer initialized with isPerFile={}, {} selection cycles", isPerFile, selectionCycles.size());
    }

    /**
     * Returns the total number of reads processed by this producer.
     *
     * @return the count of processed reads
     */
    public int getTotalProcessedReads() {
        return totalProcessedReads;
    }

    /**
     * Main execution method that processes all sequencing files.
     * <p>
     * This method:
     * <ol>
     *   <li>Validates input files</li>
     *   <li>Iterates through all forward (and optionally reverse) read files</li>
     *   <li>Instantiates the appropriate reader backend</li>
     *   <li>Reads and queues all sequences</li>
     *   <li>Adds a poison pill to signal completion</li>
     * </ol>
     * </p>
     *
     * @throws IllegalStateException if file validation fails or reader instantiation fails
     * @throws RuntimeException if an unexpected error occurs during processing
     */
    @Override
    public void run() {
        try {
            // Retrieve file paths from configuration
            String[] forwardFiles = getForwardFiles();
            String[] reverseFiles = getReverseFiles();

            // Validate file configuration
            validateFileConfiguration(forwardFiles, reverseFiles);

            // Process each file pair
            processFiles(forwardFiles, reverseFiles);

            // Signal completion to consumers
            addPoisonPill();

            log.info("Producer completed successfully. Total reads processed: {}", totalProcessedReads);

        } catch (Exception e) {
            log.error("Fatal error in producer thread", e);
            throw new RuntimeException("Producer thread failed", e);
        }
    }

    /**
     * Retrieves forward read file paths from configuration.
     *
     * @return array of forward file paths
     */
    private String[] getForwardFiles() {
        // TODO: Implement method to extract forward files from config
        return this.config.AptaplexParser.forwardFiles;
    }

    /**
     * Retrieves reverse read file paths from configuration.
     *
     * @return array of reverse file paths, or empty array for single-end sequencing
     */
    private String[] getReverseFiles() {
        // TODO: Implement method to extract reverse files from config
        return this.config.AptaplexParser.reverseFiles;
    }

    /**
     * Validates the file configuration for consistency and existence.
     *
     * @param forwardFiles array of forward read file paths
     * @param reverseFiles array of reverse read file paths
     * @throws IllegalStateException if validation fails
     */
    private void validateFileConfiguration(String[] forwardFiles, String[] reverseFiles) {
        // Check that at least one forward file is specified
        if (forwardFiles == null || forwardFiles.length == 0) {
            log.error("No forward read files specified in configuration");
            throw new IllegalStateException("No forward read files specified. Please check your configuration.");
        }

        // Validate paired-end configuration
        if (reverseFiles != null && reverseFiles.length > 0) {
            if (forwardFiles.length != reverseFiles.length) {
                log.error("File count mismatch: {} forward files, {} reverse files",
                        forwardFiles.length, reverseFiles.length);
                throw new IllegalStateException("The number of forward and reverse read files must be identical.");
            }
            log.info("Processing {} paired-end file pairs", forwardFiles.length);
        } else {
            log.info("Processing {} single-end files", forwardFiles.length);
        }

        // Validate that files exist
        validateFilesExist(forwardFiles, reverseFiles);
    }

    /**
     * Checks that all specified files exist and are readable.
     *
     * @param forwardFiles array of forward read file paths
     * @param reverseFiles array of reverse read file paths
     * @throws IllegalStateException if any file is missing or unreadable
     */
    private void validateFilesExist(String[] forwardFiles, String[] reverseFiles) {
        for (String file : forwardFiles) {
            Path path = Paths.get(file);
            if (!Files.exists(path)) {
                log.error("Forward file does not exist: {}", file);
                throw new IllegalStateException("Forward file not found: " + file);
            }
            if (!Files.isReadable(path)) {
                log.error("Forward file is not readable: {}", file);
                throw new IllegalStateException("Forward file not readable: " + file);
            }
        }

        if (reverseFiles != null) {
            for (String file : reverseFiles) {
                Path path = Paths.get(file);
                if (!Files.exists(path)) {
                    log.error("Reverse file does not exist: {}", file);
                    throw new IllegalStateException("Reverse file not found: " + file);
                }
                if (!Files.isReadable(path)) {
                    log.error("Reverse file is not readable: {}", file);
                    throw new IllegalStateException("Reverse file not readable: " + file);
                }
            }
        }
    }

    /**
     * Processes all forward and reverse file pairs.
     *
     * @param forwardFiles array of forward read file paths
     * @param reverseFiles array of reverse read file paths
     * @throws InterruptedException if thread is interrupted while adding to queue
     */
    private void processFiles(String[] forwardFiles, String[] reverseFiles) throws InterruptedException {
        for (int i = 0; i < forwardFiles.length; i++) {
            Path forwardPath = Paths.get(forwardFiles[i]);
            Path reversePath = (reverseFiles != null && reverseFiles.length > 0)
                    ? Paths.get(reverseFiles[i])
                    : null;

            log.info("Processing file pair {}/{}: forward={}, reverse={}",
                    i + 1, forwardFiles.length, forwardPath.getFileName(),
                    reversePath != null ? reversePath.getFileName() : "none");

            processFilePair(forwardPath, reversePath, i);
        }
    }

    /**
     * Processes a single pair of forward and reverse read files.
     *
     * @param forwardPath path to the forward read file
     * @param reversePath path to the reverse read file, or null for single-end
     * @param fileIndex index of this file pair in the overall file list
     * @throws InterruptedException if thread is interrupted while adding to queue
     */
    private void processFilePair(Path forwardPath, Path reversePath, int fileIndex)
            throws InterruptedException {

        Reader reader = null;
        try {
            // Instantiate the appropriate reader backend
            reader = createReader(forwardPath, reversePath);

            // Process all reads from this file pair
            int fileReadCount = 0;
            Read read = reader.getNextRead();

            while (read != null) {
                // Add metadata to the read
                enrichRead(read, forwardPath, reversePath, fileIndex);

                // Add read to queue for consumer processing
                queue.put(read);
                totalProcessedReads++;
                fileReadCount++;

                // Periodic logging for large files
                if (fileReadCount % 100000 == 0) {
                    log.debug("Processed {} reads from current file", fileReadCount);
                }

                // Get next read
                read = reader.getNextRead();
            }

            log.info("Completed processing file pair. Reads from this file: {}", fileReadCount);

        } finally {
            // Always close the reader to free resources
            if (reader != null) {
                reader.close();
                log.debug("Reader closed for file: {}", forwardPath.getFileName());
            }
        }
    }

    /**
     * Creates and initializes a Reader instance using reflection.
     * <p>
     * The reader class is determined by the configuration and instantiated
     * dynamically to allow for different backend implementations.
     * </p>
     *
     * @param forwardPath path to the forward read file
     * @param reversePath path to the reverse read file, or null
     * @return initialized Reader instance
     * @throws IllegalStateException if reader cannot be instantiated
     */
    private Reader createReader(Path forwardPath, Path reversePath) {
        Class<?> readerClass = config.AptaplexParser.backend;

        if (readerClass == null) {
            log.error("No reader backend specified in configuration");
            throw new IllegalStateException("AptaplexParser.backend not configured");
        }

        try {
            log.debug("Instantiating reader: {}", readerClass.getName());

            // Instantiate reader with file paths
            Reader reader = (Reader) readerClass
                    .getConstructor(Path.class, Path.class)
                    .newInstance(forwardPath, reversePath);

            log.debug("Reader instantiated successfully");
            return reader;

        } catch (NoSuchMethodException e) {
            log.error("Reader class {} does not have required constructor(Path, Path)",
                    readerClass.getName(), e);
            throw new IllegalStateException(
                    "Reader backend missing required constructor: " + readerClass.getName(), e);

        } catch (InvocationTargetException e) {
            log.error("Error invoking reader constructor", e.getCause());
            throw new IllegalStateException(
                    "Failed to invoke reader constructor: " + e.getCause().getMessage(),
                    e.getCause());

        } catch (InstantiationException | IllegalAccessException e) {
            log.error("Cannot instantiate reader class: {}", readerClass.getName(), e);
            throw new IllegalStateException(
                    "Cannot instantiate reader backend: " + readerClass.getName(), e);

        } catch (ClassCastException e) {
            log.error("Configured class {} is not a Reader", readerClass.getName(), e);
            throw new IllegalStateException(
                    "Configured backend is not a valid Reader: " + readerClass.getName(), e);
        }
    }

    /**
     * Enriches a read with metadata about its source and selection cycle.
     *
     * @param read the read to enrich
     * @param forwardPath source forward file path
     * @param reversePath source reverse file path, or null
     * @param fileIndex index of the file pair being processed
     */
    private void enrichRead(Read read, Path forwardPath, Path reversePath, int fileIndex) {
        // Add source file information
        read.source_forward = forwardPath;
        read.source_reverse = reversePath;

        // If processing in per-file mode, assign selection cycle based on file index
        if (isPerFile) {
            read.selection_cycle = selectionCycles.get(fileIndex);
            log.debug("Per-file mode: assigned selection cycle '{}' for file index {}", 
                    selectionCycles.get(fileIndex).getName(), fileIndex);
        }
    }

    /**
     * Adds a poison pill to the queue to signal consumers that processing is complete.
     *
     * @throws RuntimeException if interrupted while adding poison pill
     */
    private void addPoisonPill() {
        try {
            log.info("Adding poison pill to parsing queue");
            queue.put(PoisonPill.INSTANCE);
        } catch (InterruptedException e) {
            log.error("Interrupted while adding poison pill", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to add poison pill to queue", e);
        }
    }
}