package com.lambdaworks.redis.cluster;

import static com.google.code.tempusfugit.temporal.Duration.seconds;
import static com.google.code.tempusfugit.temporal.Timeout.timeout;
import static com.lambdaworks.redis.ScriptOutputType.STATUS;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.code.tempusfugit.temporal.Duration;
import com.google.code.tempusfugit.temporal.ThreadSleep;
import com.google.code.tempusfugit.temporal.WaitFor;
import com.google.common.collect.Lists;
import com.lambdaworks.redis.RedisClusterAsyncConnection;
import com.lambdaworks.redis.cluster.models.partitions.Partitions;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class AdvancedClusterClientTest extends AbstractClusterTest {

    protected RedisAdvancedClusterConnection<String, String> connection;

    @Before
    public void before() throws Exception {

        WaitFor.waitOrTimeout(() -> {
            return clusterRule.isStable();
        }, timeout(seconds(5)), new ThreadSleep(Duration.millis(500)));

        connection = clusterClient.connectClusterAsync();
    }

    @After
    public void after() throws Exception {
        connection.close();
    }

    @Test
    public void partitions() throws Exception {

        Partitions partitions = connection.getPartitions();
        assertThat(partitions).hasSize(4);

        partitions.iterator().forEachRemaining(
                redisClusterNode -> System.out.println(redisClusterNode.getNodeId() + ": " + redisClusterNode.getFlags() + " "
                        + redisClusterNode.getUri()));
    }

    @Test
    public void testNodeSelectionCount() throws Exception {
        assertThat(connection.all().size()).isEqualTo(4);
        assertThat(connection.slaves().size()).isEqualTo(0);
        assertThat(connection.masters().size()).isEqualTo(4);

        assertThat(connection.nodes(redisClusterNode -> redisClusterNode.is(RedisClusterNode.NodeFlag.MYSELF)).size())
                .isEqualTo(1);
    }

    @Test
    public void testNodeSelection() throws Exception {

        NodeSelection<String, String> onlyMe = connection.nodes(redisClusterNode -> redisClusterNode.getFlags().contains(
                RedisClusterNode.NodeFlag.MYSELF));

        Map<RedisClusterNode, RedisClusterAsyncConnection<String, String>> map = onlyMe.asMap();

        assertThat(map).hasSize(1);

        RedisClusterAsyncConnection<String, String> node = onlyMe.node(0);
        assertThat(node).isNotNull();
    }

    @Test
    public void testMultiNodeOperations() throws Exception {

        List<String> expectation = Lists.newArrayList();
        for (char c = 'a'; c < 'z'; c++) {
            String key = new String(new char[] { c, c, c });
            expectation.add(key);
            connection.set(key, value);
        }

        List<String> result = new Vector<>();

        CompletableFuture.allOf(connection.masters().keys(result::add, "*").futures()).get();

        assertThat(result).hasSize(expectation.size());

        Collections.sort(expectation);
        Collections.sort(result);

        assertThat(result).isEqualTo(expectation);
    }

    @Test
    public void testAsynchronicityOfMultiNodeExeccution() throws Exception {

        RedisAdvancedClusterConnection<String, String> connection2 = clusterClient.connectClusterAsync();

        NodeSelectionAsyncOperations<String, String> masters = connection2.masters();
        CompletableFuture.allOf(masters.configSet("lua-time-limit", "10").futures()).get();
        AsyncExecutions<Object> eval = masters.eval("while true do end", STATUS, new String[0]);
        assertThat(eval.nodes()).hasSize(4);

        for (CompletableFuture<Object> future : eval.futures()) {

            assertThat(future.isDone()).isFalse();
            assertThat(future.isCancelled()).isFalse();
        }

        AsyncExecutions<String> kill = connection.masters().scriptKill();
        CompletableFuture.allOf(kill.futures()).get();

        for (CompletionStage<String> execution : kill) {
            assertThat(execution.toCompletableFuture().get()).isEqualTo("OK");
        }

        for (CompletableFuture<Object> future : eval.futures()) {
            assertThat(future.isDone()).isTrue();
        }
    }
}
