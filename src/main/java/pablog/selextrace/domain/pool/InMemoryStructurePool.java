package pablog.selextrace.domain.pool;

import java.io.Serial;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/// In-memory implementation of [StructurePool] backed by ensemble-based
/// structure prediction tools such as CapR, SFold, and RNAfold.
///
/// The layout of the `double[]` structure array depends on the prediction
/// tool used:
///
///   - **CapR / SFold:** Stores the probabilities of five secondary structure
///     contexts — hairpin, bulge, internal, multi-loop, and dangling — concatenated
///     in that order. A sequence of length `N` therefore requires `5*N`
///     elements:
///       - `[0, N)` — hairpin probabilities
///       - `[N, 2N)` — bulge probabilities
///       - `[2N, 3N)` — internal probabilities
///       - `[3N, 4N)` — multi-loop probabilities
///       - `[4N, 5N)` — dangling probabilities
///   - **RNAfold -p:** Stores base-pair probabilities as a linearized upper
///     triangular matrix (diagonal excluded). Index conversion utilities are
///     provided by [utilities.Index].
///
/// @author Jan Hoinka
/// @see StructurePool
public class InMemoryStructurePool implements StructurePool {

    @Serial
    private static final long serialVersionUID = 1L;

    private final AptamerPool aptamerPool;
    private final ConcurrentMap<Integer, double[]> structures = new ConcurrentHashMap<>();
    private volatile boolean readOnly;

    public InMemoryStructurePool(AptamerPool aptamerPool) {
        this.aptamerPool = aptamerPool;
    }

    @Override
    public void registerStructure(int id, double[] structure) {
        if (readOnly)
            throw new IllegalStateException("Structure pool is read-only");
        if (structure == null)
            throw new IllegalArgumentException("Structure profile is required");

        structures.put(id, Arrays.copyOf(structure, structure.length));
    }

    @Override
    public double[] getStructure(int id) {
        double[] structure = structures.get(id);
        return structure == null ? null : Arrays.copyOf(structure, structure.length);
    }

    @Override
    public void close() {
        // No-op for in-memory storage.
    }

    @Override
    public void setReadOnly() {
        readOnly = true;
    }

    @Override
    public void setReadWrite() {
        readOnly = false;
    }

    @Override
    public void commitStructures() {
        // No-op for in-memory storage.
    }

    @Override
    public Iterable<Entry<Integer, double[]>> iterator() {
        return () -> new Iterator<>() {
            private final Iterator<Entry<Integer, double[]>> delegate = structures.entrySet().iterator();

            @Override
            public boolean hasNext() {
                return delegate.hasNext();
            }

            @Override
            public Entry<Integer, double[]> next() {
                Entry<Integer, double[]> entry = delegate.next();
                return new AbstractMap.SimpleEntry<>(entry.getKey(), Arrays.copyOf(entry.getValue(), entry.getValue().length));
            }
        };
    }

    @Override
    public Iterable<Entry<byte[], double[]>> sequence_iterator() {
        return () -> new Iterator<>() {
            private final Iterator<Entry<Integer, double[]>> delegate = iterator().iterator();

            @Override
            public boolean hasNext() {
                return delegate.hasNext();
            }

            @Override
            public Entry<byte[], double[]> next() {
                Entry<Integer, double[]> entry = delegate.next();
                byte[] sequence = aptamerPool.getAptamer(entry.getKey());
                return new AbstractMap.SimpleEntry<>(sequence, entry.getValue());
            }
        };
    }

    @Override
    public int size() {
        return structures.size();
    }
}