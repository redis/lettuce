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
    private static final RedisURI redis1 = RedisURI.Builder.redis(host, TestSettings.proxyPort()).withPassword(passwd).build();

    private static final RedisURI redis2 = RedisURI.Builder.redis(host, TestSettings.proxyPort(1)).withPassword(passwd).build();

    // Redis Endpoints directly connecting to the backing redis instances
    private static final RedisURI redis1Direct = RedisURI.Builder.redis(host, redis1_port).build();

    private static final RedisURI redis2Direct = RedisURI.Builder.redis(host, redis2_port).build();

    // Map of proxy endpoints to backing redis instances
    private static Map<RedisURI, RedisURI> proxyEndpointMap = new HashMap<>();
    {
        proxyEndpointMap.put(redis1, redis1Direct);
        proxyEndpointMap.put(redis2, redis2Direct);
    }

    private RedisCommands<String, String> connection1;

    private RedisCommands<String, String> connection2;

    private static final Pattern pattern = Pattern.compile("tcp_port:(\\d+)");

    private static final ToxiproxyClient tp = new ToxiproxyClient("localhost", TestSettings.proxyAdminPort());

    private static Proxy redisProxy1;

    private static Proxy redisProxy2;


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
        if (redisProxy1 != null) redisProxy1.delete();
        if (redisProxy2 != null) redisProxy2.delete();
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
        connection1 = client.connect(redis1Direct).sync();
        connection2 = client.connect(redis2Direct).sync();

        WithPassword.enableAuthentication(this.connection1);
        this.connection1.auth(passwd);

        WithPassword.enableAuthentication(this.connection2);
        this.connection2.auth(passwd);
    }

    @AfterEach
    void after() {

        if (connection1 != null) {
            WithPassword.disableAuthentication(connection1);
            connection1.configRewrite();
            connection1.getStatefulConnection().close();
        }

        if (connection2 != null) {
            WithPassword.disableAuthentication(connection2);
            connection2.configRewrite();
            connection2.getStatefulConnection().close();
        }

    }

    @Test
    void testMultiDbSwitchActive() {
        Set<RedisURI> availableEndpoints = LettuceSets.unmodifiableSet(redis1, redis2);
        MultiDbClient multiDbClient = MultiDbClient.create(availableEndpoints);
        ClientOptions clientOptions = ClientOptions.builder()
                .socketOptions(SocketOptions.builder().connectTimeout(Duration.ofSeconds(2)).build()).build();
        multiDbClient.setOptions(clientOptions);

        try (StatefulRedisConnection<String, String> connection = multiDbClient.connect(StringCodec.UTF8)) {

            String server = connection.sync().info("server");
            assertServerIs(server, redis1);

            multiDbClient.setActive(redis2);

            server = connection.sync().info("server");
            assertServerIs(server, redis2);
        } finally {
            multiDbClient.shutdown();
        }
    }

    @Test
    void testMultipleConnectionsSwitchActiveEndpoint() {
        Set<RedisURI> availableEndpoints = LettuceSets.unmodifiableSet(redis1, redis2);
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
            RedisURI newActive = initialActive.equals(redis1) ? redis2 : redis1;
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

    private void assertServerIs(String server, RedisURI endpoint) {
        Matcher matcher = pattern.matcher(server);

        RedisURI expected = proxyEndpointMap.get(endpoint);
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group(1)).isEqualTo("" + expected.getPort());
    }

}
