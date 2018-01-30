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

import java.lang.reflect.*;
import java.util.*;

import io.lettuce.core.internal.LettuceAssert;

/**
 * Basic {@link TypeDiscoverer} that contains basic functionality to discover property types.
 */
class TypeDiscoverer<S> implements TypeInformation<S> {

    private final Type type;
    private final Map<TypeVariable<?>, Type> typeVariableMap;
    private final int hashCode;

    private boolean componentTypeResolved = false;
    private TypeInformation<?> componentType;

    private boolean valueTypeResolved = false;
    private TypeInformation<?> valueType;

    private Class<S> resolvedType;

    /**
     * Creates a new {@link TypeDiscoverer} for the given type and type variable map.
     *
     * @param type must not be {@literal null}.
     * @param typeVariableMap must not be {@literal null}.
     */
    protected TypeDiscoverer(Type type, Map<TypeVariable<?>, Type> typeVariableMap) {

        LettuceAssert.notNull(type, "Type must not be null");
        LettuceAssert.notNull(typeVariableMap, "TypeVariableMap must not be null");

        this.type = type;
        this.typeVariableMap = typeVariableMap;
        this.hashCode = 17 + (31 * type.hashCode()) + (31 * typeVariableMap.hashCode());
    }

    /**
     * Returns the type variable map.
     *
     * @return
     */
    public Map<TypeVariable<?>, Type> getTypeVariableMap() {
        return typeVariableMap;
    }

    /**
     * Creates {@link TypeInformation} for the given {@link Type}.
     *
     * @param fieldType
     * @return
     */
    @SuppressWarnings({ "rawtypes", "unchecked", "deprecation" })
    protected TypeInformation<?> createInfo(Type fieldType) {

        if (fieldType.equals(this.type)) {
            return this;
        }

        if (fieldType instanceof Class) {
            return new ClassTypeInformation((Class<?>) fieldType);
        }

        Class<S> resolveType = resolveClass(fieldType);
        Map<TypeVariable, Type> variableMap = new HashMap<TypeVariable, Type>();
        variableMap.putAll(GenericTypeResolver.getTypeVariableMap(resolveType));

        if (fieldType instanceof ParameterizedType) {

            ParameterizedType parameterizedType = (ParameterizedType) fieldType;

            TypeVariable<Class<S>>[] typeParameters = resolveType.getTypeParameters();
            Type[] arguments = parameterizedType.getActualTypeArguments();

            for (int i = 0; i < typeParameters.length; i++) {
                variableMap.put(typeParameters[i], arguments[i]);
            }

            return new ParametrizedTypeInformation(parameterizedType, this, variableMap);
        }

        if (fieldType instanceof TypeVariable) {
            TypeVariable<?> variable = (TypeVariable<?>) fieldType;
            return new TypeVariableTypeInformation(variable, type, this, variableMap);
        }

        if (fieldType instanceof GenericArrayType) {
            return new GenericArrayTypeInformation((GenericArrayType) fieldType, this, variableMap);
        }

        if (fieldType instanceof WildcardType) {

            WildcardType wildcardType = (WildcardType) fieldType;
            return new WildcardTypeInformation(wildcardType, variableMap);
        }

        throw new IllegalArgumentException();
    }

    /**
     * Resolves the given type into a plain {@link Class}.
     *
     * @param type
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected Class<S> resolveClass(Type type) {

        Map<TypeVariable, Type> map = new HashMap<TypeVariable, Type>();
        map.putAll(getTypeVariableMap());

        return (Class<S>) ResolvableType.forType(type, new TypeVariableMapVariableResolver(map)).resolve(Object.class);
    }

    /**
     * Resolves the given type into a {@link Type}.
     *
     * @param type
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected Type resolveType(Type type) {

        Map<TypeVariable, Type> map = new HashMap<>();
        map.putAll(getTypeVariableMap());

        return ResolvableType.forType(type, new TypeVariableMapVariableResolver(map)).getType();
    }

    public List<TypeInformation<?>> getParameterTypes(Constructor<?> constructor) {

        LettuceAssert.notNull(constructor, "Constructor must not be null!");

        Type[] types = constructor.getGenericParameterTypes();
        List<TypeInformation<?>> result = new ArrayList<TypeInformation<?>>(types.length);

        for (Type parameterType : types) {
            result.add(createInfo(parameterType));
        }

        return result;
    }

    public Class<S> getType() {

        if (resolvedType == null) {
            this.resolvedType = resolveClass(type);
        }

        return this.resolvedType;
    }

    @Override
    public Type getGenericType() {
        return resolveType(type);
    }

    @Override
    public ClassTypeInformation<?> getRawTypeInformation() {
        return ClassTypeInformation.from(getType()).getRawTypeInformation();
    }

    public TypeInformation<?> getActualType() {

        if (isMap()) {
            return getMapValueType();
        }

        if (isCollectionLike()) {
            return getComponentType();
        }

        return this;
    }

    public boolean isMap() {
        return Map.class.isAssignableFrom(getType());
    }

    public TypeInformation<?> getMapValueType() {

        if (!valueTypeResolved) {
            this.valueType = doGetMapValueType();
            this.valueTypeResolved = true;
        }

        return this.valueType;
    }

    protected TypeInformation<?> doGetMapValueType() {

        if (isMap()) {
            return getTypeArgument(Map.class, 1);
        }

        List<TypeInformation<?>> arguments = getTypeArguments();

        if (arguments.size() > 1) {
            return arguments.get(1);
        }

        return null;
    }

    public boolean isCollectionLike() {

        Class<?> rawType = getType();

        if (rawType.isArray() || Iterable.class.equals(rawType)) {
            return true;
        }

        return Collection.class.isAssignableFrom(rawType);
    }

    public final TypeInformation<?> getComponentType() {

        if (!componentTypeResolved) {
            this.componentType = doGetComponentType();
            this.componentTypeResolved = true;
        }

        return this.componentType;
    }

    protected TypeInformation<?> doGetComponentType() {

        Class<S> rawType = getType();

        if (rawType.isArray()) {
            return createInfo(rawType.getComponentType());
        }

        if (isMap()) {
            return getTypeArgument(Map.class, 0);
        }

        if (Iterable.class.isAssignableFrom(rawType)) {
            return getTypeArgument(Iterable.class, 0);
        }

        List<TypeInformation<?>> arguments = getTypeArguments();

        if (arguments.size() > 0) {
            return arguments.get(0);
        }

        return null;
    }

    public TypeInformation<?> getReturnType(Method method) {

        return createInfo(method.getGenericReturnType());
    }

    public List<TypeInformation<?>> getParameterTypes(Method method) {

        LettuceAssert.notNull(method, "Method most not be null!");

        Type[] types = method.getGenericParameterTypes();
        List<TypeInformation<?>> result = new ArrayList<TypeInformation<?>>(types.length);

        for (Type parameterType : types) {
            result.add(createInfo(parameterType));
        }

        return result;
    }

    public TypeInformation<?> getSuperTypeInformation(Class<?> superType) {

        Class<?> rawType = getType();

        if (!superType.isAssignableFrom(rawType)) {
            return null;
        }

        if (getType().equals(superType)) {
            return this;
        }

        List<Type> candidates = new ArrayList<Type>();

        Type genericSuperclass = rawType.getGenericSuperclass();
        if (genericSuperclass != null) {
            candidates.add(genericSuperclass);
        }
        candidates.addAll(Arrays.asList(rawType.getGenericInterfaces()));

        for (Type candidate : candidates) {

            TypeInformation<?> candidateInfo = createInfo(candidate);

            if (superType.equals(candidateInfo.getType())) {
                return candidateInfo;
            } else {
                TypeInformation<?> nestedSuperType = candidateInfo.getSuperTypeInformation(superType);
                if (nestedSuperType != null) {
                    return nestedSuperType;
                }
            }
        }

        return null;
    }

    public List<TypeInformation<?>> getTypeArguments() {
        return Collections.emptyList();
    }

    public boolean isAssignableFrom(TypeInformation<?> target) {
        return target.getSuperTypeInformation(getType()).equals(this);
    }

    public TypeInformation<?> getTypeArgument(Class<?> bound, int index) {

        Class<?>[] arguments = GenericTypeResolver.resolveTypeArguments(getType(), bound);

        if (arguments == null) {
            return getSuperTypeInformation(bound) instanceof ParametrizedTypeInformation ? ClassTypeInformation.OBJECT : null;
        }

        return createInfo(arguments[index]);
    }

    @Override
    public boolean equals(Object obj) {

        if (obj == this) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (!this.getClass().equals(obj.getClass())) {
            return false;
        }

        TypeDiscoverer<?> that = (TypeDiscoverer<?>) obj;

        return this.type.equals(that.type) && this.typeVariableMap.equals(that.typeVariableMap);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @SuppressWarnings({ "serial", "rawtypes" })
    private static class TypeVariableMapVariableResolver implements ResolvableType.VariableResolver {

        private final Map<TypeVariable, Type> typeVariableMap;

        public TypeVariableMapVariableResolver(Map<TypeVariable, Type> typeVariableMap) {
            this.typeVariableMap = typeVariableMap;
        }

        @Override
        public ResolvableType resolveVariable(TypeVariable<?> variable) {
            Type type = this.typeVariableMap.get(variable);
            return (type != null ? ResolvableType.forType(type) : null);
        }

        @Override
        public Object getSource() {
            return this.typeVariableMap;
        }
    }
}
