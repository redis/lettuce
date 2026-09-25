package io.lettuce.core.universal;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import io.lettuce.core.RedisURI;
import io.lettuce.core.StatefulRedisConnectionImpl;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTestSettings;
import io.lettuce.core.cluster.api.reactive.RedisAdvancedClusterReactiveCommands;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.ProtocolVersion;
import io.lettuce.test.settings.TestSettings;

/**
 * PoC: same client code against a standalone server and an OSS cluster, using the existing reactive factories. Runs under RESP3
 * (mode from the {@code HELLO} handshake) and RESP2 (mode from {@code INFO server}).
 */
@Tag(INTEGRATION_TEST)
class UniversalClientIntegrationTests {

    private static final Duration T = Duration.ofSeconds(5);

    @ParameterizedTest
    @EnumSource(ProtocolVersion.class)
    void standalone(ProtocolVersion protocol) throws Exception {

        UniversalClient client = UniversalClient
                .create(RedisURI.Builder.redis(TestSettings.host(), TestSettings.port()).build());
        client.setOptions(ClusterClientOptions.builder().protocolVersion(protocol).build());

        try (StatefulRedisUniversalConnection<String, String> conn = client.connect()) {

            assertThat(conn.getTopologyMode()).isEqualTo(TopologyMode.STANDALONE);
            assertThat(conn.isCluster()).isFalse();

            // the probe connection was kept as the delegate; detection ran on the negotiated protocol
            StatefulRedisConnectionImpl<String, String> delegate = (StatefulRedisConnectionImpl<String, String>) ((StatefulRedisUniversalConnectionImpl<String, String>) conn)
                    .getDelegate();
            assertThat(delegate.getConnectionState().getNegotiatedProtocolVersion()).isEqualTo(protocol);

            // existing standalone factory, unchanged
            RedisReactiveCommands<String, String> cmds = conn.commands(RedisReactiveCommands.factory());
            assertThat(cmds.set("universal:standalone", "alice").block(T)).isEqualTo("OK");
            assertThat(cmds.get("universal:standalone").block(T)).isEqualTo("alice");
            assertThat(cmds.select(1).block(T)).isEqualTo("OK"); // only meaningful on standalone

            // cached per factory key by the delegate
            assertThat(conn.commands(RedisReactiveCommands.factory())).isSameAs(cmds);

            // wrong flavour fails fast
            assertThatThrownBy(() -> conn.commands(RedisAdvancedClusterReactiveCommands.factory()))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("STANDALONE");

            // second connect, async path: mode is cached on the client, no probe
            try (StatefulRedisUniversalConnection<String, String> again = client.connectAsync(StringCodec.UTF8).get(5,
                    TimeUnit.SECONDS)) {
                assertThat(again.getTopologyMode()).isEqualTo(TopologyMode.STANDALONE);
            }
        } finally {
            client.shutdown();
        }
    }

    @ParameterizedTest
    @EnumSource(ProtocolVersion.class)
    void cluster(ProtocolVersion protocol) throws Exception {

        UniversalClient client = UniversalClient
                .create(RedisURI.Builder.redis(ClusterTestSettings.host, ClusterTestSettings.port1).build());
        client.setOptions(ClusterClientOptions.builder().protocolVersion(protocol).build());

        try (StatefulRedisUniversalConnection<String, String> conn = client.connect()) {

            assertThat(conn.getTopologyMode()).isEqualTo(TopologyMode.CLUSTER);
            assertThat(conn.isCluster()).isTrue();
            assertThat(conn.getOptions().getProtocolVersion()).isEqualTo(protocol);

            // existing cluster factory, unchanged: slot routing, fan-out, partitioned multi-key all come from the real impl
            RedisAdvancedClusterReactiveCommands<String, String> cmds = conn
                    .commands(RedisAdvancedClusterReactiveCommands.factory());
            assertThat(cmds.set("universal:cluster:a", "1").block(T)).isEqualTo("OK");
            assertThat(cmds.set("universal:cluster:b", "2").block(T)).isEqualTo("OK");
            assertThat(cmds.get("universal:cluster:a").block(T)).isEqualTo("1");
            assertThat(cmds.del("universal:cluster:a", "universal:cluster:b").block(T)).isEqualTo(2L); // cross-slot,
                                                                                                       // partitioned
            assertThat(cmds.dbsize().block(T)).isNotNull(); // fan-out, aggregated
            assertThat(cmds.getStatefulConnection().getPartitions()).isNotEmpty(); // node-addressed view, cluster only

            assertThat(conn.commands(RedisAdvancedClusterReactiveCommands.factory())).isSameAs(cmds);

            assertThatThrownBy(() -> conn.commands(RedisReactiveCommands.factory())).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("CLUSTER");

            try (StatefulRedisUniversalConnection<String, String> again = client.connectAsync(StringCodec.UTF8).get(5,
                    TimeUnit.SECONDS)) {
                assertThat(again.getTopologyMode()).isEqualTo(TopologyMode.CLUSTER);
            }
        } finally {
            client.shutdown();
        }
    }

}
