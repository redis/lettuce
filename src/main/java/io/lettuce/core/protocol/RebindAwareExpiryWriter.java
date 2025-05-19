/*
 * Copyright 2017-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.protocol;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisChannelWriter;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.internal.ExceptionFactory;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.resource.ClientResources;
import io.netty.channel.ChannelPipeline;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static io.lettuce.core.TimeoutOptions.TimeoutSource;

/**
 * Extension to {@link RedisChannelWriter} that expires commands. Command timeout starts at the time the command is written
 * regardless to {@link #setAutoFlushCommands(boolean) flushing mode} (user-controlled batching).
 *
 * @author Mark Paluch
 * @author Tianyi Yang
 * @since 5.1
 * @see TimeoutOptions
 */
public class RebindAwareExpiryWriter implements RedisChannelWriter, RebindAwareComponent {

    private static final Logger log = LoggerFactory.getLogger(RebindAwareExpiryWriter.class);

    private final RedisChannelWriter delegate;

    private final TimeoutSource source;

    private final TimeUnit timeUnit;

    private final ScheduledExecutorService executorService;

    private final Timer timer;

    private final boolean applyConnectionTimeout;

    private final Duration relaxedTimeout;

    private volatile long timeout = -1;

    private volatile boolean relaxTimeouts = false;

    private boolean registered = false;

    /**
     * Create a new {@link RebindAwareExpiryWriter}.
     *
     * @param delegate must not be {@code null}.
     * @param clientOptions must not be {@code null}.
     * @param clientResources must not be {@code null}.
     */
    public RebindAwareExpiryWriter(RedisChannelWriter delegate, ClientOptions clientOptions, ClientResources clientResources) {

        LettuceAssert.notNull(delegate, "RedisChannelWriter must not be null");
        LettuceAssert.isTrue(isSupported(clientOptions), "Command timeout not enabled");
        LettuceAssert.notNull(clientResources, "ClientResources must not be null");

        TimeoutOptions timeoutOptions = clientOptions.getTimeoutOptions();
        this.delegate = delegate;
        this.source = timeoutOptions.getSource();
        this.applyConnectionTimeout = timeoutOptions.isApplyConnectionTimeout();
        this.relaxedTimeout = timeoutOptions.getRelaxedTimeout();
        this.timeUnit = source.getTimeUnit();
        this.executorService = clientResources.eventExecutorGroup();
        this.timer = clientResources.timer();
    }

    /**
     * Check whether {@link ClientOptions} is configured to timeout commands.
     *
     * @param clientOptions must not be {@code null}.
     * @return {@code true} if {@link ClientOptions} are configured to timeout commands.
     */
    public static boolean isSupported(ClientOptions clientOptions) {

        LettuceAssert.notNull(clientOptions, "ClientOptions must not be null");

        return isSupported(clientOptions.getTimeoutOptions());
    }

    private static boolean isSupported(TimeoutOptions timeoutOptions) {

        LettuceAssert.notNull(timeoutOptions, "TimeoutOptions must not be null");

        return timeoutOptions.isTimeoutCommands();
    }

    @Override
    public void setConnectionFacade(ConnectionFacade connectionFacade) {
        delegate.setConnectionFacade(connectionFacade);
    }

    @Override
    public ClientResources getClientResources() {
        return delegate.getClientResources();
    }

    @Override
    public void setAutoFlushCommands(boolean autoFlush) {
        delegate.setAutoFlushCommands(autoFlush);
    }

    @Override
    public <K, V, T> RedisCommand<K, V, T> write(RedisCommand<K, V, T> command) {

        registerAsRebindAwareComponent();
        potentiallyExpire(command, getExecutorService());
        return delegate.write(command);
    }

    @Override
    public <K, V> Collection<RedisCommand<K, V, ?>> write(Collection<? extends RedisCommand<K, V, ?>> redisCommands) {

        ScheduledExecutorService executorService = getExecutorService();
        registerAsRebindAwareComponent();

        for (RedisCommand<K, V, ?> command : redisCommands) {
            potentiallyExpire(command, executorService);
        }

        return delegate.write(redisCommands);
    }

    @Override
    public void flushCommands() {
        delegate.flushCommands();
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public CompletableFuture<Void> closeAsync() {
        return delegate.closeAsync();
    }

    @Override
    public void reset() {
        relaxTimeouts = false;
        registered = false;
        delegate.reset();
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeUnit.convert(timeout.toNanos(), TimeUnit.NANOSECONDS);
    }

    public RedisChannelWriter getDelegate() {
        return delegate;
    }

    private ScheduledExecutorService getExecutorService() {
        return this.executorService;
    }

    private void potentiallyExpire(RedisCommand<?, ?, ?> command, ScheduledExecutorService executors) {

        long timeout = applyConnectionTimeout ? this.timeout : source.getTimeout(command);

        if (timeout <= 0) {
            return;
        }

        Timeout commandTimeout = timer.newTimeout(t -> {
            if (!command.isDone()) {
                executors.submit(() -> {
                    if (relaxTimeouts) {
                        relaxedAttempt(command, executors);
                    } else {
                        command.completeExceptionally(ExceptionFactory.createTimeoutException(command.getType().toString(),
                                Duration.ofNanos(timeUnit.toNanos(timeout))));
                    }
                });

            }
        }, timeout, timeUnit);

        if (command instanceof CompleteableCommand) {
            ((CompleteableCommand<?>) command).onComplete((o, o2) -> commandTimeout.cancel());
        }

    }

    // when relaxing the timeouts - instead of expiring immediately, we will start a new timer with 10 seconds
    private void relaxedAttempt(RedisCommand<?, ?, ?> command, ScheduledExecutorService executors) {

        Timeout commandTimeout = timer.newTimeout(t -> {
            if (!command.isDone()) {
                executors.submit(() -> command.completeExceptionally(ExceptionFactory.createTimeoutException(relaxedTimeout)));
            }
        }, relaxedTimeout.toMillis(), TimeUnit.MILLISECONDS);

        if (command instanceof CompleteableCommand) {
            ((CompleteableCommand<?>) command).onComplete((o, o2) -> commandTimeout.cancel());
        }
    }

    private void registerAsRebindAwareComponent() {
        //

        if (registered) {
            return;
        }

        if (delegate instanceof DefaultEndpoint) {
            DefaultEndpoint endpoint = (DefaultEndpoint) delegate;
            ChannelPipeline pipeline = endpoint.channel.pipeline();
            RebindAwareConnectionWatchdog watchdog = pipeline.get(RebindAwareConnectionWatchdog.class);
            if (watchdog != null) {
                watchdog.setRebindListener(this);
            }
        }

        registered = true;
    }

    @Override
    public void onRebindStarted() {
        if (!relaxedTimeout.isNegative()) {
            log.info("Re-bind started, relaxing timeouts with an additional {}ms", relaxedTimeout.toMillis());
            this.relaxTimeouts = true;
        } else {
            log.debug("Re-bind started, but timeout relaxing is disabled");
            this.relaxTimeouts = false;
        }
    }

    @Override
    public void onRebindCompleted() {
        // Consider the rebind complete after another relaxed timeout cycle.
        //
        // The reasoning behind that is we can't really be sure when all the enqueued commands have
        // successfully been written to the wire and then the reply was received
        timer.newTimeout(t -> getExecutorService().submit(() -> this.relaxTimeouts = false), relaxedTimeout.toMillis(),
                TimeUnit.MILLISECONDS);
    }

}
