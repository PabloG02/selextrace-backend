package pablog.aptasuite.parsing.io;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the parsing logic for FASTQ files.
 *
 * @author Jan Hoinka
 */
public class FastqReader implements Reader {

    private static final Logger log = LoggerFactory.getLogger(FastqReader.class);

    /** The buffered reader for the forward file */
    private BufferedReader forwardReader;

    /** The buffered reader for the reverse file */
    private BufferedReader reverseReader;

    /** The path for the forward file */
    private Path forwardFile;

    /** The path for the reverse file */
    private Path reverseFile;

    /** Buffers for forward and reverse lines */
    private String buffer;

    /**
     * Constructor
     *
     * @param forwardFile forward reads in FASTQ format, optionally gzip compressed
     * @param reverseFile reverse reads in FASTQ format, optionally gzip compressed.
     *                    Null if single-end sequencing was performed
     */
    public FastqReader(Path forwardFile, Path reverseFile) {
        this.forwardFile = forwardFile;
        this.reverseFile = reverseFile;

        // Initialize readers (gzip or plain)
        this.forwardReader = openReader(forwardFile, "forward");
        if (reverseFile != null) {
            this.reverseReader = openReader(reverseFile, "reverse");
        }
    }

    private BufferedReader openReader(Path file, String label) {
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new GZIPInputStream(new FileInputStream(file.toFile()))));
            log.info("Opened gzip-compressed {} file in FASTQ format: {}", label, file);
            return reader;
        } catch (IOException e) {
            // Try as plain text
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file.toFile())));
                log.info("Opened {} file in FASTQ format: {}", label, file);
                return reader;
            } catch (FileNotFoundException e1) {
                log.error("Error opening {} file: {}", label, file, e1);
                throw new RuntimeException("Could not open " + label + " file: " + file, e1);
            }
        }
    }

    @Override
    public Read getNextRead() {
        Read r = new Read();
        try {
            // Forward reads (4 lines per record)
            buffer = forwardReader.readLine();
            if (buffer == null) {
                return null;
            }

            r.forward_read = forwardReader.readLine().getBytes();
            r.metadata_forward = (buffer + "\n" + forwardReader.readLine()).getBytes();
            r.forward_quality = forwardReader.readLine().getBytes();

            // Reverse reads, if applicable
            if (reverseReader != null) {
                buffer = reverseReader.readLine();
                if (buffer == null) {
                    return null;
                }

                r.reverse_read = reverseReader.readLine().getBytes();
                r.metadata_reverse = (buffer + "\n" + reverseReader.readLine()).getBytes();
                r.reverse_quality = reverseReader.readLine().getBytes();
            }

        } catch (IOException e) {
            log.error("Error while parsing FASTQ files.", e);
            throw new RuntimeException("Error while parsing FASTQ files.", e);
        }

        return r;
    }

    @Override
    public void close() {
        try {
            if (forwardReader != null) {
                forwardReader.close();
            }
        } catch (IOException e) {
            log.warn("Error closing forward file: {}", forwardFile, e);
        }

        try {
            if (reverseReader != null) {
                reverseReader.close();
            }
        } catch (IOException e) {
            log.warn("Error closing reverse file: {}", reverseFile, e);
        }
    }
}
