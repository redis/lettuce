package com.lambdaworks.redis.cluster;

import static com.google.code.tempusfugit.temporal.Duration.seconds;
import static com.google.code.tempusfugit.temporal.Timeout.timeout;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;
import com.lambdaworks.redis.LettuceFutures;
import com.lambdaworks.redis.RedisFuture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.code.tempusfugit.temporal.Condition;
import com.google.code.tempusfugit.temporal.Duration;
import com.google.code.tempusfugit.temporal.ThreadSleep;
import com.google.code.tempusfugit.temporal.WaitFor;
import com.lambdaworks.redis.RedisClusterAsyncConnection;
import com.lambdaworks.redis.RedisClusterConnection;
import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class AdvancedClusterClientTest extends AbstractClusterTest {

    private RedisAdvancedClusterAsyncConnection<String, String> connection;

    @Before
    public void before() throws Exception {

        WaitFor.waitOrTimeout(new Condition() {
            @Override
            public boolean isSatisfied() {
                return clusterRule.isStable();
            }
        }, timeout(seconds(5)), new ThreadSleep(Duration.millis(500)));

        connection = clusterClient.connectClusterAsync();
    }

    @After
    public void after() throws Exception {
        connection.close();
    }

    @Test
    public void nodeConnections() throws Exception {

        assertThat(clusterClient.getPartitions()).hasSize(4);

        for (RedisClusterNode redisClusterNode : clusterClient.getPartitions()) {
            RedisClusterAsyncConnection<String, String> nodeConnection = connection.getConnection(redisClusterNode.getNodeId());

            String myid = nodeConnection.clusterMyId().get();
            assertThat(myid).isEqualTo(redisClusterNode.getNodeId());
        }
    }

    @Test(expected = RedisException.class)
    public void unknownNodeId() throws Exception {

        connection.getConnection("unknown");
    }

    @Test(expected = RedisException.class)
    public void invalidHost() throws Exception {
        connection.getConnection("invalid-host", -1);
    }

    @Test
    public void doWeirdThingsWithClusterconnections() throws Exception {

        assertThat(clusterClient.getPartitions()).hasSize(4);

        for (RedisClusterNode redisClusterNode : clusterClient.getPartitions()) {
            RedisClusterAsyncConnection<String, String> nodeConnection = connection.getConnection(redisClusterNode.getNodeId());

            nodeConnection.close();

            RedisClusterAsyncConnection<String, String> nextConnection = connection.getConnection(redisClusterNode.getNodeId());
            assertThat(connection).isNotSameAs(nextConnection);

        }
    }

    @Test
    public void syncConnections() throws Exception {

        assertThat(clusterClient.getPartitions()).hasSize(4);

        RedisAdvancedClusterConnection<String, String> sync = clusterClient.connectCluster();
        for (RedisClusterNode redisClusterNode : clusterClient.getPartitions()) {
            RedisClusterConnection<String, String> nodeConnection = sync.getConnection(redisClusterNode.getNodeId());

            String myid = nodeConnection.clusterMyId();
            assertThat(myid).isEqualTo(redisClusterNode.getNodeId());
        }
    }

    @Test
    public void pipelining() throws Exception {

        RedisAdvancedClusterConnection<String, String> verificationConnection = clusterClient.connectCluster();

        // preheat the first connection
        connection.get(key(0)).get();

        int iterations = 1000;
        connection.setAutoFlushCommands(false);
        List<RedisFuture<?>> futures = Lists.newArrayList();
        for (int i = 0; i < iterations; i++) {
            futures.add(connection.set(key(i), value(i)));
        }

        for (int i = 0; i < iterations; i++) {
            assertThat(verificationConnection.get(key(i))).as("Key " + key(i) + " must be null").isNull();
        }

        connection.flushCommands();
        boolean result = LettuceFutures.awaitAll(5, TimeUnit.SECONDS, futures.toArray(new RedisFuture[futures.size()]));
        assertThat(result).isTrue();

        for (int i = 0; i < iterations; i++) {
            assertThat(verificationConnection.get(key(i))).as("Key " + key(i) + " must be " + value(i)).isEqualTo(value(i));
        }

        verificationConnection.close();

    }

    protected String value(int i) {
        return value + "-" + i;
    }

    protected String key(int i) {
        return key + "-" + i;
    }

}
