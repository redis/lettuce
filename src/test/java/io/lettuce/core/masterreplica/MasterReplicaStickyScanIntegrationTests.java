/*
 * Copyright 2020-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.masterreplica;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import io.lettuce.core.AbstractRedisClientTest;
import io.lettuce.core.KeyScanCursor;
import io.lettuce.core.MapScanCursor;
import io.lettuce.core.ReadFrom;
import io.lettuce.core.RedisURI;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScoredValueScanCursor;
import io.lettuce.core.ValueScanCursor;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.models.role.RedisInstance;
import io.lettuce.core.models.role.RoleParser;
import io.lettuce.test.TestFutures;
import io.lettuce.test.WithPassword;
import io.lettuce.test.settings.TestSettings;

/**
 * Integration tests for sticky {@code SCAN} cursors on Master/Replica connections,
 * <a href="https://github.com/redis/lettuce/issues/3287">#3287</a>.
 *
 * {@code SCAN} family cursors are node-local. When {@link ReadFrom} spreads reads across nodes, continuation requests (cursor
 * != 0) must be routed to the node that served the initial request, otherwise the iteration yields undefined results
 * (missing/duplicate elements). These tests iterate by passing the <b>returned cursor object</b> back into the next call (the
 * canonical usage, also used by {@code ScanIterator}/{@code ScanStream}), which is the carrier for the cursor's source node.
 *
 * Matrix: {SCAN, HSCAN, SSCAN, ZSCAN} x {sync, async, reactive} under {@link ReadFrom#ANY}, plus a {@link ReadFrom#UPSTREAM}
 * control per command (single-node iteration, must stay green). Runs against the standard master/replica pair provided by the
 * project's docker-env ({@link TestSettings#port(int) port(3)}/{@code port(4)}).
 *
 * @author Sanghun Lee
 */
@Tag(INTEGRATION_TEST)
class MasterReplicaStickyScanIntegrationTests extends AbstractRedisClientTest {

    enum Api {
        SYNC, ASYNC, REACTIVE
    }

    private static final String HASH_KEY = "sticky-3287-hash";

    private static final String SET_KEY = "sticky-3287-set";

    private static final String ZSET_KEY = "sticky-3287-zset";

    private static final String KEY_PREFIX = "sticky-3287-key-";

    private static final String KEY_MATCH = KEY_PREFIX + "*";

    // > *-max-listpack-entries (default 128) so collections are hashtable-encoded and the SCAN family actually
    // paginates. Listpack-encoded collections return everything with cursor 0 and would hide the bug.
    private static final int ELEMENT_COUNT = 1000;

    private static final int PAGE_SIZE = 10;

    private static final int MAX_CALLS = 100_000;

    private RedisURI upstream;

    private RedisURI replica;

    private RedisCommands<String, String> connection1;

    private RedisCommands<String, String> connection2;

    private StatefulRedisMasterReplicaConnection<String, String> connection;

    @BeforeEach
    void before() {

        RedisURI node1 = RedisURI.Builder.redis(host, TestSettings.port(3)).build();
        RedisURI node2 = RedisURI.Builder.redis(host, TestSettings.port(4)).build();

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

        upstream.setAuthentication(passwd);
        replica.setAuthentication(passwd);

        connection = MasterReplica.connect(client, StringCodec.UTF8, Arrays.asList(upstream, replica));

        seed();
    }

    @AfterEach
    void after() {

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

    // seed on master, then wait for the replica to catch up before spreading reads across nodes.
    private void seed() {

        connection.setReadFrom(ReadFrom.UPSTREAM);

        RedisCommands<String, String> sync = connection.sync();
        sync.flushall();

        Map<String, String> hash = new HashMap<>();
        Map<String, String> keys = new HashMap<>();
        String[] members = new String[ELEMENT_COUNT];
        Object[] scoredValues = new Object[ELEMENT_COUNT * 2];

        for (int i = 0; i < ELEMENT_COUNT; i++) {
            hash.put("field-" + i, "value-" + i);
            keys.put(KEY_PREFIX + i, "value-" + i);
            members[i] = "member-" + i;
            scoredValues[i * 2] = (double) i;
            scoredValues[i * 2 + 1] = "member-" + i;
        }

        sync.hset(HASH_KEY, hash);
        sync.mset(keys);
        sync.sadd(SET_KEY, members);
        sync.zadd(ZSET_KEY, scoredValues);

        sync.waitForReplication(1, 5000);
    }

    // --- ReadFrom.ANY: reads spread across master + replica. Without sticky cursors this is #3287 (undefined results).

    @ParameterizedTest
    @EnumSource(Api.class)
    void scanWithReadFromAnyReturnsAllKeys(Api api) {
        connection.setReadFrom(ReadFrom.ANY);
        assertThat(scanAll(api)).as("full SCAN under ReadFrom.ANY must return every key").hasSize(ELEMENT_COUNT);
    }

    @ParameterizedTest
    @EnumSource(Api.class)
    void hscanWithReadFromAnyReturnsAllFields(Api api) {
        connection.setReadFrom(ReadFrom.ANY);
        assertThat(hscanAll(api)).as("full HSCAN under ReadFrom.ANY must return every field").hasSize(ELEMENT_COUNT);
    }

    @ParameterizedTest
    @EnumSource(Api.class)
    void sscanWithReadFromAnyReturnsAllMembers(Api api) {
        connection.setReadFrom(ReadFrom.ANY);
        assertThat(sscanAll(api)).as("full SSCAN under ReadFrom.ANY must return every member").hasSize(ELEMENT_COUNT);
    }

    @ParameterizedTest
    @EnumSource(Api.class)
    void zscanWithReadFromAnyReturnsAllMembers(Api api) {
        connection.setReadFrom(ReadFrom.ANY);
        assertThat(zscanAll(api)).as("full ZSCAN under ReadFrom.ANY must return every member").hasSize(ELEMENT_COUNT);
    }

    // --- ReadFrom.UPSTREAM controls: single-node iteration, green today, guards against regressions.

    @Test
    void scanWithReadFromUpstreamReturnsAllKeys() {
        connection.setReadFrom(ReadFrom.UPSTREAM);
        assertThat(scanAll(Api.SYNC)).hasSize(ELEMENT_COUNT);
    }

    @Test
    void hscanWithReadFromUpstreamReturnsAllFields() {
        connection.setReadFrom(ReadFrom.UPSTREAM);
        assertThat(hscanAll(Api.SYNC)).hasSize(ELEMENT_COUNT);
    }

    @Test
    void sscanWithReadFromUpstreamReturnsAllMembers() {
        connection.setReadFrom(ReadFrom.UPSTREAM);
        assertThat(sscanAll(Api.SYNC)).hasSize(ELEMENT_COUNT);
    }

    @Test
    void zscanWithReadFromUpstreamReturnsAllMembers() {
        connection.setReadFrom(ReadFrom.UPSTREAM);
        assertThat(zscanAll(Api.SYNC)).hasSize(ELEMENT_COUNT);
    }

    // --- full iterations, passing the returned cursor object back into the next call.

    private Set<String> scanAll(Api api) {

        ScanArgs args = ScanArgs.Builder.matches(KEY_MATCH).limit(PAGE_SIZE);
        Set<String> seen = new HashSet<>();
        KeyScanCursor<String> cursor = null;
        int calls = 0;

        do {
            cursor = scanNext(api, cursor, args);
            seen.addAll(cursor.getKeys());
        } while (!cursor.isFinished() && ++calls < MAX_CALLS);

        return seen;
    }

    private KeyScanCursor<String> scanNext(Api api, KeyScanCursor<String> cursor, ScanArgs args) {
        switch (api) {
            case SYNC:
                return cursor == null ? connection.sync().scan(args) : connection.sync().scan(cursor, args);
            case ASYNC:
                return TestFutures
                        .getOrTimeout(cursor == null ? connection.async().scan(args) : connection.async().scan(cursor, args));
            case REACTIVE:
                return (cursor == null ? connection.reactive().scan(args) : connection.reactive().scan(cursor, args))
                        .block(Duration.ofSeconds(10));
            default:
                throw new IllegalStateException("Unsupported API " + api);
        }
    }

    private Set<String> hscanAll(Api api) {

        ScanArgs args = ScanArgs.Builder.limit(PAGE_SIZE);
        Set<String> seen = new HashSet<>();
        MapScanCursor<String, String> cursor = null;
        int calls = 0;

        do {
            cursor = hscanNext(api, cursor, args);
            seen.addAll(cursor.getMap().keySet());
        } while (!cursor.isFinished() && ++calls < MAX_CALLS);

        return seen;
    }

    private MapScanCursor<String, String> hscanNext(Api api, MapScanCursor<String, String> cursor, ScanArgs args) {
        switch (api) {
            case SYNC:
                return cursor == null ? connection.sync().hscan(HASH_KEY, args)
                        : connection.sync().hscan(HASH_KEY, cursor, args);
            case ASYNC:
                return TestFutures.getOrTimeout(cursor == null ? connection.async().hscan(HASH_KEY, args)
                        : connection.async().hscan(HASH_KEY, cursor, args));
            case REACTIVE:
                return (cursor == null ? connection.reactive().hscan(HASH_KEY, args)
                        : connection.reactive().hscan(HASH_KEY, cursor, args)).block(Duration.ofSeconds(10));
            default:
                throw new IllegalStateException("Unsupported API " + api);
        }
    }

    private Set<String> sscanAll(Api api) {

        ScanArgs args = ScanArgs.Builder.limit(PAGE_SIZE);
        Set<String> seen = new HashSet<>();
        ValueScanCursor<String> cursor = null;
        int calls = 0;

        do {
            cursor = sscanNext(api, cursor, args);
            seen.addAll(cursor.getValues());
        } while (!cursor.isFinished() && ++calls < MAX_CALLS);

        return seen;
    }

    private ValueScanCursor<String> sscanNext(Api api, ValueScanCursor<String> cursor, ScanArgs args) {
        switch (api) {
            case SYNC:
                return cursor == null ? connection.sync().sscan(SET_KEY, args) : connection.sync().sscan(SET_KEY, cursor, args);
            case ASYNC:
                return TestFutures.getOrTimeout(cursor == null ? connection.async().sscan(SET_KEY, args)
                        : connection.async().sscan(SET_KEY, cursor, args));
            case REACTIVE:
                return (cursor == null ? connection.reactive().sscan(SET_KEY, args)
                        : connection.reactive().sscan(SET_KEY, cursor, args)).block(Duration.ofSeconds(10));
            default:
                throw new IllegalStateException("Unsupported API " + api);
        }
    }

    private Set<String> zscanAll(Api api) {

        ScanArgs args = ScanArgs.Builder.limit(PAGE_SIZE);
        Set<String> seen = new HashSet<>();
        ScoredValueScanCursor<String> cursor = null;
        int calls = 0;

        do {
            cursor = zscanNext(api, cursor, args);
            cursor.getValues().forEach(scoredValue -> seen.add(scoredValue.getValue()));
        } while (!cursor.isFinished() && ++calls < MAX_CALLS);

        return seen;
    }

    private ScoredValueScanCursor<String> zscanNext(Api api, ScoredValueScanCursor<String> cursor, ScanArgs args) {
        switch (api) {
            case SYNC:
                return cursor == null ? connection.sync().zscan(ZSET_KEY, args)
                        : connection.sync().zscan(ZSET_KEY, cursor, args);
            case ASYNC:
                return TestFutures.getOrTimeout(cursor == null ? connection.async().zscan(ZSET_KEY, args)
                        : connection.async().zscan(ZSET_KEY, cursor, args));
            case REACTIVE:
                return (cursor == null ? connection.reactive().zscan(ZSET_KEY, args)
                        : connection.reactive().zscan(ZSET_KEY, cursor, args)).block(Duration.ofSeconds(10));
            default:
                throw new IllegalStateException("Unsupported API " + api);
        }
    }

}
