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
import io.lettuce.core.internal.LettuceAssert;

/**
 * {@link RedisCodec}-aware implementation of {@link CommandOutputFactoryResolver}. This implementation inspects
 * {@link RedisCodec} regarding its type and enhances {@link OutputSelector} for {@link CommandOutputFactory} resolution.
 *
 * @author Mark Paluch
 * @since 5.0
 */
public class CodecAwareOutputFactoryResolver implements CommandOutputFactoryResolver {

    private final CommandOutputFactoryResolver delegate;

    private final RedisCodec<?, ?> redisCodec;

    /**
     * Create a new {@link CodecAwareOutputFactoryResolver} given {@link CommandOutputFactoryResolver} and {@link RedisCodec}.
     *
     * @param delegate must not be {@code null}.
     * @param redisCodec must not be {@code null}.
     */
    public CodecAwareOutputFactoryResolver(CommandOutputFactoryResolver delegate, RedisCodec<?, ?> redisCodec) {

        LettuceAssert.notNull(delegate, "CommandOutputFactoryResolver delegate must not be null");
        LettuceAssert.notNull(redisCodec, "RedisCodec must not be null");

        this.delegate = delegate;
        this.redisCodec = redisCodec;
    }

    @Override
    public CommandOutputFactory resolveCommandOutput(OutputSelector outputSelector) {
        return delegate.resolveCommandOutput(new OutputSelector(outputSelector.getOutputType(), redisCodec));
    }

    @Override
    public CommandOutputFactory resolveStreamingCommandOutput(OutputSelector outputSelector) {
        return delegate.resolveStreamingCommandOutput(new OutputSelector(outputSelector.getOutputType(), redisCodec));
    }

}
