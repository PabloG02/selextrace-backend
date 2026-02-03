package pablog.aptasuite.domain.pool;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * In-memory implementation of the AptamerPool interface.
 * This implementation stores all data in memory using concurrent hash maps
 * for fast access and thread-safe operations.
 */
public class InMemoryAptamerPool implements AptamerPool {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(InMemoryAptamerPool.class);

    /**
     * Forward map: aptamer sequence (as byte array key) -> unique ID
     * Using ByteArrayWrapper to enable proper equals/hashCode for byte arrays
     */
    private final ConcurrentMap<ByteArrayWrapper, Integer> aptamerToId;

    /**
     * Reverse map: unique ID -> aptamer sequence
     */
    private final ConcurrentMap<Integer, byte[]> idToAptamer;

    /**
     * Bounds map: unique ID -> bounds array [start_inclusive, end_exclusive]
     */
    private final ConcurrentMap<Integer, int[]> idToBounds;

    /**
     * Atomic counter for generating unique IDs
     * IDs start from 1 (incremented before assignment)
     */
    private final AtomicInteger idCounter;

    /**
     * Constructor - initializes empty pool
     */
    public InMemoryAptamerPool() {
        log.info("Instantiating InMemoryAptamerPool");

        this.aptamerToId = new ConcurrentHashMap<>();
        this.idToAptamer = new ConcurrentHashMap<>();
        this.idToBounds = new ConcurrentHashMap<>();
        this.idCounter = new AtomicInteger(0);

        log.debug("InMemoryAptamerPool instantiation complete");
    }

    @Override
    public synchronized int registerAptamer(byte[] a, int rr_start, int rr_end) {
        // Check if already registered
        int existingId = getIdentifier(a);
        if (existingId != -1) {
            return existingId;
        }

        // Generate new ID and register
        int newId = idCounter.incrementAndGet();

        // Store in both directions
        aptamerToId.put(new ByteArrayWrapper(a), newId);
        idToAptamer.put(newId, a);
        idToBounds.put(newId, new int[]{rr_start, rr_end});

        return newId;
    }

    @Override
    public int registerAptamer(String a, int rr_start, int rr_end) {
        return registerAptamer(a.getBytes(), rr_start, rr_end);
    }

    @Override
    public int getIdentifier(byte[] a) {
        Integer id = aptamerToId.get(new ByteArrayWrapper(a));
        return (id != null) ? id : -1;
    }

    @Override
    public int getIdentifier(String a) {
        return getIdentifier(a.getBytes());
    }

    @Override
    public byte[] getAptamer(int id) {
        return idToAptamer.get(id);
    }

    @Override
    public AptamerBounds getAptamerBounds(int id) {
        int[] bounds = idToBounds.get(id);
        return (bounds != null) ? new AptamerBounds(bounds) : null;
    }

    @Override
    public Boolean containsAptamer(byte[] a) {
        return aptamerToId.containsKey(new ByteArrayWrapper(a));
    }

    @Override
    public Boolean containsAptamer(String a) {
        return containsAptamer(a.getBytes());
    }

    @Override
    public Boolean containsAptamer(int id) {
        return idToAptamer.containsKey(id);
    }

    @Override
    public int size() {
        return idToAptamer.size();
    }

    @Override
    public void clear() {
        log.debug("Clearing InMemoryAptamerPool");

        aptamerToId.clear();
        idToAptamer.clear();
        idToBounds.clear();
        idCounter.set(0);
    }

    @Override
    public void close() {
        // No resources to close for in-memory implementation
        log.debug("Closing InMemoryAptamerPool (no-op)");
    }

    @Override
    public void setReadOnly() {
        // Not applicable for in-memory implementation
        log.debug("setReadOnly called (no-op for in-memory)");
    }

    @Override
    public void setReadWrite() {
        // Not applicable for in-memory implementation
        log.debug("setReadWrite called (no-op for in-memory)");
    }

    @Override
    public Iterable<Entry<byte[], Integer>> iterator() {
        return new AptamerIterator();
    }

    @Override
    public Iterable<Entry<Integer, byte[]>> inverse_view_iterator() {
        return new InverseIterator();
    }

    @Override
    public Iterable<Integer> id_iterator() {
        return new IdIterator();
    }

    @Override
    public Iterable<Entry<Integer, int[]>> bounds_iterator() {
        return new BoundsIterator();
    }

    // ==================== Iterator Implementations ====================

    private class AptamerIterator implements Iterable<Entry<byte[], Integer>> {
        @Override
        public Iterator<Entry<byte[], Integer>> iterator() {
            Iterator<Entry<ByteArrayWrapper, Integer>> baseIterator = aptamerToId.entrySet().iterator();

            return new Iterator<Entry<byte[], Integer>>() {
                @Override
                public boolean hasNext() {
                    return baseIterator.hasNext();
                }

                @Override
                public Entry<byte[], Integer> next() {
                    Entry<ByteArrayWrapper, Integer> entry = baseIterator.next();
                    return new AbstractMap.SimpleEntry<>(entry.getKey().data, entry.getValue());
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }

    private class InverseIterator implements Iterable<Entry<Integer, byte[]>> {
        @Override
        public Iterator<Entry<Integer, byte[]>> iterator() {
            return new Iterator<>() {
                private final Iterator<Entry<Integer, byte[]>> baseIterator = idToAptamer.entrySet().iterator();

                @Override
                public boolean hasNext() {
                    return baseIterator.hasNext();
                }

                @Override
                public Entry<Integer, byte[]> next() {
                    return baseIterator.next();
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }

    private class IdIterator implements Iterable<Integer> {
        @Override
        public Iterator<Integer> iterator() {
            return idToAptamer.keySet().iterator();
        }
    }

    private class BoundsIterator implements Iterable<Entry<Integer, int[]>> {
        @Override
        public Iterator<Entry<Integer, int[]>> iterator() {
            return idToBounds.entrySet().iterator();
        }
    }

    // ==================== Helper Classes ====================

    /**
     * Wrapper class for byte arrays to enable proper equals/hashCode
     * for use as HashMap keys
     */
    private static class ByteArrayWrapper implements Serializable {
        private static final long serialVersionUID = 1L;

        private final byte[] data;
        private final int hashCode;

        public ByteArrayWrapper(byte[] data) {
            if (data == null) {
                throw new NullPointerException("Data cannot be null");
            }
            this.data = data;
            this.hashCode = computeHashCode(data);
        }

        private static int computeHashCode(byte[] data) {
            int result = 1;
            for (byte b : data) {
                result = 31 * result + b;
            }
            return result;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof ByteArrayWrapper)) return false;

            ByteArrayWrapper that = (ByteArrayWrapper) other;
            if (this.data.length != that.data.length) return false;

            return Arrays.equals(this.data, that.data);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }
    }
}