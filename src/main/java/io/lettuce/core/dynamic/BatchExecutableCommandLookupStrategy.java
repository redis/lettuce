package io.lettuce.core.dynamic;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.dynamic.batch.BatchExecutor;
import io.lettuce.core.dynamic.output.CommandOutputFactoryResolver;
import io.lettuce.core.dynamic.parameter.ExecutionSpecificParameters;
import io.lettuce.core.internal.LettuceAssert;

/**
 * @author Mark Paluch
 * @since 5.0
 */
class BatchExecutableCommandLookupStrategy extends ExecutableCommandLookupStrategySupport {

    private final Set<Class<?>> SYNCHRONOUS_RETURN_TYPES = new HashSet<Class<?>>(Arrays.asList(Void.class, Void.TYPE));

    private final Batcher batcher;

    private final StatefulConnection<Object, Object> connection;

    public BatchExecutableCommandLookupStrategy(List<RedisCodec<?, ?>> redisCodecs,
            CommandOutputFactoryResolver commandOutputFactoryResolver, CommandMethodVerifier commandMethodVerifier,
            Batcher batcher, StatefulConnection<Object, Object> connection) {

        super(redisCodecs, commandOutputFactoryResolver, commandMethodVerifier);
        this.batcher = batcher;
        this.connection = connection;
    }

    public static boolean supports(CommandMethod method) {
        return method.isBatchExecution() || isForceFlush(method);
    }

    private static boolean isForceFlush(CommandMethod method) {
        return method.getName().equals("flush") && method.getMethod().getDeclaringClass().equals(BatchExecutor.class);
    }

    @Override
    public ExecutableCommand resolveCommandMethod(CommandMethod method, RedisCommandsMetadata metadata) {

        LettuceAssert.isTrue(!method.isReactiveExecution(),
                () -> String.format("Command method %s not supported by this command lookup strategy", method));

        ExecutionSpecificParameters parameters = (ExecutionSpecificParameters) method.getParameters();

        if (parameters.hasTimeoutIndex()) {
            throw new IllegalArgumentException(
                    String.format("Timeout and batching is not supported, offending command method %s ", method));
        }

        if (isForceFlush(method)) {

            return new ExecutableCommand() {

                @Override
                public Object execute(Object[] parameters) throws ExecutionException, InterruptedException {
                    BatchExecutableCommand.synchronize(batcher.flush(), connection);
                    return null;
                }

                @Override
                public CommandMethod getCommandMethod() {
                    return method;
                }

            };
        }

        if (method.isFutureExecution() || SYNCHRONOUS_RETURN_TYPES.contains(method.getReturnType().getRawClass())) {

            CommandFactory commandFactory = super.resolveCommandFactory(method, metadata);
            return new BatchExecutableCommand(method, commandFactory, batcher, connection);
        }

        throw new IllegalArgumentException(
                String.format("Batching command method %s must declare either a Future or void return type", method));
    }

}
