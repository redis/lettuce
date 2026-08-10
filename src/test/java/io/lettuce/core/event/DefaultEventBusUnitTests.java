package io.lettuce.core.event;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.lettuce.core.Subscription;
import reactor.core.Disposable;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

/**
 * @author Mark Paluch
 */
@Tag(UNIT_TEST)
@ExtendWith(MockitoExtension.class)
class DefaultEventBusUnitTests {

    @Mock
    private Event event;

    @Test
    void publishToSubscriber() {

        EventBus sut = new DefaultEventBus(Schedulers.immediate());

        StepVerifier.create(sut.get()).then(() -> sut.publish(event)).expectNext(event).thenCancel().verify();
    }

    @Test
    void publishToMultipleSubscribers() throws Exception {

        EventBus sut = new DefaultEventBus(Schedulers.parallel());

        ArrayBlockingQueue<Event> arrayQueue = new ArrayBlockingQueue<>(5);

        Disposable disposable1 = sut.get().doOnNext(arrayQueue::add).subscribe();
        StepVerifier.create(sut.get().doOnNext(arrayQueue::add)).then(() -> sut.publish(event)).expectNext(event).thenCancel()
                .verify();

        assertThat(arrayQueue.take()).isEqualTo(event);
        assertThat(arrayQueue.take()).isEqualTo(event);
        disposable1.dispose();
    }

    @Test
    void subscribeCallbackReceivesEvent() throws Exception {

        EventBus sut = new DefaultEventBus(Schedulers.immediate());
        ArrayBlockingQueue<Event> received = new ArrayBlockingQueue<>(5);

        Subscription subscription = sut.subscribe(received::add);
        sut.publish(event);

        assertThat(received.poll(1, TimeUnit.SECONDS)).isEqualTo(event);
        subscription.close();
    }

    @Test
    void subscribeByTypeFiltersEvents() throws Exception {

        EventBus sut = new DefaultEventBus(Schedulers.immediate());
        ArrayBlockingQueue<EventA> received = new ArrayBlockingQueue<>(5);

        Subscription subscription = sut.subscribe(EventA.class, received::add);
        sut.publish(new EventB());
        EventA expected = new EventA();
        sut.publish(expected);

        assertThat(received.poll(1, TimeUnit.SECONDS)).isSameAs(expected);
        assertThat(received).isEmpty();
        subscription.close();
    }

    @Test
    void closeStopsDelivery() throws Exception {

        EventBus sut = new DefaultEventBus(Schedulers.immediate());
        ArrayBlockingQueue<Event> received = new ArrayBlockingQueue<>(5);

        Subscription subscription = sut.subscribe(received::add);
        subscription.close();
        sut.publish(event);

        assertThat(received.poll(200, TimeUnit.MILLISECONDS)).isNull();
    }

    static class EventA implements Event {
    }

    static class EventB implements Event {
    }

}
