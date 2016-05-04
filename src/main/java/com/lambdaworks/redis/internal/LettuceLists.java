package com.lambdaworks.redis.internal;

import java.util.*;

/**
 * Static utility methods for {@link List} instances. This class is part of the internal API and may change without further
 * notice.
 * 
 * @author Mark Paluch
 * @since 4.2
 */
public class LettuceLists {

    /**
     * prevent instances.
     */
    private LettuceLists() {

    }

    /**
     * Creates a new {@link ArrayList} containing all elements from {@code elements}.
     * 
     * @param elements the elements that the list should contain, must not be {@literal null}.
     * @return a new {@link ArrayList} containing all elements from {@code elements}.
     */
    @SafeVarargs
    public final static <T> List<T> newList(T... elements) {
        LettuceAssert.notNull(elements, "Elements must not be null");
        List<T> list = new ArrayList<>(elements.length);
        for (T element : elements) {
            list.add(element);
        }

        return list;
    }

    /**
     * Creates a new {@link ArrayList} containing all elements from {@code elements}.
     * 
     * @param elements the elements that the list should contain, must not be {@literal null}.
     * @return a new {@link ArrayList} containing all elements from {@code elements}.
     */
    public final static <E> List<E> newList(Iterable<? extends E> elements) {
        LettuceAssert.notNull(elements, "Iterable must not be null");

        if (elements instanceof Collection<?>) {
            return new ArrayList<>((Collection<? extends E>) elements);
        }

        return newList(elements.iterator());
    }

    /**
     * Creates a new {@link ArrayList} containing all elements from {@code elements}.
     * 
     * @param elements the elements that the list should contain, must not be {@literal null}.
     * @return a new {@link ArrayList} containing all elements from {@code elements}.
     */
    public final static <E> List<E> newList(Iterator<? extends E> elements) {
        LettuceAssert.notNull(elements, "Iterator must not be null");

        List<E> objects = new ArrayList<>();
        while (elements.hasNext()) {
            objects.add(elements.next());
        }

        return objects;
    }

    /**
     * Creates a new unmodifiable {@link ArrayList} containing all elements from {@code elements}.
     *
     * @param elements the elements that the list should contain, must not be {@literal null}.
     * @return a new {@link ArrayList} containing all elements from {@code elements}.
     */
    public static <E> List<E> unmodifiableList(E... elements) {
        return Collections.unmodifiableList(newList(elements));
    }

    /**
     * Creates a new unmodifiable {@link ArrayList} containing all elements from {@code elements}.
     *
     * @param elements the elements that the list should contain, must not be {@literal null}.
     * @return a new {@link ArrayList} containing all elements from {@code elements}.
     */
    public static <E> List<E> unmodifiableList(
            Collection<? extends E> elements) {
        return Collections.unmodifiableList(new ArrayList<>(elements));
    }
}
