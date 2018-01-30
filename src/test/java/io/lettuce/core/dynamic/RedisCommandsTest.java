/*
 * Copyright 2011-2018 the original author or authors.
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
package io.lettuce.core.dynamic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mockito.Mockito;

import io.lettuce.core.AbstractRedisClientTest;
import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

/**
 * @author Mark Paluch
 */
public class RedisCommandsTest extends AbstractRedisClientTest {

    @Test
    public void verifierShouldCatchMisspelledDeclarations() {

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
    public void disabledVerifierDoesNotReportTypo() {

        RedisCommandFactory factory = new RedisCommandFactory(redis.getStatefulConnection());
        factory.setVerifyCommandMethods(false);

        assertThat(factory.getCommands(WithTypo.class)).isNotNull();
    }

    @Test
    public void doesNotFailIfCommandRetrievalFails() {

        StatefulRedisConnection connectionMock = Mockito.mock(StatefulRedisConnection.class);
        RedisCommands commandsMock = Mockito.mock(RedisCommands.class);

        when(connectionMock.sync()).thenReturn(commandsMock);
        doThrow(new RedisCommandExecutionException("ERR unknown command 'COMMAND'")).when(commandsMock).command();

        RedisCommandFactory factory = new RedisCommandFactory(connectionMock);

        assertThat(factory).hasFieldOrPropertyWithValue("verifyCommandMethods", false);
    }

    @Test
    public void verifierShouldCatchTooFewParametersDeclarations() {

        RedisCommandFactory factory = new RedisCommandFactory(redis.getStatefulConnection());

        try {
            factory.getCommands(TooFewParameters.class);
            fail("Missing CommandCreationException");
        } catch (CommandCreationException e) {
            assertThat(e).hasMessageContaining("Command GET accepts 1 parameters but method declares 0 parameter");
        }
    }

    interface TooFewParameters extends Commands {

        String get();

    }

    interface WithTypo extends Commands {

        String gat(String key);

    }
}
