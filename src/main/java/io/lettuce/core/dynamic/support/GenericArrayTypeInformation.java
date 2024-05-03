package io.lettuce.core.dynamic.support;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Map;

/**
 * Special {@link TypeDiscoverer} handling {@link GenericArrayType}s.
 */
class GenericArrayTypeInformation<S> extends ParentTypeAwareTypeInformation<S> {

    private final GenericArrayType type;

    /**
     * Creates a new {@link GenericArrayTypeInformation} for the given {@link GenericArrayTypeInformation} and
     * {@link TypeDiscoverer}.
     *
     * @param type must not be {@code null}.
     * @param parent must not be {@code null}.
     * @param typeVariableMap must not be {@code null}.
     */
    protected GenericArrayTypeInformation(GenericArrayType type, TypeDiscoverer<?> parent,
            Map<TypeVariable<?>, Type> typeVariableMap) {

        super(type, parent, typeVariableMap);
        this.type = type;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<S> getType() {
        return (Class<S>) Array.newInstance(resolveClass(type.getGenericComponentType()), 0).getClass();
    }

    @Override
    protected TypeInformation<?> doGetComponentType() {

        Type componentType = type.getGenericComponentType();
        return createInfo(componentType);
    }

    @Override
    public String toString() {
        return type.toString();
    }

}
