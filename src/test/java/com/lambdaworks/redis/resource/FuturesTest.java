package com.lambdaworks.redis.resource;

import static com.google.code.tempusfugit.temporal.Duration.seconds;
import static com.google.code.tempusfugit.temporal.Timeout.timeout;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import com.google.code.tempusfugit.temporal.Condition;
import com.google.code.tempusfugit.temporal.WaitFor;

import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.ImmediateEventExecutor;
import io.netty.util.concurrent.Promise;

/**
 * @author Mark Paluch
 */
public class FuturesTest {

    @Test(expected = IllegalArgumentException.class)
    public void testPromise() throws Exception {
        new Futures.PromiseAggregator(null);
    }

    @Test(expected = IllegalStateException.class)
    public void notArmed() throws Exception {
        Futures.PromiseAggregator<Boolean, Promise<Boolean>> sut = new Futures.PromiseAggregator<Boolean, Promise<Boolean>>(
                new DefaultPromise<Boolean>(ImmediateEventExecutor.INSTANCE));
        sut.add(new DefaultPromise<Boolean>(ImmediateEventExecutor.INSTANCE));
    }

    @Test(expected = IllegalStateException.class)
    public void expectAfterArmed() throws Exception {
        Futures.PromiseAggregator<Boolean, Promise<Boolean>> sut = new Futures.PromiseAggregator<Boolean, Promise<Boolean>>(
                new DefaultPromise<Boolean>(ImmediateEventExecutor.INSTANCE));
        sut.arm();

        sut.expectMore(1);
    }

    @Test(expected = IllegalStateException.class)
    public void armTwice() throws Exception {
        Futures.PromiseAggregator<Boolean, Promise<Boolean>> sut = new Futures.PromiseAggregator<Boolean, Promise<Boolean>>(
                new DefaultPromise<Boolean>(ImmediateEventExecutor.INSTANCE));
        sut.arm();
        sut.arm();
    }

    @Test
    public void regularUse() throws Exception {
        final DefaultPromise<Boolean> target = new DefaultPromise<Boolean>(GlobalEventExecutor.INSTANCE);
        Futures.PromiseAggregator<Boolean, Promise<Boolean>> sut = new Futures.PromiseAggregator<Boolean, Promise<Boolean>>(
                target);

        sut.expectMore(1);
        sut.arm();
        DefaultPromise<Boolean> part = new DefaultPromise<Boolean>(GlobalEventExecutor.INSTANCE);
        sut.add(part);

        assertThat(target.isDone()).isFalse();

        part.setSuccess(true);

        WaitFor.waitOrTimeout(new Condition() {
            @Override
            public boolean isSatisfied() {
                return target.isDone();
            }
        }, timeout(seconds(5)));

        assertThat(target.isDone()).isTrue();
    }
}
