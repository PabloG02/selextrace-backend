package pablog.selextrace.config;

import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public record AptaTraceConfiguration(
        Integer kmerLength,
        Boolean filterClusters,
        Integer alpha
) {
    // Default values for motif analysis configuration
    public AptaTraceConfiguration() {
        this(6, true, 10);
    }

    public static AptaTraceConfiguration mergeWithDefaults(AptaTraceConfiguration overrides) {
        AptaTraceConfiguration defaults = new AptaTraceConfiguration();
        if (overrides == null) {
            return defaults;
        }

        return new AptaTraceConfiguration(
                Objects.requireNonNullElse(overrides.kmerLength(), defaults.kmerLength()),
                Objects.requireNonNullElse(overrides.filterClusters(), defaults.filterClusters()),
                Objects.requireNonNullElse(overrides.alpha(), defaults.alpha())
        );
    }
}
