package pablog.selextrace.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
    public List<MotifAnalysis> listAnalyses(@PathVariable String experimentId) {
        return motifAnalysisService.listAnalyses(experimentId);
    }

    @GetMapping("/{analysisId}")
    public MotifAnalysis getAnalysis(@PathVariable String experimentId, @PathVariable String analysisId) {
        return motifAnalysisService.getAnalysis(experimentId, analysisId);
    }

    @PostMapping
    public MotifAnalysis createAnalysis(
            @PathVariable String experimentId,
            @RequestBody(required = false) AptaTraceConfiguration request
    ) {
        return motifAnalysisService.createAnalysis(experimentId, request);
    }
}