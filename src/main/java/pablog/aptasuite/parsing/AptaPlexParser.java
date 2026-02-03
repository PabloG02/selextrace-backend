/**
 * 
 */
package pablog.aptasuite.parsing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pablog.aptasuite.config.ExperimentConfiguration;
import pablog.aptasuite.domain.experiment.Experiment;
import pablog.aptasuite.domain.experiment.SelectionCycle;
import pablog.aptasuite.domain.metadata.AptaPlexProgress;
import pablog.aptasuite.domain.metadata.Metadata;
import pablog.aptasuite.domain.metadata.ParserProgress;
import pablog.aptasuite.domain.metadata.ParserStat;
import pablog.aptasuite.parsing.pipeline.AptaPlexConsumer;
import pablog.aptasuite.parsing.pipeline.AptaPlexProducer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.*;

/**
 * @author Jan Hoinka Java implementation of AptaPlex as described in Hoinka et
 *         al. 2016, Methods This class controls the producer-consumer pattern
 *         implemented in lib.parser.aptaplex
 */
public class AptaPlexParser implements Parser, Runnable {

	private static final Logger log = LoggerFactory.getLogger(AptaPlexParser.class);

	/**
	 * Experiment configuration containing all parsing parameters
	 */
	private final ExperimentConfiguration config;
	private final Experiment experiment;

	/**
	 * The progress of the parser instance. Writable to the consumers and thread-safe
	 */
	private static AptaPlexProgress progress;
	
	/**
	 * Instance of the exporter that will be passed to all consumer threads
	 * Pair.first = forward lane, Pair.second = reverse lane;
	 */
//	private Map<Path, Pair<ExportWriter,ExportWriter>> undeterminedExportWriterMap = null;
	
	public AptaPlexParser(ExperimentConfiguration config, Experiment experiment) {
		this.config = config;
		this.experiment = experiment;
		progress = new AptaPlexProgress();
	}
	
	public void parse() {

		// Creating shared object
		BlockingQueue<Object> sharedQueue = new ArrayBlockingQueue<>(config.AptaplexParser.BlockingQueueSize);

		// We need to know how many threads we can use on the system
		int num_threads = Math.min(Runtime.getRuntime().availableProcessors(), config.Performance.maxNumberOfCores);

		// We need to pass a write if failed reads are to be written to file
//		if (Configuration.getParameters().getBoolean("AptaplexParser.UndeterminedToFile")) {
//
//			// we need to make sure the export folder is available
//			Path directory = Paths.get(Configuration.getParameters().getString("Experiment.projectPath"), "export");
//		    if (! directory.toFile().exists()){
//		        directory.toFile().mkdir();
//		        // If you require it to make the entire directory path including parents,
//		        // use directory.mkdirs(); here instead.
//		    }
//
//			undeterminedExportWriterMap = new HashMap<Path, Pair<ExportWriter,ExportWriter>>();
//
//		}
		
		// Creating Producer and Consumer Threads using the ExecutorService to manage them
		ExecutorService es = Executors.newCachedThreadPool();
//		es.execute(new Thread(new AptaPlexProducer(sharedQueue, undeterminedExportWriterMap), "AptaPlex Producer"));
//		es.execute(new Thread(new AptaPlexConsumer(sharedQueue, progress, undeterminedExportWriterMap), "AptaPlex Consumer 1"));
//
//		for (int x=1; x<num_threads-1; x++){
//			es.execute(new Thread(new AptaPlexConsumer(sharedQueue, progress, undeterminedExportWriterMap), "AptaPlex Consumer " + (x+1)));
//		}

		es.execute(new Thread(new AptaPlexProducer(sharedQueue, config, experiment.getSelectionCycles()), "AptaPlex Producer"));
		es.execute(new Thread(new AptaPlexConsumer(sharedQueue, progress, config, experiment.getMetadata(), experiment.getSelectionCycles()), "AptaPlex Consumer 1"));

		for (int x=1; x<num_threads-1; x++){
			es.execute(new Thread(new AptaPlexConsumer(sharedQueue, progress, config, experiment.getMetadata(), experiment.getSelectionCycles()), "AptaPlex Consumer " + (x+1)));
		}
		
		// Make sure threads are GCed once completed
		es.shutdown();
		
		// Wait until all threads are done
		try {
			es.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS); //wait forever
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// At this point we have to close all file handles in the undetermined lanes
//		if (undeterminedExportWriterMap != null) {
//
//			for ( Entry<Path, Pair<ExportWriter, ExportWriter>> entry : this.undeterminedExportWriterMap.entrySet()) {
//
//				entry.getValue().first.close();
//				if (entry.getValue().second != null) {
//					entry.getValue().second.close();
//				}
//			}
//		}
		
	}

	public void parsingCompleted() {

		// Now that we have the data set any pools and cycles to read only
		this.experiment.getPool().setReadOnly();
		for (SelectionCycle cycle : this.experiment.getSelectionCycles()) {
			if (cycle != null) {
				cycle.setReadOnly();
			}
		}
		
		// Store the final progress data to the metadata statistics
		Metadata metadata = this.experiment.getMetadata();
		
		metadata.parserStatistics.put(ParserStat.PROCESSED_READS, progress.totalProcessedReads.get());
		metadata.parserStatistics.put(ParserStat.ACCEPTED_READS, progress.totalAcceptedReads.get());
		metadata.parserStatistics.put(ParserStat.CONTIG_ASSEMBLY_FAILS, progress.totalContigAssemblyFails.get());
		metadata.parserStatistics.put(ParserStat.INVALID_ALPHABET, progress.totalInvalidContigs.get());
		metadata.parserStatistics.put(ParserStat.FIVE_PRIME_ERROR, progress.totalUnmatchablePrimer5.get());
		metadata.parserStatistics.put(ParserStat.THREE_PRIME_ERROR, progress.totalUnmatchablePrimer3.get());
		metadata.parserStatistics.put(ParserStat.INVALID_CYCLE, progress.totalInvalidCycle.get());
		metadata.parserStatistics.put(ParserStat.TOTAL_PRIMER_OVERLAPS, progress.totalPrimerOverlaps.get());
		
		log.info("Parsing Completed, Data storage set to read-only and metadata written to file");
	}

	public ParserProgress Progress() {
		return progress;
	}

	@Override
	public void run() {
		
		parse();
		
		parsingCompleted();
		
	}

}
