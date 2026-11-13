package io.lettuce.core

import io.lettuce.TestTags
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.coroutines
import io.lettuce.test.LettuceExtension
import io.lettuce.test.condition.EnabledOnCommand
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Tag
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
@Tag(TestTags.INTEGRATION_TEST)
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

    /**
     * Scenario ZMOV-C03: through the coroutine API a move from a missing source returns `null`, a member move returns the
     * moved pair, and a blocking move that times out returns `null`.
     */
    @Test
    @EnabledOnCommand("BZMOVE")
    fun shouldMoveSortedSetMembersWithCoroutines() {

        runBlocking {
            val api = connection.coroutines()
            api.del("{z}s", "{z}d")

            assertThat(api.zmove("{z}s", "{z}d", ZMoveArgs.Builder.min())).isNull()

            api.zadd("{z}s", 1.0, "a", 2.0, "b", 3.0, "c")
            assertThat(api.zmovemember("{z}s", "{z}d", "a")).containsExactly(ScoredValue.just(1.0, "a"))

            api.del("{z}s", "{z}d")
            assertThat(api.bzmove("{z}s", "{z}d", BZMoveArgs.Builder.min().timeout(0.1))).isNull()
        }
    }
}
