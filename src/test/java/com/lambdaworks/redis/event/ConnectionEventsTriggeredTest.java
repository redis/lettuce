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

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.junit.Test;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import com.lambdaworks.TestClientResources;
import com.lambdaworks.redis.AbstractTest;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.event.connection.ConnectionEvent;

/**
 * @author Mark Paluch
 */
public class ConnectionEventsTriggeredTest extends AbstractTest {

    @Test
    public void testConnectionEvents() throws Exception {

        RedisClient client = RedisClient.create(TestClientResources.get(), RedisURI.Builder.redis(host, port).build());

        Flux<ConnectionEvent> publisher = client.getResources().eventBus().get()
                .filter(event -> event instanceof ConnectionEvent).cast(ConnectionEvent.class);

        StepVerifier.create(publisher).then(() -> client.connect().close()).assertNext(event -> {
            assertThat(event.remoteAddress()).isNotNull();
            assertThat(event.localAddress()).isNotNull();
            assertThat(event.toString()).contains("->");
        }).expectNextCount(3).thenCancel().verify(Duration.of(5, ChronoUnit.SECONDS));

        client.shutdown();
    }
}
