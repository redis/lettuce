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
package io.lettuce.core.dynamic.output;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.dynamic.support.ResolvableType;
import io.lettuce.core.internal.LettuceAssert;

/**
 * Selector {@link CommandOutputFactory} resolution.
 * <p>
 * A {@link OutputSelector} is based on the result {@link ResolvableType} and {@link io.lettuce.core.codec.RedisCodec}.
 * The codec supplies types for generics resolution of {@link io.lettuce.core.output.CommandOutput}.
 *
 * @author Mark Paluch
 * @since 5.0
 */
public class OutputSelector {

    private final ResolvableType outputType;
    private final RedisCodec<?, ?> redisCodec;

    /**
     * Creates a new {@link OutputSelector} given {@link ResolvableType} and {@link RedisCodec}.
     *
     * @param outputType must not be {@literal null}.
     * @param redisCodec must not be {@literal null}.
     */
    public OutputSelector(ResolvableType outputType, RedisCodec<?, ?> redisCodec) {

        LettuceAssert.notNull(outputType, "Output type must not be null!");
        LettuceAssert.notNull(redisCodec, "RedisCodec must not be null!");

        this.outputType = outputType;
        this.redisCodec = redisCodec;
    }

    /**
     * @return the output type.
     */
    public ResolvableType getOutputType() {
        return outputType;
    }

    /**
     *
     * @return the associated codec.
     */
    public RedisCodec<?, ?> getRedisCodec() {
        return redisCodec;
    }
}
