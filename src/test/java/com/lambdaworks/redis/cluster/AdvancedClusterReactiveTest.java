/*
 * Copyright 2011-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lambdaworks.redis.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.lambdaworks.KeysAndValues;
import com.lambdaworks.redis.*;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.cluster.api.reactive.RedisAdvancedClusterReactiveCommands;
import com.lambdaworks.redis.cluster.api.reactive.RedisClusterReactiveCommands;
import com.lambdaworks.redis.cluster.api.sync.RedisAdvancedClusterCommands;
import com.lambdaworks.redis.cluster.api.sync.RedisClusterCommands;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;
import com.lambdaworks.redis.codec.Utf8StringCodec;
import com.lambdaworks.util.ReactiveSyncInvocationHandler;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Mark Paluch
 */
public class AdvancedClusterReactiveTest extends AbstractClusterTest {

    public static final String KEY_ON_NODE_1 = "a";
    public static final String KEY_ON_NODE_2 = "b";

    private RedisAdvancedClusterReactiveCommands<String, String> commands;
    private RedisCommands<String, String> syncCommands;

    @Before
    public void before() throws Exception {
        commands = clusterClient.connect().reactive();
        syncCommands = ReactiveSyncInvocationHandler.sync(commands.getStatefulConnection());
    }

    @After
    public void after() throws Exception {
        commands.getStatefulConnection().close();
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
    public void msetCrossSlot() throws Exception {

        Mono<String> mset = commands.mset(KeysAndValues.MAP);
        assertThat(block(mset)).isEqualTo("OK");

        for (String mykey : KeysAndValues.KEYS) {
            String s1 = syncCommands.get(mykey);
            assertThat(s1).isEqualTo(KeysAndValues.MAP.get(mykey));
        }
    }

    @Test
    public void msetnxCrossSlot() throws Exception {

        Map<String, String> mset = prepareMset();

        String key = mset.keySet().iterator().next();
        Map<String, String> submap = Collections.singletonMap(key, mset.get(key));

        assertThat(block(commands.msetnx(submap))).isTrue();
        assertThat(block(commands.msetnx(mset))).isFalse();

        for (String mykey : mset.keySet()) {
            String s1 = syncCommands.get(mykey);
            assertThat(s1).isEqualTo(mset.get(mykey));
        }
    }

    @Test
    public void mgetCrossSlot() throws Exception {

        msetCrossSlot();

        Map<Integer, List<String>> partitioned = SlotHash.partition(new Utf8StringCodec(), KeysAndValues.KEYS);
        assertThat(partitioned.size()).isGreaterThan(100);

        Flux<KeyValue<String, String>> flux = commands.mget(KeysAndValues.KEYS.toArray(new String[KeysAndValues.COUNT]));
        List<KeyValue<String, String>> result = flux.collectList().block();

        assertThat(result).hasSize(KeysAndValues.COUNT);
        assertThat(result.stream().map(Value::getValue).collect(Collectors.toList())).isEqualTo(KeysAndValues.VALUES);
    }

    @Test
    public void mgetCrossSlotStreaming() throws Exception {

        msetCrossSlot();

        KeyValueStreamingAdapter<String, String> result = new KeyValueStreamingAdapter<>();

        Mono<Long> mono = commands.mget(result, KeysAndValues.KEYS.toArray(new String[KeysAndValues.COUNT]));
        Long count = block(mono);

        assertThat(result.getMap()).hasSize(KeysAndValues.COUNT);
        assertThat(count).isEqualTo(KeysAndValues.COUNT);
    }

    @Test
    public void delCrossSlot() throws Exception {

        msetCrossSlot();

        Mono<Long> mono = commands.del(KeysAndValues.KEYS.toArray(new String[KeysAndValues.COUNT]));
        Long result = block(mono);

        assertThat(result).isEqualTo(KeysAndValues.COUNT);

        for (String mykey : KeysAndValues.KEYS) {
            String s1 = syncCommands.get(mykey);
            assertThat(s1).isNull();
        }
    }

    @Test
    public void unlinkCrossSlot() throws Exception {

        msetCrossSlot();

        Mono<Long> mono = commands.unlink(KeysAndValues.KEYS.toArray(new String[KeysAndValues.COUNT]));
        Long result = block(mono);

        assertThat(result).isEqualTo(KeysAndValues.COUNT);

        for (String mykey : KeysAndValues.KEYS) {
            String s1 = syncCommands.get(mykey);
            assertThat(s1).isNull();
        }
    }

    @Test
    public void clientSetname() throws Exception {

        String name = "test-cluster-client";

        assertThat(clusterClient.getPartitions().size()).isGreaterThan(0);

        block(commands.clientSetname(name));

        for (RedisClusterNode redisClusterNode : clusterClient.getPartitions()) {
            RedisClusterCommands<String, String> nodeConnection = commands.getStatefulConnection().sync()
                    .getConnection(redisClusterNode.getNodeId());
            assertThat(nodeConnection.clientList()).contains(name);
        }
    }

    @Test(expected = Exception.class)
    public void clientSetnameRunOnError() throws Exception {
        block(commands.clientSetname("not allowed"));
    }

    @Test
    public void dbSize() throws Exception {

        writeKeysToTwoNodes();

        Long dbsize = block(commands.dbsize());
        assertThat(dbsize).isEqualTo(2);
    }

    @Test
    public void flushall() throws Exception {

        writeKeysToTwoNodes();

        assertThat(block(commands.flushall())).isEqualTo("OK");

        Long dbsize = syncCommands.dbsize();
        assertThat(dbsize).isEqualTo(0);
    }

    @Test
    public void flushdb() throws Exception {

        writeKeysToTwoNodes();

        assertThat(block(commands.flushdb())).isEqualTo("OK");

        Long dbsize = syncCommands.dbsize();
        assertThat(dbsize).isEqualTo(0);
    }

    @Test
    public void keys() throws Exception {

        writeKeysToTwoNodes();

        List<String> result = commands.keys("*").collectList().block();

        assertThat(result).contains(KEY_ON_NODE_1, KEY_ON_NODE_2);
    }

    @Test
    public void keysStreaming() throws Exception {

        writeKeysToTwoNodes();
        ListStreamingAdapter<String> result = new ListStreamingAdapter<>();

        assertThat(block(commands.keys(result, "*"))).isEqualTo(2);
        assertThat(result.getList()).contains(KEY_ON_NODE_1, KEY_ON_NODE_2);
    }

    @Test
    public void randomKey() throws Exception {

        writeKeysToTwoNodes();

        assertThat(block(commands.randomkey())).isIn(KEY_ON_NODE_1, KEY_ON_NODE_2);
    }

    @Test
    public void scriptFlush() throws Exception {
        assertThat(block(commands.scriptFlush())).isEqualTo("OK");
    }

    @Test
    public void scriptKill() throws Exception {
        assertThat(block(commands.scriptKill())).isEqualTo("OK");
    }

    @Test
    @Ignore("Run me manually, I will shutdown all your cluster nodes so you need to restart the Redis Cluster after this test")
    public void shutdown() throws Exception {
        commands.shutdown(true).subscribe();
    }

    @Test
    public void readFromSlaves() throws Exception {

        RedisClusterReactiveCommands<String, String> connection = commands.getConnection(host, port4);
        block(connection.readOnly());
        block(commands.set(key, value));
        NodeSelectionAsyncTest.waitForReplication(commands.getStatefulConnection().async(), key, port4);

        AtomicBoolean error = new AtomicBoolean();
        connection.get(key).doOnError(throwable -> error.set(true)).block();

        assertThat(error.get()).isFalse();

        block(connection.readWrite());

        try {
            block(connection.get(key).doOnError(throwable -> error.set(true)));
            fail("Missing exception");
        } catch (Exception e) {
            assertThat(error.get()).isTrue();
        }
    }

    @Test
    public void clusterScan() throws Exception {

        RedisAdvancedClusterCommands<String, String> sync = commands.getStatefulConnection().sync();
        sync.mset(KeysAndValues.MAP);

        Set<String> allKeys = new HashSet<>();

        KeyScanCursor<String> scanCursor = null;
        do {

            if (scanCursor == null) {
                scanCursor = block(commands.scan());
            } else {
                scanCursor = block(commands.scan(scanCursor));
            }
            allKeys.addAll(scanCursor.getKeys());
        } while (!scanCursor.isFinished());

        assertThat(allKeys).containsAll(KeysAndValues.KEYS);

    }

    @Test
    public void clusterScanWithArgs() throws Exception {

        RedisAdvancedClusterCommands<String, String> sync = commands.getStatefulConnection().sync();
        sync.mset(KeysAndValues.MAP);

        Set<String> allKeys = new HashSet<>();

        KeyScanCursor<String> scanCursor = null;
        do {

            if (scanCursor == null) {
                scanCursor = block(commands.scan(ScanArgs.Builder.matches("a*")));
            } else {
                scanCursor = block(commands.scan(scanCursor, ScanArgs.Builder.matches("a*")));
            }
            allKeys.addAll(scanCursor.getKeys());
        } while (!scanCursor.isFinished());

        assertThat(allKeys)
                .containsAll(KeysAndValues.KEYS.stream().filter(k -> k.startsWith("a")).collect(Collectors.toList()));

    }

    @Test
    public void clusterScanStreaming() throws Exception {

        RedisAdvancedClusterCommands<String, String> sync = commands.getStatefulConnection().sync();
        sync.mset(KeysAndValues.MAP);

        ListStreamingAdapter<String> adapter = new ListStreamingAdapter<>();

        StreamScanCursor scanCursor = null;
        do {

            if (scanCursor == null) {
                scanCursor = block(commands.scan(adapter));
            } else {
                scanCursor = block(commands.scan(adapter, scanCursor));
            }
        } while (!scanCursor.isFinished());

        assertThat(adapter.getList()).containsAll(KeysAndValues.KEYS);

    }

    @Test
    public void clusterScanStreamingWithArgs() throws Exception {

        RedisAdvancedClusterCommands<String, String> sync = commands.getStatefulConnection().sync();
        sync.mset(KeysAndValues.MAP);

        ListStreamingAdapter<String> adapter = new ListStreamingAdapter<>();

        StreamScanCursor scanCursor = null;
        do {

            if (scanCursor == null) {
                scanCursor = block(commands.scan(adapter, ScanArgs.Builder.matches("a*")));
            } else {
                scanCursor = block(commands.scan(adapter, scanCursor, ScanArgs.Builder.matches("a*")));
            }
        } while (!scanCursor.isFinished());

        assertThat(adapter.getList()).containsAll(
                KeysAndValues.KEYS.stream().filter(k -> k.startsWith("a")).collect(Collectors.toList()));

    }

    private <T> T block(Mono<T> mono) {
        return mono.block();
    }


    private void writeKeysToTwoNodes() {
        syncCommands.set(KEY_ON_NODE_1, value);
        syncCommands.set(KEY_ON_NODE_2, value);
    }
    
    protected Map<String, String> prepareMset() {
        Map<String, String> mset = new HashMap<>();
        for (char c = 'a'; c < 'z'; c++) {
            String key = new String(new char[] { c, c, c });
            mset.put(key, "value-" + key);
        }
        return mset;
    }
}
