/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.cluster;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import io.lettuce.core.FlushMode;
import io.lettuce.core.KeyScanCursor;
import io.lettuce.core.KeyValue;
import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.RedisException;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.StreamScanCursor;
import io.lettuce.core.TestSupport;
import io.lettuce.core.Value;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands;
import io.lettuce.core.cluster.api.reactive.RedisAdvancedClusterReactiveCommands;
import io.lettuce.core.cluster.api.reactive.RedisClusterReactiveCommands;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import io.lettuce.core.cluster.api.sync.RedisClusterCommands;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.codec.Base16;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.test.KeyValueStreamingAdapter;
import io.lettuce.test.KeysAndValues;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.ListStreamingAdapter;
import io.lettuce.test.TestFutures;
import io.lettuce.test.condition.EnabledOnCommand;
import io.netty.util.internal.ConcurrentSet;

/**
 * @author Mark Paluch
 */
@Tag(INTEGRATION_TEST)
@ExtendWith(LettuceExtension.class)
class AdvancedClusterReactiveIntegrationTests extends TestSupport {

    private static final String KEY_ON_NODE_1 = "a";

    private static final String KEY_ON_NODE_2 = "b";

    private final RedisClusterClient clusterClient;

    private final RedisAdvancedClusterReactiveCommands<String, String> commands;

    private final RedisAdvancedClusterCommands<String, String> syncCommands;

    @Inject
    AdvancedClusterReactiveIntegrationTests(RedisClusterClient clusterClient,
            StatefulRedisClusterConnection<String, String> connection) {
        this.clusterClient = clusterClient;
        this.commands = connection.reactive();
        this.syncCommands = connection.sync();
    }

    @BeforeEach
    void setUp() {
        syncCommands.flushall();
    }

    @Test
    void unknownNodeId() {
        assertThatThrownBy(() -> commands.getConnection("unknown")).isInstanceOf(RedisException.class);
    }

    @Test
    void invalidHost() {
        assertThatThrownBy(() -> commands.getConnection("invalid-host", -1)).isInstanceOf(RedisException.class);
    }

    @Test
    void msetCrossSlot() {

        StepVerifier.create(commands.mset(KeysAndValues.MAP)).expectNext("OK").verifyComplete();

        for (String mykey : KeysAndValues.KEYS) {
            String s1 = syncCommands.get(mykey);
            assertThat(s1).isEqualTo(KeysAndValues.MAP.get(mykey));
        }
    }

    @Test
    void msetnxCrossSlot() {

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
    void mgetCrossSlot() {

        msetCrossSlot();

        Map<Integer, List<String>> partitioned = SlotHash.partition(StringCodec.UTF8, KeysAndValues.KEYS);
        assertThat(partitioned.size()).isGreaterThan(100);

        Flux<KeyValue<String, String>> flux = commands.mget(KeysAndValues.KEYS.toArray(new String[KeysAndValues.COUNT]));
        List<KeyValue<String, String>> result = flux.collectList().block();

        assertThat(result).hasSize(KeysAndValues.COUNT);
        assertThat(result.stream().map(Value::getValue).collect(Collectors.toList())).isEqualTo(KeysAndValues.VALUES);
    }

    @Test
    void mgetCrossSlotStreaming() {

        msetCrossSlot();

        KeyValueStreamingAdapter<String, String> result = new KeyValueStreamingAdapter<>();

        StepVerifier.create(commands.mget(result, KeysAndValues.KEYS.toArray(new String[KeysAndValues.COUNT])))
                .expectNext((long) KeysAndValues.COUNT).verifyComplete();
    }

    @Test
    void delCrossSlot() {

        msetCrossSlot();

        StepVerifier.create(commands.del(KeysAndValues.KEYS.toArray(new String[KeysAndValues.COUNT])))
                .expectNext((long) KeysAndValues.COUNT).verifyComplete();

        for (String mykey : KeysAndValues.KEYS) {
            String s1 = syncCommands.get(mykey);
            assertThat(s1).isNull();
        }
    }

    @Test
    @EnabledOnCommand("UNLINK")
    void unlinkCrossSlot() {

        msetCrossSlot();

        StepVerifier.create(commands.unlink(KeysAndValues.KEYS.toArray(new String[KeysAndValues.COUNT])))
                .expectNext((long) KeysAndValues.COUNT).verifyComplete();

        for (String mykey : KeysAndValues.KEYS) {
            String s1 = syncCommands.get(mykey);
            assertThat(s1).isNull();
        }
    }

    @Test
    void clientSetname() {

        String name = "test-cluster-client";

        assertThat(clusterClient.getPartitions().size()).isGreaterThan(0);

        StepVerifier.create(commands.clientSetname(name)).expectNext("OK").verifyComplete();

        for (RedisClusterNode redisClusterNode : clusterClient.getPartitions()) {
            RedisClusterCommands<String, String> nodeConnection = commands.getStatefulConnection().sync()
                    .getConnection(redisClusterNode.getNodeId());
            assertThat(nodeConnection.clientList()).contains(name);
        }

        StepVerifier.create(commands.clientGetname()).expectNext(name).verifyComplete();
    }

    @Test
    void clientSetnameRunOnError() {

        try {
            StepVerifier.create(commands.clientSetname("not allowed")).expectError().verify();
        } catch (RuntimeException e) {

            // sometimes reactor.core.Exceptions$CancelException: The subscriber has denied dispatching happens
            if (!e.getClass().getSimpleName().contains("CancelException")) {
                throw e;
            }
        }
    }

    @Test
    void dbSize() {

        writeKeysToTwoNodes();

        StepVerifier.create(commands.dbsize()).expectNext(2L).verifyComplete();
    }

    @Test
    void flushall() {

        writeKeysToTwoNodes();

        StepVerifier.create(commands.flushall()).expectNext("OK").verifyComplete();

        Long dbsize = syncCommands.dbsize();
        assertThat(dbsize).isEqualTo(0);
    }

    @Test
    void flushallSync() {

        writeKeysToTwoNodes();

        StepVerifier.create(commands.flushall(FlushMode.SYNC)).expectNext("OK").verifyComplete();

        Long dbsize = syncCommands.dbsize();
        assertThat(dbsize).isEqualTo(0);
    }

    @Test
    void flushallAsync() {

        writeKeysToTwoNodes();

        StepVerifier.create(commands.flushall(FlushMode.ASYNC)).expectNext("OK").verifyComplete();

        Long dbsize = syncCommands.dbsize();
        assertThat(dbsize).isEqualTo(0);
    }

    @Test
    void flushdb() {

        writeKeysToTwoNodes();

        StepVerifier.create(commands.flushdb()).expectNext("OK").verifyComplete();

        Long dbsize = syncCommands.dbsize();
        assertThat(dbsize).isEqualTo(0);
    }

    @Test
    void flushdbSync() {

        writeKeysToTwoNodes();

        StepVerifier.create(commands.flushdb(FlushMode.SYNC)).expectNext("OK").verifyComplete();

        Long dbsize = syncCommands.dbsize();
        assertThat(dbsize).isEqualTo(0);
    }

    @Test
    void flushdbAsync() {

        writeKeysToTwoNodes();

        StepVerifier.create(commands.flushdb(FlushMode.ASYNC)).expectNext("OK").verifyComplete();

        Long dbsize = syncCommands.dbsize();
        assertThat(dbsize).isEqualTo(0);
    }

    @Test
    void keys() {

        writeKeysToTwoNodes();

        StepVerifier.create(commands.keys("*")).recordWith(ConcurrentSet::new).expectNextCount(2)
                .consumeRecordedWith(actual -> assertThat(actual).contains(KEY_ON_NODE_1, KEY_ON_NODE_2)).verifyComplete();
    }

    @Test
    void keysDoesNotRunIntoRaceConditions() {

        List<RedisFuture<?>> futures = new ArrayList<>();
        RedisClusterAsyncCommands<String, String> async = commands.getStatefulConnection().async();
        TestFutures.awaitOrTimeout(async.flushall());

        for (int i = 0; i < 1000; i++) {
            futures.add(async.set("key-" + i, "value-" + i));
        }

        TestFutures.awaitOrTimeout(futures);

        for (int i = 0; i < 1000; i++) {
            CompletableFuture<Long> future = commands.keys("*").count().toFuture();
            TestFutures.awaitOrTimeout(future);
            assertThat(future).isCompletedWithValue(1000L);
        }
    }

    @Test
    void keysStreaming() {

        writeKeysToTwoNodes();
        ListStreamingAdapter<String> result = new ListStreamingAdapter<>();

        StepVerifier.create(commands.keys(result, "*")).expectNext(2L).verifyComplete();
        assertThat(result.getList()).contains(KEY_ON_NODE_1, KEY_ON_NODE_2);
    }

    @Test
    void randomKey() {

        writeKeysToTwoNodes();

        StepVerifier.create(commands.randomkey())
                .consumeNextWith(actual -> assertThat(actual).isIn(KEY_ON_NODE_1, KEY_ON_NODE_2)).verifyComplete();
    }

    @Test
    void scriptFlush() {
        StepVerifier.create(commands.scriptFlush()).expectNext("OK").verifyComplete();
    }

    @Test
    void scriptKill() {
        StepVerifier.create(commands.scriptKill()).expectNext("OK").verifyComplete();
    }

    @Test
    void scriptLoad() {

        scriptFlush();

        String script = "return true";

        String sha = Base16.digest(script.getBytes());

        StepVerifier.create(commands.scriptExists(sha)).expectNext(false).verifyComplete();

        StepVerifier.create(commands.scriptLoad(script)).expectNext(sha).verifyComplete();

        StepVerifier.create(commands.scriptExists(sha)).expectNext(true).verifyComplete();
    }

    @Test
    @Disabled("Run me manually, I will shutdown all your cluster nodes so you need to restart the Redis Cluster after this test")
    void shutdown() {
        commands.shutdown(true).subscribe();
    }

    @Test
    void readFromReplicas() {

        RedisClusterReactiveCommands<String, String> connection = commands.getConnection(ClusterTestSettings.host,
                ClusterTestSettings.port4);
        connection.readOnly().subscribe();
        commands.set(key, value).subscribe();

        NodeSelectionAsyncIntegrationTests.waitForReplication(commands.getStatefulConnection().async(), ClusterTestSettings.key,
                ClusterTestSettings.port4);

        AtomicBoolean error = new AtomicBoolean();
        connection.get(key).doOnError(throwable -> error.set(true)).block();

        assertThat(error.get()).isFalse();

        connection.readWrite().subscribe();

        StepVerifier.create(connection.get(key)).expectError(RedisCommandExecutionException.class).verify();
    }

    @Test
    void clusterScan() {

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
    void clusterScanWithArgs() {

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
    void clusterScanStreaming() {

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
    void clusterScanStreamingWithArgs() {

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

        assertThat(adapter.getList())
                .containsAll(KeysAndValues.KEYS.stream().filter(k -> k.startsWith("a")).collect(Collectors.toList()));
    }

    private void writeKeysToTwoNodes() {
        syncCommands.set(KEY_ON_NODE_1, value);
        syncCommands.set(KEY_ON_NODE_2, value);
    }

    Map<String, String> prepareMset() {
        Map<String, String> mset = new HashMap<>();
        for (char c = 'a'; c < 'z'; c++) {
            String key = new String(new char[] { c, c, c });
            mset.put(key, "value-" + key);
        }
        return mset;
    }

}
