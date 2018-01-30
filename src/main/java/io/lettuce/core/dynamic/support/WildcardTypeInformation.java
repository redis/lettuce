/*
 * Copyright 2016-2018 the original author or authors.
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
     * @param type must not be {@literal null}.
     * @param typeVariableMap must not be {@literal null}.
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
