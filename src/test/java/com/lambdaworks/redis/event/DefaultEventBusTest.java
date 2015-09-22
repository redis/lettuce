package com.lambdaworks.redis.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;
import rx.schedulers.TestScheduler;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultEventBusTest {

    @Mock
    private RedisEvent redisEvent;

    @Test
    public void publishToSubscriber() throws Exception {
        TestScheduler testScheduler = Schedulers.test();
        EventBus sut = new DefaultEventBus(testScheduler);

        TestSubscriber<RedisEvent> subscriber = new TestSubscriber<RedisEvent>();
        sut.get().subscribe(subscriber);

        sut.publish(redisEvent);

        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        assertThat(subscriber.getOnNextEvents()).hasSize(1).contains(redisEvent);
    }
}
