package pablog.selextrace.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import pablog.selextrace.dto.BppmResponseDTO;
import pablog.selextrace.dto.ContextProbabilityResponseDTO;
import pablog.selextrace.dto.MfeResponseDTO;
import pablog.selextrace.service.PredictionService;

@RestController
@RequestMapping("/api/predictions")
@CrossOrigin(origins = "*")
public class PredictionController {

    @Autowired
    private PredictionService predictionService;

    @GetMapping("/mfe")
    public MfeResponseDTO computeMfe(@RequestParam String sequence) {
        if (!StringUtils.hasText(sequence)) {
            throw new IllegalArgumentException("Sequence cannot be empty");
        }
        return predictionService.computeMfe(sequence);
    }

    @GetMapping("/bppm")
    public BppmResponseDTO computeBppm(@RequestParam String sequence) {
        if (!StringUtils.hasText(sequence)) {
            throw new IllegalArgumentException("Sequence cannot be empty");
        }
        return predictionService.computeBppm(sequence);
    }

    @GetMapping("/context-probabilities")
    public ContextProbabilityResponseDTO computeContextProbabilities(@RequestParam String sequence) {
        if (!StringUtils.hasText(sequence)) {
            throw new IllegalArgumentException("Sequence cannot be empty");
        }
        return predictionService.computeContextProbabilities(sequence);
    }
}