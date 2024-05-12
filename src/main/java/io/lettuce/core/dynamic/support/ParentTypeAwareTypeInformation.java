package io.lettuce.core.dynamic.support;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class for {@link TypeInformation} implementations that need parent type awareness.
 */
abstract class ParentTypeAwareTypeInformation<S> extends TypeDiscoverer<S> {

    private final TypeDiscoverer<?> parent;

    private int hashCode;

    /**
     * Creates a new {@link ParentTypeAwareTypeInformation}.
     *
     * @param type must not be {@code null}.
     * @param parent must not be {@code null}.
     * @param map must not be {@code null}.
     */
    protected ParentTypeAwareTypeInformation(Type type, TypeDiscoverer<?> parent, Map<TypeVariable<?>, Type> map) {

        super(type, mergeMaps(parent, map));
        this.parent = parent;
    }

    /**
     * Merges the type variable maps of the given parent with the new map.
     *
     * @param parent must not be {@code null}.
     * @param map must not be {@code null}.
     * @return
     */
    private static Map<TypeVariable<?>, Type> mergeMaps(TypeDiscoverer<?> parent, Map<TypeVariable<?>, Type> map) {

        Map<TypeVariable<?>, Type> typeVariableMap = new HashMap<TypeVariable<?>, Type>();
        typeVariableMap.putAll(map);
        typeVariableMap.putAll(parent.getTypeVariableMap());

        return typeVariableMap;
    }

    @Override
    protected TypeInformation<?> createInfo(Type fieldType) {

        if (parent.getType().equals(fieldType)) {
            return parent;
        }

        return super.createInfo(fieldType);
    }

    @Override
    public boolean equals(Object obj) {

        if (!super.equals(obj)) {
            return false;
        }

        if (!this.getClass().equals(obj.getClass())) {
            return false;
        }

        ParentTypeAwareTypeInformation<?> that = (ParentTypeAwareTypeInformation<?>) obj;
        return this.parent == null ? that.parent == null : this.parent.equals(that.parent);
    }

    @Override
    public int hashCode() {

        if (this.hashCode == 0) {
            this.hashCode = super.hashCode() + 31 * parent.hashCode();
        }

        return this.hashCode;
    }

}
