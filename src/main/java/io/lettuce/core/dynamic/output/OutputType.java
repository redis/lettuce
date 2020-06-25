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
package io.lettuce.core.dynamic.output;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.dynamic.support.ResolvableType;
import io.lettuce.core.dynamic.support.TypeInformation;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.output.CommandOutput;

/**
 * Type descriptor for a {@link io.lettuce.core.output.CommandOutput}.
 * <p>
 * This value object describes the primary output type and the produced {@link TypeInformation} by the {@link CommandOutput}
 * type.
 * <p>
 * {@link OutputType} makes a distinction whether a {@link CommandOutput} is a {@link io.lettuce.core.output.StreamingOutput} by
 * providing {@code streaming}. Streaming outputs produce usually a component type hence they require an own {@link OutputType}
 * descriptor.
 *
 * @author Mark Paluch
 * @since 5.0
 */
@SuppressWarnings("rawtypes")
public class OutputType {

    private final Class<? extends CommandOutput> commandOutputClass;

    private final TypeInformation<?> typeInformation;

    private final boolean streaming;

    /**
     * Create a new {@link OutputType} given {@code primaryType}, the {@code commandOutputClass}, {@link TypeInformation} and
     * whether the {@link OutputType} is for a {@link io.lettuce.core.output.StreamingOutput}.
     *
     * @param commandOutputClass must not be {@code null}.
     * @param typeInformation must not be {@code null}.
     * @param streaming {@code true} if the type descriptor concerns the {@link io.lettuce.core.output.StreamingOutput}
     */
    OutputType(Class<? extends CommandOutput> commandOutputClass, TypeInformation<?> typeInformation, boolean streaming) {

        LettuceAssert.notNull(commandOutputClass, "CommandOutput class must not be null");
        LettuceAssert.notNull(typeInformation, "TypeInformation must not be null");

        this.commandOutputClass = commandOutputClass;
        this.typeInformation = typeInformation;
        this.streaming = streaming;
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

    public ResolvableType withCodec(RedisCodec<?, ?> codec) {
        return ResolvableType.forClass(typeInformation.getType());
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
        sb.append(" [commandOutputClass=").append(commandOutputClass);
        sb.append(", typeInformation=").append(typeInformation);
        sb.append(", streaming=").append(streaming);
        sb.append(']');
        return sb.toString();
    }

}
