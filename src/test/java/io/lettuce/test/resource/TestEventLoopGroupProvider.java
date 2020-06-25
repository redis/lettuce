/*
 * Copyright 2018-2020 the original author or authors.
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
package io.lettuce.test.resource;

import java.util.concurrent.TimeUnit;

import io.lettuce.core.resource.DefaultEventLoopGroupProvider;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.ImmediateEventExecutor;
import io.netty.util.concurrent.Promise;

/**
 * A {@link io.lettuce.core.resource.EventLoopGroupProvider} suitable for testing. Preserves the event loop groups between
 * tests. Every time a new {@link TestEventLoopGroupProvider} instance is created, shutdown hook is added
 * {@link Runtime#addShutdownHook(Thread)}.
 *
 * @author Mark Paluch
 */
class TestEventLoopGroupProvider extends DefaultEventLoopGroupProvider {

    public TestEventLoopGroupProvider() {
        super(10);
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                try {
                    TestEventLoopGroupProvider.this.shutdown(100, 100, TimeUnit.MILLISECONDS).get(10, TimeUnit.SECONDS);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        });
    }

    @Override
    public Promise<Boolean> release(EventExecutorGroup eventLoopGroup, long quietPeriod, long timeout, TimeUnit unit) {
        DefaultPromise<Boolean> result = new DefaultPromise<>(ImmediateEventExecutor.INSTANCE);
        result.setSuccess(true);

        return result;
    }

}
