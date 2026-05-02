package pablog.selextrace.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pablog.selextrace.config.AptaTraceConfiguration;
import pablog.selextrace.model.MotifAnalysis;
import pablog.selextrace.service.MotifAnalysisService;

import java.util.List;

@RestController
@RequestMapping("/api/experiments/{experimentId}/motifs")
@CrossOrigin(origins = "*")
public class MotifController {

    private final MotifAnalysisService motifAnalysisService;

    public MotifController(MotifAnalysisService motifAnalysisService) {
        this.motifAnalysisService = motifAnalysisService;
    }

    @GetMapping
    @PreAuthorize("hasPermission(#experimentId, 'experiment', 'view')")
    public List<MotifAnalysis> listAnalyses(@PathVariable String experimentId) {
        return motifAnalysisService.listAnalyses(experimentId);
    }

    @GetMapping("/{analysisId}")
    @PreAuthorize("hasPermission(#experimentId, 'experiment', 'view')")
    public MotifAnalysis getAnalysis(@PathVariable String experimentId, @PathVariable String analysisId) {
        return motifAnalysisService.getAnalysis(experimentId, analysisId);
    }

    @PostMapping
    @PreAuthorize("hasPermission(#experimentId, 'experiment', 'manage')")
    public MotifAnalysis createAnalysis(
            @PathVariable String experimentId,
            @RequestBody(required = false) AptaTraceConfiguration request
    ) {
        return motifAnalysisService.createAnalysis(experimentId, request);
    }

    @DeleteMapping("/{analysisId}")
    @PreAuthorize("hasPermission(#experimentId, 'experiment', 'manage')")
    public void deleteAnalysis(@PathVariable String experimentId, @PathVariable String analysisId) {
        motifAnalysisService.deleteAnalysis(experimentId, analysisId);
    }
}
