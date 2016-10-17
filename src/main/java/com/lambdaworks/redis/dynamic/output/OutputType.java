package com.lambdaworks.redis.dynamic.output;

import com.lambdaworks.redis.dynamic.support.TypeInformation;
import com.lambdaworks.redis.internal.LettuceAssert;
import com.lambdaworks.redis.output.CommandOutput;

/**
 * Type descriptor for a {@link com.lambdaworks.redis.output.CommandOutput}.
 * <p>
 * This value object describes the primary output type and the produced {@link TypeInformation} by the {@link CommandOutput}
 * type.
 * <p>
 * {@link OutputType} makes a distinction whether a {@link CommandOutput} is a
 * {@link com.lambdaworks.redis.output.StreamingOutput} by providing {@code streaming}. Streaming outputs produce usually a
 * component type hence they require an own {@link OutputType} descriptor.
 * 
 * @author Mark Paluch
 * @since 5.0
 */
public class OutputType {

    private final Class<?> primaryType;
    private final Class<? extends CommandOutput> commandOutputClass;
    private final TypeInformation<?> typeInformation;
    private final boolean streaming;

    /**
     * Create a new {@link OutputType} given {@code primaryType}, the {@code commandOutputClass}, {@link TypeInformation} and
     * whether the {@link OutputType} is for a {@link com.lambdaworks.redis.output.StreamingOutput}.
     * 
     * @param primaryType must not be {@literal null}.
     * @param commandOutputClass must not be {@literal null}.
     * @param typeInformation must not be {@literal null}.
     * @param streaming {@literal true} if the type descriptor concerns the {@link com.lambdaworks.redis.output.StreamingOutput}
     *        result type.
     */
    public OutputType(Class<?> primaryType, Class<? extends CommandOutput> commandOutputClass,
            TypeInformation<?> typeInformation, boolean streaming) {

        LettuceAssert.notNull(primaryType, "Primary type must not be null");
        LettuceAssert.notNull(commandOutputClass, "CommandOutput class must not be null");
        LettuceAssert.notNull(typeInformation, "TypeInformation must not be null");

        this.primaryType = primaryType;
        this.commandOutputClass = commandOutputClass;
        this.typeInformation = typeInformation;
        this.streaming = streaming;
    }

    /**
     * @return
     */
    public Class<?> getPrimaryType() {
        return primaryType;
    }

    /**
     * @return
     */
    public TypeInformation<?> getTypeInformation() {
        return typeInformation;
    }

    /**
     * @return
     */
    public boolean isStreaming() {
        return streaming;
    }

    /**
     * @return
     */
    public Class<?> getCommandOutputClass() {
        return commandOutputClass;
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [primaryType=").append(primaryType);
        sb.append(", commandOutputClass=").append(commandOutputClass);
        sb.append(", typeInformation=").append(typeInformation);
        sb.append(", streaming=").append(streaming);
        sb.append(']');
        return sb.toString();
    }
}
