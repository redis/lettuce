package io.lettuce.core.commands;

import io.lettuce.core.*;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.masterreplica.MasterReplica;
import io.lettuce.core.masterreplica.StatefulRedisMasterReplicaConnection;
import io.lettuce.core.models.role.RedisInstance;
import io.lettuce.core.models.role.RoleParser;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.condition.EnabledOnCommand;
import io.lettuce.test.settings.TestSettings;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * @author Mark Paluch
 */
@Tag(INTEGRATION_TEST)
@ExtendWith(LettuceExtension.class)
@EnabledOnCommand("GEOADD")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GeoMasterReplicaIntegrationTests extends AbstractRedisClientTest {

    private StatefulRedisMasterReplicaConnection<String, String> masterReplica;

    private RedisCommands<String, String> upstream;

    private RedisCommands<String, String> connection1;

    private RedisCommands<String, String> connection2;

    @BeforeEach
    void before() {

        RedisURI node1 = RedisURI.Builder.redis(host, TestSettings.port(3)).withDatabase(2).build();
        RedisURI node2 = RedisURI.Builder.redis(host, TestSettings.port(4)).withDatabase(2).build();

        connection1 = client.connect(node1).sync();
        connection2 = client.connect(node2).sync();

        connection1.flushall();
        connection2.flushall();

        RedisInstance node1Instance = RoleParser.parse(this.connection1.role());
        RedisInstance node2Instance = RoleParser.parse(this.connection2.role());

        if (node1Instance.getRole().isUpstream() && node2Instance.getRole().isReplica()) {
            upstream = connection1;
        } else if (node2Instance.getRole().isUpstream() && node1Instance.getRole().isReplica()) {
            upstream = connection2;
        } else {
            assumeTrue(false,
                    String.format("Cannot run the test because I don't have a distinct master and replica but %s and %s",
                            node1Instance, node2Instance));
        }

        masterReplica = MasterReplica.connect(client, StringCodec.UTF8, Arrays.asList(node1, node2));
        masterReplica.setReadFrom(ReadFrom.REPLICA);
    }

    @AfterEach
    void after() {

        if (connection1 != null) {
            connection1.getStatefulConnection().close();
        }

        if (connection2 != null) {
            connection2.getStatefulConnection().close();
        }

        if (masterReplica != null) {
            masterReplica.close();
        }
    }

    @BeforeEach
    void setUp() {
        this.redis.flushall();
    }

    @Test
    void georadiusReadFromReplica() {

        prepareGeo(upstream);

        upstream.waitForReplication(1, 1000);

        Set<String> georadius = masterReplica.sync().georadius(key, 8.6582861, 49.5285695, 1, GeoArgs.Unit.km);
        assertThat(georadius).hasSize(1).contains("Weinheim");
    }

    @Test
    void georadiusbymemberReadFromReplica() {

        prepareGeo(upstream);
        upstream.waitForReplication(1, 100);

        Set<String> empty = masterReplica.sync().georadiusbymember(key, "Bahn", 1, GeoArgs.Unit.km);
        assertThat(empty).hasSize(1).contains("Bahn");
    }

    protected void prepareGeo(RedisCommands<String, String> redis) {
        redis.geoadd(key, 8.6638775, 49.5282537, "Weinheim");
        redis.geoadd(key, 8.3796281, 48.9978127, "EFS9", 8.665351, 49.553302, "Bahn");
    }

    private static double getY(List<GeoWithin<String>> georadius, int i) {
        return georadius.get(i).getCoordinates().getY().doubleValue();
    }

    private static double getX(List<GeoWithin<String>> georadius, int i) {
        return georadius.get(i).getCoordinates().getX().doubleValue();
    }

}
