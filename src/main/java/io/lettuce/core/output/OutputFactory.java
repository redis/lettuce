package io.lettuce.core.output;

import java.util.*;

/**
 * @author Mark Paluch
 */
class OutputFactory {

    static <T> List<T> newList(int capacity) {

        if (capacity < 1) {
            return Collections.emptyList();
        }

        return new ArrayList<>(Math.max(1, capacity));
    }

    static <V> Set<V> newSet(int capacity) {

        if (capacity < 1) {
            return Collections.emptySet();
        }

        return new LinkedHashSet<>(capacity, 1);
    }

}
