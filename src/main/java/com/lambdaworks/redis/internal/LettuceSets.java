package com.lambdaworks.redis.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Static utility methods for {@link Set} instances.This class is part of the internal API and may change without further
 * notice.
 * 
 * @author Mark Paluch
 * @since 4.2
 */
public class LettuceSets {

    /**
     * prevent instances.
     */
    private LettuceSets() {

    }

    /**
     * Creates a new {@code HashSet} containing all elements from {@code elements}.
     * 
     * @param elements the elements that the set should contain, must not be {@literal null}.
     * @return a new {@code HashSet} containing all elements from {@code elements}.
     */
    public static <E> Set<E> newHashSet(Collection<? extends E> elements) {
        LettuceAssert.notNull(elements, "Collection must not be null");

        HashSet<E> set = new HashSet<E>(elements.size());
        set.addAll(elements);
        return set;
    }

    /**
     * Creates a new {@code HashSet} containing all elements from {@code elements}.
     * 
     * @param elements the elements that the set should contain, must not be {@literal null}.
     * @return a new {@code HashSet} containing all elements from {@code elements}.
     */
    public static <E> Set<E> newHashSet(Iterable<? extends E> elements) {
        LettuceAssert.notNull(elements, "Iterable must not be null");

        if (elements instanceof Collection<?>) {
            return newHashSet((Collection<E>) elements);
        }

        Set<E> set = new HashSet<>();
        for (E e : elements) {
            set.add(e);
        }
        return set;
    }

    /**
     * Creates a new {@code HashSet} containing all elements from {@code elements}.
     * 
     * @param elements the elements that the set should contain, must not be {@literal null}.
     * @return a new {@code HashSet} containing all elements from {@code elements}.
     */
    public static <E> Set<E> newHashSet(E... elements) {
        LettuceAssert.notNull(elements, "Elements must not be null");

        HashSet<E> set = new HashSet<E>(elements.length);
        for (E element : elements) {
            set.add(element);
        }
        return set;
    }

    /**
     * Creates a new unmodifiable {@code HashSet} containing all elements from {@code elements}.
     *
     * @param elements the elements that the set should contain, must not be {@literal null}.
     * @return a new {@code HashSet} containing all elements from {@code elements}.
     */
    public static <K> Set<K> unmodifiableSet(K... elements) {
        return Collections.unmodifiableSet(newHashSet(elements));
    }
}
