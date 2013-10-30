package se.repos.vfile.gen;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A MultiMap, i.e a map that maps each key to a set.
 * 
 * @see VFile
 */
public class MultiMap<K, V> {

    private LinkedHashMap<K, LinkedHashSet<V>> values;

    public MultiMap() {
        this.values = new LinkedHashMap<K, LinkedHashSet<V>>();
    }

    public Set<V> get(K key) {
        if (key == null) {
            throw new NullPointerException();
        }
        Set<V> vals = this.values.get(key);
        if (vals == null) {
            return new LinkedHashSet<V>();
        }
        return vals;
    }

    public boolean containsKey(K key) {
        return this.values.containsKey(key);
    }

    public void put(K key, V value) {
        if (key == null || value == null) {
            throw new NullPointerException();
        }
        LinkedHashSet<V> kValues = this.values.get(key);
        if (kValues == null) {
            kValues = new LinkedHashSet<V>();
            kValues.add(value);
            this.values.put(key, kValues);
        } else {
            kValues.add(value);
        }
    }

    public Set<V> remove(K key) {
        Set<V> vals = this.values.remove(key);
        if (vals == null) {
            return new LinkedHashSet<V>();
        }
        return vals;
    }

    public Set<K> keySet() {
        return this.values.keySet();
    }

    public boolean isEmpty() {
        return this.values.isEmpty();
    }
}
