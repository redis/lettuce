package com.lambdaworks.redis.internal;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Static utility methods for {@link Map} instances. This class is part of the internal API and may change without further
 * notice.
 * 
 * @author Mark Paluch
 * @since 4.2
 */
public class LettuceMaps {

    /**
     * prevent instances.
     */
    private LettuceMaps() {

    }

    /**
     * Creates a new {@link HashMap}.
     * 
     * @return a new, empty {@link HashMap}.
     */
    public final static <K, V> HashMap<K, V> newHashMap() {
        return new HashMap<>();
    }

    /**
     * Creates a new {@link HashMap} containing all elements from {@code map}.
     *
     * @param map a map whose mappings are to be placed in the new map, must not be {@literal null}.
     * @return a new {@link HashMap} containing all elements from {@code map}.
     */
    public final static <K, V> HashMap<K, V> newHashMap(Map<? extends K, ? extends V> map) {
        LettuceAssert.notNull(map, "Map must not be null");
        return new HashMap<>(map);
    }

    /**
     * Creates a new {@link LinkedHashMap}.
     * 
     * @return new, empty {@link LinkedHashMap}.
     */
    public final static <K, V> LinkedHashMap<K, V> newLinkedHashMap() {
        return new LinkedHashMap<>();
    }

    /**
     * Creates a new {@link LinkedHashMap} containing all elements from {@code map}.
     * 
     * @param map a map whose mappings are to be placed in the new map, must not be {@literal null}.
     * @return a new {@link LinkedHashMap} containing all elements from {@code map}.
     */
    public final static <K, V> Map<K, V> newLinkedHashMap(Map<? extends K, ? extends V> map) {
        LettuceAssert.notNull(map, "Map must not be null");
        return new LinkedHashMap<>(map);
    }

    /**
     * Creates a new {@link TreeMap}.
     * 
     * @return
     */
    public static <K, V> TreeMap<K, V> newTreeMap() {
        return new TreeMap<>();
    }

    /**
     * Creates a new {@link TreeMap} using a {@code comparator}.
     * 
     * @param comparator the comparator to be used within the {@link TreeMap}, must not be {@literal null}.
     * @return a new, empty {@link TreeMap}.
     */
    public static <K, V> TreeMap<K, V> newTreeMap(Comparator<? super K> comparator) {
        LettuceAssert.notNull(comparator, "Comparator must not be null");
        return new TreeMap<>(comparator);
    }

    /**
     * Creates a new {@link ConcurrentHashMap}.
     * 
     * @return a new, empty {@link ConcurrentHashMap}.
     */
    public static <K, V> ConcurrentHashMap<K, V> newConcurrentMap() {
        return new ConcurrentHashMap<>();
    }
}
