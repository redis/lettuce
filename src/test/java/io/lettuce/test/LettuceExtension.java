/*
 * Copyright 2018-2020 the original author or authors.
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
package io.lettuce.test;

import java.io.Closeable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.enterprise.inject.New;
import javax.inject.Inject;

import org.junit.jupiter.api.extension.*;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.dynamic.support.ResolvableType;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.test.resource.DefaultRedisClient;
import io.lettuce.test.resource.DefaultRedisClusterClient;
import io.lettuce.test.resource.TestClientResources;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * JUnit 5 {@link Extension} providing parameter resolution for connection resources and that reacts to callbacks.
 *
 * The following resource types are supported by this extension:
 * <ul>
 * <li>{@link ClientResources} (singleton)</li>
 * <li>{@link RedisClient} (singleton)</li>
 * <li>{@link RedisClusterClient} (singleton)</li>
 * <li>{@link StatefulRedisConnection} (singleton and dedicated instances via {@code @New})</li>
 * <li>{@link StatefulRedisPubSubConnection} (singleton and dedicated instances via {@code @New})</li>
 * <li>{@link StatefulRedisClusterConnection} (singleton and dedicated instances via {@code @New})</li>
 * </ul>
 *
 * Tests that want to use this extension need to annotate injection points with {@code @Inject}:
 *
 * <pre class="code">
 * &#064;ExtendWith(LettuceExtension.class)
 * public class CustomCommandTest {
 *
 *     private final RedisCommands&lt;String, String&gt; redis;
 *
 *     &#064;Inject
 *     public CustomCommandTest(StatefulRedisConnection&lt;String, String&gt; connection) {
 *         this.redis = connection.sync();
 *     }
 *
 * }
 * </pre>
 *
 * <h3>Resource lifecycle</h3>
 *
 * This extension allocates resources lazily and stores them in its {@link ExtensionContext}
 * {@link org.junit.jupiter.api.extension.ExtensionContext.Store} for reuse across multiple tests. Client and
 * {@link ClientResources} are allocated through{@link DefaultRedisClient} respective {@link TestClientResources} so shutdown is
 * managed by the actual suppliers. Singleton connection resources are closed after the test class (test container) is finished.
 * Newable connection resources are closed after the actual test is finished.
 *
 * <h3>Newable resources</h3> Some tests require a dedicated connection. These can be obtained by annotating the parameter with
 * {@code @New}.
 *
 * @author Mark Paluch
 * @since 5.1.1
 * @see ParameterResolver
 * @see Inject
 * @see New
 * @see BeforeEachCallback
 * @see AfterEachCallback
 * @see AfterAllCallback
 */
public class LettuceExtension implements ParameterResolver, AfterAllCallback, AfterEachCallback {

    private static final InternalLogger LOGGER = InternalLoggerFactory.getInstance(LettuceExtension.class);

    private final ExtensionContext.Namespace LETTUCE = ExtensionContext.Namespace.create("lettuce.parameters");

    private static final Set<Class<?>> SUPPORTED_INJECTABLE_TYPES = new HashSet<>(Arrays.asList(StatefulRedisConnection.class,
            StatefulRedisPubSubConnection.class, RedisCommands.class, RedisClient.class, ClientResources.class,
            StatefulRedisClusterConnection.class, RedisClusterClient.class));

    private static final Set<Class<?>> CLOSE_AFTER_EACH = new HashSet<>(Arrays.asList(StatefulRedisConnection.class,
            StatefulRedisPubSubConnection.class, StatefulRedisClusterConnection.class));

    private static final List<Supplier<?>> SUPPLIERS = Arrays.asList(ClientResourcesSupplier.INSTANCE,
            RedisClusterClientSupplier.INSTANCE, RedisClientSupplier.INSTANCE, StatefulRedisConnectionSupplier.INSTANCE,
            StatefulRedisPubSubConnectionSupplier.INSTANCE, StatefulRedisClusterConnectionSupplier.INSTANCE);

    private static final List<Function<?, ?>> RESOURCE_FUNCTIONS = Collections.singletonList(RedisCommandsFunction.INSTANCE);

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {

        if (SUPPORTED_INJECTABLE_TYPES.contains(parameterContext.getParameter().getType())) {

            if (parameterContext.isAnnotated(Inject.class)
                    || parameterContext.getDeclaringExecutable().isAnnotationPresent(Inject.class)) {
                return true;
            }

            LOGGER.warn("Parameter type " + parameterContext.getParameter().getType()
                    + " supported but injection target is not annotated with @Inject");
        }

        return false;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {

        ExtensionContext.Store store = getStore(extensionContext);
        Parameter parameter = parameterContext.getParameter();
        Type parameterizedType = parameter.getParameterizedType();
        if (parameterContext.isAnnotated(New.class)) {

            Object instance = doGetInstance(parameterizedType);

            if (instance instanceof Closeable || instance instanceof AutoCloseable) {

                CloseAfterTest closeables = store.getOrComputeIfAbsent(CloseAfterTest.class, it -> new CloseAfterTest(),
                        CloseAfterTest.class);

                closeables.add(closeables);
            }

            return instance;
        }

        return store.getOrComputeIfAbsent(parameter.getType(), it -> doGetInstance(parameterizedType));
    }

    private Object doGetInstance(Type parameterizedType) {

        Optional<ResourceFunction> resourceFunction = findFunction(parameterizedType);
        return resourceFunction.map(it -> it.function.apply(findSupplier(it.dependsOn.getType()).get())).orElseGet(
                () -> findSupplier(parameterizedType).get());
    }

    /**
     * Attempt to resolve the {@code requestedResourceType}.
     *
     * @param extensionContext
     * @param requestedResourceType
     * @param <T>
     * @return
     */
    public <T> T resolve(ExtensionContext extensionContext, Class<T> requestedResourceType) {

        ExtensionContext.Store store = getStore(extensionContext);

        return (T) store.getOrComputeIfAbsent(requestedResourceType, it -> findSupplier(requestedResourceType).get());
    }

    private ExtensionContext.Store getStore(ExtensionContext extensionContext) {
        return extensionContext.getStore(LETTUCE);
    }

    @Override
    public void afterAll(ExtensionContext context) {

        ExtensionContext.Store store = getStore(context);

        CLOSE_AFTER_EACH.forEach(it -> {

            StatefulConnection connection = store.get(it, StatefulConnection.class);

            if (connection != null) {
                connection.close();
                store.remove(StatefulRedisConnection.class);
            }
        });
    }

    @Override
    public void afterEach(ExtensionContext context) {

        DefaultRedisClient.get().setOptions(ClientOptions.builder().build());
        DefaultRedisClient.get().setDefaultTimeout(Duration.ofSeconds(60));

        ExtensionContext.Store store = getStore(context);
        CloseAfterTest closeables = store.get(CloseAfterTest.class, CloseAfterTest.class);

        if (closeables != null) {

            List<Object> copy = new ArrayList<>(closeables);

            closeables.clear();

            copy.forEach(it -> {
                try {
                    if (it instanceof Closeable) {
                        ((Closeable) it).close();
                    } else if (it instanceof AutoCloseable) {
                        ((AutoCloseable) it).close();
                    }
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            });
        }
    }

    @SuppressWarnings("unchecked")
    private static Supplier<Object> findSupplier(Type type) {

        ResolvableType requested = ResolvableType.forType(type);

        Supplier<?> supplier = SUPPLIERS.stream().filter(it -> {

            ResolvableType providedType = ResolvableType.forType(it.getClass()).as(Supplier.class).getGeneric(0);

            if (requested.isAssignableFrom(providedType)) {
                return true;
            }
            return false;
        }).findFirst().orElseThrow(() -> new NoSuchElementException("Cannot find a factory for " + type));

        return (Supplier) supplier;
    }

    private static Optional<ResourceFunction> findFunction(Type type) {

        ResolvableType requested = ResolvableType.forType(type);

        return RESOURCE_FUNCTIONS.stream().map(it -> {

            ResolvableType dependsOn = ResolvableType.forType(it.getClass()).as(Function.class).getGeneric(0);
            ResolvableType providedType = ResolvableType.forType(it.getClass()).as(Function.class).getGeneric(1);

            return new ResourceFunction(dependsOn, providedType, it);
        }).filter(it -> requested.isAssignableFrom(it.provides)).findFirst();
    }

    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Connection {
        boolean requiresNew() default false;
    }

    static class CloseAfterTest extends ArrayList<Object> {
    }

    static class ResourceFunction {

        final ResolvableType dependsOn;
        final ResolvableType provides;
        final Function<Object, Object> function;

        public ResourceFunction(ResolvableType dependsOn, ResolvableType provides, Function<?, ?> function) {
            this.dependsOn = dependsOn;
            this.provides = provides;
            this.function = (Function) function;
        }
    }

    enum ClientResourcesSupplier implements Supplier<ClientResources> {

        INSTANCE;

        @Override
        public ClientResources get() {
            return TestClientResources.get();
        }
    }

    enum RedisClientSupplier implements Supplier<RedisClient> {

        INSTANCE;

        @Override
        public RedisClient get() {
            return DefaultRedisClient.get();
        }
    }

    enum RedisClusterClientSupplier implements Supplier<RedisClusterClient> {

        INSTANCE;

        @Override
        public RedisClusterClient get() {
            return DefaultRedisClusterClient.get();
        }
    }

    enum StatefulRedisConnectionSupplier implements Supplier<StatefulRedisConnection<String, String>> {

        INSTANCE;

        @Override
        public StatefulRedisConnection<String, String> get() {
            return RedisClientSupplier.INSTANCE.get().connect();
        }
    }

    enum StatefulRedisPubSubConnectionSupplier implements Supplier<StatefulRedisPubSubConnection<String, String>> {

        INSTANCE;

        @Override
        public StatefulRedisPubSubConnection<String, String> get() {
            return RedisClientSupplier.INSTANCE.get().connectPubSub();
        }
    }

    enum StatefulRedisClusterConnectionSupplier implements Supplier<StatefulRedisClusterConnection<String, String>> {

        INSTANCE;

        @Override
        public StatefulRedisClusterConnection<String, String> get() {
            return RedisClusterClientSupplier.INSTANCE.get().connect();
        }
    }

    enum RedisCommandsFunction implements Function<StatefulRedisConnection<String, String>, RedisCommands<String, String>> {
        INSTANCE;

        @Override
        public RedisCommands<String, String> apply(StatefulRedisConnection<String, String> connection) {
            return connection.sync();
        }
    }
}
