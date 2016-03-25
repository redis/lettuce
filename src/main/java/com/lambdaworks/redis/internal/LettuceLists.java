package com.lambdaworks.redis.internal;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

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
     * Creates a new {@link ArrayList}.
     * 
     * @return a new, empty {@link ArrayList}.
     */
    public final static <T> List<T> newList() {
        return new ArrayList<T>();
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
        List<T> list = newListWithExpectedSize(elements.length);
        for (T element : elements) {
            list.add(element);
        }

        return list;
    }

    /**
     * Creates a new {@link List} with an initial size of {@code size}.
     * 
     * @param expectedSize the number of elements you expect to add to the returned set.
     * @return a new {@link ArrayList}.
     */
    public static <E> List<E> newListWithExpectedSize(int expectedSize) {
        return new ArrayList<E>(expectedSize);
    }

    /**
     * Creates a new {@link ArrayList} containing all elements from {@code elements}.
     * 
     * @param elements the elements that the list should contain, must not be {@literal null}.
     * @return a new {@link ArrayList} containing all elements from {@code elements}.
     */
    public final static <E> List<E> newList(Collection<? extends E> elements) {
        LettuceAssert.notNull(elements, "Collection must not be null");
        return new ArrayList<>(elements);
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
            return newList((Collection<? extends E>) elements);
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

        List<E> objects = newList();
        while (elements.hasNext()) {
            objects.add(elements.next());
        }

        return objects;
    }

    /**
     * Creates a new immutable and empty {@link List}.
     * 
     * @return a new immutable and empty {@link List}.
     */
    public static <E> List<E> emptyList() {
        return Collections.emptyList();
    }

    /**
     * Creates a new {@link CopyOnWriteArrayList}.
     * 
     * @return a new, empty {@link CopyOnWriteArrayList}.
     */
    public static <E> List<E> newSynchronizedList() {
        return new CopyOnWriteArrayList<>();

    }
}
