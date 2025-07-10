package io.lettuce.core.masterreplica;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.AbstractRedisClientTest;
import io.lettuce.core.ReadFrom;
import io.lettuce.core.RedisException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.models.role.RedisInstance;
import io.lettuce.core.models.role.RoleParser;
import io.lettuce.test.WithPassword;
import io.lettuce.test.settings.TestSettings;

/**
 * Integration tests for static master/replica via {@link MasterReplica}.
 *
 * @author Mark Paluch
 */
@Tag(INTEGRATION_TEST)
class StaticMasterReplicaIntegrationTests extends AbstractRedisClientTest {

    private StatefulRedisMasterReplicaConnection<String, String> connection;

    private RedisURI upstream;

    private RedisURI replica;

    private RedisCommands<String, String> connection1;

    private RedisCommands<String, String> connection2;

    @BeforeEach
    void before() throws Exception {

        RedisURI node1 = RedisURI.Builder.redis(host, TestSettings.port(3)).withClientName("my-client").withDatabase(2).build();
        RedisURI node2 = RedisURI.Builder.redis(host, TestSettings.port(4)).withClientName("my-client").withDatabase(2).build();

        connection1 = client.connect(node1).sync();
        connection2 = client.connect(node2).sync();

        RedisInstance node1Instance = RoleParser.parse(this.connection1.role());
        RedisInstance node2Instance = RoleParser.parse(this.connection2.role());

        if (node1Instance.getRole().isUpstream() && node2Instance.getRole().isReplica()) {
            upstream = node1;
            replica = node2;
        } else if (node2Instance.getRole().isUpstream() && node1Instance.getRole().isReplica()) {
            upstream = node2;
            replica = node1;
        } else {
            assumeTrue(false,
                    String.format("Cannot run the test because I don't have a distinct master and replica but %s and %s",
                            node1Instance, node2Instance));
        }

        WithPassword.enableAuthentication(this.connection1);
        this.connection1.auth(passwd);
        this.connection1.configSet("masterauth", passwd.toString());

        WithPassword.enableAuthentication(this.connection2);
        this.connection2.auth(passwd);
        this.connection2.configSet("masterauth", passwd.toString());

        node1.setAuthentication(passwd);
        node2.setAuthentication(passwd);

        connection = MasterReplica.connect(client, StringCodec.UTF8, Arrays.asList(upstream, replica));
        connection.setReadFrom(ReadFrom.REPLICA);
    }

    @AfterEach
    void after() throws Exception {

        if (connection1 != null) {
            WithPassword.disableAuthentication(connection1);
            connection1.configSet("masterauth", "");
            connection1.configRewrite();
            connection1.getStatefulConnection().close();
        }

        if (connection2 != null) {
            WithPassword.disableAuthentication(connection2);
            connection2.configSet("masterauth", "");
            connection2.configRewrite();
            connection2.getStatefulConnection().close();
        }

        if (connection != null) {
            connection.close();
        }
    }

    @Test
    void testMasterReplicaStandaloneBasic() {

        String server = connection.sync().info("server");

        Pattern pattern = Pattern.compile("tcp_port:(\\d+)");
        Matcher matcher = pattern.matcher(server);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group(1)).isEqualTo("6483");
        assertThat(connection.getReadFrom()).isEqualTo(ReadFrom.REPLICA);
    }

    @Test
    void testMasterReplicaReadWrite() {

        RedisCommands<String, String> redisCommands = connection.sync();
        redisCommands.set(key, value);
        redisCommands.waitForReplication(1, 100);

        assertThat(redisCommands.get(key)).isEqualTo(value);
    }

    @Test
    void noReplicaForRead() {

        connection.close();

        connection = MasterReplica.connect(client, StringCodec.UTF8, Collections.singletonList(upstream));
        connection.setReadFrom(ReadFrom.REPLICA);

        assertThatThrownBy(() -> replicaCall(connection)).isInstanceOf(RedisException.class);
    }

    @Test
    void shouldWorkWithMasterOnly() {

        connection.close();

        connection = MasterReplica.connect(client, StringCodec.UTF8, Collections.singletonList(upstream));

        connection.sync().set(key, value);
        assertThat(connection.sync().get(key)).isEqualTo("value");
    }

    @Test
    void shouldWorkWithReplicaOnly() {

        connection.close();

        connection = MasterReplica.connect(client, StringCodec.UTF8, Collections.singletonList(replica));
        connection.setReadFrom(ReadFrom.UPSTREAM_PREFERRED);

        assertThat(connection.sync().info()).isNotEmpty();
    }

    @Test
    void noUpstreamForWrite() {

        connection.close();

        connection = MasterReplica.connect(client, StringCodec.UTF8, Collections.singletonList(replica));

        assertThatThrownBy(() -> connection.sync().set(key, value)).isInstanceOf(RedisException.class);
    }

    @Test
    void masterReplicaConnectionShouldSetClientName() {

        assertThat(connection.sync().clientGetname()).isEqualTo("my-client");
        connection.sync().quit();
        assertThat(connection.sync().clientGetname()).isEqualTo("my-client");

        connection.close();
    }

    static String replicaCall(StatefulRedisMasterReplicaConnection<String, String> connection) {
        return connection.sync().info("replication");
    }

    @Test
    void testConnectionCount() {

        MasterReplicaConnectionProvider connectionProvider = getConnectionProvider();

        assertThat(connectionProvider.getConnectionCount()).isEqualTo(0);
        replicaCall(connection);

        assertThat(connectionProvider.getConnectionCount()).isEqualTo(1);

        connection.sync().set(key, value);
        assertThat(connectionProvider.getConnectionCount()).isEqualTo(2);
    }

    @Test
    void testReconfigureTopology() {
        MasterReplicaConnectionProvider connectionProvider = getConnectionProvider();

        replicaCall(connection);

        connectionProvider.setKnownNodes(Collections.emptyList());

        assertThat(connectionProvider.getConnectionCount()).isEqualTo(0);
    }

    MasterReplicaConnectionProvider getConnectionProvider() {
        MasterReplicaChannelWriter writer = ((StatefulRedisMasterReplicaConnectionImpl) connection).getChannelWriter();
        return writer.getUpstreamReplicaConnectionProvider();
    }

}
