/*
 * Copyright 2017-2018 the original author or authors.
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
package io.lettuce.core.dynamic;

import java.util.List;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.dynamic.codec.AnnotationRedisCodecResolver;
import io.lettuce.core.dynamic.output.CodecAwareOutputFactoryResolver;
import io.lettuce.core.dynamic.output.CommandOutputFactoryResolver;
import io.lettuce.core.dynamic.segment.AnnotationCommandSegmentFactory;
import io.lettuce.core.dynamic.segment.CommandSegments;

/**
 * @author Mark Paluch
 * @since 5.0
 */
abstract class ExecutableCommandLookupStrategySupport implements ExecutableCommandLookupStrategy {

    private final List<RedisCodec<?, ?>> redisCodecs;
    private final CommandOutputFactoryResolver commandOutputFactoryResolver;
    private final CommandFactoryResolver commandFactoryResolver;
    private final CommandMethodVerifier commandMethodVerifier;

    public ExecutableCommandLookupStrategySupport(List<RedisCodec<?, ?>> redisCodecs,
            CommandOutputFactoryResolver commandOutputFactoryResolver, CommandMethodVerifier commandMethodVerifier) {

        this.redisCodecs = redisCodecs;
        this.commandOutputFactoryResolver = commandOutputFactoryResolver;
        this.commandMethodVerifier = commandMethodVerifier;
        this.commandFactoryResolver = new DefaultCommandFactoryResolver();
    }

    protected CommandFactory resolveCommandFactory(CommandMethod commandMethod, RedisCommandsMetadata commandsMetadata) {
        return commandFactoryResolver.resolveRedisCommandFactory(commandMethod, commandsMetadata);
    }

    @SuppressWarnings("unchecked")
    class DefaultCommandFactoryResolver implements CommandFactoryResolver {

        final AnnotationCommandSegmentFactory commandSegmentFactory = new AnnotationCommandSegmentFactory();
        final AnnotationRedisCodecResolver codecResolver;

        DefaultCommandFactoryResolver() {
            codecResolver = new AnnotationRedisCodecResolver(redisCodecs);
        }

        @Override
        public CommandFactory resolveRedisCommandFactory(CommandMethod commandMethod, RedisCommandsMetadata commandsMetadata) {

            RedisCodec<?, ?> codec = codecResolver.resolve(commandMethod);

            if (codec == null) {
                throw new CommandCreationException(commandMethod, "Cannot resolve RedisCodec");
            }

            CodecAwareOutputFactoryResolver outputFactoryResolver = new CodecAwareOutputFactoryResolver(
                    commandOutputFactoryResolver, codec);
            CommandSegments commandSegments = commandSegmentFactory.createCommandSegments(commandMethod);

            commandMethodVerifier.validate(commandSegments, commandMethod);

            return new CommandSegmentCommandFactory(commandSegments, commandMethod, codec, outputFactoryResolver);
        }
    }
}
