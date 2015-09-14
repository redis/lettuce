package com.lambdaworks.redis.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

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

        EventExecutorGroup eventExecutors = sut.eventExecutorGroup();
        NioEventLoopGroup eventLoopGroup = sut.eventLoopGroupProvider().allocate(NioEventLoopGroup.class);

        assertThat(sut.shutdown().get()).isTrue();

        assertThat(eventExecutors.isTerminated()).isTrue();
        assertThat(eventLoopGroup.isTerminated()).isTrue();

        Future<Boolean> shutdown = sut.eventLoopGroupProvider().shutdown(0, 0, TimeUnit.SECONDS);
        assertThat(shutdown.get()).isTrue();

    }

    @Test
    public void testBuilder() throws Exception {

        DefaultClientResources sut = new DefaultClientResources.Builder().ioThreadPoolSize(4).computationThreadPoolSize(4)
                .build();

        EventExecutorGroup eventExecutors = sut.eventExecutorGroup();
        NioEventLoopGroup eventLoopGroup = sut.eventLoopGroupProvider().allocate(NioEventLoopGroup.class);

        assertThat(eventExecutors.iterator()).hasSize(4);
        assertThat(eventLoopGroup.executorCount()).isEqualTo(4);

        assertThat(sut.ioThreadPoolSize()).isEqualTo(4);

        assertThat(sut.shutdown().get()).isTrue();
    }

    @Test
    public void testProvidedExecutors() throws Exception {

        EventExecutorGroup executorMock = mock(EventExecutorGroup.class);
        EventLoopGroupProvider groupProviderMock = mock(EventLoopGroupProvider.class);

        DefaultClientResources sut = new DefaultClientResources.Builder().eventExecutorGroup(executorMock)
                .eventLoopGroupProvider(groupProviderMock).build();

        assertThat(sut.eventExecutorGroup()).isSameAs(executorMock);
        assertThat(sut.eventLoopGroupProvider()).isSameAs(groupProviderMock);

        assertThat(sut.shutdown().get()).isTrue();

        verifyZeroInteractions(executorMock);
        verifyZeroInteractions(groupProviderMock);
    }
}
