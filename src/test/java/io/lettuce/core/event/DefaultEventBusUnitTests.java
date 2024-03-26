package io.lettuce.core.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.ArrayBlockingQueue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.Disposable;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

/**
 * @author Mark Paluch
 */
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
}
