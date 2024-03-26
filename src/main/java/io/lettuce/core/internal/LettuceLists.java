package io.lettuce.core.internal;

import java.util.*;

/**
 * Static utility methods for {@link List} instances. This class is part of the internal API and may change without further
 * notice.
 *
 * @author Mark Paluch
 * @since 4.2
 */
@SuppressWarnings("varargs")
public final class LettuceLists {

    /**
     * prevent instances.
     */
    private LettuceLists() {
    }

    /**
     * Creates a new {@link ArrayList} containing all elements from {@code elements}.
     *
     * @param elements the elements that the list should contain, must not be {@code null}.
     * @param <T> the element type
     * @return a new {@link ArrayList} containing all elements from {@code elements}.
     */
    @SafeVarargs
    public static <T> List<T> newList(T... elements) {

        LettuceAssert.notNull(elements, "Elements must not be null");
        List<T> list = new ArrayList<>(elements.length);
        Collections.addAll(list, elements);

        return list;
    }

    /**
     * Creates a new {@link ArrayList} containing all elements from {@code elements}.
     *
     * @param elements the elements that the list should contain, must not be {@code null}.
     * @param <T> the element type
     * @return a new {@link ArrayList} containing all elements from {@code elements}.
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> newList(Iterable<? extends T> elements) {
        LettuceAssert.notNull(elements, "Iterable must not be null");

        if (elements instanceof Collection<?>) {
            return new ArrayList<>((Collection<? extends T>) elements);
        }

        return newList(elements.iterator());
    }

    /**
     * Creates a new {@link ArrayList} containing all elements from {@code elements}.
     *
     * @param elements the elements that the list should contain, must not be {@code null}.
     * @param <T> the element type
     * @return a new {@link ArrayList} containing all elements from {@code elements}.
     */
    public static <T> List<T> newList(Iterator<? extends T> elements) {
        LettuceAssert.notNull(elements, "Iterator must not be null");

        List<T> objects = new ArrayList<>();
        while (elements.hasNext()) {
            objects.add(elements.next());
        }

        return objects;
    }

    /**
     * Creates a new unmodifiable {@link ArrayList} containing all elements from {@code elements}.
     *
     * @param elements the elements that the list should contain, must not be {@code null}.
     * @param <T> the element type
     * @return a new {@link ArrayList} containing all elements from {@code elements}.
     */
    @SafeVarargs
    public static <T> List<T> unmodifiableList(T... elements) {
        return Collections.unmodifiableList(newList(elements));
    }

    /**
     * Creates a new unmodifiable {@link ArrayList} containing all elements from {@code elements}.
     *
     * @param elements the elements that the list should contain, must not be {@code null}.
     * @param <T> the element type
     * @return a new {@link ArrayList} containing all elements from {@code elements}.
     */
    public static <T> List<T> unmodifiableList(Collection<? extends T> elements) {
        return Collections.unmodifiableList(new ArrayList<>(elements));
    }

}
