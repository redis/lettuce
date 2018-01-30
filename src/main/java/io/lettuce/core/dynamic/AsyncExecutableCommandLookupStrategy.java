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

import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.dynamic.output.CommandOutputFactoryResolver;
import io.lettuce.core.internal.LettuceAssert;

/**
 * @author Mark Paluch
 * @since 5.0
 */
class AsyncExecutableCommandLookupStrategy extends ExecutableCommandLookupStrategySupport {

    private final StatefulConnection<Object, Object> connection;

    public AsyncExecutableCommandLookupStrategy(List<RedisCodec<?, ?>> redisCodecs,
            CommandOutputFactoryResolver commandOutputFactoryResolver, CommandMethodVerifier commandMethodVerifier,
            StatefulConnection<Object, Object> connection) {

        super(redisCodecs, commandOutputFactoryResolver, commandMethodVerifier);
        this.connection = connection;
    }

    @Override
    public ExecutableCommand resolveCommandMethod(CommandMethod method, RedisCommandsMetadata metadata) {

        LettuceAssert.isTrue(!method.isReactiveExecution(),
                String.format("Command method %s not supported by this command lookup strategy", method));

        CommandFactory commandFactory = super.resolveCommandFactory(method, metadata);

        return new AsyncExecutableCommand(method, commandFactory, connection);
    }
}
