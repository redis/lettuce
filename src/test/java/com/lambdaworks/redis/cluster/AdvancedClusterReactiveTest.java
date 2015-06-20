package com.lambdaworks.redis.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import rx.Observable;

import com.google.common.collect.Lists;
import com.lambdaworks.RandomKeys;
import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.cluster.api.rx.RedisAdvancedClusterReactiveCommands;
import com.lambdaworks.redis.cluster.api.rx.RedisClusterReactiveCommands;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;
import com.lambdaworks.redis.codec.Utf8StringCodec;
import com.lambdaworks.redis.commands.rx.RxSyncInvocationHandler;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class AdvancedClusterReactiveTest extends AbstractClusterTest {

    private RedisAdvancedClusterReactiveCommands<String, String> commands;
    private RedisCommands<String, String> sync;

    @Before
    public void before() throws Exception {
        ClusterSetup.setup2Master2Slaves(clusterRule);
        commands = clusterClient.connectClusterAsync().getStatefulConnection().reactive();
        sync = RxSyncInvocationHandler.sync(commands.getStatefulConnection());
    }

    @After
    public void after() throws Exception {
        commands.close();
    }

    @Test(expected = RedisException.class)
    public void unknownNodeId() throws Exception {

        commands.getConnection("unknown");
    }

    @Test(expected = RedisException.class)
    public void invalidHost() throws Exception {
        commands.getConnection("invalid-host", -1);
    }

    @Test
    public void doWeirdThingsWithClusterconnections() throws Exception {

        assertThat(clusterClient.getPartitions()).hasSize(4);

        for (RedisClusterNode redisClusterNode : clusterClient.getPartitions()) {
            RedisClusterReactiveCommands<String, String> nodeConnection = commands.getConnection(redisClusterNode.getNodeId());

            nodeConnection.close();

            RedisClusterReactiveCommands<String, String> nextConnection = commands.getConnection(redisClusterNode.getNodeId());
            assertThat(commands).isNotSameAs(nextConnection);
        }
    }

    @Test
    public void msetCrossSlot() throws Exception {

        Observable<String> mset = commands.mset(RandomKeys.MAP);
        List<String> result = Lists.newArrayList(mset.toBlocking().toIterable());
        assertThat(result).hasSize(1).contains("OK");

        for (String mykey : RandomKeys.KEYS) {
            String s1 = sync.get(mykey);
            assertThat(s1).isEqualTo(RandomKeys.MAP.get(mykey));
        }
    }

    @Test
    public void msetnxCrossSlot() throws Exception {

        List<Boolean> result = Lists.newArrayList(commands.msetnx(RandomKeys.MAP).toBlocking().toIterable());

        assertThat(result).hasSize(1).contains(true);

        for (String mykey : RandomKeys.KEYS) {
            String s1 = sync.get(mykey);
            assertThat(s1).isEqualTo(RandomKeys.MAP.get(mykey));
        }
    }

    @Test
    public void mgetCrossSlot() throws Exception {

        msetCrossSlot();

        Map<Integer, List<String>> partitioned = SlotHash.partition(new Utf8StringCodec(), RandomKeys.KEYS);
        assertThat(partitioned.size()).isGreaterThan(100);

        Observable<String> observable = commands.mget(RandomKeys.KEYS.toArray(new String[RandomKeys.COUNT]));
        List<String> result = observable.toList().toBlocking().single();

        assertThat(result).hasSize(RandomKeys.COUNT);
        assertThat(result).isEqualTo(RandomKeys.VALUES);
    }

    @Test
    public void delCrossSlot() throws Exception {

        msetCrossSlot();

        Observable<List<Long>> observable = commands.del(RandomKeys.KEYS.toArray(new String[RandomKeys.COUNT])).toList();
        List<Long> result = observable.toBlocking().first();

        assertThat(result).hasSize(1).contains((long) RandomKeys.COUNT);

        for (String mykey : RandomKeys.KEYS) {
            String s1 = sync.get(mykey);
            assertThat(s1).isNull();
        }
    }

    @Test
    public void readFromSlaves() throws Exception {

        RedisClusterReactiveCommands<String, String> connection = commands.getConnection(host, port4);
        connection.readOnly().toBlocking().first();
        commands.set(key, value).toBlocking().first();
        AdvancedClusterClientTest.waitForReplication(commands.getStatefulConnection().async(), key, port4);

        AtomicBoolean error = new AtomicBoolean();
        connection.get(key).doOnError(throwable -> error.set(true)).toBlocking().toFuture().get();

        assertThat(error.get()).isFalse();

        connection.readWrite().toBlocking().first();

        try {
            connection.get(key).doOnError(throwable -> error.set(true)).toBlocking().first();
            fail("Missing exception");
        } catch (Exception e) {
            assertThat(error.get()).isTrue();
        }
    }
}
