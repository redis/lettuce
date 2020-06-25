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
package io.lettuce.core.dynamic;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.*;

import io.lettuce.core.AbstractRedisReactiveCommands;
import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.dynamic.batch.BatchSize;
import io.lettuce.core.dynamic.intercept.DefaultMethodInvokingInterceptor;
import io.lettuce.core.dynamic.intercept.InvocationProxyFactory;
import io.lettuce.core.dynamic.intercept.MethodInterceptor;
import io.lettuce.core.dynamic.intercept.MethodInvocation;
import io.lettuce.core.dynamic.output.CommandOutputFactoryResolver;
import io.lettuce.core.dynamic.output.OutputRegistry;
import io.lettuce.core.dynamic.output.OutputRegistryCommandOutputFactoryResolver;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.internal.LettuceLists;
import io.lettuce.core.models.command.CommandDetail;
import io.lettuce.core.models.command.CommandDetailParser;
import io.lettuce.core.protocol.RedisCommand;
import io.lettuce.core.support.ConnectionWrapping;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Factory to create Redis Command interface instances.
 * <p>
 * This class is the entry point to implement command interfaces and obtain a reference to the implementation. Redis Command
 * interfaces provide a dynamic API that are declared in userland code. {@link RedisCommandFactory} and its supportive classes
 * analyze method declarations and derive from those factories to create and execute {@link RedisCommand}s.
 *
 * <h3>Example</h3>
 *
 * <pre class="code">
 *
 * public interface MyRedisCommands extends Commands {
 *
 *     String get(String key); // Synchronous Execution of GET
 *
 *     &#064;Command(&quot;GET&quot;)
 *     byte[] getAsBytes(String key); // Synchronous Execution of GET returning data as byte array
 *
 *     &#064;Command(&quot;SET&quot;)
 *     // synchronous execution applying a Timeout
 *     String setSync(String key, String value, Timeout timeout);
 *
 *     Future&lt;String&gt; set(String key, String value); // asynchronous SET execution
 *
 *     &#064;Command(&quot;SET&quot;)
 *     Mono&lt;String&gt; setReactive(String key, String value, SetArgs setArgs); // reactive SET execution using SetArgs
 *
 *     &#064;CommandNaming(split = DOT)
 *     // support for Redis Module command notation -&gt; NR.RUN
 *     double nrRun(String key, int... indexes);
 *
 * }
 *
 * RedisCommandFactory factory = new RedisCommandFactory(connection);
 *
 * MyRedisCommands commands = factory.getCommands(MyRedisCommands.class);
 *
 * String value = commands.get(&quot;key&quot;);
 *
 * </pre>
 *
 * @author Mark Paluch
 * @since 5.0
 * @see io.lettuce.core.dynamic.annotation.Command
 * @see CommandMethod
 */
public class RedisCommandFactory {

    private final InternalLogger log = InternalLoggerFactory.getInstance(getClass());

    private final StatefulConnection<?, ?> connection;

    private final DefaultCommandMethodVerifier commandMethodVerifier;

    private final List<RedisCodec<?, ?>> redisCodecs = new ArrayList<>();

    private CommandOutputFactoryResolver commandOutputFactoryResolver = new OutputRegistryCommandOutputFactoryResolver(
            new OutputRegistry());

    private boolean verifyCommandMethods = true;

    /**
     * Create a new {@link CommandFactory} given {@link StatefulConnection}.
     *
     * @param connection must not be {@code null}.
     */
    public RedisCommandFactory(StatefulConnection<?, ?> connection) {
        this(connection, LettuceLists.newList(new ByteArrayCodec(), new StringCodec(StandardCharsets.UTF_8)));
    }

    /**
     * Create a new {@link CommandFactory} given {@link StatefulConnection} and a {@link List} of {@link RedisCodec}s to use
     *
     * @param connection must not be {@code null}.
     * @param redisCodecs must not be {@code null}.
     */
    public RedisCommandFactory(StatefulConnection<?, ?> connection, Iterable<? extends RedisCodec<?, ?>> redisCodecs) {

        LettuceAssert.notNull(connection, "Redis Connection must not be null");
        LettuceAssert.notNull(redisCodecs, "Iterable of RedisCodec must not be null");

        this.connection = connection;
        this.redisCodecs.addAll(LettuceLists.newList(redisCodecs));
        this.commandMethodVerifier = new DefaultCommandMethodVerifier(getCommands(connection));
    }

    @SuppressWarnings("unchecked")
    private List<CommandDetail> getCommands(StatefulConnection<?, ?> connection) {

        List<Object> commands = Collections.emptyList();
        try {
            if (connection instanceof StatefulRedisConnection) {
                commands = ((StatefulRedisConnection) connection).sync().command();
            }

            if (connection instanceof StatefulRedisClusterConnection) {
                commands = ((StatefulRedisClusterConnection) connection).sync().command();
            }
        } catch (RedisCommandExecutionException e) {
            log.debug("Cannot obtain command metadata", e);
        }

        if (commands.isEmpty()) {
            setVerifyCommandMethods(false);
        }

        return CommandDetailParser.parse(commands);
    }

    /**
     * Set a {@link CommandOutputFactoryResolver}.
     *
     * @param commandOutputFactoryResolver must not be {@code null}.
     */
    public void setCommandOutputFactoryResolver(CommandOutputFactoryResolver commandOutputFactoryResolver) {

        LettuceAssert.notNull(commandOutputFactoryResolver, "CommandOutputFactoryResolver must not be null");

        this.commandOutputFactoryResolver = commandOutputFactoryResolver;
    }

    /**
     * Enables/disables command verification which checks the command name against Redis {@code COMMAND} and the argument count.
     *
     * @param verifyCommandMethods {@code true} to enable command verification (default) or {@code false} to disable
     *        command verification.
     */
    public void setVerifyCommandMethods(boolean verifyCommandMethods) {
        this.verifyCommandMethods = verifyCommandMethods;
    }

    /**
     * Returns a Redis Commands interface instance for the given interface.
     *
     * @param commandInterface must not be {@code null}.
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

        factory.addInterceptor(new DefaultMethodInvokingInterceptor());
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

    @SuppressWarnings({ "rawtypes", "unchecked" })
    class CompositeCommandLookupStrategy implements ExecutableCommandLookupStrategy {

        private final AsyncExecutableCommandLookupStrategy async;

        private final ReactiveExecutableCommandLookupStrategy reactive;

        CompositeCommandLookupStrategy() {

            CommandMethodVerifier verifier = verifyCommandMethods ? commandMethodVerifier : CommandMethodVerifier.NONE;

            AbstractRedisReactiveCommands reactive = getReactiveCommands();

            LettuceAssert.isTrue(reactive != null, "Reactive commands is null");

            this.async = new AsyncExecutableCommandLookupStrategy(redisCodecs, commandOutputFactoryResolver, verifier,
                    (StatefulConnection) connection);

            this.reactive = new ReactiveExecutableCommandLookupStrategy(redisCodecs, commandOutputFactoryResolver, verifier,
                    reactive);
        }

        private AbstractRedisReactiveCommands getReactiveCommands() {

            Object reactive = null;

            if (connection instanceof StatefulRedisConnection) {
                reactive = ((StatefulRedisConnection) connection).reactive();
            }

            if (connection instanceof StatefulRedisClusterConnection) {
                reactive = ((StatefulRedisClusterConnection) connection).reactive();
            }

            if (reactive != null && Proxy.isProxyClass(reactive.getClass())) {

                InvocationHandler invocationHandler = Proxy.getInvocationHandler(reactive);
                reactive = ConnectionWrapping.unwrap(invocationHandler);
            }

            return (AbstractRedisReactiveCommands) reactive;
        }

        @Override
        public ExecutableCommand resolveCommandMethod(CommandMethod method, RedisCommandsMetadata metadata) {

            if (method.isReactiveExecution()) {
                return reactive.resolveCommandMethod(method, metadata);
            }

            return async.resolveCommandMethod(method, metadata);
        }

    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    class BatchAwareCommandLookupStrategy implements ExecutableCommandLookupStrategy {

        private final ExecutableCommandLookupStrategy fallbackStrategy;

        private final boolean globalBatching;

        private final CommandMethodVerifier verifier;

        private final long batchSize;

        private Batcher batcher = Batcher.NONE;

        private BatchExecutableCommandLookupStrategy batchingStrategy;

        public BatchAwareCommandLookupStrategy(ExecutableCommandLookupStrategy fallbackStrategy,
                RedisCommandsMetadata metadata) {

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
