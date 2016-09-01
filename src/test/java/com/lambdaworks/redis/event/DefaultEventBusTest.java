package com.lambdaworks.redis.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import com.lambdaworks.redis.reactive.TestSubscriber;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import reactor.core.scheduler.Schedulers;

/**
 * @author Mark Paluch
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultEventBusTest {

    @Mock
    private Event event;

    @Test
    public void publishToSubscriber() throws Exception {

        EventBus sut = new DefaultEventBus(Schedulers.immediate());

        TestSubscriber<Event> subscriber = TestSubscriber.create();
        sut.get().subscribe(subscriber);

        sut.publish(event);

        subscriber.awaitAndAssertNextValues(event);
    }

    @Test
    public void publishToMultipleSubscribers() throws Exception {

        EventBus sut = new DefaultEventBus(Schedulers.immediate());

        TestSubscriber<Event> subscriber1 = TestSubscriber.create();
        TestSubscriber<Event> subscriber2 = TestSubscriber.create();
        sut.get().subscribe(subscriber1);
        sut.get().subscribe(subscriber2);

        sut.publish(event);

        subscriber1.awaitAndAssertNextValues(event);
        subscriber2.awaitAndAssertNextValues(event);
    }
}
