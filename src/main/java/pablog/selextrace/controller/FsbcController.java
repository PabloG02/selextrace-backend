package pablog.selextrace.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
    public List<FsbcAnalysis> listAnalyses(@PathVariable String experimentId) {
        return fsbcAnalysisService.listAnalyses(experimentId);
    }

    @GetMapping("/{analysisId}")
    public FsbcAnalysis getAnalysis(@PathVariable String experimentId, @PathVariable String analysisId) {
        return fsbcAnalysisService.getAnalysis(experimentId, analysisId);
    }

    @PostMapping
    public FsbcAnalysis createAnalysis(
            @PathVariable String experimentId,
            @RequestBody(required = false) FsbcConfiguration request
    ) {
        return fsbcAnalysisService.createAnalysis(experimentId, request);
    }

    @DeleteMapping("/{analysisId}")
    public void deleteAnalysis(@PathVariable String experimentId, @PathVariable String analysisId) {
        fsbcAnalysisService.deleteAnalysis(experimentId, analysisId);
    }
}
