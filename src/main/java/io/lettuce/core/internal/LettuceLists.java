/*
 * Copyright 2011-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
     * @param elements the elements that the list should contain, must not be {@literal null}.
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
     * @param elements the elements that the list should contain, must not be {@literal null}.
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
     * @param elements the elements that the list should contain, must not be {@literal null}.
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
     * @param elements the elements that the list should contain, must not be {@literal null}.
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
     * @param elements the elements that the list should contain, must not be {@literal null}.
     * @param <T> the element type
     * @return a new {@link ArrayList} containing all elements from {@code elements}.
     */
    public static <T> List<T> unmodifiableList(Collection<? extends T> elements) {
        return Collections.unmodifiableList(new ArrayList<>(elements));
    }
}
