/*
 * Copyright 2017 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.Utf8StringCodec;
import io.lettuce.core.models.role.RedisInstance;

/**
 * @author Mark Paluch
 */
@RunWith(MockitoJUnitRunner.class)
public class MasterSlaveConnectionProviderTest {

    public static final Utf8StringCodec CODEC = new Utf8StringCodec();

    private MasterSlaveConnectionProvider<String, String> sut;

    @Mock
    RedisClient clientMock;

    @Mock
    StatefulRedisConnection<String, String> nodeConnectionMock;

    @Mock
    RedisCommands<String, String> commandsMock;

    @Before
    public void before() {

        sut = new MasterSlaveConnectionProvider<>(clientMock, CODEC, RedisURI.create("localhost", 1), Collections.emptyMap());
        sut.setKnownNodes(Arrays.asList(new RedisMasterSlaveNode("localhost", 1, RedisURI.create("localhost", 1),
                RedisInstance.Role.MASTER)));
    }

    @Test
    public void shouldCloseConnections() {

        when(clientMock.connect(eq(CODEC), any())).thenReturn(nodeConnectionMock);

        StatefulRedisConnection<String, String> connection = sut.getConnection(MasterSlaveConnectionProvider.Intent.READ);
        assertThat(connection).isNotNull();

        sut.close();

        verify(nodeConnectionMock).close();
    }

}
