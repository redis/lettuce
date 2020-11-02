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
package io.lettuce.core

import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.coroutines
import io.lettuce.test.LettuceExtension
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import javax.inject.Inject

/**
 * Integration tests for [ScanFlow].
 *
 * @author Mikhael Sokolov
 * @author Mark Paluch
 */
@ExtendWith(LettuceExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ScanFlowIntegrationTests @Inject constructor(private val connection: StatefulRedisConnection<String, String>) : TestSupport() {

    val iterations = 300

    @BeforeEach
    fun setUp() {
        connection.sync().flushall()
    }

    @Test
    fun `should scan iteratively`() = runBlocking<Unit> {
        with(connection.coroutines()) {
            repeat(iterations) {
                set("key - $it", value)
            }
            assertThat(ScanFlow.scan(this, ScanArgs.Builder.limit(200)).take(250).toList()).hasSize(250)
            assertThat(ScanFlow.scan(this).count()).isEqualTo(iterations)
        }
    }

    @Test
    fun `should hscan iteratively`() = runBlocking<Unit> {
        with(connection.coroutines()) {
            repeat(iterations) {
                hset(key, "field-$it", "value-$it")
            }

            assertThat(ScanFlow.hscan(this, key, ScanArgs.Builder.limit(200)).take(250).toList()).hasSize(250)
            assertThat(ScanFlow.hscan(this, key).count()).isEqualTo(iterations)
        }
    }

    @Test
    fun shouldSscanIteratively() = runBlocking<Unit> {
        with(connection.coroutines()) {
            repeat(iterations) {
                sadd(key, "value-$it")
            }

            assertThat(ScanFlow.sscan(this, key, ScanArgs.Builder.limit(200)).take(250).toList()).hasSize(250)
            assertThat(ScanFlow.sscan(this, key).count()).isEqualTo(iterations)
        }
    }

    @Test
    fun shouldZscanIteratively() = runBlocking<Unit> {
        with(connection.coroutines()) {
            repeat(iterations) {
                zadd(key, 1001.0, "value-$it")
            }

            assertThat(ScanFlow.zscan(this, key, ScanArgs.Builder.limit(200)).take(250).toList()).hasSize(250)
            assertThat(ScanFlow.zscan(this, key).count()).isEqualTo(iterations)
        }
    }
}
