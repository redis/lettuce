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
package io.lettuce.core.dynamic.support;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.List;
import java.util.Map;

/**
 * Interface to access types and resolving generics on the way.
 */
public interface TypeInformation<S> {

    Type getGenericType();

    /**
     * Returns the {@link TypeInformation}s for the parameters of the given {@link Constructor}.
     *
     * @param constructor must not be {@literal null}.
     * @return
     */
    List<TypeInformation<?>> getParameterTypes(Constructor<?> constructor);

    /**
     * Returns whether the type can be considered a collection, which means it's a container of elements, e.g. a
     * {@link java.util.Collection} and {@link java.lang.reflect.Array} or anything implementing {@link Iterable}. If this
     * returns {@literal true} you can expect {@link #getComponentType()} to return a non-{@literal null} value.
     *
     * @return
     */
    boolean isCollectionLike();

    /**
     * Returns the component type for {@link java.util.Collection}s or the key type for {@link java.util.Map}s.
     *
     * @return
     */
    TypeInformation<?> getComponentType();

    /**
     * Returns whether the property is a {@link java.util.Map}. If this returns {@literal true} you can expect
     * {@link #getComponentType()} as well as {@link #getMapValueType()} to return something not {@literal null}.
     *
     * @return
     */
    boolean isMap();

    /**
     * Will return the type of the value in case the underlying type is a {@link java.util.Map}.
     *
     * @return
     */
    TypeInformation<?> getMapValueType();

    /**
     * Returns the type of the property. Will resolve generics and the generic context of
     *
     * @return
     */
    Class<S> getType();

    /**
     * Returns a {@link ClassTypeInformation} to represent the {@link TypeInformation} of the raw type of the current instance.
     *
     * @return
     */
    ClassTypeInformation<?> getRawTypeInformation();

    /**
     * Transparently returns the {@link java.util.Map} value type if the type is a {@link java.util.Map}, returns the component
     * type if the type {@link #isCollectionLike()} or the simple type if none of this applies.
     *
     * @return
     */
    TypeInformation<?> getActualType();

    /**
     * Returns a {@link TypeInformation} for the return type of the given {@link Method}. Will potentially resolve generics
     * information against the current types type parameter bindings.
     *
     * @param method must not be {@literal null}.
     * @return
     */
    TypeInformation<?> getReturnType(Method method);

    /**
     * Returns the {@link TypeInformation}s for the parameters of the given {@link Method}.
     *
     * @param method must not be {@literal null}.
     * @return
     */
    List<TypeInformation<?>> getParameterTypes(Method method);

    /**
     * Returns the {@link TypeInformation} for the given raw super type.
     *
     * @param superType must not be {@literal null}.
     * @return the {@link TypeInformation} for the given raw super type or {@literal null} in case the current
     *         {@link TypeInformation} does not implement the given type.
     */
    TypeInformation<?> getSuperTypeInformation(Class<?> superType);

    /**
     * Returns if the current {@link TypeInformation} can be safely assigned to the given one. Mimics semantics of
     * {@link Class#isAssignableFrom(Class)} but takes generics into account. Thus it will allow to detect that a
     * {@code List<Long>} is assignable to {@code List<? extends Number>}.
     *
     * @param target
     * @return
     */
    boolean isAssignableFrom(TypeInformation<?> target);

    /**
     * Returns the {@link TypeInformation} for the type arguments of the current {@link TypeInformation}.
     *
     * @return
     */
    List<TypeInformation<?>> getTypeArguments();

    Map<TypeVariable<?>, Type> getTypeVariableMap();
}
