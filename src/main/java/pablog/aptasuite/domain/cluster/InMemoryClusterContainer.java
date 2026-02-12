package pablog.aptasuite.domain.cluster;

import pablog.aptasuite.domain.pool.AptamerPool;

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Thread-safe in-memory implementation of ClusterContainer.
 */
public class InMemoryClusterContainer implements ClusterContainer {

    private static final long serialVersionUID = 1L;

    private final AptamerPool pool;
    private final ConcurrentMap<Integer, Integer> aptamerToCluster = new ConcurrentHashMap<>();

    public InMemoryClusterContainer(AptamerPool pool) {
        this.pool = pool;
    }

    @Override
    public int addToCluster(String a, int clusterId) {
        int id = pool.getIdentifier(a);
        if (id == -1) {
            id = pool.registerAptamer(a, 0, a.length());
        }
        return addToCluster(id, clusterId);
    }

    @Override
    public int addToCluster(byte[] a, int clusterId) {
        int id = pool.getIdentifier(a);
        if (id == -1) {
            id = pool.registerAptamer(a, 0, a.length);
        }
        return addToCluster(id, clusterId);
    }

    @Override
    public int addToCluster(int a, int clusterId) {
        aptamerToCluster.put(a, clusterId);
        return clusterId;
    }

    @Override
    public int reassignClusterId(int a, int clusterId) {
        if (!aptamerToCluster.containsKey(a)) {
            return -1;
        }
        aptamerToCluster.put(a, clusterId);
        return clusterId;
    }

    @Override
    public boolean containsAptamer(String a) {
        int id = pool.getIdentifier(a);
        return containsAptamer(id);
    }

    @Override
    public boolean containsAptamer(byte[] a) {
        int id = pool.getIdentifier(a);
        return containsAptamer(id);
    }

    @Override
    public boolean containsAptamer(int a) {
        return aptamerToCluster.containsKey(a);
    }

    @Override
    public int getClusterId(int a) {
        return aptamerToCluster.getOrDefault(a, -1);
    }

    @Override
    public int getSize() {
        return aptamerToCluster.size();
    }

    @Override
    public int getNumberOfClusters() {
        return (int) aptamerToCluster.values().stream().distinct().count();
    }

    @Override
    public void setReadOnly() {
        // No-op for in-memory storage.
    }

    @Override
    public void setReadWrite() {
        // No-op for in-memory storage.
    }

    @Override
    public void close() {
        // No-op for in-memory storage.
    }

    @Override
    public Iterable<Entry<Integer, Integer>> iterator() {
        return aptamerToCluster.entrySet();
    }

    @Override
    public Iterable<Entry<byte[], Integer>> sequence_iterator() {
        return () -> new Iterator<>() {
            private final Iterator<Entry<Integer, Integer>> delegate = aptamerToCluster.entrySet().iterator();

            @Override
            public boolean hasNext() {
                return delegate.hasNext();
            }

            @Override
            public Entry<byte[], Integer> next() {
                Entry<Integer, Integer> entry = delegate.next();
                byte[] sequence = pool.getAptamer(entry.getKey());
                return new AbstractMap.SimpleEntry<>(sequence, entry.getValue());
            }
        };
    }
}
