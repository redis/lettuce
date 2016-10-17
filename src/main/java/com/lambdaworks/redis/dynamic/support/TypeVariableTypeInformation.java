package com.lambdaworks.redis.dynamic.support;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Map;

import com.lambdaworks.redis.internal.LettuceAssert;

/**
 * Special {@link TypeDiscoverer} to determine the actual type for a {@link TypeVariable}. Will consider the context the
 * {@link TypeVariable} is being used in.
 */
public class TypeVariableTypeInformation<T> extends ParentTypeAwareTypeInformation<T> {

    private final TypeVariable<?> variable;
    private final Type owningType;

    /**
     * Creates a bew {@link TypeVariableTypeInformation} for the given {@link TypeVariable} owning {@link Type} and parent
     * {@link TypeDiscoverer}.
     * 
     * @param variable must not be {@literal null}
     * @param owningType must not be {@literal null}
     * @param parent
     */
    public TypeVariableTypeInformation(TypeVariable<?> variable, Type owningType, TypeDiscoverer<?> parent,
            Map<TypeVariable<?>, Type> typeVariableMap) {

        super(variable, parent, typeVariableMap);

        LettuceAssert.notNull(variable, "TypeVariable must not be null");

        this.variable = variable;
        this.owningType = owningType;
    }

    @Override
    public Class<T> getType() {

        int index = getIndex(variable);

        if (owningType instanceof ParameterizedType && index != -1) {
            Type fieldType = ((ParameterizedType) owningType).getActualTypeArguments()[index];
            return resolveType(fieldType);
        }

        return resolveType(variable);
    }

    /**
     * Returns the index of the type parameter binding the given {@link TypeVariable}.
     * 
     * @param variable
     * @return
     */
    private int getIndex(TypeVariable<?> variable) {

        Class<?> rawType = resolveType(owningType);
        TypeVariable<?>[] typeParameters = rawType.getTypeParameters();

        for (int i = 0; i < typeParameters.length; i++) {
            if (variable.equals(typeParameters[i])) {
                return i;
            }
        }

        return -1;
    }

    @Override
    public boolean equals(Object obj) {

        if (obj == this) {
            return true;
        }

        if (!(obj instanceof TypeVariableTypeInformation)) {
            return false;
        }

        TypeVariableTypeInformation<?> that = (TypeVariableTypeInformation<?>) obj;

        return getType().equals(that.getType());
    }

    @Override
    public String toString() {
        return variable.getName();
    }

    public String getVariableName() {
        return variable.getName();
    }
}
