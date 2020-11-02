/*
 * Copyright 2020 the original author or authors.
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
package io.lettuce.core.api.coroutines

import io.lettuce.core.RedisClient
import io.lettuce.core.TestSupport
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.coroutines
import io.lettuce.core.cluster.RedisClusterClient
import io.lettuce.core.cluster.api.coroutines
import io.lettuce.core.sentinel.SentinelTestSettings
import io.lettuce.core.sentinel.api.coroutines
import io.lettuce.test.LettuceExtension
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import javax.inject.Inject

/**
 * Integration tests for Kotlin Coroutine extensions.
 *
 * @author Mark Paluch
 */
@ExtendWith(LettuceExtension::class)
class CoroutinesIntegrationTests : TestSupport() {

    @Test
    @Inject
    internal fun shouldInvokeCoroutineCorrectlyForStandalone(connection: StatefulRedisConnection<String, String>) {

        runBlocking {

            val api = connection.coroutines()
            api.set("key", "value")

            assertThat(api.get("key")).isEqualTo("value")
        }
    }

    @Test
    @Inject
    internal fun shouldInvokeCoroutineCorrectlyForCluster(client: RedisClusterClient) {

        val connection = client.connect();
        runBlocking {

            val api = connection.coroutines()
            api.set("key", "value")

            assertThat(api.get("key")).isEqualTo("value")
        }

        connection.close();
    }

    @Test
    @Inject
    internal fun shouldInvokeCoroutineCorrectlyForSentinel(client: RedisClient) {

        val connection = client.connectSentinel(SentinelTestSettings.SENTINEL_URI)

        runBlocking {

            val api = connection.coroutines()

            assertThat(api.master(SentinelTestSettings.MASTER_ID)).isNotEmpty
            assertThat(api.slaves(SentinelTestSettings.MASTER_ID)).isNotEmpty
        }

        connection.close()
    }
}
