package io.lettuce.core.multidb;

import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import eu.rekawek.toxiproxy.model.Toxic;
import io.lettuce.core.AbstractRedisClientTest;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.internal.LettuceSets;
import io.lettuce.core.masterreplica.MasterReplica;
import io.lettuce.test.WithPassword;
import io.lettuce.test.settings.TestSettings;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for master/replica via {@link MasterReplica}.
 *
 * @author Mark Paluch
 */
@Tag(INTEGRATION_TEST)
class MultiDbClientIntegrationTests extends AbstractRedisClientTest {

    // Backing redis instances
    private static final int redis1_port = TestSettings.port(8);

    private static final int redis2_port = TestSettings.port(9);

    // Redis Endpoints exposed by toxiproxy
    private static final RedisURI redis1ProxyUri = RedisURI.Builder.redis(host, TestSettings.proxyPort()).withPassword(passwd).build();

    private static final RedisURI redis2ProxyUri = RedisURI.Builder.redis(host, TestSettings.proxyPort(1)).withPassword(passwd).build();

    // Redis Endpoints directly connecting to the backing redis instances
    private static final RedisURI redis1Uri = RedisURI.Builder.redis(host, redis1_port).build();

    private static final RedisURI redis2Uri = RedisURI.Builder.redis(host, redis2_port).build();

    // Map of proxy endpoints to backing redis instances
    private static Map<RedisURI, RedisURI> proxyEndpointMap = new HashMap<>();
    {
        proxyEndpointMap.put(redis1ProxyUri, redis1Uri);
        proxyEndpointMap.put(redis2ProxyUri, redis2Uri);
    }

    private RedisCommands<String, String> redis1Conn;

    private RedisCommands<String, String> redis2Conn;

    private static final Pattern pattern = Pattern.compile("tcp_port:(\\d+)");

    private static final ToxiproxyClient tp = new ToxiproxyClient("localhost", TestSettings.proxyAdminPort());

    private static Proxy redisProxy1;

    private static Proxy redisProxy2;

    // Map of proxy endpoints to backing redis instances
    private Map<RedisURI, RedisCommands<String, String>> commands = new HashMap<>();

    @BeforeAll
    public static void setupToxiproxy() throws IOException {
        if (tp.getProxyOrNull("redis-1") != null) {
            tp.getProxy("redis-1").delete();
        }
        if (tp.getProxyOrNull("redis-2") != null) {
            tp.getProxy("redis-2").delete();
        }

        redisProxy1 = tp.createProxy("redis-1", "0.0.0.0:" + TestSettings.proxyPort(), "redis-failover:" + redis1_port);
        redisProxy2 = tp.createProxy("redis-2", "0.0.0.0:" + TestSettings.proxyPort(1), "redis-failover:" + redis2_port);
    }

    @AfterAll
    public static void cleanupToxiproxy() throws IOException {
        if (redisProxy1 != null)
            redisProxy1.delete();
        if (redisProxy2 != null)
            redisProxy2.delete();
    }

    @BeforeEach
    public void enableAllToxiproxy() throws IOException {
        tp.getProxies().forEach(proxy -> {
            try {
                proxy.enable();
                for (Toxic toxic : proxy.toxics().getAll()) {
                    toxic.remove();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @BeforeEach
    void before() {
        redis1Conn = client.connect(redis1Uri).sync();
        redis2Conn = client.connect(redis2Uri).sync();

        commands.put(redis1Uri, redis1Conn);
        commands.put(redis2Uri, redis2Conn);

        WithPassword.enableAuthentication(this.redis1Conn);
        this.redis1Conn.auth(passwd);

        WithPassword.enableAuthentication(this.redis2Conn);
        this.redis2Conn.auth(passwd);
    }

    @AfterEach
    void after() {

        if (redis1Conn != null) {
            WithPassword.disableAuthentication(redis1Conn);
            redis1Conn.configRewrite();
            redis1Conn.getStatefulConnection().close();
        }

        if (redis2Conn != null) {
            WithPassword.disableAuthentication(redis2Conn);
            redis2Conn.configRewrite();
            redis2Conn.getStatefulConnection().close();
        }

    }

    @Test
    void testMultiDbSwitchActive() {
        Set<RedisURI> availableEndpoints = LettuceSets.unmodifiableSet(redis1ProxyUri, redis2ProxyUri);
        MultiDbClient multiDbClient = MultiDbClient.create(availableEndpoints);
        ClientOptions clientOptions = ClientOptions.builder()
                .socketOptions(SocketOptions.builder().connectTimeout(Duration.ofSeconds(2)).build()).build();
        multiDbClient.setOptions(clientOptions);

        try (StatefulRedisConnection<String, String> connection = multiDbClient.connect(StringCodec.UTF8)) {

            String server = connection.sync().info("server");
            assertServerIs(server, redis1ProxyUri);

            multiDbClient.setActive(redis2ProxyUri);

            server = connection.sync().info("server");
            assertServerIs(server, redis2ProxyUri);
        } finally {
            multiDbClient.shutdown();
        }
    }

    @Test
    void testMultipleConnectionsSwitchActiveEndpoint() {
        Set<RedisURI> availableEndpoints = LettuceSets.unmodifiableSet(redis1ProxyUri, redis2ProxyUri);
        MultiDbClient multiDbClient = MultiDbClient.create(availableEndpoints);
        ClientOptions clientOptions = ClientOptions.builder()
                .socketOptions(SocketOptions.builder().connectTimeout(Duration.ofSeconds(2)).build()).build();
        multiDbClient.setOptions(clientOptions);

        try (StatefulRedisConnection<String, String> connection1 = multiDbClient.connect(StringCodec.UTF8);
                StatefulRedisConnection<String, String> connection2 = multiDbClient.connect(StringCodec.UTF8);
                StatefulRedisConnection<String, String> connection3 = multiDbClient.connect(StringCodec.UTF8)) {

            // Initially all connections should route to the first endpoint (east or west)
            String server1 = connection1.sync().info("server");
            String server2 = connection2.sync().info("server");
            String server3 = connection3.sync().info("server");

            // All should be on the same initial endpoint
            RedisURI initialActive = multiDbClient.getActive();
            assertServerIs(server1, initialActive);
            assertServerIs(server2, initialActive);
            assertServerIs(server3, initialActive);

            // Switch to the other endpoint
            RedisURI newActive = initialActive.equals(redis1ProxyUri) ? redis2ProxyUri : redis1ProxyUri;
            multiDbClient.setActive(newActive);

            // All connections should now route to the new active endpoint
            server1 = connection1.sync().info("server");
            server2 = connection2.sync().info("server");
            server3 = connection3.sync().info("server");

            assertServerIs(server1, newActive);
            assertServerIs(server2, newActive);
            assertServerIs(server3, newActive);

            // Switch back to the original endpoint
            multiDbClient.setActive(initialActive);

            // All connections should route back to the original endpoint
            server1 = connection1.sync().info("server");
            server2 = connection2.sync().info("server");
            server3 = connection3.sync().info("server");

            assertServerIs(server1, initialActive);
            assertServerIs(server2, initialActive);
            assertServerIs(server3, initialActive);

        } finally {
            multiDbClient.shutdown();
        }
    }

    @Test
    void testPendingCommandsRequeuedAfterEndpointRemoval() {
        Set<RedisURI> availableEndpoints = LettuceSets.unmodifiableSet(redis1ProxyUri, redis2ProxyUri);
        MultiDbClient multiDbClient = MultiDbClient.create(availableEndpoints);
        ClientOptions clientOptions = ClientOptions.builder()
                .socketOptions(SocketOptions.builder().connectTimeout(Duration.ofSeconds(2)).build()).build();
        multiDbClient.setOptions(clientOptions);

        try (StatefulRedisConnection<String, String> connection = multiDbClient.connect(StringCodec.UTF8)) {

            // Get the initial active endpoint
            RedisURI initialActive = multiDbClient.getActive();

            // Disable auto-flush to buffer commands
            connection.setAutoFlushCommands(false);

            // Issue a command that will be buffered
            connection.async().set("testkey", "testvalue");

            // Verify command is not yet executed on the initial endpoint
            RedisURI initialDirect = proxyEndpointMap.get(initialActive);
            assertThat(commands.get(initialDirect).get("testkey")).isNull();

            // Switch to the other endpoint
            RedisURI newActive = initialActive.equals(redis1ProxyUri) ? redis2ProxyUri : redis1ProxyUri;
            multiDbClient.setActive(newActive);

            // Remove the old active endpoint
            boolean removed = multiDbClient.removeEndpoint(initialActive);
            assertThat(removed).isTrue();

            // Flush commands - they should now be sent to the new active endpoint
            connection.flushCommands();

            // Verify command was executed on the NEW active endpoint
            RedisCommands<String, String> initial = commands.get(proxyEndpointMap.get(initialActive));
            RedisCommands<String, String> active = commands.get(proxyEndpointMap.get(newActive));
            assertThat(active.get("testkey")).isEqualTo("testvalue");
            active.del("testkey"); // Cleanup

            assertThat(initial.get("testkey")).isNull();

        } finally {
            multiDbClient.shutdown();
        }
    }

    private void assertServerIs(String server, RedisURI endpoint) {
        Matcher matcher = pattern.matcher(server);

        RedisURI expected = proxyEndpointMap.get(endpoint);
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group(1)).isEqualTo("" + expected.getPort());
    }

}
