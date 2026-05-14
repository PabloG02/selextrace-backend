package pablog.selextrace.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pablog.selextrace.config.FsbcConfiguration;
import pablog.selextrace.dto.response.FsbcAnalysisDTO;
import pablog.selextrace.service.FsbcAnalysisService;

import java.util.List;

@RestController
@RequestMapping("/api/experiments/{experimentId}/fsbc")
@CrossOrigin(origins = "*")
public class FsbcController {

    private final FsbcAnalysisService fsbcAnalysisService;

    public FsbcController(FsbcAnalysisService fsbcAnalysisService) {
        this.fsbcAnalysisService = fsbcAnalysisService;
    }

    @GetMapping
    @PreAuthorize("hasPermission(#experimentId, 'experiment', 'view')")
    public List<FsbcAnalysisDTO> listAnalyses(@PathVariable Long experimentId) {
        return fsbcAnalysisService.listAnalyses(experimentId);
    }

    @GetMapping("/{analysisId}")
    @PreAuthorize("hasPermission(#experimentId, 'experiment', 'view')")
    public FsbcAnalysisDTO getAnalysis(@PathVariable Long experimentId, @PathVariable Long analysisId) {
        return fsbcAnalysisService.getAnalysis(experimentId, analysisId);
    }

    @PostMapping
    @PreAuthorize("hasPermission(#experimentId, 'experiment', 'manage')")
    public FsbcAnalysisDTO createAnalysis(
            @PathVariable Long experimentId,
            @RequestBody(required = false) FsbcConfiguration request
    ) {
        return fsbcAnalysisService.createAnalysis(experimentId, request);
    }

    @DeleteMapping("/{analysisId}")
    @PreAuthorize("hasPermission(#experimentId, 'experiment', 'manage')")
    public void deleteAnalysis(@PathVariable Long experimentId, @PathVariable Long analysisId) {
        fsbcAnalysisService.deleteAnalysis(experimentId, analysisId);
    }
}
