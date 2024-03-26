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
                () -> String.format("Command method %s not supported by this command lookup strategy", method));

        CommandFactory commandFactory = super.resolveCommandFactory(method, metadata);

        return new AsyncExecutableCommand(method, commandFactory, connection);
    }

}
