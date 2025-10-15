package io.lettuce.core.multidb;

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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
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

    private RedisURI east = RedisURI.Builder.redis(host, TestSettings.port(3)).withPassword(passwd).withDatabase(2).build();

    private RedisURI west = RedisURI.Builder.redis(host, TestSettings.port(4)).withPassword(passwd).withDatabase(2).build();

    private RedisCommands<String, String> connection1;

    private RedisCommands<String, String> connection2;

    private static final Pattern pattern = Pattern.compile("tcp_port:(\\d+)");

    @BeforeEach
    void before() {

        RedisURI node1 = RedisURI.Builder.redis(host, TestSettings.port(3)).withDatabase(2).build();
        RedisURI node2 = RedisURI.Builder.redis(host, TestSettings.port(4)).withDatabase(2).build();

        connection1 = client.connect(node1).sync();
        connection2 = client.connect(node2).sync();

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
        Set<RedisURI> availableEndpoints = LettuceSets.unmodifiableSet(east, west);
        MultiDbClient multiDbClient = MultiDbClient.create(availableEndpoints);
        ClientOptions clientOptions = ClientOptions.builder()
                .socketOptions(SocketOptions.builder().connectTimeout(Duration.ofSeconds(2)).build()).build();
        multiDbClient.setOptions(clientOptions);

        try (StatefulRedisConnection<String, String> connection = multiDbClient.connect(StringCodec.UTF8)) {

            String server = connection.sync().info("server");
            assertServerIs(server, east);

            multiDbClient.setActive(west);

            server = connection.sync().info("server");
            assertServerIs(server, west);
        } finally {
            multiDbClient.shutdown();
        }
    }

    @Test
    void testMultipleConnectionsSwitchActiveEndpoint() {
        Set<RedisURI> availableEndpoints = LettuceSets.unmodifiableSet(east, west);
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
            RedisURI newActive = initialActive.equals(east) ? west : east;
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

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group(1)).isEqualTo("" + endpoint.getPort());
    }

}
