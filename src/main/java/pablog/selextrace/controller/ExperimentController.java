package pablog.selextrace.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;
import pablog.selextrace.config.ExperimentConfiguration;
import pablog.selextrace.dto.CreateExperimentDtos;
import pablog.selextrace.dto.ExperimentSummaryDTO;
import pablog.selextrace.dto.auth.AuthDtos;
import pablog.selextrace.dto.response.ExperimentDTO;
import pablog.selextrace.mapper.ConfigurationMapper;
import pablog.selextrace.model.persistence.ExperimentRecord;
import pablog.selextrace.security.CurrentUserService;
import pablog.selextrace.service.AuthorizationService;
import pablog.selextrace.service.ExperimentPersistenceService;
import pablog.selextrace.service.ExperimentProcessor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/experiments")
@CrossOrigin(origins = "*")
public class ExperimentController {

    private final ConfigurationMapper configurationMapper;
    private final ExperimentPersistenceService experimentPersistenceService;
    private final CurrentUserService currentUserService;
    private final AuthorizationService authorizationService;

    public ExperimentController(
            ConfigurationMapper configurationMapper,
            ExperimentPersistenceService experimentPersistenceService,
            CurrentUserService currentUserService,
            AuthorizationService authorizationService
    ) {
        this.configurationMapper = configurationMapper;
        this.experimentPersistenceService = experimentPersistenceService;
        this.currentUserService = currentUserService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public List<ExperimentSummaryDTO> getAllExperiments() {
        return experimentPersistenceService.findExperimentSummaries(currentUserService.requireUser());
    }

    @GetMapping("/{id}")
    public ExperimentDTO getExperiment(@PathVariable Long id) {
        return experimentPersistenceService.findExperimentResponseById(id, currentUserService.requireUser())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Experiment not found in PostgreSQL"));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ExperimentDTO createExperiment(
            @RequestPart("data") CreateExperimentDtos.CreateExperimentDto dto,
//            @RequestPart(value = "forwardFiles", required = false) Map<String, MultipartFile> forwardFiles,
//            @RequestPart(value = "reverseFiles", required = false) Map<String, MultipartFile> reverseFiles
            HttpServletRequest request
    ) throws Exception {
        var currentUser = currentUserService.requireUser();

        Long targetProjectId = dto.projectId();
        if (targetProjectId != null) {
            authorizationService.assertCanManageProject(currentUser, targetProjectId);
        }

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
        return experimentPersistenceService.persistExperiment(record, currentUser, targetProjectId);
    }

    @PatchMapping("/{id}/project")
    public ExperimentDTO transferExperimentToProject(
            @PathVariable Long id,
            @RequestBody AuthDtos.ExperimentProjectTransferRequest request
    ) {
        Long targetProjectId = request == null ? null : request.projectId();
        return experimentPersistenceService.transferExperimentToProject(
                currentUserService.requireUser(),
                id,
                targetProjectId
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExperiment(@PathVariable Long id) {
        authorizationService.assertCanManageExperiment(currentUserService.requireUser(), id);
        boolean deleted = experimentPersistenceService.deleteExperiment(id);
        if (!deleted) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Experiment not found in PostgreSQL");
        }
        return ResponseEntity.noContent().build();
    }
}
