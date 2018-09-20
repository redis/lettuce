/*
 * Copyright 2011-2018 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import io.lettuce.test.Wait;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.ImmediateEventExecutor;
import io.netty.util.concurrent.Promise;

/**
 * @author Mark Paluch
 */
class FuturesUnitTests {

    @Test
    void testPromise() {
        assertThatThrownBy(() -> new Futures.PromiseAggregator(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void notArmed() {

        Futures.PromiseAggregator<Boolean, Promise<Boolean>> sut = new Futures.PromiseAggregator<>(
                new DefaultPromise<>(ImmediateEventExecutor.INSTANCE));
        assertThatThrownBy(() -> sut.add(new DefaultPromise<>(ImmediateEventExecutor.INSTANCE))).isInstanceOf(
                IllegalStateException.class);
    }

    @Test
    void expectAfterArmed() {
        Futures.PromiseAggregator<Boolean, Promise<Boolean>> sut = new Futures.PromiseAggregator<>(
                new DefaultPromise<>(ImmediateEventExecutor.INSTANCE));
        sut.arm();

        assertThatThrownBy(() -> sut.expectMore(1)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void armTwice() {

        Futures.PromiseAggregator<Boolean, Promise<Boolean>> sut = new Futures.PromiseAggregator<>(
                new DefaultPromise<>(ImmediateEventExecutor.INSTANCE));

        sut.arm();
        assertThatThrownBy(sut::arm).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void regularUse() {

        DefaultPromise<Boolean> target = new DefaultPromise<>(GlobalEventExecutor.INSTANCE);
        Futures.PromiseAggregator<Boolean, Promise<Boolean>> sut = new Futures.PromiseAggregator<>(target);

        sut.expectMore(1);
        sut.arm();
        DefaultPromise<Boolean> part = new DefaultPromise<>(GlobalEventExecutor.INSTANCE);
        sut.add(part);

        assertThat(target.isDone()).isFalse();

        part.setSuccess(true);

        Wait.untilTrue(target::isDone).waitOrTimeout();

        assertThat(target.isDone()).isTrue();
    }
}
