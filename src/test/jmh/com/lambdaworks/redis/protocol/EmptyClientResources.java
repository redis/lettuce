package com.lambdaworks.redis.protocol;

import com.lambdaworks.redis.event.DefaultEventPublisherOptions;
import com.lambdaworks.redis.event.EventBus;
import com.lambdaworks.redis.event.EventPublisherOptions;
import com.lambdaworks.redis.metrics.CommandLatencyCollector;
import com.lambdaworks.redis.resource.ClientResources;
import com.lambdaworks.redis.resource.Delay;
import com.lambdaworks.redis.resource.DnsResolver;
import com.lambdaworks.redis.resource.EventLoopGroupProvider;
import io.netty.util.concurrent.*;

import java.util.concurrent.TimeUnit;

/**
 * @author Mark Paluch
 */
public class EmptyClientResources implements ClientResources {

    public static final DefaultEventPublisherOptions PUBLISHER_OPTIONS = DefaultEventPublisherOptions.disabled();
    public static final EmptyClientResources INSTANCE = new EmptyClientResources();

    @Override
    public Future<Boolean> shutdown() {
        return new SucceededFuture<>(GlobalEventExecutor.INSTANCE, true);
    }

    @Override
    public Future<Boolean> shutdown(long quietPeriod, long timeout, TimeUnit timeUnit) {
        return new SucceededFuture<>(GlobalEventExecutor.INSTANCE, true);
    }

    @Override
    public EventLoopGroupProvider eventLoopGroupProvider() {
        return null;
    }

    @Override
    public EventExecutorGroup eventExecutorGroup() {
        return null;
    }

    @Override
    public int ioThreadPoolSize() {
        return 0;
    }

    @Override
    public int computationThreadPoolSize() {
        return 0;
    }

    @Override
    public EventBus eventBus() {
        return null;
    }

    @Override
    public EventPublisherOptions commandLatencyPublisherOptions() {
        return PUBLISHER_OPTIONS;
    }

    @Override
    public CommandLatencyCollector commandLatencyCollector() {
        return null;
    }

    @Override
    public DnsResolver dnsResolver() {
        return null;
    }

    @Override
    public Delay reconnectDelay() {
        return null;
    }
}
