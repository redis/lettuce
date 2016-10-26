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
 * @author Mark Paluch
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
