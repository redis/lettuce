/*
 * Copyright 2018 the original author or authors.
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
package io.lettuce.core.masterslave;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import reactor.util.function.Tuples;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;

/**
 * @author Mark Paluch
 */
@RunWith(MockitoJUnitRunner.class)
public class ConnectionsTest {

    @Mock
    private StatefulRedisConnection<String, String> connection1;

    @Before
    public void before() {
        when(connection1.closeAsync()).thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    public void shouldCloseConnectionCompletingAfterCloseSignal() {

        Connections connections = new Connections(5, Collections.emptyList());
        connections.closeAsync();

        verifyZeroInteractions(connection1);

        connections.onAccept(Tuples.of(RedisURI.create("localhost", 6379), connection1));

        verify(connection1).closeAsync();
    }
}
