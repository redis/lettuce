package com.lambdaworks.redis.resource;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.Future;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class DefaultEventLoopGroupProviderTest {

    @Test
    public void shutdownTerminatedEventLoopGroup() throws Exception {
        DefaultEventLoopGroupProvider sut = new DefaultEventLoopGroupProvider(1);

        NioEventLoopGroup eventLoopGroup = sut.allocate(NioEventLoopGroup.class);

        Future<Boolean> shutdown = sut.release(eventLoopGroup, 10, 10, TimeUnit.MILLISECONDS);
        shutdown.get();

        Future<Boolean> shutdown2 = sut.release(eventLoopGroup, 10, 10, TimeUnit.MILLISECONDS);
        shutdown2.get();
    }

    @Test(expected = IllegalStateException.class)
    public void getAfterShutdown() throws Exception {
        DefaultEventLoopGroupProvider sut = new DefaultEventLoopGroupProvider(1);

        sut.shutdown(10, 10, TimeUnit.MILLISECONDS).get();
        sut.allocate(NioEventLoopGroup.class);
    }
}