/*
 * Copyright 2020 the original author or authors.
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
package io.lettuce.test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import io.lettuce.core.dynamic.support.ReflectionUtils;
import io.lettuce.core.internal.LettuceAssert;

/**
 * Reflection test utility.
 */
public final class ReflectionTestUtils {

    /**
     * Get the value of the {@linkplain Field field} with the given {@code name} from the provided {@code targetObject}.
     * <p>
     * This method delegates to {@link #getField(Object, Class, String)}, supplying {@code null} for the {@code targetClass}
     * argument.
     *
     * @param targetObject the target object from which to get the field; never {@code null}
     * @param name the name of the field to get; never {@code null}
     * @return the field's current value
     * @see #getField(Class, String)
     */
    public static <T> T getField(Object targetObject, String name) {
        return getField(targetObject, null, name);
    }

    /**
     * Get the value of the static {@linkplain Field field} with the given {@code name} from the provided {@code targetClass}.
     * <p>
     * This method delegates to {@link #getField(Object, Class, String)}, supplying {@code null} for the {@code targetObject}
     * argument.
     *
     * @param targetClass the target class from which to get the static field; never {@code null}
     * @param name the name of the field to get; never {@code null}
     * @return the field's current value
     * @see #getField(Object, String)
     */
    public static <T> T getField(Class<?> targetClass, String name) {
        return getField(null, targetClass, name);
    }

    /**
     * Get the value of the {@linkplain Field field} with the given {@code name} from the provided
     * {@code targetObject}/{@code targetClass}.
     * <p>
     * This method traverses the class hierarchy in search of the desired field. In addition, an attempt will be made to make
     * non-{@code public} fields <em>accessible</em>, thus allowing one to get {@code protected}, {@code private}, and
     * <em>package-private</em> fields.
     *
     * @param targetObject the target object from which to get the field; may be {@code null} if the field is static
     * @param targetClass the target class from which to get the field; may be {@code null} if the field is an instance field
     * @param name the name of the field to get; never {@code null}
     * @return the field's current value
     */
    public static <T> T getField(Object targetObject, Class<?> targetClass, String name) {
        LettuceAssert.isTrue(targetObject != null || targetClass != null,
                "Either targetObject or targetClass for the field must be specified");

        if (targetClass == null) {
            targetClass = targetObject.getClass();
        }

        Field field = findField(targetClass, name);
        if (field == null) {
            throw new IllegalArgumentException(String.format("Could not find field '%s' on %s or target class [%s]", name,
                    safeToString(targetObject), targetClass));
        }

        makeAccessible(field);

        try {
            return (T) field.get(targetObject);
        } catch (IllegalAccessException ex) {
            ReflectionUtils.handleReflectionException(ex);
            throw new IllegalStateException(
                    "Unexpected reflection exception - " + ex.getClass().getName() + ": " + ex.getMessage());
        }
    }

    // Field handling

    /**
     * Attempt to find a {@link Field field} on the supplied {@link Class} with the supplied {@code name}. Searches all
     * superclasses up to {@link Object}.
     *
     * @param clazz the class to introspect
     * @param name the name of the field
     * @return the corresponding Field object, or {@code null} if not found
     */
    public static Field findField(Class<?> clazz, String name) {
        return findField(clazz, name, null);
    }

    /**
     * Attempt to find a {@link Field field} on the supplied {@link Class} with the supplied {@code name} and/or {@link Class
     * type}. Searches all superclasses up to {@link Object}.
     *
     * @param clazz the class to introspect
     * @param name the name of the field (may be {@code null} if type is specified)
     * @param type the type of the field (may be {@code null} if name is specified)
     * @return the corresponding Field object, or {@code null} if not found
     */
    public static Field findField(Class<?> clazz, String name, Class<?> type) {
        LettuceAssert.notNull(clazz, "Class must not be null");
        LettuceAssert.isTrue(name != null || type != null, "Either name or type of the field must be specified");
        Class<?> searchType = clazz;
        while (Object.class != searchType && searchType != null) {
            Field[] fields = searchType.getDeclaredFields();
            for (Field field : fields) {
                if ((name == null || name.equals(field.getName())) && (type == null || type.equals(field.getType()))) {
                    return field;
                }
            }
            searchType = searchType.getSuperclass();
        }
        return null;
    }

    /**
     * Make the given field accessible, explicitly setting it accessible if necessary. The {@code setAccessible(true)} method is
     * only called when actually necessary, to avoid unnecessary conflicts with a JVM SecurityManager (if active).
     *
     * @param field the field to make accessible
     * @see java.lang.reflect.Field#setAccessible
     */
    private static void makeAccessible(Field field) {
        if ((!Modifier.isPublic(field.getModifiers()) || !Modifier.isPublic(field.getDeclaringClass().getModifiers())
                || Modifier.isFinal(field.getModifiers())) && !field.isAccessible()) {
            field.setAccessible(true);
        }
    }

    private static String safeToString(Object target) {
        try {
            return String.format("target object [%s]", target);
        } catch (Exception ex) {
            return String.format("target of type [%s] whose toString() method threw [%s]",
                    (target != null ? target.getClass().getName() : "unknown"), ex);
        }
    }

}
