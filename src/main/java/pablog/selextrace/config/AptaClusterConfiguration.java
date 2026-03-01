package pablog.selextrace.config;

import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public record AptaClusterConfiguration(
        Integer randomizedRegionSize,
        Integer lshDimension,
        Integer lshIterations,
        Integer editDistance,
        Integer kmerSize,
        Integer kmerCutoffIterations
) {
    // Default values for clustering configuration
    public AptaClusterConfiguration() {
        this(null, null, 5, 5, 3, 10000);
    }

    public static AptaClusterConfiguration mergeWithDefaults(AptaClusterConfiguration overrides) {
        AptaClusterConfiguration defaults = new AptaClusterConfiguration();
        if (overrides == null) {
            return defaults;
        }

        return new AptaClusterConfiguration(
                overrides.randomizedRegionSize(),
                overrides.lshDimension(),
                Objects.requireNonNullElse(overrides.lshIterations(), defaults.lshIterations()),
                Objects.requireNonNullElse(overrides.editDistance(), defaults.editDistance()),
                Objects.requireNonNullElse(overrides.kmerSize(), defaults.kmerSize()),
                Objects.requireNonNullElse(overrides.kmerCutoffIterations(), defaults.kmerCutoffIterations())
        );
    }
}
