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
package io.lettuce.core.commands.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.commands.ServerCommandTest;
import io.lettuce.util.ReactiveSyncInvocationHandler;

/**
 * @author Mark Paluch
 */
public class ServerReactiveCommandTest extends ServerCommandTest {

    private RedisReactiveCommands<String, String> reactive;

    @Before
    public void openConnection() throws Exception {
        super.openConnection();
        reactive = redis.getStatefulConnection().reactive();
    }

    @Override
    protected RedisCommands<String, String> connect() {
        return ReactiveSyncInvocationHandler.sync(client.connect());
    }

    /**
     * Luckily these commands do not destroy anything in contrast to sync/async.
     */
    @Test
    public void shutdown() throws Exception {
        reactive.shutdown(true);
        assertThat(reactive.getStatefulConnection().isOpen()).isTrue();
    }

    @Test
    public void debugOom() throws Exception {
        reactive.debugOom();
        assertThat(reactive.getStatefulConnection().isOpen()).isTrue();
    }

    @Test
    public void debugSegfault() throws Exception {
        reactive.debugSegfault();
        assertThat(reactive.getStatefulConnection().isOpen()).isTrue();
    }

    @Test
    public void debugRestart() throws Exception {
        reactive.debugRestart(1L);
        assertThat(reactive.getStatefulConnection().isOpen()).isTrue();
    }

    @Test
    public void migrate() throws Exception {
        reactive.migrate("host", 1234, "key", 1, 10);
        assertThat(reactive.getStatefulConnection().isOpen()).isTrue();
    }
}
