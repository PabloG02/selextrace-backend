package pablog.selextrace.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pablog.selextrace.config.FsbcConfiguration;
import pablog.selextrace.model.FsbcAnalysis;
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
    public List<FsbcAnalysis> listAnalyses(@PathVariable String experimentId) {
        return fsbcAnalysisService.listAnalyses(experimentId);
    }

    @GetMapping("/{analysisId}")
    @PreAuthorize("hasPermission(#experimentId, 'experiment', 'view')")
    public FsbcAnalysis getAnalysis(@PathVariable String experimentId, @PathVariable String analysisId) {
        return fsbcAnalysisService.getAnalysis(experimentId, analysisId);
    }

    @PostMapping
    @PreAuthorize("hasPermission(#experimentId, 'experiment', 'manage')")
    public FsbcAnalysis createAnalysis(
            @PathVariable String experimentId,
            @RequestBody(required = false) FsbcConfiguration request
    ) {
        return fsbcAnalysisService.createAnalysis(experimentId, request);
    }

    @DeleteMapping("/{analysisId}")
    @PreAuthorize("hasPermission(#experimentId, 'experiment', 'manage')")
    public void deleteAnalysis(@PathVariable String experimentId, @PathVariable String analysisId) {
        fsbcAnalysisService.deleteAnalysis(experimentId, analysisId);
    }
}
