package pablog.selextrace.domain.experiment;

import pablog.selextrace.domain.metadata.Metadata;
import pablog.selextrace.domain.pool.AptamerPool;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public class Experiment implements Serializable {

    private static final long serialVersionUID = -8897258365542742275L;

    private final String name;
    private final String description;
    private final List<SelectionCycle> selectionCycles;
    private final Metadata metadata;
    private final AptamerPool pool;

    // TODO: Try to remove public access
    /// Private constructor used by the Builder.
    public Experiment(String name, String description,
               List<SelectionCycle> selectionCycles,
               Metadata metadata,
               AptamerPool pool) {
        this.name = Objects.requireNonNull(name, "Name is required");
        this.description = description;
        this.selectionCycles = Objects.requireNonNull(selectionCycles, "Selection cycles are required");
        this.metadata = Objects.requireNonNull(metadata, "Metadata is required");
        this.pool = Objects.requireNonNull(pool, "Pool is required");
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<SelectionCycle> getSelectionCycles() {
        return selectionCycles;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public AptamerPool getPool() {
        return pool;
    }
}
