package com.lambdaworks.redis.dynamic.output;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.lambdaworks.redis.dynamic.support.TypeInformation;
import com.lambdaworks.redis.internal.LettuceAssert;

/**
 * Selector {@link CommandOutputFactory} resolution.
 * <p>
 * A {@link OutputSelector} is based on the result {@link TypeInformation} and can supply additionaly whether the type is a key
 * or value type. An optional {@link Map} with type variables (usually {@link com.lambdaworks.redis.codec.RedisCodec} type
 * variables) can be supplied to resolve type variables inside of {@link com.lambdaworks.redis.output.CommandOutput}.
 *
 * @author Mark Paluch
 * @since 5.0
 */
public class OutputSelector {

    private final TypeInformation<?> typeInformation;
    private final boolean key;
    private final boolean value;
    private final Map<String, TypeInformation<?>> typeVariables;

    /**
     * Create a new {@link OutputSelector} given {@link TypeInformation}.
     *
     * @param typeInformation must not be {@literal null}.
     */
    public OutputSelector(TypeInformation<?> typeInformation) {
        this(typeInformation, false, false, Collections.emptyMap());
    }

    /**
     * Create a new {@link OutputSelector} given {@link TypeInformation}, key/value flags and {@link Map} of type variables.
     *
     * @param typeInformation must not be {@literal null}.
     * @param key
     * @param value
     * @param typeVariables must not be {@literal null}.
     */
    public OutputSelector(TypeInformation<?> typeInformation, boolean key, boolean value,
            Map<String, TypeInformation<?>> typeVariables) {

        LettuceAssert.notNull(typeInformation, "Result TypeInformation must not be null");
        LettuceAssert.notNull(typeVariables, "Map of type variables must not be null");

        this.typeInformation = typeInformation;
        this.key = key;
        this.value = value;
        this.typeVariables = Collections.unmodifiableMap(new HashMap<>(typeVariables));
    }

    /**
     * @param typeVariableName
     * @return {@literal true} if the {@code typeVariableName} is provided.
     */
    public boolean containsTypeVariable(String typeVariableName) {
        return typeVariables.containsKey(typeVariableName);
    }

    /**
     * @param typeInformation
     * @return the {@link TypeInformation} for a type variable.
     */
    public TypeInformation<?> getTypeVariable(String typeInformation) {
        return typeVariables.get(typeInformation);
    }

    /**
     * @return the resulting {@link TypeInformation}.
     */
    public TypeInformation<?> getTypeInformation() {
        return typeInformation;
    }

    /**
     * @return {@link Map} of type variables.
     */
    public Map<String, TypeInformation<?>> getTypeVariables() {
        return Collections.unmodifiableMap(typeVariables);
    }

    /**
     * @return {@literal true} if the {@link #getTypeInformation()} is a key-type.
     */
    public boolean isKey() {
        return key;
    }

    /**
     * @return {@literal true} if the {@link #getTypeInformation()} is a value-type.
     */
    public boolean isValue() {
        return value;
    }
}
