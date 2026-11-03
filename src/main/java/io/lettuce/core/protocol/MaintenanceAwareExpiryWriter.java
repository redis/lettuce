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
import io.netty.util.Timeout;
import io.netty.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
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
 * The logic is only applied when the {@link ClientOptions#getMaintNotificationsConfig()} is enabled.
 *
 * @author Tihomir Mateev
 * @since 7.0
 * @see TimeoutOptions
 * @see MaintenanceAwareComponent
 * @see MaintenanceAwareConnectionWatchdog
 * @see ClientOptions#getMaintNotificationsConfig()
 */
public class MaintenanceAwareExpiryWriter extends CommandExpiryWriter implements MaintenanceAwareComponent {

    private static final Logger log = LoggerFactory.getLogger(MaintenanceAwareExpiryWriter.class);

    private final RedisChannelWriter delegate;

    private final TimeoutSource source;

    private final TimeUnit timeUnit;

    private final ScheduledExecutorService executorService;

    private final Timer timer;

    private final boolean applyConnectionTimeout;

    private final Duration relaxedTimeout;

    private volatile boolean relaxTimeouts = false;

    private Timeout relaxTimeout;

    /**
     * Create a new {@link MaintenanceAwareExpiryWriter}.
     *
     * @param delegate must not be {@code null}.
     * @param clientOptions must not be {@code null}.
     * @param clientResources must not be {@code null}.
     */
    public MaintenanceAwareExpiryWriter(RedisChannelWriter delegate, ClientOptions clientOptions,
            ClientResources clientResources) {

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

        potentiallyExpire(command, executorService);
        return delegate.write(command);
    }

    @Override
    public <K, V> Collection<RedisCommand<K, V, ?>> write(Collection<? extends RedisCommand<K, V, ?>> redisCommands) {

        for (RedisCommand<K, V, ?> command : redisCommands) {
            potentiallyExpire(command, executorService);
        }

        return delegate.write(redisCommands);
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

    @Override
    public void onRebindStarted(Duration time, SocketAddress endpoint) {
        enableRelaxedTimeout("Re-bind started");
    }

    @Override
    public void onRebindCompleted() {
        disableRelaxedTimeoutDelayed("Re-bind completed", relaxedTimeout);
    }

    @Override
    public void onMigrateStarted(String shards) {
        enableRelaxedTimeout("Migration started for shards: " + shards);
    }

    @Override
    public void onMigrateCompleted(String shards) {
        disableRelaxedTimeoutDelayed("Migration completed: " + shards, relaxedTimeout);
    }

    @Override
    public void onFailoverStarted(String shards) {
        enableRelaxedTimeout("Failover started for shards: " + shards);
    }

    @Override
    public void onFailoverCompleted(String shards) {
        disableRelaxedTimeoutDelayed("Failover completed: " + shards, relaxedTimeout);
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

    private void disableRelaxedTimeoutDelayed(String reason, Duration gracePeriod) {
        if (relaxTimeout != null) {
            boolean canceled = relaxTimeout.cancel();
            log.debug("Canceled previous disable relax timeout task : {}", canceled);
        }

        // Consider the operation complete after another relaxed timeout cycle.
        //
        // The reasoning behind that is we can't really be sure when all the enqueued commands have
        // successfully been written to the wire and then the reply was received
        log.debug("{}, scheduling timeout relaxation disable after {}ms", reason, gracePeriod.toMillis());
        relaxTimeout = timer.newTimeout(t -> executorService.submit(() -> {
            log.debug("Disabling timeout relaxation after {}", reason);
            this.relaxTimeouts = false;
        }), gracePeriod.toMillis(), TimeUnit.MILLISECONDS);
    }

}
