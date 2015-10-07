package com.lambdaworks.redis.event.metrics;

import com.lambdaworks.redis.event.EventBus;
import com.lambdaworks.redis.event.EventPublisherOptions;
import com.lambdaworks.redis.metrics.CommandLatencyCollector;

import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.ScheduledFuture;

/**
 * Default implementation of a {@link CommandLatencyCollector} for command latencies.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class DefaultCommandLatencyEventPublisher implements MetricEventPublisher {

    private final EventExecutorGroup eventExecutorGroup;
    private final EventPublisherOptions options;
    private final EventBus eventBus;
    private final CommandLatencyCollector commandLatencyCollector;

    private final Runnable EMITTER = new Runnable() {
        @Override
        public void run() {
            emitMetricsEvent();
        }
    };

    private volatile ScheduledFuture<?> scheduledFuture;

    public DefaultCommandLatencyEventPublisher(EventExecutorGroup eventExecutorGroup, EventPublisherOptions options,
            EventBus eventBus, CommandLatencyCollector commandLatencyCollector) {
        this.eventExecutorGroup = eventExecutorGroup;
        this.options = options;
        this.eventBus = eventBus;
        this.commandLatencyCollector = commandLatencyCollector;

        if (options.eventEmitInterval() > 0) {
            scheduledFuture = this.eventExecutorGroup.scheduleAtFixedRate(EMITTER, options.eventEmitInterval(),
                    options.eventEmitInterval(), options.eventEmitIntervalUnit());
        }
    }

    @Override
    public boolean isEnabled() {
        return options.eventEmitInterval() > 0 && scheduledFuture != null;
    }

    @Override
    public void shutdown() {

        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
            scheduledFuture = null;
        }
    }

    @Override
    public void emitMetricsEvent() {

        if (!isEnabled() || !commandLatencyCollector.isEnabled()) {
            return;
        }

        eventBus.publish(new CommandLatencyEvent(commandLatencyCollector.retrieveMetrics()));
    }

}
