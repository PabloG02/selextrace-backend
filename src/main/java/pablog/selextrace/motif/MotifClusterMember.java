package pablog.selextrace.motif;

import java.io.Serial;
import java.io.Serializable;

public record MotifClusterMember(
        int aptamerId,
        int lastRoundCount,
        double lastRoundProportion
) implements Serializable {}