package pablog.aptasuite.cluster;

import org.eclipse.collections.api.list.primitive.MutableIntList;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Minimal bucket map for LSH hashing.
 */
public class Buckets {

    private final Map<Integer, MutableIntList> buckets;

    private Buckets(int expectedSize) {
        int capacity = Math.max(16, expectedSize * 2);
        this.buckets = new HashMap<>(capacity);
    }

    public static Buckets withExpectedSize(int expectedSize) {
        return new Buckets(expectedSize);
    }

    public int size() {
        return buckets.size();
    }

    public boolean contains(int key) {
        return buckets.containsKey(key);
    }

    public MutableIntList get(int key) {
        return buckets.get(key);
    }

    public void justPut(int key, MutableIntList value) {
        buckets.put(key, value);
    }

    public Set<Map.Entry<Integer, MutableIntList>> entrySet() {
        return buckets.entrySet();
    }
}
