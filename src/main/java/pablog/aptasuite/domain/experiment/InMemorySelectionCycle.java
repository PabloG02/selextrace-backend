package pablog.aptasuite.domain.experiment;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import pablog.aptasuite.domain.pool.AptamerPool;

/**
 * Thread-safe in-memory {@link SelectionCycle} implementation. The class keeps
 * track of aptamer counts using concurrent data structures so that multiple
 * consumer threads can safely register reads.
 */
public class InMemorySelectionCycle implements SelectionCycle {

    private static final long serialVersionUID = 1L;

    private final AptamerPool pool;
    private final List<SelectionCycle> allCycles;

    private final String name;
    private final int round;
    private final boolean controlSelection;
    private final boolean counterSelection;

    private final ConcurrentMap<Integer, AtomicInteger> counts = new ConcurrentHashMap<>();
    private final AtomicInteger totalSize = new AtomicInteger();
    private final AtomicInteger uniqueSize = new AtomicInteger();

    private volatile byte[] barcode5Prime;
    private volatile byte[] barcode3Prime;

    public InMemorySelectionCycle(String name, int round, boolean isControlSelection, boolean isCounterSelection, AptamerPool pool, List<SelectionCycle> allCycles) {
        this.name = Objects.requireNonNull(name, "Selection cycle name must not be null");
        this.round = round;
        this.controlSelection = isControlSelection;
        this.counterSelection = isCounterSelection;
        this.pool = Objects.requireNonNull(pool, "Aptamer pool must not be null");
        this.allCycles = Objects.requireNonNull(allCycles, "All cycles list must not be null");
    }

    @Override
    public int addToSelectionCycle(byte[] sequence, int rrStart, int rrEnd) {
        return addToSelectionCycle(sequence, rrStart, rrEnd, 1);
    }

    @Override
    public int addToSelectionCycle(String sequence, int rrStart, int rrEnd) {
        return addToSelectionCycle(sequence.getBytes(), rrStart, rrEnd, 1);
    }

    @Override
    public int addToSelectionCycle(byte[] sequence, int rrStart, int rrEnd, int count) {
        AptamerPool pool = this.pool;
        int id = pool.registerAptamer(sequence, rrStart, rrEnd);

        AtomicInteger existingCounter = counts.putIfAbsent(id, new AtomicInteger(count));
        if (existingCounter == null) {
            uniqueSize.incrementAndGet();
        } else {
            existingCounter.addAndGet(count);
        }

        totalSize.addAndGet(count);
        return id;
    }

    @Override
    public int addToSelectionCycle(String sequence, int rrStart, int rrEnd, int count) {
        return addToSelectionCycle(sequence.getBytes(), rrStart, rrEnd, count);
    }

    @Override
    public boolean containsAptamer(String sequence) {
        return containsAptamer(sequence.getBytes());
    }

    @Override
    public boolean containsAptamer(byte[] sequence) {
        int id = this.pool.getIdentifier(sequence);
        return containsAptamer(id);
    }

    @Override
    public boolean containsAptamer(int id) {
        return counts.containsKey(id);
    }

    @Override
    public int getAptamerCardinality(String sequence) {
        return getAptamerCardinality(sequence.getBytes());
    }

    @Override
    public int getAptamerCardinality(byte[] sequence) {
        int id = this.pool.getIdentifier(sequence);
        return getAptamerCardinality(id);
    }

    @Override
    public int getAptamerCardinality(int id) {
        AtomicInteger value = counts.get(id);
        return value == null ? 0 : value.get();
    }

    @Override
    public int getSize() {
        return totalSize.get();
    }

    @Override
    public int getUniqueSize() {
        return uniqueSize.get();
    }

    @Override
    public SelectionCycle getNextSelectionCycle() {
        List<SelectionCycle> cycles = this.allCycles;
        for (int i = round + 1; i < cycles.size(); i++) {
            SelectionCycle candidate = cycles.get(i);
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    @Override
    public SelectionCycle getPreviousSelectionCycle() {
        List<SelectionCycle> cycles = this.allCycles;
        for (int i = round - 1; i >= 0; i--) {
            SelectionCycle candidate = cycles.get(i);
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    @Override
    public List<SelectionCycle> getControlCycles() {
//        List<List<SelectionCycle>> controls = Configuration.getExperiment().getControlSelectionCycles();
//        List<SelectionCycle> list = controls.size() > round ? controls.get(round) : null;
//        return list == null ? new ArrayList<>() : new ArrayList<>(list);
        // TODO
        return new ArrayList<>();
    }

    @Override
    public List<SelectionCycle> getCounterSelectionCycles() {
//        List<List<SelectionCycle>> counters = Configuration.getExperiment().getCounterSelectionCycles();
//        List<SelectionCycle> list = counters.size() > round ? counters.get(round) : null;
//        return list == null ? new ArrayList<>() : new ArrayList<>(list);
        // TODO
        return new ArrayList<>();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getRound() {
        return round;
    }

    @Override
    public boolean isControlSelection() {
        return controlSelection;
    }

    @Override
    public boolean isCounterSelection() {
        return counterSelection;
    }

    @Override
    public void setReadOnly() {
        // No-op for in-memory representation.
    }

    @Override
    public void setReadWrite() {
        // No-op for in-memory representation.
    }

    @Override
    public void close() {
        // Nothing to close.
    }

    @Override
    public Iterable<Entry<Integer, Integer>> iterator() {
        return () -> new Iterator<Entry<Integer, Integer>>() {
            private final Iterator<Entry<Integer, AtomicInteger>> delegate = counts.entrySet().iterator();

            @Override
            public boolean hasNext() {
                return delegate.hasNext();
            }

            @Override
            public Entry<Integer, Integer> next() {
                Entry<Integer, AtomicInteger> entry = delegate.next();
                return new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue().get());
            }
        };
    }

    @Override
    public Iterable<Entry<byte[], Integer>> sequence_iterator() {
        return () -> new Iterator<Entry<byte[], Integer>>() {
            private final Iterator<Entry<Integer, Integer>> delegate = iterator().iterator();

            @Override
            public boolean hasNext() {
                return delegate.hasNext();
            }

            @Override
            public Entry<byte[], Integer> next() {
                Entry<Integer, Integer> entry = delegate.next();
                byte[] sequence = InMemorySelectionCycle.this.pool.getAptamer(entry.getKey());
                return new AbstractMap.SimpleEntry<>(sequence, entry.getValue());
            }
        };
    }

    @Override
    public Iterable<Integer> id_iterator() {
        return () -> counts.keySet().iterator();
    }

    @Override
    public void setBarcodeFivePrime(byte[] barcode) {
        this.barcode5Prime = barcode == null ? null : barcode.clone();
    }

    @Override
    public byte[] getBarcodeFivePrime() {
        return barcode5Prime == null ? null : barcode5Prime.clone();
    }

    @Override
    public void setBarcodeThreePrime(byte[] barcode) {
        this.barcode3Prime = barcode == null ? null : barcode.clone();
    }

    @Override
    public byte[] getBarcodeThreePrime() {
        return barcode3Prime == null ? null : barcode3Prime.clone();
    }

    @Override
    public String toString() {
        return name;
    }
}
