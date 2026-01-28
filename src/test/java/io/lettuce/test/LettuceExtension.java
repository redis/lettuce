package io.lettuce.test;

import java.io.Closeable;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
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
import io.lettuce.core.failover.MultiDbClient;
import io.lettuce.core.failover.MultiDbOptions;
import io.lettuce.core.failover.MultiDbTestSupport;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.test.resource.DefaultRedisClient;
import io.lettuce.test.resource.DefaultRedisClusterClient;
import io.lettuce.test.resource.DefaultRedisMultiDbClient;
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
 * <li>{@link MultiDbClient} (singleton and qualified instances via {@code @NoFailback})</li>
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
 * <h3>Qualified injection for MultiDbClient</h3>
 *
 * Multiple {@link MultiDbClient} instances with different configurations can be injected using qualifier annotations:
 *
 * <pre class="code">
 * &#064;ExtendWith(LettuceExtension.class)
 * public class MultiDbTest {
 *
 *     &#064;Inject
 *     public MultiDbTest(MultiDbClient defaultClient, &#064;NoFailback MultiDbClient noFailbackClient) {
 *         // defaultClient has failback enabled (default)
 *         // noFailbackClient has failback disabled
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
 * @author Ali Takavci
 * @since 5.1.1
 * @see ParameterResolver
 * @see Inject
 * @see New
 * @see NoFailback
 * @see BeforeEachCallback
 * @see AfterEachCallback
 * @see AfterAllCallback
 */
public class LettuceExtension implements ParameterResolver, AfterAllCallback, AfterEachCallback {

    private static final InternalLogger LOGGER = InternalLoggerFactory.getInstance(LettuceExtension.class);

    private final ExtensionContext.Namespace LETTUCE = ExtensionContext.Namespace.create("lettuce.parameters");

    private static final Set<Class<?>> SUPPORTED_INJECTABLE_TYPES = new HashSet<>(Arrays.asList(StatefulRedisConnection.class,
            StatefulRedisPubSubConnection.class, RedisCommands.class, RedisClient.class, ClientResources.class,
            StatefulRedisClusterConnection.class, RedisClusterClient.class, MultiDbClient.class));

    private static final Set<Class<?>> CLOSE_AFTER_EACH = new HashSet<>(Arrays.asList(StatefulRedisConnection.class,
            StatefulRedisPubSubConnection.class, StatefulRedisClusterConnection.class));

    private static final List<Supplier<?>> SUPPLIERS = Arrays.asList(ClientResourcesSupplier.INSTANCE,
            RedisClusterClientSupplier.INSTANCE, RedisClientSupplier.INSTANCE, StatefulRedisConnectionSupplier.INSTANCE,
            StatefulRedisPubSubConnectionSupplier.INSTANCE, StatefulRedisClusterConnectionSupplier.INSTANCE,
            RedisMultiDbClientSupplier.INSTANCE);

    private static final List<Function<?, ?>> RESOURCE_FUNCTIONS = Collections.singletonList(RedisCommandsFunction.INSTANCE);

    /**
     * Registry mapping qualifier annotation types to their corresponding suppliers. This allows for extensible qualifier
     * support without hardcoding qualifier logic in the core resolution method.
     */
    private static final Map<Class<? extends Annotation>, QualifiedSupplier<?>> QUALIFIED_SUPPLIERS = new HashMap<>();

    static {
        // Register qualified suppliers
        QUALIFIED_SUPPLIERS.put(NoFailback.class,
                new QualifiedSupplier<>(MultiDbClient.class, RedisMultiDbClientNoFailbackSupplier.INSTANCE));
    }

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

        // Determine qualifier for the parameter
        String qualifier = getQualifier(parameterContext);

        if (parameterContext.isAnnotated(New.class)) {

            Object instance = doGetInstance(parameterizedType, qualifier);

            if (instance instanceof Closeable || instance instanceof AutoCloseable) {

                CloseAfterTest closeables = store.getOrComputeIfAbsent(CloseAfterTest.class, it -> new CloseAfterTest(),
                        CloseAfterTest.class);

                closeables.add(instance);
            }

            return instance;
        }

        // Use qualified key for storage to support multiple instances of the same type
        String storeKey = parameter.getType().getName() + qualifier;
        return store.getOrComputeIfAbsent(storeKey, it -> doGetInstance(parameterizedType, qualifier));
    }

    /**
     * Determines the qualifier for a parameter based on its annotations. Searches the registered qualifier annotations and
     * returns a qualifier string if found.
     *
     * @param parameterContext the parameter context
     * @return the qualifier string (e.g., ":NoFailback"), or empty string if no qualifier
     */
    private String getQualifier(ParameterContext parameterContext) {
        // Check all registered qualifier annotations
        for (Class<? extends Annotation> qualifierType : QUALIFIED_SUPPLIERS.keySet()) {
            if (parameterContext.isAnnotated(qualifierType)) {
                return ":" + qualifierType.getSimpleName();
            }
        }
        return "";
    }

    /**
     * Gets an instance of the requested type, optionally using a qualified supplier if a qualifier is provided.
     *
     * @param parameterizedType the requested type
     * @param qualifier the qualifier string (empty for default, or e.g., ":NoFailback" for qualified)
     * @return the instance
     */
    private Object doGetInstance(Type parameterizedType, String qualifier) {

        // Check if we have a qualified supplier for this qualifier
        if (!qualifier.isEmpty()) {
            Optional<QualifiedSupplier<?>> qualifiedSupplier = findQualifiedSupplier(parameterizedType, qualifier);
            if (qualifiedSupplier.isPresent()) {
                return qualifiedSupplier.get().supplier.get();
            }
        }

        // Default behavior for non-qualified or other types
        Optional<ResourceFunction> resourceFunction = findFunction(parameterizedType);
        return resourceFunction.map(it -> it.function.apply(findSupplier(it.dependsOn.getType()).get()))
                .orElseGet(() -> findSupplier(parameterizedType).get());
    }

    /**
     * Finds a qualified supplier that matches the given type and qualifier.
     *
     * @param parameterizedType the requested type
     * @param qualifier the qualifier string (e.g., ":NoFailback")
     * @return the qualified supplier if found
     */
    @SuppressWarnings("unchecked")
    private Optional<QualifiedSupplier<?>> findQualifiedSupplier(Type parameterizedType, String qualifier) {
        return (Optional<QualifiedSupplier<?>>) (Optional<?>) QUALIFIED_SUPPLIERS.entrySet().stream()
                .filter(entry -> qualifier.equals(":" + entry.getKey().getSimpleName())).map(Map.Entry::getValue)
                .filter(qualifiedSupplier -> qualifiedSupplier.canProvide(parameterizedType)).findFirst();
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

        // Clean up all qualified instances
        QUALIFIED_SUPPLIERS.forEach((qualifierType, qualifiedSupplier) -> {
            String qualifierKey = qualifiedSupplier.targetType.getName() + ":" + qualifierType.getSimpleName();
            Object instance = store.get(qualifierKey, qualifiedSupplier.targetType);
            if (instance != null) {
                if (instance instanceof AutoCloseable) {
                    try {
                        ((AutoCloseable) instance).close();
                    } catch (Exception e) {
                        LOGGER.warn("Failed to close qualified instance: " + qualifierKey, e);
                    }
                }
                store.remove(qualifierKey);
            }
        });
    }

    @Override
    public void afterEach(ExtensionContext context) {

        DefaultRedisClient.get().setOptions(ClientOptions.builder().build());

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

    /**
     * Holder for a qualified supplier that associates a target type with its supplier. This allows the extension to dynamically
     * resolve qualified instances based on the parameter type and qualifier annotation.
     *
     * @param <T> the type of object this supplier provides
     */
    static class QualifiedSupplier<T> {

        final Class<T> targetType;

        final Supplier<T> supplier;

        public QualifiedSupplier(Class<T> targetType, Supplier<T> supplier) {
            this.targetType = targetType;
            this.supplier = supplier;
        }

        /**
         * Checks if this qualified supplier can provide an instance for the given type.
         *
         * @param requestedType the requested type
         * @return true if this supplier can provide the requested type
         */
        public boolean canProvide(Type requestedType) {
            ResolvableType requested = ResolvableType.forType(requestedType);
            return requested.isAssignableFrom(ResolvableType.forClass(targetType));
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

    enum RedisMultiDbClientSupplier implements Supplier<MultiDbClient> {

        INSTANCE;

        @Override
        public MultiDbClient get() {
            return DefaultRedisMultiDbClient.get();
        }

    }

    enum RedisMultiDbClientNoFailbackSupplier implements Supplier<MultiDbClient> {

        INSTANCE;

        @Override
        public MultiDbClient get() {
            MultiDbOptions options = MultiDbOptions.builder().failbackSupported(false).build();
            return MultiDbClient.create(MultiDbTestSupport.DBs, options);
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
