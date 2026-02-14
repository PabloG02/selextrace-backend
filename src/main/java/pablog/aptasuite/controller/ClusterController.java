package pablog.aptasuite.controller;

import org.springframework.web.bind.annotation.*;
import pablog.aptasuite.config.AptaClusterConfiguration;
import pablog.aptasuite.model.ClusterAnalysis;
import pablog.aptasuite.service.AptaClusterService;

import java.util.List;

@RestController
@RequestMapping("/api/experiments/{experimentId}/clusters")
@CrossOrigin(origins = "*")
public class ClusterController {

    private final AptaClusterService aptaClusterService;

    public ClusterController(AptaClusterService aptaClusterService) {
        this.aptaClusterService = aptaClusterService;
    }

    @GetMapping
    public List<ClusterAnalysis> listAnalyses(@PathVariable String experimentId) {
        return aptaClusterService.listAnalyses(experimentId);
    }

    @GetMapping("/{analysisId}")
    public ClusterAnalysis getAnalysis(@PathVariable String experimentId, @PathVariable String analysisId) {
        return aptaClusterService.getAnalysis(experimentId, analysisId);
    }

    @PostMapping
    public ClusterAnalysis createAnalysis(
            @PathVariable String experimentId,
            @RequestBody(required = false) AptaClusterConfiguration request
    ) {
        return aptaClusterService.createAnalysis(experimentId, request);
    }
}
