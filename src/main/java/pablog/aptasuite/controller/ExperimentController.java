package pablog.aptasuite.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import pablog.aptasuite.config.ExperimentConfiguration;
import pablog.aptasuite.dto.CreateExperimentDtos;
import pablog.aptasuite.dto.ExperimentOverviewDTO;
import pablog.aptasuite.dto.ExperimentSummaryDTO;
import pablog.aptasuite.mapper.ConfigurationMapper;
import pablog.aptasuite.model.ExperimentOverviewDocument;
import pablog.aptasuite.service.ExperimentProcessor;
import pablog.aptasuite.repository.ExperimentOverviewRepository;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/experiments")
@CrossOrigin(origins = "*")
public class ExperimentController {

    private final ExperimentOverviewRepository overviewRepository;

    public ExperimentController(ExperimentOverviewRepository overviewRepository) {
        this.overviewRepository = overviewRepository;
    }

    @GetMapping
    public List<ExperimentSummaryDTO> getAllExperiments() {
        return overviewRepository.findAll()
                .stream()
                .map(this::toSummary)
                .toList();
    }

    private ExperimentSummaryDTO toSummary(ExperimentOverviewDocument doc) {
        var overview = doc.getOverview();
        var generalInfo = overview.experimentDetails().generalInformation();
        return new ExperimentSummaryDTO(
                doc.getId(),
                generalInfo.name(),
                generalInfo.description(),
                Instant.now()
        );
    }

    @GetMapping("/{id}")
    public ExperimentOverviewDTO getExperiment(@PathVariable String id) {
        return overviewRepository.findById(id)
                .map(ExperimentOverviewDocument::getOverview)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Experiment overview not found in MongoDB"));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ExperimentOverviewDTO createExperiment(
            @RequestPart("data") CreateExperimentDtos.CreateExperimentDto dto,
//            @RequestPart(value = "forwardFiles", required = false) Map<String, MultipartFile> forwardFiles,
//            @RequestPart(value = "reverseFiles", required = false) Map<String, MultipartFile> reverseFiles
            HttpServletRequest request
    ) throws Exception {
        MultipartHttpServletRequest multipart = (MultipartHttpServletRequest) request;

        // Parse forwardFiles[r14] → forwardFiles map with key "r14"
        Map<String, MultipartFile> forwardFiles = new HashMap<>();
        Map<String, MultipartFile> reverseFiles = new HashMap<>();

        multipart.getFileMap().forEach((name, file) -> {
            if (name.startsWith("forwardFiles[")) {
                String key = name.substring("forwardFiles[".length(), name.length() - 1);
                forwardFiles.put(key, file);
            } else if (name.startsWith("reverseFiles[")) {
                String key = name.substring("reverseFiles[".length(), name.length() - 1);
                reverseFiles.put(key, file);
            }
        });

        ExperimentConfiguration config = ConfigurationMapper.fromDto(dto, forwardFiles, reverseFiles);

        ExperimentOverviewDTO overview = new ExperimentProcessor().processData(config);
        return persistOverview(overview);
    }

    private ExperimentOverviewDTO persistOverview(ExperimentOverviewDTO overview) {
        overviewRepository.save(new ExperimentOverviewDocument(overview));
        return overview;
    }
}