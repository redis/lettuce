/*
 * Copyright 2011-2017 the original author or authors.
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
package io.lettuce.core.cluster;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import io.lettuce.KeysAndValues;
import io.lettuce.core.*;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.api.reactive.RedisAdvancedClusterReactiveCommands;
import io.lettuce.core.cluster.api.reactive.RedisClusterReactiveCommands;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import io.lettuce.core.cluster.api.sync.RedisClusterCommands;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.codec.Utf8StringCodec;
import io.lettuce.util.ReactiveSyncInvocationHandler;

import io.netty.util.internal.ConcurrentSet;

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

        StepVerifier.create(commands.mset(KeysAndValues.MAP)).expectNext("OK").verifyComplete();

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

        StepVerifier.create(commands.msetnx(submap)).expectNext(true).verifyComplete();
        StepVerifier.create(commands.msetnx(mset)).expectNext(false).verifyComplete();

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

        StepVerifier.create(commands.mget(result, KeysAndValues.KEYS.toArray(new String[KeysAndValues.COUNT])))
                .expectNext((long) KeysAndValues.COUNT).verifyComplete();
    }

    @Test
    public void delCrossSlot() throws Exception {

        msetCrossSlot();

        StepVerifier.create(commands.del(KeysAndValues.KEYS.toArray(new String[KeysAndValues.COUNT])))
                .expectNext((long) KeysAndValues.COUNT).verifyComplete();

        for (String mykey : KeysAndValues.KEYS) {
            String s1 = syncCommands.get(mykey);
            assertThat(s1).isNull();
        }
    }

    @Test
    public void unlinkCrossSlot() throws Exception {

        msetCrossSlot();

        StepVerifier.create(commands.unlink(KeysAndValues.KEYS.toArray(new String[KeysAndValues.COUNT])))
                .expectNext((long) KeysAndValues.COUNT).verifyComplete();

        for (String mykey : KeysAndValues.KEYS) {
            String s1 = syncCommands.get(mykey);
            assertThat(s1).isNull();
        }
    }

    @Test
    public void clientSetname() throws Exception {

        String name = "test-cluster-client";

        assertThat(clusterClient.getPartitions().size()).isGreaterThan(0);

        StepVerifier.create(commands.clientSetname(name)).expectNext("OK").verifyComplete();

        for (RedisClusterNode redisClusterNode : clusterClient.getPartitions()) {
            RedisClusterCommands<String, String> nodeConnection = commands.getStatefulConnection().sync()
                    .getConnection(redisClusterNode.getNodeId());
            assertThat(nodeConnection.clientList()).contains(name);
        }
    }

    @Test
    public void clientSetnameRunOnError() throws Exception {
        StepVerifier.create(commands.clientSetname("not allowed")).expectError().verify();
    }

    @Test
    public void dbSize() throws Exception {

        writeKeysToTwoNodes();

        StepVerifier.create(commands.dbsize()).expectNext(2L).verifyComplete();
    }

    @Test
    public void flushall() throws Exception {

        writeKeysToTwoNodes();

        StepVerifier.create(commands.flushall()).expectNext("OK").verifyComplete();

        Long dbsize = syncCommands.dbsize();
        assertThat(dbsize).isEqualTo(0);
    }

    @Test
    public void flushdb() throws Exception {

        writeKeysToTwoNodes();

        StepVerifier.create(commands.flushdb()).expectNext("OK").verifyComplete();

        Long dbsize = syncCommands.dbsize();
        assertThat(dbsize).isEqualTo(0);
    }

    @Test
    public void keys() throws Exception {

        writeKeysToTwoNodes();

        StepVerifier.create(commands.keys("*")).recordWith(ConcurrentSet::new).expectNextCount(2)
                .consumeRecordedWith(actual -> assertThat(actual).contains(KEY_ON_NODE_1, KEY_ON_NODE_2)).verifyComplete();
    }

    @Test
    public void keysStreaming() throws Exception {

        writeKeysToTwoNodes();
        ListStreamingAdapter<String> result = new ListStreamingAdapter<>();

        StepVerifier.create(commands.keys(result, "*")).expectNext(2L).verifyComplete();
        assertThat(result.getList()).contains(KEY_ON_NODE_1, KEY_ON_NODE_2);
    }

    @Test
    public void randomKey() throws Exception {

        writeKeysToTwoNodes();

        StepVerifier.create(commands.randomkey())
                .consumeNextWith(actual -> assertThat(actual).isIn(KEY_ON_NODE_1, KEY_ON_NODE_2)).verifyComplete();
    }

    @Test
    public void scriptFlush() throws Exception {
        StepVerifier.create(commands.scriptFlush()).expectNext("OK").verifyComplete();
    }

    @Test
    public void scriptKill() throws Exception {
        StepVerifier.create(commands.scriptKill()).expectNext("OK").verifyComplete();
    }

    @Test
    @Ignore("Run me manually, I will shutdown all your cluster nodes so you need to restart the Redis Cluster after this test")
    public void shutdown() throws Exception {
        commands.shutdown(true).subscribe();
    }

    @Test
    public void readFromSlaves() throws Exception {

        RedisClusterReactiveCommands<String, String> connection = commands.getConnection(host, port4);
        connection.readOnly().subscribe();
        commands.set(key, value).subscribe();

        NodeSelectionAsyncTest.waitForReplication(commands.getStatefulConnection().async(), key, port4);

        AtomicBoolean error = new AtomicBoolean();
        connection.get(key).doOnError(throwable -> error.set(true)).block();

        assertThat(error.get()).isFalse();

        connection.readWrite().subscribe();

        StepVerifier.create(connection.get(key)).expectError(RedisCommandExecutionException.class).verify();
    }

    @Test
    public void clusterScan() throws Exception {

        RedisAdvancedClusterCommands<String, String> sync = commands.getStatefulConnection().sync();
        sync.mset(KeysAndValues.MAP);

        Set<String> allKeys = new HashSet<>();

        KeyScanCursor<String> scanCursor = null;
        do {

            if (scanCursor == null) {
                scanCursor = commands.scan().block();
            } else {
                scanCursor = commands.scan(scanCursor).block();
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
                scanCursor = commands.scan(ScanArgs.Builder.matches("a*")).block();
            } else {
                scanCursor = commands.scan(scanCursor, ScanArgs.Builder.matches("a*")).block();
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
                scanCursor = commands.scan(adapter).block();
            } else {
                scanCursor = commands.scan(adapter, scanCursor).block();
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
                scanCursor = commands.scan(adapter, ScanArgs.Builder.matches("a*")).block();
            } else {
                scanCursor = commands.scan(adapter, scanCursor, ScanArgs.Builder.matches("a*")).block();
            }
        } while (!scanCursor.isFinished());

        assertThat(adapter.getList()).containsAll(
                KeysAndValues.KEYS.stream().filter(k -> k.startsWith("a")).collect(Collectors.toList()));

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
