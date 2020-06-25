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
package io.lettuce.core.dynamic.support;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.*;
import java.util.Map.Entry;

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.internal.LettuceClassUtils;

/**
 * {@link TypeInformation} for a plain {@link Class}.
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class ClassTypeInformation<S> extends TypeDiscoverer<S> {

    public static final ClassTypeInformation<Collection> COLLECTION = new ClassTypeInformation(Collection.class);

    public static final ClassTypeInformation<List> LIST = new ClassTypeInformation(List.class);

    public static final ClassTypeInformation<Set> SET = new ClassTypeInformation(Set.class);

    public static final ClassTypeInformation<Map> MAP = new ClassTypeInformation(Map.class);

    public static final ClassTypeInformation<Object> OBJECT = new ClassTypeInformation(Object.class);

    private static final Map<Class<?>, Reference<ClassTypeInformation<?>>> CACHE = Collections
            .synchronizedMap(new WeakHashMap<Class<?>, Reference<ClassTypeInformation<?>>>());

    static {
        for (ClassTypeInformation<?> info : Arrays.asList(COLLECTION, LIST, SET, MAP, OBJECT)) {
            CACHE.put(info.getType(), new WeakReference<>(info));
        }
    }

    private final Class<S> type;

    /**
     * Simple factory method to easily create new instances of {@link ClassTypeInformation}.
     *
     * @param <S>
     * @param type must not be {@code null}.
     * @return
     */
    public static <S> ClassTypeInformation<S> from(Class<S> type) {

        LettuceAssert.notNull(type, "Type must not be null!");

        Reference<ClassTypeInformation<?>> cachedReference = CACHE.get(type);
        TypeInformation<?> cachedTypeInfo = cachedReference == null ? null : cachedReference.get();

        if (cachedTypeInfo != null) {
            return (ClassTypeInformation<S>) cachedTypeInfo;
        }

        ClassTypeInformation<S> result = new ClassTypeInformation<S>(type);
        CACHE.put(type, new WeakReference<ClassTypeInformation<?>>(result));
        return result;
    }

    /**
     * Creates a {@link TypeInformation} from the given method's return type.
     *
     * @param method must not be {@code null}.
     * @return
     */
    public static <S> TypeInformation<S> fromReturnTypeOf(Method method) {

        LettuceAssert.notNull(method, "Method must not be null!");
        return new ClassTypeInformation(method.getDeclaringClass()).createInfo(method.getGenericReturnType());
    }

    /**
     * Creates a {@link TypeInformation} from the given method's parameter type.
     *
     * @param method must not be {@code null}.
     * @return
     */
    public static <S> TypeInformation<S> fromMethodParameter(Method method, int index) {

        LettuceAssert.notNull(method, "Method must not be null!");
        return new ClassTypeInformation(method.getDeclaringClass()).createInfo(method.getGenericParameterTypes()[index]);
    }

    /**
     * Creates {@link ClassTypeInformation} for the given type.
     *
     * @param type
     */
    ClassTypeInformation(Class<S> type) {
        super(getUserClass(type), getTypeVariableMap(type));
        this.type = type;
    }

    /**
     * Return the user-defined class for the given class: usually simply the given class, but the original class in case of a
     * CGLIB-generated subclass.
     *
     * @param clazz the class to check
     * @return the user-defined class
     */
    private static Class<?> getUserClass(Class<?> clazz) {
        if (clazz != null && clazz.getName().contains(LettuceClassUtils.CGLIB_CLASS_SEPARATOR)) {
            Class<?> superclass = clazz.getSuperclass();
            if (superclass != null && Object.class != superclass) {
                return superclass;
            }
        }
        return clazz;
    }

    /**
     * Little helper to allow us to create a generified map, actually just to satisfy the compiler.
     *
     * @param type must not be {@code null}.
     * @return
     */
    private static Map<TypeVariable<?>, Type> getTypeVariableMap(Class<?> type) {
        return getTypeVariableMap(type, new HashSet<Type>());
    }

    @SuppressWarnings("deprecation")
    private static Map<TypeVariable<?>, Type> getTypeVariableMap(Class<?> type, Collection<Type> visited) {

        if (visited.contains(type)) {
            return Collections.emptyMap();
        } else {
            visited.add(type);
        }

        Map<TypeVariable, Type> source = GenericTypeResolver.getTypeVariableMap(type);
        Map<TypeVariable<?>, Type> map = new HashMap<>(source.size());

        for (Entry<TypeVariable, Type> entry : source.entrySet()) {

            Type value = entry.getValue();
            map.put(entry.getKey(), entry.getValue());

            if (value instanceof Class) {
                map.putAll(getTypeVariableMap((Class<?>) value, visited));
            }
        }

        return map;
    }

    @Override
    public Class<S> getType() {
        return type;
    }

    @Override
    public ClassTypeInformation<?> getRawTypeInformation() {
        return this;
    }

    @Override
    public boolean isAssignableFrom(TypeInformation<?> target) {
        return getType().isAssignableFrom(target.getType());
    }

    @Override
    public String toString() {
        return type.getName();
    }

}
