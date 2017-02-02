/*
 * Copyright 2011-2016 the original author or authors.
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
import java.util.*;

import org.springframework.util.Assert;

import com.lambdaworks.redis.AbstractRedisReactiveCommands;
import com.lambdaworks.redis.api.StatefulConnection;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.codec.ByteArrayCodec;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.codec.StringCodec;
import com.lambdaworks.redis.dynamic.batch.BatchSize;
import com.lambdaworks.redis.dynamic.intercept.InvocationProxyFactory;
import com.lambdaworks.redis.dynamic.intercept.MethodInterceptor;
import com.lambdaworks.redis.dynamic.intercept.MethodInvocation;
import com.lambdaworks.redis.dynamic.output.CommandOutputFactoryResolver;
import com.lambdaworks.redis.dynamic.output.OutputRegistry;
import com.lambdaworks.redis.dynamic.output.OutputRegistryCommandOutputFactoryResolver;
import com.lambdaworks.redis.internal.LettuceAssert;
import com.lambdaworks.redis.internal.LettuceLists;
import com.lambdaworks.redis.models.command.CommandDetail;
import com.lambdaworks.redis.models.command.CommandDetailParser;
import com.lambdaworks.redis.protocol.LettuceCharsets;
import com.lambdaworks.redis.protocol.RedisCommand;

/**
 * Factory to create Redis Command interface instances.
 * <p>
 * This class is the entry point to implement command interfaces and obtain a reference to the implementation. Redis Command
 * interfaces provide a dynamic API that are declared in userland code. {@link RedisCommandFactory} and its supportive classes
 * analyze method declarations and derive from those factories to create and execute {@link RedisCommand}s.
 *
 * <h3>Example</h3> <code><pre>
public interface MyRedisCommands {

    String get(String key); // Synchronous Execution of GET

    &#64;Command("GET")
    byte[] getAsBytes(String key); // Synchronous Execution of GET returning data as byte array

    &#64;Command("SET") // synchronous execution applying a Timeout
    String setSync(String key, String value, Timeout timeout);

    Future<String> set(String key, String value); // asynchronous SET execution

    &#64;Command("SET")
    Mono<String> setReactive(String key, String value, SetArgs setArgs); // reactive SET execution using SetArgs

    &#64;CommandNaming(split = DOT) // support for Redis Module command notation -> NR.RUN
    double nrRun(String key, int... indexes);
}

 RedisCommandFactory factory = new RedisCommandFactory(connection);

 MyRedisCommands commands = factory.getCommands(MyRedisCommands.class);

 String value = commands.get("key");

 * </pre></code>
 *
 * @author Mark Paluch
 * @since 5.0
 * @see com.lambdaworks.redis.dynamic.annotation.Command
 * @see CommandMethod
 */
public class RedisCommandFactory {

    private final StatefulConnection<?, ?> connection;
    private final DefaultCommandMethodVerifier commandMethodVerifier;
    private final List<RedisCodec<?, ?>> redisCodecs = new ArrayList<>();

    private CommandOutputFactoryResolver commandOutputFactoryResolver = new OutputRegistryCommandOutputFactoryResolver(
            new OutputRegistry());

    private boolean verifyCommandMethods = true;

    /**
     * Create a new {@link CommandFactory} given {@link StatefulConnection}.
     * 
     * @param connection must not be {@literal null}.
     */
    public RedisCommandFactory(StatefulConnection<?, ?> connection) {
        this(connection, LettuceLists.newList(new ByteArrayCodec(), new StringCodec(LettuceCharsets.UTF8)));
    }

    /**
     * Create a new {@link CommandFactory} given {@link StatefulConnection} and a {@link List} of {@link RedisCodec}s to use
     *
     * @param connection must not be {@literal null}.
     * @param redisCodecs must not be {@literal null}.
     */
    public RedisCommandFactory(StatefulConnection<?, ?> connection, Iterable<? extends RedisCodec<?, ?>> redisCodecs) {

        LettuceAssert.notNull(connection, "Redis Connection must not be null");
        LettuceAssert.notNull(redisCodecs, "Iterable of RedisCodec must not be null");

        this.connection = connection;
        this.redisCodecs.addAll(LettuceLists.newList(redisCodecs));

        commandMethodVerifier = new DefaultCommandMethodVerifier(getCommands(connection));
    }

    @SuppressWarnings("unchecked")
    private List<CommandDetail> getCommands(StatefulConnection<?, ?> connection) {

        List<Object> commands = Collections.emptyList();
        if (connection instanceof StatefulRedisConnection) {
            commands = ((StatefulRedisConnection) connection).sync().command();
        }

        if (connection instanceof StatefulRedisClusterConnection) {
            commands = ((StatefulRedisClusterConnection) connection).sync().command();
        }

        if (commands.isEmpty()) {
            verifyCommandMethods = false;
        }

        return CommandDetailParser.parse(commands);
    }

    /**
     * Set a {@link CommandOutputFactoryResolver}.
     * 
     * @param commandOutputFactoryResolver must not be {@literal null}.
     */
    public void setCommandOutputFactoryResolver(CommandOutputFactoryResolver commandOutputFactoryResolver) {

        LettuceAssert.notNull(commandOutputFactoryResolver, "CommandOutputFactoryResolver must not be null");

        this.commandOutputFactoryResolver = commandOutputFactoryResolver;
    }

    /**
     * Enables/disables command verification which checks the command name against Redis {@code COMMAND} and the argument count.
     * 
     * @param verifyCommandMethods {@literal true} to enable command verification (default) or {@literal false} to disable
     *        command verification.
     */
    public void setVerifyCommandMethods(boolean verifyCommandMethods) {
        this.verifyCommandMethods = verifyCommandMethods;
    }

    /**
     * Returns a Redis Commands interface instance for the given interface.
     * 
     * @param commandInterface must not be {@literal null}.
     * @param <T> command interface type.
     * @return the implemented Redis Commands interface.
     */
    public <T extends Commands> T getCommands(Class<T> commandInterface) {

        LettuceAssert.notNull(commandInterface, "Redis Command Interface must not be null");

        RedisCommandsMetadata metadata = new DefaultRedisCommandsMetadata(commandInterface);

        InvocationProxyFactory factory = new InvocationProxyFactory();
        factory.addInterface(commandInterface);

        BatchAwareCommandLookupStrategy lookupStrategy = new BatchAwareCommandLookupStrategy(
                new CompositeCommandLookupStrategy(), metadata);

        factory.addInterceptor(new CommandFactoryExecutorMethodInterceptor(metadata, lookupStrategy));
        return factory.createProxy(commandInterface.getClassLoader());
    }

    /**
     * {@link CommandFactory}-based {@link MethodInterceptor} to create and invoke Redis Commands using asynchronous and
     * synchronous execution models.
     *
     * @author Mark Paluch
     */
    static class CommandFactoryExecutorMethodInterceptor implements MethodInterceptor {

        private final Map<Method, ExecutableCommand> commandMethods = new HashMap<>();

        CommandFactoryExecutorMethodInterceptor(RedisCommandsMetadata redisCommandsMetadata,
                ExecutableCommandLookupStrategy strategy) {

            for (Method method : redisCommandsMetadata.getMethods()) {

                ExecutableCommand executableCommand = strategy.resolveCommandMethod(DeclaredCommandMethod.create(method),
                        redisCommandsMetadata);
                commandMethods.put(method, executableCommand);
            }
        }

        @Override
        public Object invoke(MethodInvocation invocation) throws Throwable {

            Method method = invocation.getMethod();
            Object[] arguments = invocation.getArguments();

            if (hasFactoryFor(method)) {

                ExecutableCommand executableCommand = commandMethods.get(method);
                return executableCommand.execute(arguments);
            }

            return invocation.proceed();
        }

        private boolean hasFactoryFor(Method method) {
            return commandMethods.containsKey(method);
        }
    }

    class CompositeCommandLookupStrategy implements ExecutableCommandLookupStrategy {

        private final AsyncExecutableCommandLookupStrategy async;
        private final ReactiveExecutableCommandLookupStrategy reactive;

        CompositeCommandLookupStrategy() {

            CommandMethodVerifier verifier = verifyCommandMethods ? commandMethodVerifier : CommandMethodVerifier.NONE;

            AbstractRedisReactiveCommands reactive = null;
            if (connection instanceof StatefulRedisConnection) {
                reactive = (AbstractRedisReactiveCommands) ((StatefulRedisConnection) connection).reactive();
            }

            if (connection instanceof StatefulRedisClusterConnection) {
                reactive = (AbstractRedisReactiveCommands) ((StatefulRedisClusterConnection) connection).reactive();
            }

            Assert.state(reactive != null, "Reactive commands is null");

            this.async = new AsyncExecutableCommandLookupStrategy(redisCodecs, commandOutputFactoryResolver, verifier,
                    (StatefulConnection) connection);

            this.reactive = new ReactiveExecutableCommandLookupStrategy(redisCodecs, commandOutputFactoryResolver, verifier,
                    reactive);
        }

        @Override
        public ExecutableCommand resolveCommandMethod(CommandMethod method, RedisCommandsMetadata metadata) {

            if (method.isReactiveExecution()) {
                return reactive.resolveCommandMethod(method, metadata);
            }

            return async.resolveCommandMethod(method, metadata);
        }
    }

    class BatchAwareCommandLookupStrategy implements ExecutableCommandLookupStrategy {

        private final ExecutableCommandLookupStrategy fallbackStrategy;
        private final boolean globalBatching;
        private final CommandMethodVerifier verifier;
        private final long batchSize;

        private Batcher batcher = Batcher.NONE;
        private BatchExecutableCommandLookupStrategy batchingStrategy;

        public BatchAwareCommandLookupStrategy(ExecutableCommandLookupStrategy fallbackStrategy, RedisCommandsMetadata metadata) {

            this.fallbackStrategy = fallbackStrategy;
            this.verifier = verifyCommandMethods ? commandMethodVerifier : CommandMethodVerifier.NONE;

            if (metadata.hasAnnotation(BatchSize.class)) {

                BatchSize batchSize = metadata.getAnnotation(BatchSize.class);

                this.globalBatching = true;
                this.batchSize = batchSize.value();

            } else {

                this.globalBatching = false;
                this.batchSize = -1;
            }
        }

        @Override
        public ExecutableCommand resolveCommandMethod(CommandMethod method, RedisCommandsMetadata metadata) {

            if (BatchExecutableCommandLookupStrategy.supports(method) || globalBatching) {

                if (batcher == Batcher.NONE) {
                    batcher = new SimpleBatcher((StatefulConnection) connection, Math.toIntExact(batchSize));
                    batchingStrategy = new BatchExecutableCommandLookupStrategy(redisCodecs, commandOutputFactoryResolver,
                            verifier, batcher, (StatefulConnection) connection);
                }

                return batchingStrategy.resolveCommandMethod(method, metadata);
            }

            return fallbackStrategy.resolveCommandMethod(method, metadata);
        }
    }
}
