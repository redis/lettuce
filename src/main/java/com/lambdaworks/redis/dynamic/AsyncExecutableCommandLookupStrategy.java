/*
 * Copyright 2017 the original author or authors.
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
package com.lambdaworks.redis.dynamic;

import java.lang.reflect.Method;
import java.util.List;

import com.lambdaworks.redis.api.StatefulConnection;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.dynamic.codec.AnnotationRedisCodecResolver;
import com.lambdaworks.redis.dynamic.output.CodecAwareOutputFactoryResolver;
import com.lambdaworks.redis.dynamic.output.CommandOutputFactoryResolver;
import com.lambdaworks.redis.dynamic.segment.AnnotationCommandSegmentFactory;
import com.lambdaworks.redis.dynamic.segment.CommandSegments;
import com.lambdaworks.redis.internal.LettuceAssert;

/**
 * @author Mark Paluch
 * @since 5.0
 */
class AsyncExecutableCommandLookupStrategy implements ExecutableCommandLookupStrategy {

    private final List<RedisCodec<?, ?>> redisCodecs;
    private final CommandOutputFactoryResolver commandOutputFactoryResolver;
    private final DefaultCommandFactoryResolver commandFactoryResolver;
    private final CommandMethodVerifier commandMethodVerifier;

    private final StatefulConnection<Object, Object> connection;

    public AsyncExecutableCommandLookupStrategy(List<RedisCodec<?, ?>> redisCodecs,
            CommandOutputFactoryResolver commandOutputFactoryResolver, CommandMethodVerifier commandMethodVerifier,
            StatefulConnection<Object, Object> connection) {

        this.redisCodecs = redisCodecs;
        this.commandOutputFactoryResolver = commandOutputFactoryResolver;
        this.commandMethodVerifier = commandMethodVerifier;
        this.connection = connection;

        this.commandFactoryResolver = new DefaultCommandFactoryResolver();
    }

    @Override
    public ExecutableCommand resolveCommandMethod(Method method, RedisCommandsMetadata commandsMetadata) {

        CommandMethod commandMethod = new CommandMethod(method);

        LettuceAssert.isTrue(!commandMethod.isReactiveExecution(),
                String.format("Command method %s not supported by this command lookup strategy", method));

        CommandFactory commandFactory = commandFactoryResolver.resolveRedisCommandFactory(commandMethod, commandsMetadata);

        return new AsyncExecutableCommand(commandMethod, commandFactory, connection);
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
