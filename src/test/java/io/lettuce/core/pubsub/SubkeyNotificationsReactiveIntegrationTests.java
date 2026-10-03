/*
 * Copyright 2024-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.pubsub;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.BlockingQueue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import reactor.core.Disposable;
import io.lettuce.core.AbstractRedisClientTest;
import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.internal.LettuceFactories;
import io.lettuce.core.pubsub.api.reactive.ChannelMessage;
import io.lettuce.core.pubsub.api.reactive.PatternMessage;
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands;
import io.lettuce.test.Wait;

/**
 * Reactive integration tests for the Redis 8.8 Subkey Notifications feature.
 * <p>
 * Validates that the same wire-format notifications are observable through {@link RedisPubSubReactiveCommands} for each of the
 * four channel families. Assertions are limited to one representative test per family since the wire format itself is
 * exhaustively covered by {@link SubkeyNotificationsIntegrationTests}.
 *
 * @author Aleksandar Todorov
 */
@Tag(INTEGRATION_TEST)
class SubkeyNotificationsReactiveIntegrationTests extends AbstractRedisClientTest {

    private StatefulRedisPubSubConnection<String, String> pubSubConnection;

    private RedisPubSubReactiveCommands<String, String> pubsub;

    private String originalNotifyConfig;

    @BeforeEach
    void openPubSubConnection() {
        pubSubConnection = client.connectPubSub();
        pubsub = pubSubConnection.commands(RedisPubSubReactiveCommands.factory());

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

    @Test
    void subkeyspace_observable() {
        String hashKey = "rx-subkeyspace-" + System.nanoTime();
        String channel = SubkeyNotificationsIntegrationTests.CHANNEL_PREFIX_SUBKEYSPACE + hashKey;
        BlockingQueue<ChannelMessage<String, String>> received = LettuceFactories.newBlockingQueue();
        Disposable d = pubsub.observeChannels().doOnNext(received::add).subscribe();

        pubsub.subscribe(channel).block();
        redis.hset(hashKey, "fld", "v1");

        Wait.untilTrue(() -> received.stream().anyMatch(m -> channel.equals(m.getChannel()))).waitOrTimeout();
        ChannelMessage<String, String> m = received.stream().filter(x -> channel.equals(x.getChannel())).findFirst().get();
        assertThat(m.getChannel()).isEqualTo(channel);
        assertThat(m.getMessage()).isEqualTo("hset|3:fld");
        d.dispose();
    }

    @Test
    void subkeyevent_observable() {
        String hashKey = "rx-subkeyevent-" + System.nanoTime();
        String channel = SubkeyNotificationsIntegrationTests.CHANNEL_PREFIX_SUBKEYEVENT + "hset";
        BlockingQueue<ChannelMessage<String, String>> received = LettuceFactories.newBlockingQueue();
        Disposable d = pubsub.observeChannels().doOnNext(received::add).subscribe();

        pubsub.subscribe(channel).block();
        redis.hset(hashKey, "fld", "v1");

        Wait.untilTrue(() -> received.stream().anyMatch(m -> channel.equals(m.getChannel()))).waitOrTimeout();
        ChannelMessage<String, String> m = received.stream().filter(x -> channel.equals(x.getChannel())).findFirst().get();
        assertThat(m.getChannel()).isEqualTo(channel);
        assertThat(m.getMessage()).isEqualTo(hashKey.length() + ":" + hashKey + "|3:fld");
        d.dispose();
    }

    @Test
    void subkeyspaceitem_observable() {
        String hashKey = "rx-subkeyspaceitem-" + System.nanoTime();
        String field = "fld";
        String pattern = SubkeyNotificationsIntegrationTests.CHANNEL_PREFIX_SUBKEYSPACEITEM + hashKey + "\n*";
        String expectedChannel = SubkeyNotificationsIntegrationTests.CHANNEL_PREFIX_SUBKEYSPACEITEM + hashKey + "\n" + field;

        BlockingQueue<PatternMessage<String, String>> received = LettuceFactories.newBlockingQueue();
        Disposable d = pubsub.observePatterns().doOnNext(received::add).subscribe();

        pubsub.psubscribe(pattern).block();
        redis.hset(hashKey, field, "v1");

        Wait.untilTrue(() -> received.stream().anyMatch(m -> expectedChannel.equals(m.getChannel()))).waitOrTimeout();
        PatternMessage<String, String> m = received.stream().filter(x -> expectedChannel.equals(x.getChannel())).findFirst()
                .get();
        assertThat(m.getChannel()).isEqualTo(expectedChannel);
        assertThat(m.getPattern()).isEqualTo(pattern);
        assertThat(m.getMessage()).isEqualTo("hset");
        d.dispose();
    }

    @Test
    void subkeyspaceevent_observable() {
        String hashKey = "rx-subkeyspaceevent-" + System.nanoTime();
        String channel = SubkeyNotificationsIntegrationTests.CHANNEL_PREFIX_SUBKEYSPACEEVENT + "hset|" + hashKey;
        BlockingQueue<ChannelMessage<String, String>> received = LettuceFactories.newBlockingQueue();
        Disposable d = pubsub.observeChannels().doOnNext(received::add).subscribe();

        pubsub.subscribe(channel).block();
        redis.hset(hashKey, "fld", "v1");

        Wait.untilTrue(() -> received.stream().anyMatch(m -> channel.equals(m.getChannel()))).waitOrTimeout();
        ChannelMessage<String, String> m = received.stream().filter(x -> channel.equals(x.getChannel())).findFirst().get();
        assertThat(m.getChannel()).isEqualTo(channel);
        assertThat(m.getMessage()).isEqualTo("3:fld");
        d.dispose();
    }

}
