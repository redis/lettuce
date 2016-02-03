package com.lambdaworks.redis.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.assertj.core.api.Condition;
import org.junit.Test;

import rx.Subscription;
import rx.observers.TestSubscriber;

import com.lambdaworks.Wait;
import com.lambdaworks.redis.AbstractRedisClientTest;
import com.lambdaworks.redis.event.connection.*;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class ConnectionEventsTriggeredTest extends AbstractRedisClientTest {

    @Test
    public void testConnectionEvents() throws Exception {

        TestSubscriber<Object> subscriber = TestSubscriber.create();

        Subscription subscription = client.getResources().eventBus().get().filter(
                event -> event instanceof ConnectionEvent)
                .subscribe(subscriber);

        try {
            client.connect().close();
            Wait.untilTrue(() -> subscriber.getOnNextEvents().size() > 3).waitOrTimeout();
        } finally {
            subscription.unsubscribe();
        }

        List<Object> events = subscriber.getOnNextEvents();
        assertThat(events).areAtLeast(1, new ExpectedClassCondition(ConnectedEvent.class));
        assertThat(events).areAtLeast(1, new ExpectedClassCondition(ConnectionActivatedEvent.class));
        assertThat(events).areAtLeast(1, new ExpectedClassCondition(DisconnectedEvent.class));
        assertThat(events).areAtLeast(1, new ExpectedClassCondition(ConnectionDeactivatedEvent.class));

        ConnectionEvent event = (ConnectionEvent) events.get(0);
        assertThat(event.remoteAddress()).isNotNull();
        assertThat(event.localAddress()).isNotNull();

        assertThat(event.toString()).contains("->");
    }

    private static class ExpectedClassCondition extends Condition<Object> {
        private final Class<?> expectedClass;

        public ExpectedClassCondition(Class<?> expectedClass) {
            this.expectedClass = expectedClass;
        }

        @Override
        public boolean matches(Object value) {
            return value != null && value.getClass().equals(expectedClass);
        }
    }
}
