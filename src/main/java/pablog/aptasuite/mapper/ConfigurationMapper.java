package pablog.aptasuite.mapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import pablog.aptasuite.config.ExperimentConfiguration;
import pablog.aptasuite.dto.CreateExperimentDtos;
import pablog.aptasuite.parsing.io.FastqReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class ConfigurationMapper {

    private final Path baseDir;

    public ConfigurationMapper(@Value("${aptasuite.files.upload-dir}") String baseDir) {
        this.baseDir = Paths.get(baseDir);
    }

    public ExperimentConfiguration fromDto(
            CreateExperimentDtos.CreateExperimentDto dto,
            Map<String, MultipartFile> forwardFiles,
            Map<String, MultipartFile> reverseFiles
    ) {
        ExperimentConfiguration config = ExperimentConfiguration.defaults();

        // --- Experiment ---
        config.Experiment.name = dto.name();
        config.Experiment.description = dto.description();

        // Primers
        if (dto.sequencing() != null && dto.sequencing().primers() != null) {
            config.Experiment.primer5 = dto.sequencing().primers().fivePrime();
            config.Experiment.primer3 = dto.sequencing().primers().threePrime();
        }

        // Randomized region
        if (dto.sequencing() != null && dto.sequencing().randomizedRegion() != null) {
            switch (dto.sequencing().randomizedRegion()) {
                case CreateExperimentDtos.ExactLengthRandomizedRegion exact ->
                        config.Experiment.randomizedRegionSize = exact.exactLength();
                case CreateExperimentDtos.RangeRandomizedRegion range -> {
                        config.AptaplexParser.randomizedRegionSizeLowerBound = range.min();
                        config.AptaplexParser.randomizedRegionSizeUpperBound = range.max();
                }
            }
        }

        // --- SelectionCycle ---
        var cycles = dto.selectionCycles();
        if (cycles != null && !cycles.isEmpty()) {
            var cycle = cycles.getFirst();
            config.SelectionCycle.name = cycle.roundName();
            config.SelectionCycle.round = cycle.roundNumber();
            config.SelectionCycle.isControlSelection = cycle.isControl();
            config.SelectionCycle.isCounterSelection = cycle.isCounterSelection();
        }

        // --- Sequencing ---
        if (dto.sequencing() != null) {
            config.AptaplexParser.isPerFile = dto.sequencing().isDemultiplexed();

            // The parser backend — assuming only FASTQ supported for now
            switch (dto.sequencing().fileFormat()) {
                // case FASTA -> config.AptaplexParser.backend = FastaReader.class;
                case FASTQ -> config.AptaplexParser.backend = FastqReader.class;
            }
        }

        // Handle uploaded files
        try {
            if (cycles != null && !cycles.isEmpty()) {
                var firstCycle = cycles.getFirst();
                String cycleName = firstCycle.roundName();

                List<String> forwardPaths = new ArrayList<>();
                List<String> reversePaths = new ArrayList<>();

                // Save forward files for this cycle
                if (forwardFiles != null && forwardFiles.containsKey(cycleName)) {
                    String path = saveMultipartFile(forwardFiles.get(cycleName), dto.name(), cycleName, "forward");
                    if (path != null) forwardPaths.add(path);
                }

                // Save reverse files for this cycle
                if (reverseFiles != null && reverseFiles.containsKey(cycleName)) {
                    String path = saveMultipartFile(reverseFiles.get(cycleName), dto.name(), cycleName, "reverse");
                    if (path != null) reversePaths.add(path);
                }

                config.AptaplexParser.forwardFiles = forwardPaths.toArray(new String[0]);
                config.AptaplexParser.reverseFiles = reversePaths.toArray(new String[0]);
            } else {
                config.AptaplexParser.forwardFiles = new String[0];
                config.AptaplexParser.reverseFiles = new String[0];
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to save uploaded files", e);
        }

        return config;
    }

    private String saveMultipartFile(
            MultipartFile file,
            String experimentName,
            String cycleName,
            String direction
    ) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        // Build subdirectories per experiment and cycle
        Path uploadDir = baseDir.resolve(Paths.get(experimentName, cycleName));
        Files.createDirectories(uploadDir);

        // Use original filename (fallback to generated one if null)
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            originalFilename = direction + "_" + System.currentTimeMillis();
        }

        // Compute target path and save
        Path filePath = uploadDir.resolve(originalFilename);
        file.transferTo(filePath.toFile());

        return filePath.toAbsolutePath().toString();
    }

}