package pablog.aptasuite.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;
import pablog.aptasuite.config.ExperimentConfiguration;
import pablog.aptasuite.dto.CreateExperimentDtos;
import pablog.aptasuite.dto.ExperimentSummaryDTO;
import pablog.aptasuite.dto.response.ExperimentDTO;
import pablog.aptasuite.mapper.ConfigurationMapper;
import pablog.aptasuite.model.persistence.ExperimentRecord;
import pablog.aptasuite.service.ExperimentPersistenceService;
import pablog.aptasuite.service.ExperimentProcessor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/experiments")
@CrossOrigin(origins = "*")
public class ExperimentController {

    private final ConfigurationMapper configurationMapper;
    private final ExperimentPersistenceService experimentPersistenceService;

    public ExperimentController(
            ConfigurationMapper configurationMapper,
            ExperimentPersistenceService experimentPersistenceService
    ) {
        this.configurationMapper = configurationMapper;
        this.experimentPersistenceService = experimentPersistenceService;
    }

    @GetMapping
    public List<ExperimentSummaryDTO> getAllExperiments() {
        return experimentPersistenceService.findExperimentSummaries();
    }

    @GetMapping("/{id}")
    public ExperimentDTO getExperiment(@PathVariable String id) {
        return experimentPersistenceService.findExperimentResponseById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Experiment not found in PostgreSQL"));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ExperimentDTO createExperiment(
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

        ExperimentConfiguration config = configurationMapper.fromDto(dto, forwardFiles, reverseFiles);

        ExperimentProcessor processor = new ExperimentProcessor(config);
        processor.processData();
        ExperimentRecord record = processor.buildExperimentRecord();
        return experimentPersistenceService.persistExperiment(record);
    }
}