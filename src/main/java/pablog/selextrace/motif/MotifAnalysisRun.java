package pablog.selextrace.motif;

import java.util.ArrayList;
import java.util.List;

public record MotifAnalysisRun(
        List<String> roundNames,
        List<MotifAnalysisProfile> profiles,
        int significantKmerCount,
        int lastRoundCount
) {
    public MotifAnalysisRun {
        roundNames = roundNames == null ? List.of() : List.copyOf(new ArrayList<>(roundNames));
        profiles = profiles == null ? List.of() : List.copyOf(new ArrayList<>(profiles));
    }
}