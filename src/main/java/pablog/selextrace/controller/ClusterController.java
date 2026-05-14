package pablog.selextrace.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pablog.selextrace.config.AptaClusterConfiguration;
import pablog.selextrace.dto.response.ClusterAnalysisDTO;
import pablog.selextrace.service.AptaClusterService;

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
    @PreAuthorize("hasPermission(#experimentId, 'experiment', 'view')")
    public List<ClusterAnalysisDTO> listAnalyses(@PathVariable Long experimentId) {
        return aptaClusterService.listAnalyses(experimentId);
    }

    @GetMapping("/{analysisId}")
    @PreAuthorize("hasPermission(#experimentId, 'experiment', 'view')")
    public ClusterAnalysisDTO getAnalysis(@PathVariable Long experimentId, @PathVariable Long analysisId) {
        return aptaClusterService.getAnalysis(experimentId, analysisId);
    }

    @PostMapping
    @PreAuthorize("hasPermission(#experimentId, 'experiment', 'manage')")
    public ClusterAnalysisDTO createAnalysis(
            @PathVariable Long experimentId,
            @RequestBody(required = false) AptaClusterConfiguration request
    ) {
        return aptaClusterService.createAnalysis(experimentId, request);
    }

    @DeleteMapping("/{analysisId}")
    @PreAuthorize("hasPermission(#experimentId, 'experiment', 'manage')")
    public void deleteAnalysis(@PathVariable Long experimentId, @PathVariable Long analysisId) {
        aptaClusterService.deleteAnalysis(experimentId, analysisId);
    }
}
