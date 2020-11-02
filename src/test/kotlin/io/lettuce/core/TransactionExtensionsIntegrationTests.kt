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
package io.lettuce.core;

import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.multi
import io.lettuce.core.api.sync.multi
import io.lettuce.test.LettuceExtension
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import javax.inject.Inject

/**
 * Integration tests for Kotlin Transaction closure extensions.
 *
 * @author Mark Paluch
 * @author Mikhael Sokolov
 */
@ExtendWith(LettuceExtension::class)
class TransactionExtensionsIntegrationTests : TestSupport() {

    @BeforeEach
    @Inject
    internal fun setUp(connection: StatefulRedisConnection<String, String>) {
        connection.sync().flushall()
    }

    @Test
    @Inject
    internal fun shouldApplyMultiClosure(connection: StatefulRedisConnection<String, String>) {

        val transactionResult = connection.sync().multi {
            set("key", "value")
            get("key")
        }

        assertThat(transactionResult.get(0) as String).isEqualTo("OK")
        assertThat(transactionResult.get(1) as String).isEqualTo("value")
    }

    @Test
    @Inject
    internal fun shouldApplyMultiClosureOverAsync(connection: StatefulRedisConnection<String, String>) {

        runBlocking {

            val transactionResult = connection.async().multi {
                set("key", "value")
                get("key")
            }

            assertThat(transactionResult.get(0) as String).isEqualTo("OK")
            assertThat(transactionResult.get(1) as String).isEqualTo("value")
        }
    }

    @Test
    @Inject
    internal fun shouldDiscardMultiClosureOverAsync(connection: StatefulRedisConnection<String, String>) {

        runBlocking {
            val transactionResult = runCatching {
                connection.async().multi {
                    set("key", "value")
                    throw RedisCommandExecutionException("oops")
                }
            }

            assertThat(transactionResult.isFailure).isTrue()
            assertThat(transactionResult.exceptionOrNull()).isInstanceOf(RedisCommandExecutionException::class.java)
            assertThat(connection.async().get("key").await()).isNull()
        }
    }

}
