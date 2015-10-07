package com.lambdaworks.redis.resource;

import static com.google.code.tempusfugit.temporal.Duration.seconds;
import static com.google.code.tempusfugit.temporal.Timeout.timeout;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import rx.observers.TestSubscriber;

import com.google.code.tempusfugit.temporal.Condition;
import com.google.code.tempusfugit.temporal.WaitFor;
import com.lambdaworks.redis.event.EventBus;
import com.lambdaworks.redis.event.Event;
import com.lambdaworks.redis.metrics.CommandLatencyCollector;
import com.lambdaworks.redis.metrics.DefaultCommandLatencyCollectorOptions;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 17.09.15 12:13
 */
public class DefaultClientResourcesTest {

    @Test
    public void testDefaults() throws Exception {

        DefaultClientResources sut = DefaultClientResources.create();

        assertThat(sut.commandLatencyCollector()).isNotNull();
        assertThat(sut.commandLatencyCollector().isEnabled()).isTrue();

        EventExecutorGroup eventExecutors = sut.eventExecutorGroup();
        NioEventLoopGroup eventLoopGroup = sut.eventLoopGroupProvider().allocate(NioEventLoopGroup.class);

        eventExecutors.next().submit(mock(Runnable.class));
        eventLoopGroup.next().submit(mock(Runnable.class));

        assertThat(sut.shutdown(0, 0, TimeUnit.SECONDS).get()).isTrue();

        assertThat(eventExecutors.isTerminated()).isTrue();
        assertThat(eventLoopGroup.isTerminated()).isTrue();

        Future<Boolean> shutdown = sut.eventLoopGroupProvider().shutdown(0, 0, TimeUnit.SECONDS);
        assertThat(shutdown.get()).isTrue();

        assertThat(sut.commandLatencyCollector().isEnabled()).isFalse();
    }

    @Test
    public void testBuilder() throws Exception {

        DefaultClientResources sut = new DefaultClientResources.Builder().ioThreadPoolSize(4).computationThreadPoolSize(4)
                .commandLatencyCollectorOptions(DefaultCommandLatencyCollectorOptions.disabled()).build();

        EventExecutorGroup eventExecutors = sut.eventExecutorGroup();
        NioEventLoopGroup eventLoopGroup = sut.eventLoopGroupProvider().allocate(NioEventLoopGroup.class);

        assertThat(eventExecutors.iterator()).hasSize(4);
        assertThat(eventLoopGroup.executorCount()).isEqualTo(4);
        assertThat(sut.ioThreadPoolSize()).isEqualTo(4);
        assertThat(sut.commandLatencyCollector()).isNotNull();
        assertThat(sut.commandLatencyCollector().isEnabled()).isFalse();

        assertThat(sut.shutdown(10, 10, TimeUnit.MILLISECONDS).get()).isTrue();
    }

    @Test
    public void testProvidedResources() throws Exception {

        EventExecutorGroup executorMock = mock(EventExecutorGroup.class);
        EventLoopGroupProvider groupProviderMock = mock(EventLoopGroupProvider.class);
        EventBus eventBusMock = mock(EventBus.class);
        CommandLatencyCollector latencyCollectorMock = mock(CommandLatencyCollector.class);

        DefaultClientResources sut = new DefaultClientResources.Builder().eventExecutorGroup(executorMock)
                .eventLoopGroupProvider(groupProviderMock).eventBus(eventBusMock).commandLatencyCollector(latencyCollectorMock)
                .build();

        assertThat(sut.eventExecutorGroup()).isSameAs(executorMock);
        assertThat(sut.eventLoopGroupProvider()).isSameAs(groupProviderMock);
        assertThat(sut.eventBus()).isSameAs(eventBusMock);

        assertThat(sut.shutdown().get()).isTrue();

        verifyZeroInteractions(executorMock);
        verifyZeroInteractions(groupProviderMock);
        verify(latencyCollectorMock).isEnabled();
        verifyNoMoreInteractions(latencyCollectorMock);
    }

    @Test
    public void testSmallPoolSize() throws Exception {

        DefaultClientResources sut = new DefaultClientResources.Builder().ioThreadPoolSize(1).computationThreadPoolSize(1)
                .build();

        EventExecutorGroup eventExecutors = sut.eventExecutorGroup();
        NioEventLoopGroup eventLoopGroup = sut.eventLoopGroupProvider().allocate(NioEventLoopGroup.class);

        assertThat(eventExecutors.iterator()).hasSize(3);
        assertThat(eventLoopGroup.executorCount()).isEqualTo(3);
        assertThat(sut.ioThreadPoolSize()).isEqualTo(3);

        assertThat(sut.shutdown(10, 10, TimeUnit.MILLISECONDS).get()).isTrue();
    }

    @Test
    public void testEventBus() throws Exception {

        DefaultClientResources sut = DefaultClientResources.create();

        EventBus eventBus = sut.eventBus();

        final TestSubscriber<Event> subject = new TestSubscriber<Event>();

        eventBus.get().subscribe(subject);

        Event event = mock(Event.class);
        eventBus.publish(event);

        WaitFor.waitOrTimeout(new Condition() {
            @Override
            public boolean isSatisfied() {
                return !subject.getOnNextEvents().isEmpty();
            }
        }, timeout(seconds(2)));

        assertThat(subject.getOnNextEvents()).contains(event);
        assertThat(sut.shutdown(10, 10, TimeUnit.MILLISECONDS).get()).isTrue();
    }

}
