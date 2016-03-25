package com.lambdaworks.redis.internal;

import java.util.*;

import io.netty.util.internal.ConcurrentSet;

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
     * Creates a new {@code HashSet}.
     * 
     * @return a new, empty {@code HashSet}.
     */
    public static <E> Set<E> newHashSet() {
        return new HashSet<E>();
    }

    /**
     * Creates a new {@code HashSet} containing all elements from {@code elements}.
     * 
     * @param elements the elements that the set should contain, must not be {@literal null}.
     * @return a new {@code HashSet} containing all elements from {@code elements}.
     */
    public static <E> Set<E> newHashSet(Collection<? extends E> elements) {
        LettuceAssert.notNull(elements, "Collection must not be null");

        HashSet<E> set = newHashSetWithExpectedSize(elements.size());
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

        Set<E> set = newHashSet();
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

        HashSet<E> set = newHashSetWithExpectedSize(elements.length);
        for (E element : elements) {
            set.add(element);
        }
        return set;
    }

    /**
     * Creates a new {@code HashSet} containing all elements from {@code elements}.
     * 
     * @param expectedSize the number of elements you expect to add to the returned set.
     * @return a new, empty {@link HashSet}.
     */
    public static <E> HashSet<E> newHashSetWithExpectedSize(int expectedSize) {
        return new HashSet<E>(expectedSize);
    }

    /**
     * Creates a new {@code HashSet} containing all elements from {@code elements}.
     * 
     * @param elements the elements that the set should contain, must not be {@literal null}.
     * @return a new {@code TreeSet} containing all elements from {@code elements}.
     */
    public static <E> TreeSet<E> newTreeSet(Collection<E> elements) {
        LettuceAssert.notNull(elements, "Collection must not be null");
        return new TreeSet<>(elements);
    }

    /**
     * Creates a new synchronized {@link LinkedHashSet}.
     * 
     * @return a new, empty synchronized {@link LinkedHashSet}.
     */
    public static <K> Set<K> newConcurrentLinkedHashSet() {
        return new ConcurrentSet<>();
    }
}
