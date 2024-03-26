package io.lettuce.core.dynamic.support;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * {@link TypeInformation} for a {@link WildcardType}.
 */
class WildcardTypeInformation<S> extends TypeDiscoverer<S> {

    private final WildcardType type;

    /**
     * Creates a new {@link WildcardTypeInformation} for the given type, type variable map.
     *
     * @param type must not be {@code null}.
     * @param typeVariableMap must not be {@code null}.
     */
    protected WildcardTypeInformation(WildcardType type, Map<TypeVariable<?>, Type> typeVariableMap) {

        super(type, typeVariableMap);
        this.type = type;
    }

    @Override
    public boolean isAssignableFrom(TypeInformation<?> target) {

        for (TypeInformation<?> lowerBound : getLowerBounds()) {
            if (!target.isAssignableFrom(lowerBound)) {
                return false;
            }
        }

        for (TypeInformation<?> upperBound : getUpperBounds()) {
            if (!upperBound.isAssignableFrom(target)) {
                return false;
            }
        }

        return true;
    }

    public List<TypeInformation<?>> getUpperBounds() {
        return getBounds(type.getUpperBounds());
    }

    public List<TypeInformation<?>> getLowerBounds() {
        return getBounds(type.getLowerBounds());
    }

    private List<TypeInformation<?>> getBounds(Type[] bounds) {

        List<TypeInformation<?>> typeInformations = new ArrayList<>(bounds.length);

        Arrays.stream(bounds).map(this::createInfo).forEach(typeInformations::add);

        return typeInformations;
    }

}
