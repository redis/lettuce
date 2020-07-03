/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.internal;

import java.util.Collection;
import java.util.function.Supplier;

/**
 * Assertion utility class that assists in validating arguments. This class is part of the internal API and may change without
 * further notice.
 *
 * @author Mark Paluch
 */
public class LettuceAssert {

    /**
     * prevent instances.
     */
    private LettuceAssert() {
    }

    /**
     * Assert that a string is not empty, it must not be {@code null} and it must not be empty.
     *
     * @param string the object to check
     * @param message the exception message to use if the assertion fails
     * @throws IllegalArgumentException if the object is {@code null} or the underlying string is empty
     */
    public static void notEmpty(CharSequence string, String message) {
        if (LettuceStrings.isEmpty(string)) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Assert that a string is not empty, it must not be {@code null} and it must not be empty.
     *
     * @param string the object to check
     * @param messageSupplier the exception message supplier to use if the assertion fails
     * @throws IllegalArgumentException if the object is {@code null} or the underlying string is empty
     * @since 5.2.0
     */
    public static void notEmpty(CharSequence string, Supplier<String> messageSupplier) {
        if (LettuceStrings.isEmpty(string)) {
            throw new IllegalArgumentException(messageSupplier.get());
        }
    }

    /**
     * Assert that an object is not {@code null} .
     *
     * @param object the object to check
     * @param message the exception message to use if the assertion fails
     * @throws IllegalArgumentException if the object is {@code null}
     */
    public static void notNull(Object object, String message) {
        if (object == null) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Assert that an object is not {@code null} .
     *
     * @param object the object to check
     * @param messageSupplier the exception message supplier to use if the assertion fails
     * @throws IllegalArgumentException if the object is {@code null}
     * @since 5.2.0
     */
    public static void notNull(Object object, Supplier<String> messageSupplier) {
        if (object == null) {
            throw new IllegalArgumentException(messageSupplier.get());
        }
    }

    /**
     * Assert that an array has elements; that is, it must not be {@code null} and must have at least one element.
     *
     * @param array the array to check
     * @param message the exception message to use if the assertion fails
     * @throws IllegalArgumentException if the object array is {@code null} or has no elements
     */
    public static void notEmpty(Object[] array, String message) {
        if (array == null || array.length == 0) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Assert that an array has elements; that is, it must not be {@code null} and must have at least one element.
     *
     * @param array the array to check
     * @param messageSupplier the exception message supplier to use if the assertion fails
     * @throws IllegalArgumentException if the object array is {@code null} or has no elements
     * @since 5.2.0
     */
    public static void notEmpty(Object[] array, Supplier<String> messageSupplier) {
        if (array == null || array.length == 0) {
            throw new IllegalArgumentException(messageSupplier.get());
        }
    }

    /**
     * Assert that an array has elements; that is, it must not be {@code null} and must have at least one element.
     *
     * @param array the array to check
     * @param message the exception message to use if the assertion fails
     * @throws IllegalArgumentException if the object array is {@code null} or has no elements
     */
    public static void notEmpty(int[] array, String message) {
        if (array == null || array.length == 0) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Assert that an array has no null elements.
     *
     * @param array the array to check
     * @param message the exception message to use if the assertion fails
     * @throws IllegalArgumentException if the object array contains a {@code null} element
     */
    public static void noNullElements(Object[] array, String message) {
        if (array != null) {
            for (Object element : array) {
                if (element == null) {
                    throw new IllegalArgumentException(message);
                }
            }
        }
    }

    /**
     * Assert that an array has no null elements.
     *
     * @param array the array to check
     * @param messageSupplier the exception message supplier to use if the assertion fails
     * @throws IllegalArgumentException if the object array contains a {@code null} element
     * @since 5.2.0
     */
    public static void noNullElements(Object[] array, Supplier<String> messageSupplier) {
        if (array != null) {
            for (Object element : array) {
                if (element == null) {
                    throw new IllegalArgumentException(messageSupplier.get());
                }
            }
        }
    }

    /**
     * Assert that a {@link java.util.Collection} has no null elements.
     *
     * @param c the collection to check
     * @param message the exception message to use if the assertion fails
     * @throws IllegalArgumentException if the {@link Collection} contains a {@code null} element
     */
    public static void noNullElements(Collection<?> c, String message) {
        if (c != null) {
            for (Object element : c) {
                if (element == null) {
                    throw new IllegalArgumentException(message);
                }
            }
        }
    }

    /**
     * Assert that a {@link java.util.Collection} has no null elements.
     *
     * @param c the collection to check
     * @param messageSupplier the exception message supplier to use if the assertion fails
     * @throws IllegalArgumentException if the {@link Collection} contains a {@code null} element
     * @since 5.2.0
     */
    public static void noNullElements(Collection<?> c, Supplier<String> messageSupplier) {
        if (c != null) {
            for (Object element : c) {
                if (element == null) {
                    throw new IllegalArgumentException(messageSupplier.get());
                }
            }
        }
    }

    /**
     * Assert that {@code value} is {@code true}.
     *
     * @param value the value to check
     * @param message the exception message to use if the assertion fails
     * @throws IllegalArgumentException if the object array contains a {@code null} element
     */
    public static void isTrue(boolean value, String message) {
        if (!value) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Assert that {@code value} is {@code true}.
     *
     * @param value the value to check
     * @param messageSupplier the exception message supplier to use if the assertion fails
     * @throws IllegalArgumentException if the object array contains a {@code null} element
     * @since 5.2.0
     */
    public static void isTrue(boolean value, Supplier<String> messageSupplier) {
        if (!value) {
            throw new IllegalArgumentException(messageSupplier.get());
        }
    }

    /**
     * Ensures the truth of an expression involving the state of the calling instance, but not involving any parameters to the
     * calling method.
     *
     * @param condition a boolean expression
     * @param message the exception message to use if the assertion fails
     * @throws IllegalStateException if {@code expression} is false
     */
    public static void assertState(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    /**
     * Ensures the truth of an expression involving the state of the calling instance, but not involving any parameters to the
     * calling method.
     *
     * @param condition a boolean expression
     * @param messageSupplier the exception message supplier to use if the assertion fails
     * @throws IllegalStateException if {@code expression} is false
     * @since 5.2.0
     */
    public static void assertState(boolean condition, Supplier<String> messageSupplier) {
        if (!condition) {
            throw new IllegalStateException(messageSupplier.get());
        }
    }

}
