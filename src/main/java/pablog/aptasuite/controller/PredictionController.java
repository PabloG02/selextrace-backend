package pablog.aptasuite.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import pablog.aptasuite.dto.BppmResponseDTO;
import pablog.aptasuite.dto.ContextProbabilityResponseDTO;
import pablog.aptasuite.dto.MfeResponseDTO;
import pablog.aptasuite.service.PredictionService;

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