package io.lettuce.core.failover;

import java.io.Closeable;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import io.lettuce.core.AbstractRedisClient;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisAsyncCommandsImpl;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisConnectionStateListener;
import io.lettuce.core.RedisReactiveCommandsImpl;
import io.lettuce.core.RedisURI;
import io.lettuce.core.annotations.Experimental;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.push.PushListener;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.api.sync.RedisClusterCommands;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.failover.api.RedisNoHealthyDatabaseException;
import io.lettuce.core.failover.api.StatefulRedisMultiDbConnection;
import io.lettuce.core.failover.event.DatabaseSwitchEvent;
import io.lettuce.core.failover.event.SwitchReason;
import io.lettuce.core.failover.health.HealthStatus;
import io.lettuce.core.failover.health.HealthStatusChangeEvent;
import io.lettuce.core.failover.health.HealthStatusManager;
import io.lettuce.core.internal.AbstractInvocationHandler;
import io.lettuce.core.internal.Exceptions;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.RedisCommand;
import io.lettuce.core.resource.ClientResources;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Stateful connection wrapper that holds multiple underlying connections and delegates to the currently active one. Command
 * interfaces (sync/async/reactive) are dynamic proxies that always target the current active connection at invocation time so
 * they remain valid across switches.
 *
 * @author Ali Takavci
 * @since 7.4
 */
@Experimental
class StatefulRedisMultiDbConnectionImpl<C extends StatefulRedisConnection<K, V>, K, V>
        implements StatefulRedisMultiDbConnection<K, V> {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(StatefulRedisMultiDbConnectionImpl.class);

    protected final Map<RedisURI, RedisDatabaseImpl<C>> databases;

    protected final HealthStatusManager healthStatusManager;

    // this should not be null ever after successful initialization
    protected volatile RedisDatabaseImpl<C> current;

    protected final RedisCommands<K, V> sync;

    protected final RedisAsyncCommandsImpl<K, V> async;

    protected final RedisReactiveCommandsImpl<K, V> reactive;

    protected final RedisCodec<K, V> codec;

    protected final Set<PushListener> pushListeners = ConcurrentHashMap.newKeySet();

    protected final Set<RedisConnectionStateListener> connectionStateListeners = ConcurrentHashMap.newKeySet();

    protected final DatabaseFactory<C, K, V> connectionFactory;

    private final ReadWriteLock multiDbLock = new ReentrantReadWriteLock();

    private final Lock readLock = multiDbLock.readLock();

    private final Lock writeLock = multiDbLock.writeLock();

    private final ClientResources clientResources;

    private final MultiDbOptions multiDbOptions;

    private final RedisDatabaseDeferredCompletion<C> completion;

    private final Set<Consumer<Closeable>> onCloseListeners = ConcurrentHashMap.newKeySet();

    private final ScheduledFuture<?> failbackTask;

    /**
     * Creates a new multi-database connection with default options.
     *
     * @param initialDatabase the initial database, or {@code null} to auto-select.
     * @param connections map of database connections, must not be empty.
     * @param resources the client resources.
     * @param codec the codec for keys and values.
     * @param connectionFactory factory for creating connections, may be {@code null}.
     * @param healthStatusManager the health status manager.
     * @param completion deferred completion handler, may be {@code null}.
     */
    public StatefulRedisMultiDbConnectionImpl(RedisDatabaseImpl<C> initialDatabase,
            Map<RedisURI, RedisDatabaseImpl<C>> connections, ClientResources resources, RedisCodec<K, V> codec,
            DatabaseFactory<C, K, V> connectionFactory, HealthStatusManager healthStatusManager,
            RedisDatabaseDeferredCompletion<C> completion) {
        this(initialDatabase, connections, resources, codec, connectionFactory, healthStatusManager, completion,
                MultiDbOptions.create());
    }

    /**
     * Creates a new multi-database connection with the specified options.
     *
     * @param initialDatabase the initial database, or {@code null} to auto-select.
     * @param connections map of database connections, must not be empty.
     * @param resources the client resources.
     * @param codec the codec for keys and values.
     * @param connectionFactory factory for creating connections, may be {@code null}.
     * @param healthStatusManager the health status manager, must not be {@code null}.
     * @param completion deferred completion handler, may be {@code null}.
     * @param multiDbOptions the multi-database options, must not be {@code null}.
     */
    public StatefulRedisMultiDbConnectionImpl(RedisDatabaseImpl<C> initialDatabase,
            Map<RedisURI, RedisDatabaseImpl<C>> connections, ClientResources resources, RedisCodec<K, V> codec,
            DatabaseFactory<C, K, V> connectionFactory, HealthStatusManager healthStatusManager,
            RedisDatabaseDeferredCompletion<C> completion, MultiDbOptions multiDbOptions) {

        if (connections == null || connections.isEmpty()) {
            throw new IllegalArgumentException("connections must not be empty");
        }
        LettuceAssert.notNull(healthStatusManager, "healthStatusManager must not be null");
        LettuceAssert.notNull(multiDbOptions, "multiDbOptions must not be null");

        this.databases = new ConcurrentHashMap<>(connections);

        this.clientResources = resources;
        this.codec = codec;
        this.connectionFactory = connectionFactory;
        this.healthStatusManager = healthStatusManager;
        this.multiDbOptions = multiDbOptions;

        // Set current BEFORE registering listeners to avoid race condition where
        // onHealthStatusChange or onCircuitBreakerStateChange are triggered before current is set
        if (initialDatabase == null) {
            this.current = getNextHealthyDatabase(null);
        } else {
            this.current = initialDatabase;
        }

        LettuceAssert.notNull(current, "InitialDatabase must not be null");

        // Now register listeners - they can safely access current
        databases.values().forEach(db -> db.getCircuitBreaker().addListener(this::onCircuitBreakerStateChange));
        databases.values().forEach(db -> healthStatusManager.registerListener(db.getRedisURI(), this::onHealthStatusChange));

        // Re-validate that current is still healthy after registering listeners
        // This handles the case where the initial database became unhealthy between selection and listener registration
        RedisDatabaseImpl<C> instance = current;
        if (!instance.isHealthy()) {
            failoverFrom(instance, SwitchReason.FORCED);
            // if still unhealthy after failoverFrom, lets stop here
            if (!current.isHealthy()) {
                // remove listeners as we are going to throw an exception
                databases.values().forEach(db -> db.getCircuitBreaker().removeListener(this::onCircuitBreakerStateChange));
                databases.values()
                        .forEach(db -> healthStatusManager.unregisterListener(db.getRedisURI(), this::onHealthStatusChange));
                throw new IllegalStateException("No healthy database available");
            }
        }

        this.async = newRedisAsyncCommandsImpl();
        this.sync = newRedisSyncCommandsImpl();
        this.reactive = newRedisReactiveCommandsImpl();
        this.completion = completion;
        if (completion != null) {
            completion.whenComplete(this::onDatabaseCompletion);
        }

        // Start periodic failback checker
        if (multiDbOptions.isFailbackSupported()) {
            Duration failbackInterval = multiDbOptions.getFailbackCheckInterval();
            this.failbackTask = resources.eventExecutorGroup().scheduleAtFixedRate(this::periodicFailbackCheck,
                    failbackInterval.toMillis(), failbackInterval.toMillis(), TimeUnit.MILLISECONDS);
        } else {
            this.failbackTask = null;
        }

    }

    private void onDatabaseCompletion(RedisDatabaseImpl<C> db, Throwable e) {
        if (db != null) {
            doByExclusiveLock(() -> {
                databases.putIfAbsent(db.getRedisURI(), db);
            });
            logger.info("Async database connection completed successfully for {}", db.getRedisURI());
        } else if (e != null) {
            logger.error("Async database connection failed: {}", e.getMessage(), e);
        }
    }

    private void onCircuitBreakerStateChange(CircuitBreakerStateChangeEvent event) {
        if (logger.isInfoEnabled()) {
            logger.info("Circuit breaker id {} status changed from {} to {}", event.getCircuitBreaker().getId(),
                    event.getPreviousState(), event.getNewState());
        }

        if (logger.isDebugEnabled()) {
            RedisDatabaseImpl<C> database = databases.values().stream()
                    .filter(db -> db.getCircuitBreaker() == event.getCircuitBreaker()).findAny().orElse(null);
            if (database != null) {
                logger.debug(
                        "Circuit breaker {} running for {} changed state from {} to {}\nCurrent database at the moment is {}",
                        event.getCircuitBreaker().getId(), database.getId(), event.getPreviousState(), event.getNewState(),
                        current.getId());
            }
        }
        RedisDatabaseImpl<C> fromDb = current;
        if (!event.getNewState().isClosed() && event.getCircuitBreaker() == fromDb.getCircuitBreaker()) {
            if (logger.isInfoEnabled()) {
                logger.info("Circuit breaker {} running for {} changed state from {} to {}", event.getCircuitBreaker().getId(),
                        fromDb.getId(), event.getPreviousState(), event.getNewState());
            }
            // Start grace period for the database we're failing over from
            fromDb.startGracePeriod(multiDbOptions.getGracePeriod());
            failoverFrom(fromDb, SwitchReason.CIRCUIT_BREAKER);
        }
    }

    private void onHealthStatusChange(HealthStatusChangeEvent event) {
        if (logger.isInfoEnabled()) {
            logger.info("Health status changed for {} from {} to {}", event.getEndpoint(), event.getOldStatus(),
                    event.getNewStatus());
        }
        RedisDatabaseImpl<C> database = databases.get(event.getEndpoint());

        if (database == null) {
            logger.warn("Health status changed for unknown database: {}", event.getEndpoint());
            return;
        }

        RedisDatabaseImpl<C> fromDb = current;
        if (!event.getNewStatus().isHealthy() && fromDb == database) {
            logger.info("Current database {} became unhealthy, initiating failover", database.getId());
            // Start grace period for the database we're failing over from
            fromDb.startGracePeriod(multiDbOptions.getGracePeriod());
            failoverFrom(fromDb, SwitchReason.HEALTH_CHECK);
        }
    }

    /**
     * Maximum number of failover recursion attempts to prevent infinite loops and stack overflow.
     * <p>
     * This limit covers both:
     * <ul>
     * <li>Retry attempts when a switch operation fails</li>
     * <li>Cascading failovers when the newly selected database becomes unhealthy during the switch</li>
     * </ul>
     * <p>
     * The value of 10 is chosen as a safety net to prevent potential infinite loops in pathological scenarios where all
     * databases are unhealthy or rapidly changing state. In normal operation, failover should succeed within 1-2 attempts.
     */
    private static final int MAX_FAILOVER_RECURSION = 10;

    private void failoverFrom(RedisDatabaseImpl<C> fromDb, SwitchReason reason) {
        failoverFromRecursive(fromDb, reason, 0);
    }

    private void failoverFromRecursive(RedisDatabaseImpl<C> fromDb, SwitchReason reason, int recursionAttempt) {
        if (MAX_FAILOVER_RECURSION <= recursionAttempt++) {
            logger.warn("Max failover attempts ({}) reached, staying on current database {}", MAX_FAILOVER_RECURSION,
                    current.getId());
            return;
        }

        RedisDatabaseImpl<C> selectedDatabase = getNextHealthyDatabase(fromDb);

        if (selectedDatabase != null) {
            if (logger.isInfoEnabled()) {
                logger.info("Initiating failover from {} to {} (attempt: {})", fromDb.getId(), selectedDatabase.getId(),
                        recursionAttempt);
            }
            if (safeSwitch(selectedDatabase, true, reason)) {
                if (logger.isInfoEnabled()) {
                    logger.info("Failover successful from {} to {}", fromDb.getId(), selectedDatabase.getId());
                }
                // check if we missed any events during the switch
                if (!selectedDatabase.isHealthy()) {
                    logger.warn("Database {} became unhealthy during failover, attempting cascading failover",
                            selectedDatabase.getId());
                    failoverFromRecursive(selectedDatabase, reason, recursionAttempt);
                }
            } else {
                if (logger.isInfoEnabled()) {
                    logger.info("Failover attempt from {} to {} has failed, retrying...", fromDb.getId(),
                            selectedDatabase.getId());
                }
                failoverFromRecursive(fromDb, reason, recursionAttempt);
            }
        } else {
            // No healthy database found, stay on the current one
            // TODO: manage max attempts to failover, to throw some proper exception to notify
            // user that failovers are not recovering
            logger.info("No healthy database found, staying on current database {}", fromDb.getId());
        }
    }

    private RedisDatabaseImpl<C> getNextHealthyDatabase(RedisDatabaseImpl<C> dbToExclude) {
        return databases.values().stream().filter(RedisDatabaseImpl::isHealthy).filter(DatabasePredicates.isNot(dbToExclude))
                .max(DatabaseComparators.byWeight).orElse(null);
    }

    /**
     * Periodically checks if a higher-priority database has become healthy and performs failback if configured.
     * <p>
     * This method is called by the failback scheduler at regular intervals. It checks if there is a higher-weighted healthy
     * database available than the current one, and if so, initiates a failback to that database.
     * <p>
     * Additionally, this method checks for databases whose circuit breaker is not closed but whose grace period has ended. For
     * such databases, it resets the circuit breaker to closed state and attempts a failover if appropriate.
     */
    private void periodicFailbackCheck() {
        try {
            RedisDatabaseImpl<C> currentDb = current;
            if (currentDb == null) {
                return;
            }

            // Find the highest-weighted healthy database
            RedisDatabaseImpl<C> highestWeightedHealthy = databases.values().stream().filter(RedisDatabaseImpl::isHealthy)
                    .max(DatabaseComparators.byWeight).orElse(null);

            // If we found a healthy database with higher weight than current, failback to it
            if (highestWeightedHealthy != null && highestWeightedHealthy != currentDb
                    && highestWeightedHealthy.getWeight() > currentDb.getWeight()) {
                logger.info(
                        "Failback check: Found higher-priority healthy database {} (weight: {}) than current {} (weight: {})",
                        highestWeightedHealthy.getId(), highestWeightedHealthy.getWeight(), currentDb.getId(),
                        currentDb.getWeight());
                failoverFrom(currentDb, SwitchReason.FAILBACK);
            }
        } catch (Exception e) {
            logger.error("Error during periodic failback check", e);
        }
    }

    static class DatabaseComparators {

        public static final Comparator<RedisDatabaseImpl<?>> byWeight = Comparator
                .comparingDouble(RedisDatabaseImpl::getWeight);

    }

    static class DatabasePredicates {

        public static Predicate<RedisDatabaseImpl<?>> isNot(RedisDatabaseImpl<?> dbInstance) {
            return db -> !db.equals(dbInstance);
        }

    }

    /**
     * Returns the asynchronous API. The API is a dynamic proxy that remains valid across database switches.
     *
     * @return the asynchronous commands API.
     */
    @Override
    public RedisAsyncCommands<K, V> async() {
        return async;
    }

    /**
     * Create a new instance of {@link RedisCommands}. Can be overridden to extend.
     *
     * @return a new instance
     */
    protected RedisCommands<K, V> newRedisSyncCommandsImpl() {
        return syncHandler(async(), RedisCommands.class, RedisClusterCommands.class);
    }

    /**
     * Create a synchronous command handler proxy for the given async API.
     * <p>
     * This method creates a dynamic proxy that wraps the async API and provides synchronous command execution by blocking on
     * futures.
     *
     * @param asyncApi the async API to wrap
     * @param interfaces the interfaces to implement
     * @param <T> the type of the proxy
     * @return a synchronous command handler proxy
     */
    @SuppressWarnings("unchecked")
    protected <T> T syncHandler(Object asyncApi, Class<?>... interfaces) {
        AbstractInvocationHandler h = new MultiDbFutureSyncInvocationHandler(this, asyncApi, interfaces);
        return (T) Proxy.newProxyInstance(AbstractRedisClient.class.getClassLoader(), interfaces, h);
    }

    /**
     * Create a new instance of {@link RedisAsyncCommandsImpl}. Can be overridden to extend.
     *
     * @return a new instance
     */
    protected RedisAsyncCommandsImpl<K, V> newRedisAsyncCommandsImpl() {
        return new RedisAsyncCommandsImpl<>(this, codec, () -> this.getOptions().getJsonParser().get());
    }

    /**
     * Returns the reactive API. The API is a dynamic proxy that remains valid across database switches.
     *
     * @return the reactive commands API.
     */
    @Override
    public RedisReactiveCommands<K, V> reactive() {
        return reactive;
    }

    /**
     * Create a new instance of {@link RedisReactiveCommandsImpl}. Can be overridden to extend.
     *
     * @return a new instance
     */
    protected RedisReactiveCommandsImpl<K, V> newRedisReactiveCommandsImpl() {
        return new RedisReactiveCommandsImpl<>(this, codec, () -> this.getOptions().getJsonParser().get());
    }

    /**
     * Returns the synchronous API. The API is a dynamic proxy that remains valid across database switches.
     *
     * @return the synchronous commands API.
     */
    @Override
    public RedisCommands<K, V> sync() {
        return sync;
    }

    /**
     * Add a connection state listener. Listeners are migrated during database switches.
     *
     * @param listener must not be {@code null}.
     */
    @Override
    public void addListener(RedisConnectionStateListener listener) {
        doBySharedLock(() -> {
            connectionStateListeners.add(listener);
            current.getConnection().addListener(listener);
        });
    }

    /**
     * Remove a connection state listener.
     *
     * @param listener must not be {@code null}.
     */
    @Override
    public void removeListener(RedisConnectionStateListener listener) {
        doBySharedLock(() -> {
            connectionStateListeners.remove(listener);
            current.getConnection().removeListener(listener);
        });
    }

    /**
     * Set the command timeout for all database connections.
     *
     * @param timeout the timeout, must not be {@code null}.
     */
    @Override
    public void setTimeout(Duration timeout) {
        databases.values().forEach(db -> db.getConnection().setTimeout(timeout));
    }

    /**
     * Returns the command timeout.
     *
     * @return the timeout.
     */
    @Override
    public Duration getTimeout() {
        return current.getConnection().getTimeout();
    }

    /**
     * Dispatch a command to the current database.
     *
     * @param command the command, must not be {@code null}.
     * @param <T> the result type.
     * @return the dispatched command.
     */
    @Override
    public <T> RedisCommand<K, V, T> dispatch(RedisCommand<K, V, T> command) {
        if (!current.isHealthy() && hasNoHealthyDb()) {
            command.completeExceptionally(new RedisNoHealthyDatabaseException("No healthy database available!"));
            return command;
        }
        return current.getConnection().dispatch(command);
    }

    /**
     * Dispatch multiple commands to the current database.
     *
     * @param commands the commands, must not be {@code null}.
     * @return the dispatched commands.
     */
    @Override
    public Collection<RedisCommand<K, V, ?>> dispatch(Collection<? extends RedisCommand<K, V, ?>> commands) {
        if (!current.isHealthy() && hasNoHealthyDb()) {
            commands.forEach(
                    c -> c.completeExceptionally(new RedisNoHealthyDatabaseException("No healthy database available!")));
            return (Collection) commands;
        }
        return current.getConnection().dispatch(commands);
    }

    private boolean hasNoHealthyDb() {
        return databases.values().stream().noneMatch(RedisDatabaseImpl::isHealthy);
    }

    /**
     * Close the connection. Blocks until all database connections are closed.
     */
    @Override
    public void close() {
        closeAsync().join();
    }

    /**
     * Register MultiDbConnection as Closeable resource. Internal access only.
     *
     * @param registry registry of closeables
     */
    protected void registerAsCloseable(final Collection<Closeable> registry) {
        registry.add(this);
        onCloseListeners.add(resource -> {
            registry.remove(resource);
        });
    }

    /**
     * Close the connection asynchronously. Closes all database connections and cleans up resources.
     *
     * @return a {@link CompletableFuture} notified when the operation completes.
     */
    @Override
    public CompletableFuture<Void> closeAsync() {
        if (failbackTask != null) {
            failbackTask.cancel(false);
        }
        Stream<CompletableFuture<Void>> asyncCloseStream = databases.values().stream().map(RedisDatabaseImpl<C>::closeAsync);

        CompletableFuture<Void> closeAllFuture = CompletableFuture.allOf(asyncCloseStream.toArray(CompletableFuture[]::new));

        CompletableFuture<Void> deferredsFuture;
        if (completion != null) {
            deferredsFuture = completion.closeAsync();
        } else {
            deferredsFuture = CompletableFuture.completedFuture(null);
        }

        return closeAllFuture.whenComplete((v, t) -> {
            healthStatusManager.close();
        }).thenCompose(v -> deferredsFuture).whenComplete((v, t) -> {
            onCloseListeners.forEach(c -> c.accept(this));
        });
    }

    /**
     * Returns whether the connection is open.
     *
     * @return {@code true} if open.
     */
    @Override
    public boolean isOpen() {
        return current.getConnection().isOpen();
    }

    /**
     * Returns the client options.
     *
     * @return the options.
     */
    @Override
    public ClientOptions getOptions() {
        return current.getConnection().getOptions();
    }

    /**
     * Returns the client resources.
     *
     * @return the resources.
     */
    @Override
    public ClientResources getResources() {
        return clientResources;
    }

    /**
     * Enable or disable auto-flush for all database connections. Default is {@code true}.
     *
     * @param autoFlush the auto-flush state.
     */
    @Override
    public void setAutoFlushCommands(boolean autoFlush) {
        databases.values().forEach(db -> db.getConnection().setAutoFlushCommands(autoFlush));
    }

    /**
     * Flush outstanding commands on the current database.
     */
    @Override
    public void flushCommands() {
        current.getConnection().flushCommands();
    }

    /**
     * Returns whether the connection is in a transaction (MULTI).
     *
     * @return {@code true} if in a transaction.
     */
    @Override
    public boolean isMulti() {
        return current.getConnection().isMulti();
    }

    /**
     * Add a push listener. Listeners are migrated during database switches.
     *
     * @param listener must not be {@code null}.
     */
    @Override
    public void addListener(PushListener listener) {
        doBySharedLock(() -> {
            pushListeners.add(listener);
            current.getConnection().addListener(listener);
        });
    }

    /**
     * Remove a push listener.
     *
     * @param listener must not be {@code null}.
     */
    @Override
    public void removeListener(PushListener listener) {
        doBySharedLock(() -> {
            pushListeners.remove(listener);
            current.getConnection().removeListener(listener);
        });
    }

    /**
     * Returns the codec.
     *
     * @return the codec.
     */
    @Override
    public RedisCodec<K, V> getCodec() {
        return codec;
    }

    /**
     * Returns the current database endpoint.
     *
     * @return the current endpoint URI.
     */
    @Override
    public RedisURI getCurrentEndpoint() {
        return current.getRedisURI();
    }

    /**
     * Returns all configured database endpoints.
     *
     * @return all endpoint URIs.
     */
    @Override
    public Iterable<RedisURI> getEndpoints() {
        return databases.keySet();
    }

    /**
     * Switch to a specific database endpoint.
     *
     * @param redisURI the database URI, must not be {@code null}.
     * @throws IllegalArgumentException if the endpoint is unknown.
     * @throws IllegalStateException if the switch fails or target is unhealthy.
     */
    @Override
    public void switchTo(RedisURI redisURI) {
        RedisDatabaseImpl<C> target = databases.get(redisURI);
        if (target == null) {
            throw new IllegalArgumentException("Unknown endpoint: " + redisURI);
        }

        if (safeSwitch(target, false, SwitchReason.FORCED)) {
            // if target got unhealthy along the way, failover from it
            // For forced switches, ignore grace period when checking health
            if (!target.isHealthyIgnoreGracePeriod()) {
                failoverFrom(target, SwitchReason.FORCED);
                throw new IllegalStateException("Failed to switch to database " + target.getId() + " - target is unhealthy");
            }
        } else {
            // this should never happen in theory; as safe switch either switches or throws for external calls
            throw new IllegalStateException("Failed to switch to database " + target.getId());
        }
    }

    /**
     * Switch to the given database. This method is thread-safe and can be called from multiple threads.
     * <p>
     * This method performs the actual database switch operation under an exclusive lock. It verifies the switch is possible,
     * updates the current database reference, migrates all listeners (connection state and push listeners) from the old
     * database to the new one, and hands over the command queue.
     * <p>
     * Beyond thread-safety, there are special cases handled:
     * <ul>
     * <li>If the requested database is the same as the current one, returns {@code true} without performing any switch</li>
     * <li>If the requested database is unhealthy or circuit breaker is open, the behavior depends on {@code internalCall}</li>
     * <li>If the requested database is a different instance with the same URI, the behavior depends on
     * {@code internalCall}</li>
     * </ul>
     *
     * @param database the database to switch to
     * @param internalCall if {@code true}, validation failures return {@code false} and log errors; if {@code false},
     *        validation failures throw exceptions
     * @return {@code true} if the switch succeeded or the database was already current; {@code false} if validation failed and
     *         {@code internalCall} is {@code true}
     * @throws IllegalStateException if {@code internalCall} is {@code false} and the requested database is a different instance
     *         than registered in connection map but with the same target endpoint/uri, or if the target database is unhealthy
     *         or circuit breaker is open
     * @throws UnsupportedOperationException if {@code internalCall} is {@code false} and the source or destination endpoint
     *         cannot be located in the connection map
     * @throws IllegalArgumentException if {@code database} is {@code null}
     */
    boolean safeSwitch(RedisDatabaseImpl<?> database, boolean internalCall, SwitchReason reason) {
        if (database == null) {
            // this should never happen but in case we ever decide to remove null checks from the caller
            throw new IllegalArgumentException("Target database to switch to can not be null.");
        }
        logger.info("Initiated safe switching to database {}", database.getId());
        SwitchContext switchContext = new SwitchContext();

        doByExclusiveLock(() -> {
            RedisDatabaseImpl<C> fromDb = current;
            RedisDatabaseImpl<C> toDb = databases.get(database.getRedisURI());

            if (!verifySwitch(database, fromDb, toDb, internalCall)) {
                return;
            }

            switchContext.fromUri = fromDb.getRedisURI();
            switchContext.toUri = toDb.getRedisURI();

            switchContext.switched = true;
            if (fromDb == toDb) {
                // Nothing to do, already on the right database
                return;
            }

            current = toDb;
            logger.info("Switched to database {}", toDb.getId());

            connectionStateListeners.forEach(listener -> {
                toDb.getConnection().addListener(listener);
                fromDb.getConnection().removeListener(listener);
                if (logger.isDebugEnabled()) {
                    logger.debug("Moved connection state listener {} from {} to {}", listener, fromDb.getId(), toDb.getId());
                }
            });
            pushListeners.forEach(listener -> {
                toDb.getConnection().addListener(listener);
                fromDb.getConnection().removeListener(listener);
                if (logger.isDebugEnabled()) {
                    logger.debug("Moved push listener {} from {} to {}", listener, fromDb.getId(), toDb.getId());
                }
            });

            fromDb.getDatabaseEndpoint().handOverCommandQueue(toDb.getDatabaseEndpoint());

            doOnSwitch(fromDb, toDb);
        });

        // Publish event outside the lock to avoid holding lock during event processing
        if (switchContext.switched && !switchContext.toUri.equals(switchContext.fromUri)) {
            publishSwitchEvent(reason, switchContext.fromUri, switchContext.toUri);
        }

        return switchContext.switched;
    }

    /**
     * Extension point for subclasses to perform additional operations after a database switch. This method is called within the
     * exclusive lock.
     *
     * @param fromDb the database being switched from
     * @param toDb the database being switched to
     */
    protected void doOnSwitch(RedisDatabaseImpl<C> fromDb, RedisDatabaseImpl<C> toDb) {
        // NOOP
    }

    /**
     * Publishes a {@link DatabaseSwitchEvent} to the event bus. This method is called outside the exclusive lock to avoid
     * holding the lock during event processing.
     *
     * @param reason the reason for the database switch
     * @param fromUri the URI of the database being switched from
     * @param toUri the URI of the database being switched to
     */
    protected void publishSwitchEvent(SwitchReason reason, RedisURI fromUri, RedisURI toUri) {
        clientResources.eventBus()
                .publish(new DatabaseSwitchEvent(reason, new ImmutableRedisURI(fromUri), new ImmutableRedisURI(toUri), this));
    }

    /**
     * Verify if the switch is possible. This method is not thread-safe and should be called within a lock.
     * <p>
     * Performs three validation checks:
     * <ol>
     * <li>Verifies both source and destination databases exist in the connection map</li>
     * <li>Verifies the requested database instance matches the registered instance for the same URI</li>
     * <li>Verifies the target database is healthy and has a closed circuit breaker</li>
     * </ol>
     *
     * @param target the requested database instance to switch to
     * @param fromDb the current database (source of the switch)
     * @param toDb the database instance registered in the connection map for the target URI
     * @param internalCall if {@code true}, validation failures return {@code false} and log messages; if {@code false},
     *        validation failures throw exceptions
     * @return {@code true} if the switch is possible and all validations pass; {@code false} if validation failed and
     *         {@code internalCall} is {@code true}
     * @throws IllegalStateException if {@code internalCall} is {@code false} and either the requested database is a different
     *         instance than registered in the connection map (same URI, different object), or the target database is unhealthy
     *         or has an open circuit breaker
     * @throws UnsupportedOperationException if {@code internalCall} is {@code false} and either the source or destination
     *         database cannot be located in the connection map
     */
    private boolean verifySwitch(RedisDatabaseImpl<?> target, RedisDatabaseImpl<C> fromDb, RedisDatabaseImpl<C> toDb,
            boolean internalCall) {
        if (fromDb == null || toDb == null) {
            if (internalCall) {
                logger.info("Failed to switch to database {} - source or destination endpoint not found", target.getId());
                return false;
            }

            throw new UnsupportedOperationException(
                    "Unable to switch between endpoints - the driver was not able to locate the source or destination endpoint.");
        }

        if (target != toDb) {
            if (internalCall) {
                logger.error(
                        "Same URI with different database, this should never happen in the driver. Requested database: {}, found database: {} , endpoint: {}",
                        target.getId(), toDb.getId(), toDb.getRedisURI());
                return false;
            }

            throw new IllegalStateException(
                    "Same URI with different database, this should never happen in the driver. Requested database: "
                            + target.getId() + ", found database: " + toDb.getId() + " , endpoint: " + toDb.getRedisURI());
        }

        // For internal calls (automatic failover/failback), check grace period
        // For external calls (forced switches), ignore grace period
        boolean canSwitchTo = internalCall ? toDb.isHealthy() : toDb.isHealthyIgnoreGracePeriod();

        if (!canSwitchTo) {
            if (internalCall) {
                logger.info("Requested database ({}) is unhealthy or circuit breaker is open. Skipping switch request.",
                        toDb.getId());
                return false;
            }
            throw new IllegalStateException("Requested database (" + toDb.getId()
                    + ") is unhealthy or circuit breaker is open. Skipping switch request.");
        }

        return true;
    }

    /**
     * Execute an operation under a shared (read) lock.
     * <p>
     * This method acquires the read lock before executing the operation and releases it afterwards. Multiple threads can hold
     * the read lock simultaneously as long as no thread holds the write lock.
     *
     * @param operation the operation to execute
     */
    protected void doBySharedLock(Runnable operation) {
        readLock.lock();
        try {
            operation.run();
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Execute an operation under an exclusive (write) lock.
     * <p>
     * This method acquires the write lock before executing the operation and releases it afterwards. Only one thread can hold
     * the write lock at a time, and no other threads can hold the read lock while the write lock is held.
     *
     * @param operation the operation to execute
     */
    protected void doByExclusiveLock(Runnable operation) {
        writeLock.lock();
        try {
            operation.run();
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Returns the current database.
     *
     * @return the current database.
     */
    @Override
    public RedisDatabaseImpl<C> getCurrentDatabase() {
        return current;
    }

    /**
     * Returns the database for the specified endpoint.
     *
     * @param redisURI the database URI, must not be {@code null}.
     * @return the database.
     * @throws IllegalArgumentException if the endpoint is unknown.
     */
    @Override
    public RedisDatabaseImpl<C> getDatabase(RedisURI redisURI) {
        RedisDatabaseImpl<C> database = databases.get(redisURI);
        if (database == null) {
            throw new IllegalArgumentException("Unknown endpoint: " + redisURI);
        }
        return database;
    }

    /**
     * Checks whether the database endpoint is healthy.
     *
     * @param endpoint the database URI, must not be {@code null}.
     * @return {@code true} if healthy.
     * @throws IllegalArgumentException if the endpoint is unknown.
     */
    @Override
    public boolean isHealthy(RedisURI endpoint) {
        RedisDatabaseImpl<C> database = databases.get(endpoint);
        if (database == null) {
            throw new IllegalArgumentException("Unknown endpoint: " + endpoint);
        }
        return database.isHealthy();
    }

    /**
     * Add a database endpoint with the specified weight.
     *
     * @param redisURI the database URI, must not be {@code null}.
     * @param weight the failover weight (higher = higher priority).
     * @throws UnsupportedOperationException if created without a DatabaseFactory.
     * @throws IllegalArgumentException if the database already exists.
     */
    @Override
    public void addDatabase(RedisURI redisURI, float weight) {
        addDatabase(DatabaseConfig.builder(redisURI).weight(weight).build());
    }

    /**
     * Add a database endpoint with the specified configuration.
     *
     * @param databaseConfig the database configuration, must not be {@code null}.
     * @throws UnsupportedOperationException if created without a DatabaseFactory.
     * @throws IllegalArgumentException if the database already exists.
     * @throws RedisConnectionException if connection fails.
     */
    @Override
    public void addDatabase(DatabaseConfig databaseConfig) {
        LettuceAssert.notNull(databaseConfig, "DatabaseConfig must not be null");

        if (connectionFactory == null) {
            throw new UnsupportedOperationException(
                    "Adding databases dynamically is not supported. Connection was created without a DatabaseFactory.");
        }

        RedisURI redisURI = databaseConfig.getRedisURI();

        doByExclusiveLock(() -> {
            if (databases.containsKey(redisURI)) {
                throw new IllegalArgumentException("Database already exists: " + redisURI);
            }

            healthStatusManager.registerListener(redisURI, this::onHealthStatusChange);

            // Create new database connection using the factory
            CompletableFuture<RedisDatabaseImpl<C>> databaseFuture = connectionFactory.createDatabaseAsync(databaseConfig,
                    healthStatusManager);
            try {
                RedisDatabaseImpl<C> database = databaseFuture.get();
                databases.put(redisURI, database);

                // Add listeners to the new connection if it's the current one
                // (though it won't be current initially since we're just adding it)
                database.getCircuitBreaker().addListener(this::onCircuitBreakerStateChange);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw RedisConnectionException.create(e);
            } catch (Exception e) {
                throw RedisConnectionException.create(Exceptions.unwrap(e));
            }
        });

    }

    /**
     * Remove a database endpoint.
     *
     * @param redisURI the database URI, must not be {@code null}.
     * @throws IllegalArgumentException if the database is not found.
     * @throws UnsupportedOperationException if removing the current database.
     */
    @Override
    public void removeDatabase(RedisURI redisURI) {
        LettuceAssert.notNull(redisURI, "RedisURI must not be null");

        doByExclusiveLock(() -> {
            RedisDatabaseImpl<C> database = null;
            database = databases.get(redisURI);
            if (database == null) {
                throw new IllegalArgumentException("Database not found: " + redisURI);
            }

            if (current.getRedisURI().equals(redisURI)) {
                throw new UnsupportedOperationException("Cannot remove the currently active database: " + current.getId());
            }

            healthStatusManager.unregisterListener(redisURI, this::onHealthStatusChange);
            healthStatusManager.remove(redisURI);

            // Remove the database and close its connection
            databases.remove(redisURI);
            database.closeAsync().join();
        });
    }

    /**
     * Context object that holds the result of a database switch operation and the URIs involved.
     */
    static class SwitchContext {

        boolean switched;

        RedisURI fromUri;

        RedisURI toUri;

    }

}
