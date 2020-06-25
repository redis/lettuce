/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.dynamic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import javax.inject.Inject;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.TestSupport;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.support.AsyncConnectionPoolSupport;
import io.lettuce.core.support.BoundedAsyncPool;
import io.lettuce.core.support.BoundedPoolConfig;
import io.lettuce.core.support.ConnectionPoolSupport;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.settings.TestSettings;

/**
 * @author Mark Paluch
 */
@ExtendWith(LettuceExtension.class)
class RedisCommandsIntegrationTests extends TestSupport {

    private final RedisClient client;

    private final RedisCommands<String, String> redis;

    @Inject
    RedisCommandsIntegrationTests(RedisClient client, StatefulRedisConnection<String, String> connection) {
        this.client = client;
        this.redis = connection.sync();
    }

    @Test
    void verifierShouldCatchMisspelledDeclarations() {

        RedisCommandFactory factory = new RedisCommandFactory(redis.getStatefulConnection());

        assertThat(factory).hasFieldOrPropertyWithValue("verifyCommandMethods", true);
        try {
            factory.getCommands(WithTypo.class);
            fail("Missing CommandCreationException");
        } catch (CommandCreationException e) {
            assertThat(e).hasMessageContaining("Command GAT does not exist.");
        }
    }

    @Test
    void disabledVerifierDoesNotReportTypo() {

        RedisCommandFactory factory = new RedisCommandFactory(redis.getStatefulConnection());
        factory.setVerifyCommandMethods(false);

        assertThat(factory.getCommands(WithTypo.class)).isNotNull();
    }

    @Test
    void doesNotFailIfCommandRetrievalFails() {

        StatefulRedisConnection connectionMock = Mockito.mock(StatefulRedisConnection.class);
        RedisCommands commandsMock = Mockito.mock(RedisCommands.class);

        when(connectionMock.sync()).thenReturn(commandsMock);
        doThrow(new RedisCommandExecutionException("ERR unknown command 'COMMAND'")).when(commandsMock).command();

        RedisCommandFactory factory = new RedisCommandFactory(connectionMock);

        assertThat(factory).hasFieldOrPropertyWithValue("verifyCommandMethods", false);
    }

    @Test
    void verifierShouldCatchTooFewParametersDeclarations() {

        RedisCommandFactory factory = new RedisCommandFactory(redis.getStatefulConnection());

        try {
            factory.getCommands(TooFewParameters.class);
            fail("Missing CommandCreationException");
        } catch (CommandCreationException e) {
            assertThat(e).hasMessageContaining("Command GET accepts 1 parameters but method declares 0 parameter");
        }
    }

    @Test
    void shouldWorkWithPooledConnection() throws Exception {

        GenericObjectPool<StatefulRedisConnection<String, String>> pool = ConnectionPoolSupport
                .createGenericObjectPool(client::connect, new GenericObjectPoolConfig<>());

        try (StatefulRedisConnection<String, String> connection = pool.borrowObject()) {

            RedisCommandFactory factory = new RedisCommandFactory(connection);
            SimpleCommands commands = factory.getCommands(SimpleCommands.class);
            commands.get("foo");
        }

        pool.close();
    }

    @Test
    void shouldWorkWithAsyncPooledConnection() {

        BoundedAsyncPool<StatefulRedisConnection<String, String>> pool = AsyncConnectionPoolSupport.createBoundedObjectPool(
                () -> client.connectAsync(StringCodec.ASCII, RedisURI.create(TestSettings.host(), TestSettings.port())),
                BoundedPoolConfig.create());

        try (StatefulRedisConnection<String, String> connection = pool.acquire().join()) {

            RedisCommandFactory factory = new RedisCommandFactory(connection);
            SimpleCommands commands = factory.getCommands(SimpleCommands.class);
            commands.get("foo");
        }

        pool.close();
    }

    private interface SimpleCommands extends Commands {

        String get(String key);

    }

    private interface TooFewParameters extends Commands {

        String get();

    }

    private interface WithTypo extends Commands {

        String gat(String key);

    }

}
