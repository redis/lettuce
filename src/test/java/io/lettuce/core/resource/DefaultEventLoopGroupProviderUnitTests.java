/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.resource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import io.lettuce.test.Futures;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.Future;

/**
 * @author Mark Paluch
 */
class DefaultEventLoopGroupProviderUnitTests {

    @Test
    void shutdownTerminatedEventLoopGroup() {
        DefaultEventLoopGroupProvider sut = new DefaultEventLoopGroupProvider(1);

        NioEventLoopGroup eventLoopGroup = sut.allocate(NioEventLoopGroup.class);

        Future<Boolean> shutdown = sut.release(eventLoopGroup, 10, 10, TimeUnit.MILLISECONDS);
        Futures.await(shutdown);

        Future<Boolean> shutdown2 = sut.release(eventLoopGroup, 10, 10, TimeUnit.MILLISECONDS);
        Futures.await(shutdown2);
    }

    @Test
    void getAfterShutdown() {

        DefaultEventLoopGroupProvider sut = new DefaultEventLoopGroupProvider(1);

        Futures.await(sut.shutdown(10, 10, TimeUnit.MILLISECONDS));
        assertThatThrownBy(() -> sut.allocate(NioEventLoopGroup.class)).isInstanceOf(IllegalStateException.class);
    }

}
