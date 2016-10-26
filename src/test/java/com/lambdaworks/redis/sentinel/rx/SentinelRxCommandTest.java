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
package com.lambdaworks.redis.sentinel.rx;

import static com.lambdaworks.redis.TestSettings.hostAddr;
import static org.assertj.core.api.Assertions.assertThat;

import com.lambdaworks.redis.TestSettings;
import com.lambdaworks.redis.commands.rx.RxSyncInvocationHandler;
import com.lambdaworks.redis.sentinel.SentinelCommandTest;
import com.lambdaworks.redis.sentinel.api.async.RedisSentinelAsyncCommands;
import com.lambdaworks.redis.sentinel.api.rx.RedisSentinelReactiveCommands;

/**
 * @author Mark Paluch
 */
public class SentinelRxCommandTest extends SentinelCommandTest {

    @Override
    public void openConnection() throws Exception {

        RedisSentinelAsyncCommands<String, String> async = sentinelClient.connectSentinelAsync();
        RedisSentinelReactiveCommands<String, String> reactive = async.getStatefulConnection().reactive();
        sentinel = RxSyncInvocationHandler.sync(async.getStatefulConnection());

        try {
            sentinel.master(MASTER_ID);
        } catch (Exception e) {
            sentinelRule.monitor(MASTER_ID, hostAddr(), TestSettings.port(3), 1, true);
        }

        assertThat(reactive.isOpen()).isTrue();
        assertThat(reactive.getStatefulConnection()).isSameAs(async.getStatefulConnection());
    }
}
