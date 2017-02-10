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

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;
import rx.schedulers.TestScheduler;

/**
 * @author Mark Paluch
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultEventBusTest {

    @Mock
    private Event event;

    @Test
    public void publishToSubscriber() throws Exception {
        TestScheduler testScheduler = Schedulers.test();
        EventBus sut = new DefaultEventBus(testScheduler);

        TestSubscriber<Event> subscriber = new TestSubscriber<>();
        sut.get().subscribe(subscriber);

        sut.publish(event);

        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        assertThat(subscriber.getOnNextEvents()).hasSize(1).contains(event);
    }
}
