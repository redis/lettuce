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
package com.lambdaworks.redis.commands.rx;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import com.lambdaworks.redis.api.rx.RedisReactiveCommands;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.commands.ServerCommandTest;

public class ServerRxCommandTest extends ServerCommandTest {

    private RedisReactiveCommands<String, String> reactive;

    @Before
    public void openConnection() throws Exception {
        super.openConnection();
        reactive = redis.getStatefulConnection().reactive();
    }

    @Override
    protected RedisCommands<String, String> connect() {
        return RxSyncInvocationHandler.sync(client.connectAsync().getStatefulConnection());
    }

    /**
     * Luckily these commands do not destroy anything in contrast to sync/async.
     */
    @Test
    public void shutdown() {
        reactive.shutdown(true);
        assertThat(reactive.isOpen()).isTrue();
    }

    @Test
    public void debugOom() {
        reactive.debugOom();
        assertThat(reactive.isOpen()).isTrue();
    }

    @Test
    public void debugSegfault() {
        reactive.debugSegfault();
        assertThat(reactive.isOpen()).isTrue();
    }

    @Test
    public void debugRestart() {
        reactive.debugRestart(1L);
        assertThat(reactive.isOpen()).isTrue();
    }

    @Test
    public void migrate() throws Exception {
        reactive.migrate("host", 1234, "key", 1, 10);
        assertThat(reactive.isOpen()).isTrue();
    }
}
