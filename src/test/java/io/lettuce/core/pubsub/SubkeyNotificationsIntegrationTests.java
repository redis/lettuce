/*
 * Copyright 2024-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.pubsub;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.AbstractRedisClientTest;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.support.CapturingPubSubListener;
import io.lettuce.core.support.CapturingPubSubListener.Notification;

/**
 * Integration tests for the Redis 8.8 Subkey Notifications feature: four new keyspace channel families gated by the {@code S},
 * {@code T}, {@code I}, {@code V} flags. Tests are skipped via a {@code CONFIG SET notify-keyspace-events AKEhSTIV} probe; this
 * is intentional, since the feature adds no new RESP command and the development image misreports {@code redis_version}.
 *
 * @author Aleksandar Todorov
 */
@Tag(INTEGRATION_TEST)
class SubkeyNotificationsIntegrationTests extends AbstractRedisClientTest {

    static final String CHANNEL_PREFIX_SUBKEYSPACE = "__subkeyspace@0__:";

    static final String CHANNEL_PREFIX_SUBKEYEVENT = "__subkeyevent@0__:";

    static final String CHANNEL_PREFIX_SUBKEYSPACEITEM = "__subkeyspaceitem@0__:";

    static final String CHANNEL_PREFIX_SUBKEYSPACEEVENT = "__subkeyspaceevent@0__:";

    private StatefulRedisPubSubConnection<String, String> pubSubConnection;

    private CapturingPubSubListener<String, String> listener;

    private String originalNotifyConfig;

    protected ClientOptions getOptions() {
        return ClientOptions.builder().build();
    }

    @BeforeEach
    void openPubSubConnection() {
        client.setOptions(getOptions());
        pubSubConnection = client.connectPubSub();
        listener = new CapturingPubSubListener<>();
        pubSubConnection.addListener(listener);

        RedisCommands<String, String> sync = redis;
        originalNotifyConfig = sync.configGet("notify-keyspace-events").getOrDefault("notify-keyspace-events", "");

        try {
            sync.configSet("notify-keyspace-events", "AKEhSTIV");
        } catch (RedisCommandExecutionException e) {
            Assumptions.abort("Server does not support subkey notification flags (STIV): " + e.getMessage());
        }
    }

    @AfterEach
    void closePubSubConnection() {
        if (redis != null) {
            try {
                redis.configSet("notify-keyspace-events", originalNotifyConfig == null ? "" : originalNotifyConfig);
            } catch (Exception ignore) {
                // best-effort restore
            }
        }
        if (pubSubConnection != null) {
            pubSubConnection.close();
        }
    }

    // -------------------------------------------------------------------- subkeyspace (flag S)

    @Test
    void subkeyspace_singleHashField() {
        String hashKey = "subkeyspace-basic-" + System.nanoTime();
        String channel = CHANNEL_PREFIX_SUBKEYSPACE + hashKey;

        pubSubConnection.sync().subscribe(channel);

        redis.hset(hashKey, "fld", "v1");

        Notification<String, String> n = listener.expectMessageOn(channel);
        assertThat(n.getChannel()).isEqualTo(channel);
        assertThat(n.getPattern()).isNull();
        assertThat(n.getMessage()).isEqualTo("hset|3:fld");
    }

    @Test
    void subkeyspace_multipleFields() {
        String hashKey = "subkeyspace-multi-" + System.nanoTime();
        String channel = CHANNEL_PREFIX_SUBKEYSPACE + hashKey;

        pubSubConnection.sync().subscribe(channel);

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("f1", "v1");
        fields.put("f22", "v2");
        redis.hmset(hashKey, fields);

        Notification<String, String> n = listener.expectMessageOn(channel);
        assertThat(n.getChannel()).isEqualTo(channel);
        assertThat(n.getPattern()).isNull();
        assertThat(n.getMessage()).isEqualTo("hset|2:f1,3:f22");
    }

    @Test
    void subkeyspace_psubscribePrefix() {
        String prefix = "subkeyspace-pattern-" + System.nanoTime() + "-";
        String hashKey1 = prefix + "a";
        String hashKey2 = prefix + "b";
        String pattern = CHANNEL_PREFIX_SUBKEYSPACE + prefix + "*";
        String channel1 = CHANNEL_PREFIX_SUBKEYSPACE + hashKey1;
        String channel2 = CHANNEL_PREFIX_SUBKEYSPACE + hashKey2;

        pubSubConnection.sync().psubscribe(pattern);

        redis.hset(hashKey1, "fld", "v1");
        redis.hset(hashKey2, "fld", "v2");

        Notification<String, String> n1 = listener.expectMessageOn(channel1);
        Notification<String, String> n2 = listener.expectMessageOn(channel2);
        assertThat(n1.getChannel()).isEqualTo(channel1);
        assertThat(n2.getChannel()).isEqualTo(channel2);
        assertThat(n1.getPattern()).isEqualTo(pattern);
        assertThat(n2.getPattern()).isEqualTo(pattern);
        assertThat(n1.getMessage()).isEqualTo("hset|3:fld");
        assertThat(n2.getMessage()).isEqualTo("hset|3:fld");
    }

    // -------------------------------------------------------------------- subkeyevent (flag T)

    @Test
    void subkeyevent_singleHashField() {
        String hashKey = "subkeyevent-basic-" + System.nanoTime();
        String channel = CHANNEL_PREFIX_SUBKEYEVENT + "hset";

        pubSubConnection.sync().subscribe(channel);

        redis.hset(hashKey, "fld", "v1");

        Notification<String, String> n = listener.expectMessageOn(channel);
        assertThat(n.getChannel()).isEqualTo(channel);
        assertThat(n.getPattern()).isNull();
        assertThat(n.getMessage()).isEqualTo(hashKey.length() + ":" + hashKey + "|3:fld");
    }

    @Test
    void subkeyevent_multipleFields() {
        String hashKey = "subkeyevent-multi-" + System.nanoTime();
        String channel = CHANNEL_PREFIX_SUBKEYEVENT + "hset";

        pubSubConnection.sync().subscribe(channel);

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("f1", "v1");
        fields.put("f22", "v2");
        redis.hmset(hashKey, fields);

        Notification<String, String> n = listener.expectMessageOn(channel);
        assertThat(n.getChannel()).isEqualTo(channel);
        assertThat(n.getPattern()).isNull();
        assertThat(n.getMessage()).isEqualTo(hashKey.length() + ":" + hashKey + "|2:f1,3:f22");
    }

    // -------------------------------------------------------------------- subkeyspaceitem (flag I)

    @Test
    void subkeyspaceitem_singleHashField() {
        String hashKey = "subkeyspaceitem-basic-" + System.nanoTime();
        String field = "fld";
        String channel = CHANNEL_PREFIX_SUBKEYSPACEITEM + hashKey + "\n" + field;

        pubSubConnection.sync().subscribe(channel);

        redis.hset(hashKey, field, "v1");

        Notification<String, String> n = listener.expectMessageOn(channel);
        assertThat(n.getChannel()).isEqualTo(channel);
        assertThat(n.getPattern()).isNull();
        assertThat(n.getMessage()).isEqualTo("hset");
    }

    @Test
    void subkeyspaceitem_filtersToTargetFieldOnly() {
        String hashKey = "subkeyspaceitem-filter-" + System.nanoTime();
        String targetChannel = CHANNEL_PREFIX_SUBKEYSPACEITEM + hashKey + "\nf2";

        pubSubConnection.sync().subscribe(targetChannel);

        redis.hset(hashKey, "f1", "v1");
        redis.hset(hashKey, "f2", "v2");
        redis.hset(hashKey, "f3", "v3");

        Notification<String, String> n = listener.expectMessageOn(targetChannel);
        assertThat(n.getChannel()).isEqualTo(targetChannel);
        assertThat(n.getPattern()).isNull();
        assertThat(n.getMessage()).isEqualTo("hset");

        listener.expectNoMessageOn(targetChannel, 250, TimeUnit.MILLISECONDS);
    }

    @Test
    void subkeyspaceitem_multipleFieldsEmitOneEventPerField() {
        String hashKey = "subkeyspaceitem-multi-" + System.nanoTime();
        String pattern = CHANNEL_PREFIX_SUBKEYSPACEITEM + hashKey + "\n*";
        String channelF1 = CHANNEL_PREFIX_SUBKEYSPACEITEM + hashKey + "\nf1";
        String channelF22 = CHANNEL_PREFIX_SUBKEYSPACEITEM + hashKey + "\nf22";

        pubSubConnection.sync().psubscribe(pattern);

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("f1", "v1");
        fields.put("f22", "v2");
        redis.hmset(hashKey, fields);

        Notification<String, String> n1 = listener.expectMessageOn(channelF1);
        Notification<String, String> n2 = listener.expectMessageOn(channelF22);
        assertThat(n1.getChannel()).isEqualTo(channelF1);
        assertThat(n2.getChannel()).isEqualTo(channelF22);
        assertThat(n1.getPattern()).isEqualTo(pattern);
        assertThat(n2.getPattern()).isEqualTo(pattern);
        assertThat(n1.getMessage()).isEqualTo("hset");
        assertThat(n2.getMessage()).isEqualTo("hset");
    }

    // -------------------------------------------------------------------- subkeyspaceevent (flag V)

    @Test
    void subkeyspaceevent_singleHashField() {
        String hashKey = "subkeyspaceevent-basic-" + System.nanoTime();
        String channel = CHANNEL_PREFIX_SUBKEYSPACEEVENT + "hset|" + hashKey;

        pubSubConnection.sync().subscribe(channel);

        redis.hset(hashKey, "fld", "v1");

        Notification<String, String> n = listener.expectMessageOn(channel);
        assertThat(n.getChannel()).isEqualTo(channel);
        assertThat(n.getPattern()).isNull();
        assertThat(n.getMessage()).isEqualTo("3:fld");
    }

    @Test
    void subkeyspaceevent_multipleFields() {
        String hashKey = "subkeyspaceevent-multi-" + System.nanoTime();
        String channel = CHANNEL_PREFIX_SUBKEYSPACEEVENT + "hset|" + hashKey;

        pubSubConnection.sync().subscribe(channel);

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("f1", "v1");
        fields.put("f22", "v2");
        redis.hmset(hashKey, fields);

        Notification<String, String> n = listener.expectMessageOn(channel);
        assertThat(n.getChannel()).isEqualTo(channel);
        assertThat(n.getPattern()).isNull();
        assertThat(n.getMessage()).isEqualTo("2:f1,3:f22");
    }

}
