package pablog.selextrace.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pablog.selextrace.config.AptaTraceConfiguration;
import pablog.selextrace.dto.response.MotifAnalysisDTO;
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
    public List<MotifAnalysisDTO> listAnalyses(@PathVariable Long experimentId) {
        return motifAnalysisService.listAnalyses(experimentId);
    }

    @GetMapping("/{analysisId}")
    @PreAuthorize("hasPermission(#experimentId, 'experiment', 'view')")
    public MotifAnalysisDTO getAnalysis(@PathVariable Long experimentId, @PathVariable Long analysisId) {
        return motifAnalysisService.getAnalysis(experimentId, analysisId);
    }

    @PostMapping
    @PreAuthorize("hasPermission(#experimentId, 'experiment', 'manage')")
    public MotifAnalysisDTO createAnalysis(
            @PathVariable Long experimentId,
            @RequestBody(required = false) AptaTraceConfiguration request
    ) {
        return motifAnalysisService.createAnalysis(experimentId, request);
    }

    @DeleteMapping("/{analysisId}")
    @PreAuthorize("hasPermission(#experimentId, 'experiment', 'manage')")
    public void deleteAnalysis(@PathVariable Long experimentId, @PathVariable Long analysisId) {
        motifAnalysisService.deleteAnalysis(experimentId, analysisId);
    }
}
