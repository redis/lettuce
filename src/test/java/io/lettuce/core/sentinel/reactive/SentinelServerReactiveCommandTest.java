/*
 * Copyright 2016-2018 the original author or authors.
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
package io.lettuce.core.sentinel.reactive;

import static io.lettuce.test.settings.TestSettings.hostAddr;
import static org.assertj.core.api.Assertions.assertThat;

import io.lettuce.core.sentinel.SentinelServerCommandTest;
import io.lettuce.core.sentinel.api.async.RedisSentinelAsyncCommands;
import io.lettuce.core.sentinel.api.reactive.RedisSentinelReactiveCommands;
import io.lettuce.test.ReactiveSyncInvocationHandler;
import io.lettuce.test.settings.TestSettings;

/**
 * @author Mark Paluch
 */
public class SentinelServerReactiveCommandTest extends SentinelServerCommandTest {

    @Override
    public void openConnection() {

        RedisSentinelAsyncCommands<String, String> async = sentinelClient.connectSentinel().async();
        RedisSentinelReactiveCommands<String, String> reactive = async.getStatefulConnection().reactive();
        sentinel = ReactiveSyncInvocationHandler.sync(async.getStatefulConnection());

        try {
            sentinel.master(MASTER_ID);
        } catch (Exception e) {
            sentinelRule.monitor(MASTER_ID, hostAddr(), TestSettings.port(3), 1, true);
        }

        assertThat(reactive.isOpen()).isTrue();
        assertThat(reactive.getStatefulConnection()).isSameAs(async.getStatefulConnection());
    }
}
