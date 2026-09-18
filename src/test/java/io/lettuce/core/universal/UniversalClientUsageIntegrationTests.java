package io.lettuce.core.universal;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.lettuce.core.RedisURI;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTestSettings;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.reactive.RedisAdvancedClusterReactiveCommands;
import io.lettuce.core.protocol.ProtocolVersion;
import io.lettuce.test.settings.TestSettings;

/**
 * Presentable usage of the universal client: the two positive paths, the "mode unknown" path, and user-side misuses. Standalone
 * and OSS cluster only.
 */
@Tag(INTEGRATION_TEST)
class UniversalClientUsageIntegrationTests {

    private static final Duration T = Duration.ofSeconds(5);

    private static RedisURI standalone() {
        return RedisURI.Builder.redis(TestSettings.host(), TestSettings.port()).build();
    }

    private static RedisURI cluster() {
        return RedisURI.Builder.redis(ClusterTestSettings.host, ClusterTestSettings.port1).build();
    }

    // ---------------------------------------------------------------- positive

    @Test
    void standalone_sameClientCode() {

        // One seed, no topology stated. Options are optional; RESP3 is the default.
        UniversalClient client = UniversalClient.create(standalone());

        // First connect probes: a plain connection to the seed; the HELLO reply already carries mode=standalone.
        // Standalone → the probe IS the connection, nothing else is opened. Mode is cached on the client.
        try (StatefulRedisUniversalConnection<String, String> conn = client.connect()) {

            assertThat(conn.isCluster()).isFalse();

            // The factory a user would pass to a plain RedisClient connection. Unchanged.
            RedisReactiveCommands<String, String> cmds = conn.commands(RedisReactiveCommands.factory());

            cmds.set("usage:standalone", "alice").block(T); // one socket, no routing
            assertThat(cmds.get("usage:standalone").block(T)).isEqualTo("alice");
            assertThat(cmds.select(1).block(T)).isEqualTo("OK"); // standalone-only commands are available

            // Second connect: cached mode, no probe, straight to a plain connection.
            try (StatefulRedisUniversalConnection<String, String> second = client.connect()) {
                assertThat(second.isCluster()).isFalse();
            }
        } finally {
            client.shutdown();
        }
    }

    @Test
    void cluster_sameClientCode() {

        UniversalClient client = UniversalClient.create(cluster());

        // Probe says mode=cluster → probe closed, partitions loaded once, routed cluster connection opened.
        // From here on this is exactly the connection RedisClusterClient.connect() would return.
        try (StatefulRedisUniversalConnection<String, String> conn = client.connect()) {

            assertThat(conn.isCluster()).isTrue();

            // The factory a user would pass to a RedisClusterClient connection. Unchanged.
            RedisAdvancedClusterReactiveCommands<String, String> cmds = conn
                    .commands(RedisAdvancedClusterReactiveCommands.factory());

            cmds.set("usage:cluster:a", "alice").block(T); // CRC16 → slot → owning node
            cmds.set("usage:cluster:b", "bob").block(T);
            assertThat(cmds.get("usage:cluster:a").block(T)).isEqualTo("alice");
            assertThat(cmds.del("usage:cluster:a", "usage:cluster:b").block(T)).isEqualTo(2L); // different slots: split per
                                                                                               // node, merged
            assertThat(cmds.dbsize().block(T)).isNotNull(); // keyless: fan-out to all masters, summed
        } finally {
            client.shutdown();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "standalone", "cluster" })
    void modeUnknownUpFront(String target) {

        // The app has a URI from config and does not know what is behind it. One branch on isCluster().
        UniversalClient client = UniversalClient.create("cluster".equals(target) ? cluster() : standalone());

        try (StatefulRedisUniversalConnection<String, String> conn = client.connect()) {

            if (conn.isCluster()) {
                conn.commands(RedisAdvancedClusterReactiveCommands.factory()).set("usage:unknown", "v").block(T);
                assertThat(conn.commands(RedisAdvancedClusterReactiveCommands.factory()).get("usage:unknown").block(T))
                        .isEqualTo("v");
            } else {
                conn.commands(RedisReactiveCommands.factory()).set("usage:unknown", "v").block(T);
                assertThat(conn.commands(RedisReactiveCommands.factory()).get("usage:unknown").block(T)).isEqualTo("v");
            }
        } finally {
            client.shutdown();
        }
    }

    // ---------------------------------------------------------------- misuses

    @Test
    void misuse_clusterFactoryOnStandalone() {

        UniversalClient client = UniversalClient.create(standalone());

        try (StatefulRedisUniversalConnection<String, String> conn = client.connect()) {

            // Server is standalone, user asks for the cluster API. Refused on the first commands() call, before any
            // command is sent. The message names what was detected.
            assertThatThrownBy(() -> conn.commands(RedisAdvancedClusterReactiveCommands.factory()))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("STANDALONE");
        } finally {
            client.shutdown();
        }
    }

    @Test
    void misuse_standaloneFactoryOnCluster() {

        UniversalClient client = UniversalClient.create(cluster());

        try (StatefulRedisUniversalConnection<String, String> conn = client.connect()) {

            // Server is a cluster, user asks for the standalone API. The standalone API promises select() and multi(),
            // which cannot work here, so the facade refuses instead of letting the server fail later.
            assertThatThrownBy(() -> conn.commands(RedisReactiveCommands.factory())).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("CLUSTER");
        } finally {
            client.shutdown();
        }
    }

    @Test
    void misuse_treatingTheFacadeAsAConcreteConnection() {

        UniversalClient client = UniversalClient.create(standalone());

        try (StatefulRedisUniversalConnection<String, String> conn = client.connect()) {

            // The universal connection is neither of the two existing connection types, in either mode. Code that
            // switches on the concrete type (pooling helpers, framework adapters) will not match it.
            assertThat(conn).isNotInstanceOf(StatefulRedisConnection.class);
            assertThat(conn).isNotInstanceOf(StatefulRedisClusterConnection.class);

            // ...so this cast is the misuse, and it fails even though the server IS standalone.
            Object asObject = conn;
            assertThatThrownBy(() -> {
                @SuppressWarnings("unchecked")
                StatefulRedisConnection<String, String> c = (StatefulRedisConnection<String, String>) asObject;
                c.isMulti();
            }).isInstanceOf(ClassCastException.class);
        } finally {
            client.shutdown();
        }
    }

    @Test
    void misuse_expectingModeToChangeAtRuntime() {

        UniversalClient client = UniversalClient.create(standalone());

        try (StatefulRedisUniversalConnection<String, String> conn = client.connect()) {

            // Mode is detected once per client and fixed. Polling isCluster() to react to a server-side flip does
            // nothing; a flip surfaces as MOVED errors (standalone→cluster) or a failing topology refresh
            // (cluster→standalone) and needs a new client. Flips are out of scope for v1.
            assertThat(conn.isCluster()).isFalse();
            try (StatefulRedisUniversalConnection<String, String> again = client.connect()) {
                assertThat(again.isCluster()).isFalse(); // same answer, no re-probe
            }
        } finally {
            client.shutdown();
        }
    }

    @Test
    void misuse_changingOptionsAfterFirstConnect() {

        UniversalClient client = UniversalClient.create(standalone());

        try (StatefulRedisUniversalConnection<String, String> first = client.connect()) {

            // Options apply to connections opened AFTER the call, exactly as with RedisClient / RedisClusterClient.
            // The already-open connection stays on RESP3; only the next connect negotiates RESP2.
            client.setOptions(ClusterClientOptions.builder().protocolVersion(ProtocolVersion.RESP2).build());

            assertThat(first.getOptions().getProtocolVersion()).isEqualTo(ProtocolVersion.RESP3);
            try (StatefulRedisUniversalConnection<String, String> second = client.connect()) {
                assertThat(second.getOptions().getProtocolVersion()).isEqualTo(ProtocolVersion.RESP2);
            }
        } finally {
            client.shutdown();
        }
    }

    @Test
    void misuse_mixedSeeds() {

        // First seed is standalone, second is a cluster node. Detection asks the FIRST reachable seed, so this client is
        // standalone and the cluster seed is never used. Seeds must describe one deployment.
        UniversalClient client = UniversalClient.create(standalone(), cluster());

        try (StatefulRedisUniversalConnection<String, String> conn = client.connect()) {
            assertThat(conn.isCluster()).isFalse();
        } finally {
            client.shutdown();
        }
    }

    @Test
    void misuse_expectingClusterRulesToBeSoftened() {

        UniversalClient client = UniversalClient.create(cluster());

        try (StatefulRedisUniversalConnection<String, String> conn = client.connect()) {

            RedisAdvancedClusterReactiveCommands<String, String> cmds = conn
                    .commands(RedisAdvancedClusterReactiveCommands.factory());

            // The universal client does not change cluster semantics. Same as RedisClusterClient today:
            // cmds.select(1); ← does not compile, not on the cluster API
            // cmds.multi(); ← does not compile
            // A script touching keys on different slots is still rejected by the server:
            assertThatThrownBy(() -> cmds.eval("return 1", ScriptOutputType.INTEGER, "usage:x", "usage:y").blockFirst(T))
                    .hasMessageContaining("CROSSSLOT");
        } finally {
            client.shutdown();
        }
    }

}
