package pablog.selextrace.config;

import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public record FsbcConfiguration(
        Integer selectionCycleRound,
        Integer minLength,
        Integer maxLength,
        Boolean rnaSequence
) {
    public FsbcConfiguration() {
        this(null, 5, 10, false);
    }

    public static FsbcConfiguration mergeWithDefaults(FsbcConfiguration overrides) {
        FsbcConfiguration defaults = new FsbcConfiguration();
        if (overrides == null) {
            return defaults;
        }

        return new FsbcConfiguration(
                overrides.selectionCycleRound(),
                Objects.requireNonNullElse(overrides.minLength(), defaults.minLength()),
                Objects.requireNonNullElse(overrides.maxLength(), defaults.maxLength()),
                Objects.requireNonNullElse(overrides.rnaSequence(), defaults.rnaSequence())
        );
    }
}
