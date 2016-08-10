package com.lambdaworks.redis.protocol;

import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.lambdaworks.redis.event.DefaultEventPublisherOptions;
import com.lambdaworks.redis.event.EventBus;
import com.lambdaworks.redis.event.EventPublisherOptions;
import com.lambdaworks.redis.metrics.CommandLatencyCollector;
import com.lambdaworks.redis.metrics.CommandLatencyId;
import com.lambdaworks.redis.metrics.CommandMetrics;
import com.lambdaworks.redis.resource.ClientResources;
import com.lambdaworks.redis.resource.Delay;
import com.lambdaworks.redis.resource.DnsResolver;
import com.lambdaworks.redis.resource.EventLoopGroupProvider;

import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.SucceededFuture;

/**
 * @author Mark Paluch
 */
public class EmptyClientResources implements ClientResources {

    public static final DefaultEventPublisherOptions PUBLISHER_OPTIONS = DefaultEventPublisherOptions.disabled();
    public static final EmptyCommandLatencyCollector LATENCY_COLLECTOR = new EmptyCommandLatencyCollector();
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
        return LATENCY_COLLECTOR;
    }

    @Override
    public DnsResolver dnsResolver() {
        return null;
    }

    @Override
    public Delay reconnectDelay() {
        return null;
    }

    public static class EmptyCommandLatencyCollector implements CommandLatencyCollector {

        @Override
        public void shutdown() {

        }

        @Override
        public Map<CommandLatencyId, CommandMetrics> retrieveMetrics() {
            return null;
        }

        @Override
        public boolean isEnabled() {
            return false;
        }

        @Override
        public void recordCommandLatency(SocketAddress local, SocketAddress remote, ProtocolKeyword commandType,
                long firstResponseLatency, long completionLatency) {

        }
    }
}
