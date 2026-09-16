package io.lettuce.core.api.coroutines

import io.lettuce.TestTags
import io.lettuce.core.Consumer
import io.lettuce.core.KeyValue
import io.lettuce.core.RedisClient
import io.lettuce.core.StreamDeletionPolicy
import io.lettuce.core.TestSupport
import io.lettuce.core.XGroupCreateArgs
import io.lettuce.core.XReadArgs.StreamOffset
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.coroutines
import io.lettuce.core.cluster.RedisClusterClient
import io.lettuce.core.cluster.api.coroutines
import io.lettuce.core.models.stream.StreamEntryDeletionResult
import io.lettuce.core.sentinel.SentinelTestSettings
import io.lettuce.core.sentinel.api.coroutines
import io.lettuce.test.LettuceExtension
import io.lettuce.test.condition.EnabledOnCommand
import io.lettuce.test.condition.RedisConditions
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import javax.inject.Inject

/**
 * Integration tests for Kotlin Coroutine extensions.
 *
 * @author Mark Paluch
 */
@Tag(TestTags.INTEGRATION_TEST)
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
    @EnabledOnCommand("HGETEX")
    @Inject
    internal fun shouldInvokeHashFieldExpiryCoroutines(connection: StatefulRedisConnection<String, String>) {

        runBlocking {

            val api = connection.coroutines()
            api.del(key)

            assertThat(api.hsetex(key, mapOf("one" to "1", "two" to "2"))).isEqualTo(1L)
            assertThat(api.hgetex(key, "one", "two").toList()).containsExactly(
                KeyValue.just("one", "1"),
                KeyValue.just("two", "2")
            )
            assertThat(api.hgetdel(key, "one").toList()).containsExactly(KeyValue.just("one", "1"))
            assertThat(api.hget(key, "one")).isNull()

            api.del(key)
        }
    }

    @Test
    @Inject
    internal fun shouldInvokeBitopCoroutines(connection: StatefulRedisConnection<String, String>) {

        assumeTrue(RedisConditions.of(connection).hasVersionGreaterOrEqualsTo("8.1.240"))

        runBlocking {

            val api = connection.coroutines()
            api.del(key, "one", "two")

            // one has bits {0, 1}, two has bit {1}
            api.setbit("one", 0L, 1)
            api.setbit("one", 1L, 1)
            api.setbit("two", 1L, 1)

            assertThat(api.bitopDiff(key, "one", "two")).isEqualTo(1L) // {0}
            assertThat(api.bitcount(key)).isEqualTo(1L)

            assertThat(api.bitopDiff1(key, "one", "two")).isEqualTo(1L) // {}
            assertThat(api.bitcount(key)).isEqualTo(0L)

            assertThat(api.bitopAndor(key, "one", "two")).isEqualTo(1L) // {1}
            assertThat(api.bitcount(key)).isEqualTo(1L)

            assertThat(api.bitopOne(key, "one", "two")).isEqualTo(1L) // {0}
            assertThat(api.bitcount(key)).isEqualTo(1L)

            api.del(key, "one", "two")
        }
    }

    @Test
    @EnabledOnCommand("XACKDEL") // Redis 8.2
    @Inject
    internal fun shouldInvokeStreamDeletionCoroutines(connection: StatefulRedisConnection<String, String>) {

        runBlocking {

            val api = connection.coroutines()
            api.del(key)

            val id1 = api.xadd(key, mapOf("field1" to "value1"))!!
            val id2 = api.xadd(key, mapOf("field2" to "value2"))!!
            val id3 = api.xadd(key, mapOf("field3" to "value3"))!!

            api.xgroupCreate(StreamOffset.from(key, "0-0"), "group", XGroupCreateArgs.Builder.mkstream())
            assertThat(api.xreadgroup(Consumer.from("group", "consumer"), StreamOffset.lastConsumed(key)).toList())
                .hasSize(3)

            assertThat(api.xackdel(key, "group", id1).toList()).containsExactly(StreamEntryDeletionResult.DELETED)
            assertThat(api.xackdel(key, "group", StreamDeletionPolicy.DELETE_REFERENCES, id2).toList())
                .containsExactly(StreamEntryDeletionResult.DELETED)

            assertThat(api.xdelex(key, id3).toList()).containsExactly(StreamEntryDeletionResult.DELETED)
            assertThat(api.xdelex(key, StreamDeletionPolicy.KEEP_REFERENCES, "999999-0").toList())
                .containsExactly(StreamEntryDeletionResult.NOT_FOUND)

            api.del(key)
        }
    }

    @Test
    @EnabledOnCommand("EXPIRETIME") // Redis 7.0
    @Inject
    internal fun shouldInvokeCoroutineCorrectlyForSentinel(client: RedisClient) {

        val connection = client.connectSentinel(SentinelTestSettings.SENTINEL_URI)

        runBlocking {

            val api = connection.coroutines()

            assertThat(api.master(SentinelTestSettings.MASTER_ID)).isNotEmpty
            assertThat(api.replicas(SentinelTestSettings.MASTER_ID)).isNotEmpty
        }

        connection.close()
    }
}
