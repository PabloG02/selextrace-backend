package pablog.aptasuite.service;

import org.springframework.stereotype.Service;
import pablog.aptasuite.dto.BppmResponseDTO;
import pablog.aptasuite.dto.ContextProbabilityResponseDTO;
import pablog.aptasuite.dto.MfeResponseDTO;
import pablog.aptasuite.lib.capr.CapR;
import pablog.aptasuite.lib.rnafold.Index;
import pablog.aptasuite.lib.rnafold.MFEData;
import pablog.aptasuite.lib.rnafold.RNAFoldAPI;

import java.util.ArrayList;
import java.util.List;

@Service
public class PredictionService {

    public MfeResponseDTO computeMfe(String sequence) {
        byte[] stringBytes = sequence.getBytes();
        final RNAFoldAPI rnafoldapi = new RNAFoldAPI();
        MFEData result = rnafoldapi.getMFE(stringBytes);

        return new MfeResponseDTO(new String(result.structure), result.mfe);
    }

    public BppmResponseDTO computeBppm(String sequence) {
        int length = sequence.length();
        byte[] stringBytes = sequence.getBytes();

        final RNAFoldAPI rnafoldapi = new RNAFoldAPI();
        double[] result = rnafoldapi.getBppm(stringBytes);

        // Ragged upper-triangle matrix
        List<List<Double>> matrix = new ArrayList<>(length);

        for (int i = 0; i < length; i++) {
            List<Double> row = new ArrayList<>(length - i - 1);

            for (int j = i + 1; j < length; j++) {
                int flatIndex = Index.triu(i, j, length);
                row.add(result[flatIndex]);
            }

            matrix.add(row);
        }

        return new BppmResponseDTO(matrix);
    }

    public ContextProbabilityResponseDTO computeContextProbabilities(String sequence) {
        int length = sequence.length();
        byte[] stringBytes = sequence.getBytes();

        final CapR capr = new CapR();
        capr.ComputeStructuralProfile(stringBytes, length);

        double[] raw = capr.getStructuralProfile();

        List<Double> hairpin = new ArrayList<>(length);
        List<Double> bulge = new ArrayList<>(length);
        List<Double> internal = new ArrayList<>(length);
        List<Double> multi = new ArrayList<>(length);
        List<Double> dangling = new ArrayList<>(length);
        List<Double> paired = new ArrayList<>(length);

        for (int index = 0; index < length; index++) {
            double hairpinValue = raw[0 * length + index];
            double bulgeValue = raw[1 * length + index];
            double internalValue = raw[2 * length + index];
            double multiValue = raw[3 * length + index];
            double danglingValue = raw[4 * length + index];
            double pairedValue = 1 - hairpinValue - bulgeValue - internalValue - multiValue - danglingValue;

            hairpin.add(hairpinValue);
            bulge.add(bulgeValue);
            internal.add(internalValue);
            multi.add(multiValue);
            dangling.add(danglingValue);
            paired.add(pairedValue);
        }

        return new ContextProbabilityResponseDTO(hairpin, bulge, internal, multi, dangling, paired);
    }
}