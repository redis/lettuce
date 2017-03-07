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
package io.lettuce.core.resource;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.Future;

/**
 * @author Mark Paluch
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
