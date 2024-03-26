package io.lettuce.core.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Static utility methods for {@link Set} instances. This class is part of the internal API and may change without further
 * notice.
 *
 * @author Mark Paluch
 * @since 4.2
 */
@SuppressWarnings("varargs")
public final class LettuceSets {

    /**
     * prevent instances.
     */
    private LettuceSets() {

    }

    /**
     * Creates a new {@code HashSet} containing all elements from {@code elements}.
     *
     * @param elements the elements that the set should contain, must not be {@code null}.
     * @param <T> the element type
     * @return a new {@code HashSet} containing all elements from {@code elements}.
     */
    public static <T> Set<T> newHashSet(Collection<? extends T> elements) {
        LettuceAssert.notNull(elements, "Collection must not be null");

        HashSet<T> set = new HashSet<>(elements.size());
        set.addAll(elements);
        return set;
    }

    /**
     * Creates a new {@code HashSet} containing all elements from {@code elements}.
     *
     * @param elements the elements that the set should contain, must not be {@code null}.
     * @param <T> the element type
     * @return a new {@code HashSet} containing all elements from {@code elements}.
     */
    @SuppressWarnings("unchecked")
    public static <T> Set<T> newHashSet(Iterable<? extends T> elements) {
        LettuceAssert.notNull(elements, "Iterable must not be null");

        if (elements instanceof Collection<?>) {
            return newHashSet((Collection<T>) elements);
        }

        Set<T> set = new HashSet<>();
        for (T e : elements) {
            set.add(e);
        }
        return set;
    }

    /**
     * Creates a new {@code HashSet} containing all elements from {@code elements}.
     *
     * @param elements the elements that the set should contain, must not be {@code null}.
     * @param <T> the element type
     * @return a new {@code HashSet} containing all elements from {@code elements}.
     */
    @SafeVarargs
    public static <T> Set<T> newHashSet(T... elements) {

        LettuceAssert.notNull(elements, "Elements must not be null");

        HashSet<T> set = new HashSet<>(elements.length);
        Collections.addAll(set, elements);
        return set;
    }

    /**
     * Creates a new unmodifiable {@code HashSet} containing all elements from {@code elements}.
     *
     * @param elements the elements that the set should contain, must not be {@code null}.
     * @param <T> the element type
     * @return a new {@code HashSet} containing all elements from {@code elements}.
     */
    @SafeVarargs
    public static <T> Set<T> unmodifiableSet(T... elements) {
        return Collections.unmodifiableSet(newHashSet(elements));
    }

}
