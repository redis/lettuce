package io.lettuce.core

import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.coroutines
import io.lettuce.test.LettuceExtension
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.extension.ExtendWith
import javax.inject.Inject

/**
 * Integration tests for Coroutines.
 *
 * @author Mark Paluch
 */
@ExtendWith(LettuceExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@OptIn(ExperimentalLettuceCoroutinesApi::class)
internal class CoroutinesIntegrationTests @Inject constructor(private val connection: StatefulRedisConnection<String, String>) :
    TestSupport() {

    @Test
    @Timeout(10)
    fun shouldRepeatCoroutinesFlowExternalLoop() {

        // The usage of 128 here is meaningful, as mentioned above this is only
        // observed with multiples of 64 in the size of the requested set of keys.
        val array = Array(128) { "111" }

        repeat(1000) {
            runBlocking {

                connection.coroutines().mget(
                    *array
                ).mapNotNull { result ->
                    if (result.hasValue()) {
                        result.value as String
                    } else null
                }.toList()
            }
        }
    }

    @Test
    @Timeout(10)
    fun shouldRepeatCoroutinesFlowInternalLoop() {

        // The usage of 128 here is meaningful, as mentioned above this is only
        // observed with multiples of 64 in the size of the requested set of keys.
        val array = Array(128) { "111" }

        runBlocking {
            repeat(1000) {
                connection.coroutines().mget(
                    *array
                ).mapNotNull { result ->
                    if (result.hasValue()) {
                        result.value as String
                    } else null
                }.toList()
            }
        }
    }
}
