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
import java.util.*;

import io.lettuce.core.internal.LettuceStrings;

/**
 * Base class for all types that include parametrization of some kind. Crucial as we have to take note of the parent class we
 * will have to resolve generic parameters against.
 */
class ParametrizedTypeInformation<T> extends ParentTypeAwareTypeInformation<T> {

    private final ParameterizedType type;

    private Boolean resolved;

    /**
     * Creates a new {@link ParametrizedTypeInformation} for the given {@link Type} and parent {@link TypeDiscoverer}.
     *
     * @param type must not be {@code null}
     * @param parent must not be {@code null}
     */
    public ParametrizedTypeInformation(ParameterizedType type, TypeDiscoverer<?> parent,
            Map<TypeVariable<?>, Type> typeVariableMap) {

        super(type, parent, typeVariableMap);
        this.type = type;
    }

    @Override
    protected TypeInformation<?> doGetMapValueType() {

        if (Map.class.isAssignableFrom(getType())) {

            Type[] arguments = type.getActualTypeArguments();

            if (arguments.length > 1) {
                return createInfo(arguments[1]);
            }
        }

        Class<?> rawType = getType();

        Set<Type> supertypes = new HashSet<Type>();
        supertypes.add(rawType.getGenericSuperclass());
        supertypes.addAll(Arrays.asList(rawType.getGenericInterfaces()));

        for (Type supertype : supertypes) {

            Class<?> rawSuperType = resolveClass(supertype);

            if (Map.class.isAssignableFrom(rawSuperType)) {

                ParameterizedType parameterizedSupertype = (ParameterizedType) supertype;
                Type[] arguments = parameterizedSupertype.getActualTypeArguments();
                return createInfo(arguments[1]);
            }
        }

        return super.doGetMapValueType();
    }

    @Override
    public List<TypeInformation<?>> getTypeArguments() {

        List<TypeInformation<?>> result = new ArrayList<>();

        for (Type argument : type.getActualTypeArguments()) {
            result.add(createInfo(argument));
        }

        return result;
    }

    @Override
    public boolean isAssignableFrom(TypeInformation<?> target) {

        if (this.equals(target)) {
            return true;
        }

        Class<T> rawType = getType();
        Class<?> rawTargetType = target.getType();

        if (!rawType.isAssignableFrom(rawTargetType)) {
            return false;
        }

        TypeInformation<?> otherTypeInformation = rawType.equals(rawTargetType) ? target
                : target.getSuperTypeInformation(rawType);

        List<TypeInformation<?>> myParameters = getTypeArguments();
        List<TypeInformation<?>> typeParameters = otherTypeInformation.getTypeArguments();

        if (myParameters.size() != typeParameters.size()) {
            return false;
        }

        for (int i = 0; i < myParameters.size(); i++) {

            if (myParameters.get(i) instanceof WildcardTypeInformation) {
                if (!myParameters.get(i).isAssignableFrom(typeParameters.get(i))) {
                    return false;
                }
            } else {
                if (!myParameters.get(i).getType().equals(typeParameters.get(i).getType())) {
                    return false;
                }

                if (!myParameters.get(i).isAssignableFrom(typeParameters.get(i))) {
                    return false;
                }
            }

        }

        return true;
    }

    @Override
    protected TypeInformation<?> doGetComponentType() {
        return createInfo(type.getActualTypeArguments()[0]);
    }

    @Override
    public boolean equals(Object obj) {

        if (obj == this) {
            return true;
        }

        if (!(obj instanceof ParametrizedTypeInformation)) {
            return false;
        }

        ParametrizedTypeInformation<?> that = (ParametrizedTypeInformation<?>) obj;

        if (this.isResolvedCompletely() && that.isResolvedCompletely()) {
            return this.type.equals(that.type);
        }

        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return isResolvedCompletely() ? this.type.hashCode() : super.hashCode();
    }

    @Override
    public String toString() {

        return String.format("%s<%s>", getType().getName(),
                LettuceStrings.collectionToDelimitedString(getTypeArguments(), ",", "", ""));
    }

    private boolean isResolvedCompletely() {

        if (resolved != null) {
            return resolved;
        }

        Type[] typeArguments = type.getActualTypeArguments();

        if (typeArguments.length == 0) {
            return cacheAndReturn(false);
        }

        for (Type typeArgument : typeArguments) {

            TypeInformation<?> info = createInfo(typeArgument);

            if (info instanceof ParametrizedTypeInformation) {
                if (!((ParametrizedTypeInformation<?>) info).isResolvedCompletely()) {
                    return cacheAndReturn(false);
                }
            }

            if (!(info instanceof ClassTypeInformation)) {
                return cacheAndReturn(false);
            }
        }

        return cacheAndReturn(true);
    }

    private boolean cacheAndReturn(boolean resolved) {

        this.resolved = resolved;
        return resolved;
    }

}
