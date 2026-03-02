package pablog.selextrace.motif;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record MotifAnalysisProfile(
        String seed,
        String consensus,
        double seedPValue,
        double seedProportion,
        double motifProportion,
        int selectionContext,
        List<String> kmers,
        Map<String, String> kmerAlignment,
        List<Integer> aptamerIds,
        List<MotifClusterMember> memberAptamers,
        double[][] pwm,
        double[][] contextTrace
) implements Serializable {

    public MotifAnalysisProfile {
        kmers = kmers == null ? List.of() : List.copyOf(new ArrayList<>(kmers));
        kmerAlignment = kmerAlignment == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(kmerAlignment));
        aptamerIds = aptamerIds == null ? List.of() : List.copyOf(new ArrayList<>(aptamerIds));
        memberAptamers = memberAptamers == null ? List.of() : List.copyOf(new ArrayList<>(memberAptamers));
        pwm = deepCopy(pwm);
        contextTrace = deepCopy(contextTrace);
    }

    private static double[][] deepCopy(double[][] matrix) {
        if (matrix == null) {
            return new double[0][];
        }

        double[][] copy = new double[matrix.length][];
        for (int index = 0; index < matrix.length; index++) {
            copy[index] = matrix[index] == null ? new double[0] : matrix[index].clone();
        }
        return copy;
    }
}