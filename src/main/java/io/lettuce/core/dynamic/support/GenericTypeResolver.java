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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for resolving generic types against type variables.
 *
 * <p>
 * Mainly intended for usage within the framework, resolving method parameter types even when they are declared generically.
 */
public abstract class GenericTypeResolver {

    /**
     * Build a mapping of {@link TypeVariable#getName TypeVariable names} to {@link Class concrete classes} for the specified
     * {@link Class}. Searches all super types, enclosing types and interfaces.
     */
    @SuppressWarnings("rawtypes")
    public static Map<TypeVariable, Type> getTypeVariableMap(Class<?> clazz) {
        Map<TypeVariable, Type> typeVariableMap = new HashMap<TypeVariable, Type>();
        buildTypeVariableMap(ResolvableType.forClass(clazz), typeVariableMap);

        return typeVariableMap;
    }

    /**
     * Resolve the type arguments of the given generic interface against the given target class which is assumed to implement
     * the generic interface and possibly declare concrete types for its type variables.
     *
     * @param clazz the target class to check against.
     * @param genericIfc the generic interface or superclass to resolve the type argument from.
     * @return the resolved type of each argument, with the array size matching the number of actual type arguments, or
     *         {@code null} if not resolvable.
     */
    public static Class<?>[] resolveTypeArguments(Class<?> clazz, Class<?> genericIfc) {
        ResolvableType type = ResolvableType.forClass(clazz).as(genericIfc);
        if (!type.hasGenerics() || type.isEntirelyUnresolvable()) {
            return null;
        }
        return type.resolveGenerics(Object.class);
    }

    @SuppressWarnings("rawtypes")
    private static void buildTypeVariableMap(ResolvableType type, Map<TypeVariable, Type> typeVariableMap) {
        if (type != ResolvableType.NONE) {
            if (type.getType() instanceof ParameterizedType) {
                TypeVariable<?>[] variables = type.resolve().getTypeParameters();
                for (int i = 0; i < variables.length; i++) {
                    ResolvableType generic = type.getGeneric(i);
                    while (generic.getType() instanceof TypeVariable<?>) {
                        generic = generic.resolveType();
                    }
                    if (generic != ResolvableType.NONE) {
                        typeVariableMap.put(variables[i], generic.getType());
                    }
                }
            }
            buildTypeVariableMap(type.getSuperType(), typeVariableMap);
            for (ResolvableType interfaceType : type.getInterfaces()) {
                buildTypeVariableMap(interfaceType, typeVariableMap);
            }
            if (type.resolve().isMemberClass()) {
                buildTypeVariableMap(ResolvableType.forClass(type.resolve().getEnclosingClass()), typeVariableMap);
            }
        }
    }

}
