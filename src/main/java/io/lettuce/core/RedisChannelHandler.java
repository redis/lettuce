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
package io.lettuce.core;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.internal.AsyncCloseable;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.*;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.tracing.TraceContextProvider;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Abstract base for every Redis connection. Provides basic connection functionality and tracks open resources.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 3.0
 */
public abstract class RedisChannelHandler<K, V> implements Closeable, ConnectionFacade {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(RedisChannelHandler.class);

    @SuppressWarnings("rawtypes")
    private static final AtomicIntegerFieldUpdater<RedisChannelHandler> CLOSED = AtomicIntegerFieldUpdater
            .newUpdater(RedisChannelHandler.class, "closed");

    private static final int ST_OPEN = 0;

    private static final int ST_CLOSED = 1;

    private Duration timeout;

    private CloseEvents closeEvents = new CloseEvents();

    private final RedisChannelWriter channelWriter;

    private final ClientResources clientResources;

    private final boolean tracingEnabled;

    private final boolean debugEnabled = logger.isDebugEnabled();

    private final CompletableFuture<Void> closeFuture = new CompletableFuture<>();

    // accessed via CLOSED
    @SuppressWarnings("unused")
    private volatile int closed = ST_OPEN;

    private volatile boolean active = true;

    private volatile ClientOptions clientOptions;

    /**
     * @param writer the channel writer
     * @param timeout timeout value
     */
    public RedisChannelHandler(RedisChannelWriter writer, Duration timeout) {

        this.channelWriter = writer;
        this.clientResources = writer.getClientResources();
        this.tracingEnabled = clientResources.tracing().isEnabled();

        writer.setConnectionFacade(this);
        setTimeout(timeout);
    }

    /**
     * Set the command timeout for this connection.
     *
     * @param timeout Command timeout.
     * @since 5.0
     */
    public void setTimeout(Duration timeout) {

        LettuceAssert.notNull(timeout, "Timeout duration must not be null");
        LettuceAssert.isTrue(!timeout.isNegative(), "Timeout duration must be greater or equal to zero");

        this.timeout = timeout;

        if (channelWriter instanceof CommandExpiryWriter) {
            ((CommandExpiryWriter) channelWriter).setTimeout(timeout);
        }
    }

    /**
     * Close the connection (synchronous).
     */
    @Override
    public void close() {

        if (debugEnabled) {
            logger.debug("close()");
        }

        closeAsync().join();
    }

    /**
     * Close the connection (asynchronous).
     *
     * @since 5.1
     */
    public CompletableFuture<Void> closeAsync() {

        if (debugEnabled) {
            logger.debug("closeAsync()");
        }

        if (CLOSED.get(this) == ST_CLOSED) {
            logger.warn("Connection is already closed");
            return closeFuture;
        }

        if (CLOSED.compareAndSet(this, ST_OPEN, ST_CLOSED)) {

            active = false;
            CompletableFuture<Void> future = channelWriter.closeAsync();

            future.whenComplete((v, t) -> {

                closeEvents.fireEventClosed(this);
                closeEvents = new CloseEvents();

                if (t != null) {
                    closeFuture.completeExceptionally(t);
                } else {
                    closeFuture.complete(v);
                }
            });
        } else {
            logger.warn("Connection is already closed (concurrently)");
        }

        return closeFuture;
    }

    protected <T> RedisCommand<K, V, T> dispatch(RedisCommand<K, V, T> cmd) {

        if (debugEnabled) {
            logger.debug("dispatching command {}", cmd);
        }

        if (tracingEnabled) {

            RedisCommand<K, V, T> commandToSend = cmd;
            TraceContextProvider provider = CommandWrapper.unwrap(cmd, TraceContextProvider.class);

            if (provider == null) {
                commandToSend = new TracedCommand<>(cmd,
                        clientResources.tracing().initialTraceContextProvider().getTraceContext());
            }

            return channelWriter.write(commandToSend);
        }

        return channelWriter.write(cmd);
    }

    protected Collection<RedisCommand<K, V, ?>> dispatch(Collection<? extends RedisCommand<K, V, ?>> commands) {

        if (debugEnabled) {
            logger.debug("dispatching commands {}", commands);
        }

        if (tracingEnabled) {

            Collection<RedisCommand<K, V, ?>> withTracer = new ArrayList<>(commands.size());

            for (RedisCommand<K, V, ?> command : commands) {

                RedisCommand<K, V, ?> commandToUse = command;
                TraceContextProvider provider = CommandWrapper.unwrap(command, TraceContextProvider.class);
                if (provider == null) {
                    commandToUse = new TracedCommand<>(command,
                            clientResources.tracing().initialTraceContextProvider().getTraceContext());
                }

                withTracer.add(commandToUse);
            }

            return channelWriter.write(withTracer);

        }

        return channelWriter.write(commands);
    }

    /**
     * Register Closeable resources. Internal access only.
     *
     * @param registry registry of closeables
     * @param closeables closeables to register
     */
    public void registerCloseables(final Collection<Closeable> registry, Closeable... closeables) {

        registry.addAll(Arrays.asList(closeables));

        addListener(resource -> {
            for (Closeable closeable : closeables) {
                if (closeable == RedisChannelHandler.this) {
                    continue;
                }

                try {
                    if (closeable instanceof AsyncCloseable) {
                        ((AsyncCloseable) closeable).closeAsync();
                    } else {
                        closeable.close();
                    }
                } catch (IOException e) {
                    if (debugEnabled) {
                        logger.debug(e.toString(), e);
                    }
                }
            }

            registry.removeAll(Arrays.asList(closeables));
        });
    }

    protected void addListener(CloseEvents.CloseListener listener) {
        closeEvents.addListener(listener);
    }

    /**
     * @return true if the connection is closed (final state in the connection lifecyle).
     */
    public boolean isClosed() {
        return CLOSED.get(this) == ST_CLOSED;
    }

    /**
     * Notification when the connection becomes active (connected).
     */
    public void activated() {
        active = true;
        CLOSED.set(this, ST_OPEN);
    }

    /**
     * Notification when the connection becomes inactive (disconnected).
     */
    public void deactivated() {
        active = false;
    }

    /**
     * @return the channel writer
     */
    public RedisChannelWriter getChannelWriter() {
        return channelWriter;
    }

    /**
     * @return true if the connection is active and not closed.
     */
    public boolean isOpen() {
        return active;
    }

    @Deprecated
    @Override
    public void reset() {
        channelWriter.reset();
    }

    public ClientOptions getOptions() {
        return clientOptions;
    }

    public ClientResources getResources() {
        return clientResources;
    }

    public void setOptions(ClientOptions clientOptions) {
        LettuceAssert.notNull(clientOptions, "ClientOptions must not be null");
        this.clientOptions = clientOptions;
    }

    public Duration getTimeout() {
        return timeout;
    }

    @SuppressWarnings("unchecked")
    protected <T> T syncHandler(Object asyncApi, Class<?>... interfaces) {
        FutureSyncInvocationHandler h = new FutureSyncInvocationHandler((StatefulConnection<?, ?>) this, asyncApi, interfaces);
        return (T) Proxy.newProxyInstance(AbstractRedisClient.class.getClassLoader(), interfaces, h);
    }

    public void setAutoFlushCommands(boolean autoFlush) {
        getChannelWriter().setAutoFlushCommands(autoFlush);
    }

    public void flushCommands() {
        getChannelWriter().flushCommands();
    }

}
