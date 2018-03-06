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
package io.lettuce.core.cluster.topology;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;

/**
 * @author Christian Weitendorf
 * @author Mark Paluch
 */
@RunWith(MockitoJUnitRunner.class)
public class ConnectionsTest {

    @Mock
    private StatefulRedisConnection<String, String> connection1;

    @Mock
    private StatefulRedisConnection<String, String> connection2;

    @Before
    public void before() throws Exception {

        when(connection1.closeAsync()).thenReturn(CompletableFuture.completedFuture(null));
        when(connection2.closeAsync()).thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    public void shouldCloseAllConnections() {

        Connections sut = new Connections();
        sut.addConnection(RedisURI.create("127.0.0.1", 7380), connection1);
        sut.addConnection(RedisURI.create("127.0.0.1", 7381), connection2);

        sut.close();

        verify(connection1).closeAsync();
        verify(connection2).closeAsync();
    }

    @Test
    public void shouldCloseAllConnectionsAfterCloseSignal() {

        Connections sut = new Connections();
        sut.close();
        verifyZeroInteractions(connection1);

        sut.addConnection(RedisURI.create("127.0.0.1", 7381), connection1);
        verify(connection1).closeAsync();
    }
}
