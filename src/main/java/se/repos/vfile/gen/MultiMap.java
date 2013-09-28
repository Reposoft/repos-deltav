package se.repos.vfile.gen;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author Hugo Svallfors <keiter@lavabit.com>
 * A MultiMap, i.e a map that maps each key to a set.
 * Used in Index.
 * @see VFile
 */
public class MultiMap<K, V> {

    private LinkedHashMap<K, LinkedHashSet<V>> values;

    public MultiMap() {
        values = new LinkedHashMap<>();
    }

    public Set<V> get(K key) {
        if (key == null) {
            throw new NullPointerException();
        }
        Set<V> vals = values.get(key);
        if (vals == null) {
            return new LinkedHashSet<>();
        } else {
            return vals;
        }
    }

    public boolean containsKey(K key) {
        return values.containsKey(key);
    }

    public void put(K key, V value) {
        if (key == null || value == null) {
            throw new NullPointerException();
        }
        LinkedHashSet<V> kValues = values.get(key);
        if (kValues == null) {
            kValues = new LinkedHashSet<>();
            kValues.add(value);
            values.put(key, kValues);
        } else {
            kValues.add(value);
        }
    }

    public Set<V> remove(K key) {
        Set<V> vals = values.remove(key);
        if (vals == null) {
            return new LinkedHashSet<>();
        } else {
            return vals;
        }
    }

    public Set<K> keySet() {
        return values.keySet();
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }
}
