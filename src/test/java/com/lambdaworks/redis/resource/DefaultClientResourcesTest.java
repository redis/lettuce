/*
 * Copyright 2011-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lambdaworks.redis.resource;

import static com.google.code.tempusfugit.temporal.Duration.seconds;
import static com.google.code.tempusfugit.temporal.Timeout.timeout;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import rx.observers.TestSubscriber;

import com.google.code.tempusfugit.temporal.Condition;
import com.google.code.tempusfugit.temporal.WaitFor;
import com.lambdaworks.redis.FastShutdown;
import com.lambdaworks.redis.event.Event;
import com.lambdaworks.redis.event.EventBus;
import com.lambdaworks.redis.metrics.CommandLatencyCollector;
import com.lambdaworks.redis.metrics.DefaultCommandLatencyCollectorOptions;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.Timer;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;

/**
 * @author Mark Paluch
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

        assertThat(sut.shutdown(0, 0, TimeUnit.MILLISECONDS).get()).isTrue();
    }

    @Test
    public void testDnsResolver() throws Exception {

        DirContextDnsResolver dirContextDnsResolver = new DirContextDnsResolver("8.8.8.8");

        DefaultClientResources sut = new DefaultClientResources.Builder().dnsResolver(dirContextDnsResolver).build();

        assertThat(sut.dnsResolver()).isEqualTo(dirContextDnsResolver);
    }

    @Test
    public void testProvidedResources() throws Exception {

        EventExecutorGroup executorMock = mock(EventExecutorGroup.class);
        EventLoopGroupProvider groupProviderMock = mock(EventLoopGroupProvider.class);
        Timer timerMock = mock(Timer.class);
        EventBus eventBusMock = mock(EventBus.class);
        CommandLatencyCollector latencyCollectorMock = mock(CommandLatencyCollector.class);
        NettyCustomizer nettyCustomizer = mock(NettyCustomizer.class);

        DefaultClientResources sut = new DefaultClientResources.Builder().eventExecutorGroup(executorMock)
                .eventLoopGroupProvider(groupProviderMock).timer(timerMock).eventBus(eventBusMock)
                .commandLatencyCollector(latencyCollectorMock)
.nettyCustomizer(nettyCustomizer)
                .build();

        assertThat(sut.eventExecutorGroup()).isSameAs(executorMock);
        assertThat(sut.eventLoopGroupProvider()).isSameAs(groupProviderMock);
        assertThat(sut.timer()).isSameAs(timerMock);
        assertThat(sut.eventBus()).isSameAs(eventBusMock);
        assertThat(sut.nettyCustomizer()).isSameAs(nettyCustomizer);

        assertThat(sut.shutdown().get()).isTrue();

        verifyZeroInteractions(executorMock);
        verifyZeroInteractions(groupProviderMock);
        verifyZeroInteractions(timerMock);
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

        assertThat(sut.shutdown(0, 0, TimeUnit.MILLISECONDS).get()).isTrue();
    }

    @Test
    public void testEventBus() throws Exception {

        DefaultClientResources sut = DefaultClientResources.create();

        EventBus eventBus = sut.eventBus();

        final TestSubscriber<Event> subject = new TestSubscriber<>();

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
        assertThat(sut.shutdown(0, 0, TimeUnit.MILLISECONDS).get()).isTrue();
    }

    @Test(expected = IllegalArgumentException.class)
    public void delayInstanceShouldRejectStatefulDelay() throws Exception {

        DefaultClientResources.builder().reconnectDelay(Delay.decorrelatedJitter().get());
    }

    @Test
    public void reconnectDelayCreatesNewForStatefulDelays() throws Exception {

        DefaultClientResources resources = DefaultClientResources.builder().reconnectDelay(Delay.decorrelatedJitter()).build();

        Delay delay1 = resources.reconnectDelay();
        Delay delay2 = resources.reconnectDelay();

        assertThat(delay1).isNotSameAs(delay2);

        FastShutdown.shutdown(resources);
    }

    @Test
    public void reconnectDelayReturnsSameInstanceForStatelessDelays() throws Exception {

        DefaultClientResources resources = DefaultClientResources.builder().reconnectDelay(Delay.exponential()).build();

        Delay delay1 = resources.reconnectDelay();
        Delay delay2 = resources.reconnectDelay();

        assertThat(delay1).isSameAs(delay2);

        FastShutdown.shutdown(resources);
    }
}
