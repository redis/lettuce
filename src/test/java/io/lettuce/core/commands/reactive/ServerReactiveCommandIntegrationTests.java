/*
 * Copyright 2011-2022 the original author or authors.
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
package io.lettuce.core.commands.reactive;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.commands.ServerCommandIntegrationTests;
import io.lettuce.core.models.command.CommandDetail;
import io.lettuce.core.models.command.CommandDetailParser;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.test.ReactiveSyncInvocationHandler;

/**
 * Integration tests for {@link io.lettuce.core.api.reactive.RedisServerReactiveCommands}.
 *
 * @author Mark Paluch
 */
class ServerReactiveCommandIntegrationTests extends ServerCommandIntegrationTests {

    private RedisReactiveCommands<String, String> reactive;

    private final StatefulRedisConnection<String, String> connection;

    @Inject
    ServerReactiveCommandIntegrationTests(RedisClient client, StatefulRedisConnection<String, String> connection) {
        super(client, connection);
        this.reactive = connection.reactive();
        this.connection = connection;
    }

    protected RedisCommands<String, String> getCommands(StatefulRedisConnection<String, String> connection) {
        return ReactiveSyncInvocationHandler.sync(connection);
    }

    /**
     * Luckily these commands do not destroy anything in contrast to sync/async.
     */
    @Test
    void shutdown() {
        reactive.shutdown(true);
        assertThat(connection.isOpen()).isTrue();
    }

    @Test
    void debugOom() {
        reactive.debugOom();
        assertThat(connection.isOpen()).isTrue();
    }

    @Test
    void debugSegfault() {
        reactive.debugSegfault();
        assertThat(connection.isOpen()).isTrue();
    }

    @Test
    void debugRestart() {
        reactive.debugRestart(1L);
        assertThat(connection.isOpen()).isTrue();
    }

    @Test
    void migrate() {
        reactive.migrate("host", 1234, "key", 1, 10);
        assertThat(connection.isOpen()).isTrue();
    }

    @Test
    @Override
    public void commandInfo() {

        List<Object> result = reactive.commandInfo(CommandType.GETRANGE, CommandType.SET).collectList().block();

        assertThat(result).hasSize(2);

        List<CommandDetail> commands = CommandDetailParser.parse(result);
        assertThat(commands).hasSameSizeAs(result);
    }

}
