package github.nighter.smartspawner.language;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A simple LRU (Least Recently Used) cache implementation
 * that automatically removes the least recently accessed entries
 * when the cache reaches its capacity.
 *
 * @param <K> The type of keys maintained by this cache
 * @param <V> The type of values maintained by this cache
 */
public class LRUCache<K, V> {
    private final LinkedHashMap<K, V> cache;
    private int capacity;

    /**
     * Constructs an LRU cache with the specified capacity
     *
     * @param capacity The maximum number of entries in the cache
     */
    public LRUCache(int capacity) {
        this.capacity = capacity;
        this.cache = new LinkedHashMap<K, V>(capacity, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > LRUCache.this.capacity;
            }
        };
    }

    /**
     * Returns the value associated with the specified key,
     * or null if no mapping exists for the key
     *
     * @param key The key whose associated value is to be returned
     * @return The value associated with the key, or null if no mapping exists
     */
    public synchronized V get(K key) {
        return cache.get(key);
    }

    /**
     * Associates the specified value with the specified key in this cache
     *
     * @param key The key with which the specified value is to be associated
     * @param value The value to be associated with the specified key
     * @return The previous value associated with the key, or null if no mapping existed
     */
    public synchronized V put(K key, V value) {
        return cache.put(key, value);
    }

    /**
     * Removes all entries from the cache
     */
    public synchronized void clear() {
        cache.clear();
    }

    /**
     * Returns the number of key-value mappings in this cache
     *
     * @return The number of key-value mappings in this cache
     */
    public synchronized int size() {
        return cache.size();
    }

    /**
     * Returns the capacity of this cache
     *
     * @return The capacity of this cache
     */
    public synchronized int capacity() {
        return capacity;
    }

    /**
     * Resize the cache capacity
     *
     * @param newCapacity The new capacity for the cache
     */
    public synchronized void resize(int newCapacity) {
        this.capacity = newCapacity;
        // The LinkedHashMap will automatically adjust its size on the next put operation
    }
}