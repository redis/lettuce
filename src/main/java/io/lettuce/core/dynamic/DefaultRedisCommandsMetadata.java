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
package io.lettuce.core.dynamic;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import io.lettuce.core.internal.LettuceAssert;

/**
 * Default implementation of {@link RedisCommandsMetadata}.
 *
 * @author Mark Paluch
 * @since 5.0
 */
class DefaultRedisCommandsMetadata implements RedisCommandsMetadata {

    /** The package separator character: '.' */
    private static final char PACKAGE_SEPARATOR = '.';

    private final Class<?> apiInterface;

    /**
     * Create {@link DefaultRedisCommandsMetadata} given a {@link Class command interface}.
     *
     * @param apiInterface must not be {@literal null}.
     */
    DefaultRedisCommandsMetadata(Class<?> apiInterface) {
        this.apiInterface = apiInterface;
    }

    @Override
    public Class<?> getCommandsInterface() {
        return apiInterface;
    }

    @Override
    public Collection<Method> getMethods() {

        Set<Method> result = new HashSet<Method>();

        for (Method method : getCommandsInterface().getMethods()) {
            method = getMostSpecificMethod(method, getCommandsInterface());
            if (isQueryMethodCandidate(method)) {
                result.add(method);
            }
        }

        return Collections.unmodifiableSet(result);
    }

    /**
     * Checks whether the given method is a query method candidate.
     *
     * @param method
     * @return
     */
    private boolean isQueryMethodCandidate(Method method) {
        return !method.isBridge() && !method.isDefault();
    }

    @Override
    public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
        return getCommandsInterface().getAnnotation(annotationClass);
    }

    @Override
    public boolean hasAnnotation(Class<? extends Annotation> annotationClass) {
        return getCommandsInterface().getAnnotation(annotationClass) != null;
    }

    /**
     * Given a method, which may come from an interface, and a target class used in the current reflective invocation, find the
     * corresponding target method if there is one. E.g. the method may be {@code IFoo.bar()} and the target class may be
     * {@code DefaultFoo}. In this case, the method may be {@code DefaultFoo.bar()}. This enables attributes on that method to
     * be found.
     *
     * @param method the method to be invoked, which may come from an interface
     * @param targetClass the target class for the current invocation. May be {@code null} or may not even implement the method.
     * @return the specific target method, or the original method if the {@code targetClass} doesn't implement it or is
     *         {@code null}
     */
    public static Method getMostSpecificMethod(Method method, Class<?> targetClass) {

        if (method != null && isOverridable(method, targetClass) && targetClass != null
                && targetClass != method.getDeclaringClass()) {
            try {
                try {
                    return targetClass.getMethod(method.getName(), method.getParameterTypes());
                } catch (NoSuchMethodException ex) {
                    return method;
                }
            } catch (SecurityException ex) {
            }
        }
        return method;
    }

    /**
     * Determine whether the given method is overridable in the given target class.
     *
     * @param method the method to check
     * @param targetClass the target class to check against
     */
    private static boolean isOverridable(Method method, Class<?> targetClass) {

        if (Modifier.isPrivate(method.getModifiers())) {
            return false;
        }
        if (Modifier.isPublic(method.getModifiers()) || Modifier.isProtected(method.getModifiers())) {
            return true;
        }
        return getPackageName(method.getDeclaringClass()).equals(getPackageName(targetClass));
    }

    /**
     * Determine the name of the package of the given class, e.g. "java.lang" for the {@code java.lang.String} class.
     *
     * @param clazz the class
     * @return the package name, or the empty String if the class is defined in the default package
     */
    private static String getPackageName(Class<?> clazz) {

        LettuceAssert.notNull(clazz, "Class must not be null");
        return getPackageName(clazz.getName());
    }

    /**
     * Determine the name of the package of the given fully-qualified class name, e.g. "java.lang" for the
     * {@code java.lang.String} class name.
     *
     * @param fqClassName the fully-qualified class name
     * @return the package name, or the empty String if the class is defined in the default package
     */
    private static String getPackageName(String fqClassName) {

        LettuceAssert.notNull(fqClassName, "Class name must not be null");
        int lastDotIndex = fqClassName.lastIndexOf(PACKAGE_SEPARATOR);
        return (lastDotIndex != -1 ? fqClassName.substring(0, lastDotIndex) : "");
    }

}
