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
import io.lettuce.core.resource.ClientResources;
import io.netty.channel.ChannelPipeline;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static io.lettuce.core.TimeoutOptions.TimeoutSource;

/**
 * Extension to {@link RedisChannelWriter} that expires commands. Command timeout starts at the time the command is written
 * regardless to {@link #setAutoFlushCommands(boolean) flushing mode} (user-controlled batching).
 * <p/>
 * This implementation, compared to the {@link CommandExpiryWriter} implementation, relaxes the timeouts when a re-bind is in
 * progress. The relaxation is done by starting a new timer with the relaxed timeout value. The relaxed timeout is configured
 * via {@link TimeoutOptions#getRelaxedTimeout()}.
 * <p/>
 * The logic is only applied when the {@link ClientOptions#isProactiveRebindEnabled()} is enabled.
 *
 * @author Tihomir Mateev
 * @since 6.7
 * @see TimeoutOptions
 * @see RebindAwareComponent
 * @see RebindAwareConnectionWatchdog
 * @see ClientOptions#isProactiveRebindEnabled()
 */
public class RebindAwareExpiryWriter extends CommandExpiryWriter implements RebindAwareComponent {

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

    private Timeout relaxTimeout;

    /**
     * Create a new {@link RebindAwareExpiryWriter}.
     *
     * @param delegate must not be {@code null}.
     * @param clientOptions must not be {@code null}.
     * @param clientResources must not be {@code null}.
     */
    public RebindAwareExpiryWriter(RedisChannelWriter delegate, ClientOptions clientOptions, ClientResources clientResources) {

        super(delegate, clientOptions, clientResources);

        TimeoutOptions timeoutOptions = clientOptions.getTimeoutOptions();
        this.delegate = delegate;
        this.source = timeoutOptions.getSource();
        this.applyConnectionTimeout = timeoutOptions.isApplyConnectionTimeout();
        this.relaxedTimeout = timeoutOptions.getRelaxedTimeout();
        this.timeUnit = source.getTimeUnit();
        this.executorService = clientResources.eventExecutorGroup();
        this.timer = clientResources.timer();
    }

    @Override
    public <K, V, T> RedisCommand<K, V, T> write(RedisCommand<K, V, T> command) {
        // since the RebindAwareExpiryWriter lives outside the netty pipeline, and since it needs to be registered at a moment
        // when the pipeline is configured and ready, we can only assume the moment is right if the write() method is called
        registerAsRebindAwareComponent();

        potentiallyExpire(command, executorService);
        return delegate.write(command);
    }

    @Override
    public <K, V> Collection<RedisCommand<K, V, ?>> write(Collection<? extends RedisCommand<K, V, ?>> redisCommands) {
        // since the RebindAwareExpiryWriter lives outside the netty pipeline, and since it needs to be registered at a moment
        // when the pipeline is configured and ready, we can only assume the moment is right if the write() method is called
        registerAsRebindAwareComponent();

        for (RedisCommand<K, V, ?> command : redisCommands) {
            potentiallyExpire(command, executorService);
        }

        return delegate.write(redisCommands);
    }

    @Override
    public void reset() {
        relaxTimeouts = false;
        registered = false;
        super.reset();
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
                        relaxedAttempt(command, executors, Duration.ofNanos(timeUnit.toNanos(timeout)));
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
    private void relaxedAttempt(RedisCommand<?, ?, ?> command, ScheduledExecutorService executors, Duration initialTimeout) {

        Timeout commandTimeout = timer.newTimeout(t -> {
            if (!command.isDone()) {
                executors.submit(() -> command
                        .completeExceptionally(ExceptionFactory.createTimeoutException(initialTimeout.plus(relaxedTimeout))));
            }
        }, relaxedTimeout.toMillis(), TimeUnit.MILLISECONDS);

        if (command instanceof CompleteableCommand) {
            ((CompleteableCommand<?>) command).onComplete((o, o2) -> commandTimeout.cancel());
        }
    }

    private void registerAsRebindAwareComponent() {
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
        enableRelaxedTimeout("Re-bind started");
    }

    @Override
    public void onRebindCompleted() {
        disableRelaxedTimeoutDelayed("Re-bind completed");
    }

    @Override
    public void onMigrateStarted() {
        enableRelaxedTimeout("Migration started");
    }

    @Override
    public void onMigrateCompleted() {
        disableRelaxedTimeoutDelayed("Migration completed");
    }

    private void enableRelaxedTimeout(String reason) {
        if (relaxTimeout != null) {
            boolean canceled = relaxTimeout.cancel();
            log.debug("Canceled previous disable relax timeout task : {}", canceled);
        }

        if (!relaxedTimeout.isNegative()) {
            log.info("{}, relaxing timeouts with an additional {}ms", reason, relaxedTimeout.toMillis());
            this.relaxTimeouts = true;
        } else {
            log.debug("{}, but timeout relaxing is disabled", reason);
            this.relaxTimeouts = false;
        }
    }

    private void disableRelaxedTimeoutDelayed(String reason) {
        if (relaxTimeout != null) {
            boolean canceled = relaxTimeout.cancel();
            log.debug("Canceled previous disable relax timeout task : {}", canceled);
        }

        // Consider the operation complete after another relaxed timeout cycle.
        //
        // The reasoning behind that is we can't really be sure when all the enqueued commands have
        // successfully been written to the wire and then the reply was received
        log.debug("{}, scheduling timeout relaxation disable after {}ms", reason, relaxedTimeout.toMillis());
        relaxTimeout = timer.newTimeout(t -> executorService.submit(() -> {
            log.debug("Disabling timeout relaxation after {}", reason);
            this.relaxTimeouts = false;
        }), relaxedTimeout.toMillis(), TimeUnit.MILLISECONDS);
    }

}
