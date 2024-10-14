package io.lettuce.core.resource;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.test.TestFutures;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.Future;

/**
 * @author Mark Paluch
 */
@Tag(UNIT_TEST)
class DefaultEventLoopGroupProviderUnitTests {

    @Test
    void shutdownTerminatedEventLoopGroup() {
        DefaultEventLoopGroupProvider sut = new DefaultEventLoopGroupProvider(1);

        NioEventLoopGroup eventLoopGroup = sut.allocate(NioEventLoopGroup.class);

        Future<Boolean> shutdown = sut.release(eventLoopGroup, 10, 10, TimeUnit.MILLISECONDS);
        TestFutures.awaitOrTimeout(shutdown);

        Future<Boolean> shutdown2 = sut.release(eventLoopGroup, 10, 10, TimeUnit.MILLISECONDS);
        TestFutures.awaitOrTimeout(shutdown2);
    }

    @Test
    void getAfterShutdown() {

        DefaultEventLoopGroupProvider sut = new DefaultEventLoopGroupProvider(1);

        TestFutures.awaitOrTimeout(sut.shutdown(10, 10, TimeUnit.MILLISECONDS));
        assertThatThrownBy(() -> sut.allocate(NioEventLoopGroup.class)).isInstanceOf(IllegalStateException.class);
    }

}
