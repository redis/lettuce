/*
 * Copyright 2018-Present, Redis Ltd. and Contributors
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
package io.lettuce.core.commands;

import io.lettuce.core.*;
import io.lettuce.core.XReadArgs.StreamOffset;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.models.stream.ClaimedMessages;
import io.lettuce.core.models.stream.PendingMessage;
import io.lettuce.core.models.stream.PendingMessages;
import io.lettuce.core.models.stream.StreamEntryDeletionResult;
import io.lettuce.core.output.NestedMultiOutput;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.condition.EnabledOnCommand;
import io.lettuce.test.condition.RedisConditions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static io.lettuce.core.protocol.CommandType.XINFO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration tests for {@link io.lettuce.core.api.sync.RedisStreamCommands}.
 *
 * @author Mark Paluch
 * @author dengliming
 */
@ExtendWith(LettuceExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledOnCommand("XADD")
@Tag(INTEGRATION_TEST)
public class StreamCommandIntegrationTests extends TestSupport {

    private final RedisCommands<String, String> redis;

    @Inject
    protected StreamCommandIntegrationTests(RedisCommands<String, String> redis) {
        this.redis = redis;
    }

    @BeforeEach
    void setUp() {
        this.redis.flushall();
    }

    @Test
    void xadd() {

        assertThat(redis.xadd(key, Collections.singletonMap("key", "value"))).endsWith("-0");
        assertThat(redis.xadd(key, "foo", "bar")).isNotEmpty();
    }

    @Test
    void xaddMaxLen() {

        String id = redis.xadd(key, XAddArgs.Builder.maxlen(5), "foo", "bar");

        for (int i = 0; i < 5; i++) {
            redis.xadd(key, XAddArgs.Builder.maxlen(5), "foo", "bar");
        }

        List<StreamMessage<String, String>> messages = redis.xrange(key,
                Range.from(Range.Boundary.including(id), Range.Boundary.unbounded()));

        assertThat(messages).hasSize(5);
    }

    @Test
    void xaddMaxLenEfficientTrimming() {

        String id = redis.xadd(key, XAddArgs.Builder.maxlen(5).approximateTrimming(), "foo", "bar");

        assertThat(id).isNotNull();
    }

    @Test
    @EnabledOnCommand("XAUTOCLAIM") // Redis 6.2
    public void xaddMinidLimit() {
        redis.xadd(key, XAddArgs.Builder.minId("2").id("3"), "foo", "bar");
        redis.xadd(key, XAddArgs.Builder.minId("2").id("4"), "foo", "bar");
        assertThat(redis.xlen(key)).isEqualTo(2);
        redis.xadd(key, XAddArgs.Builder.minId("4").id("5"), "foo", "bar");
        assertThat(redis.xlen(key)).isEqualTo(2);
        redis.del(key);

        redis.configSet("stream-node-max-entries", "1");
        for (int i = 0; i < 5; i++) {
            redis.xadd(key, "foo", "bar");
        }
        redis.xadd(key, XAddArgs.Builder.maxlen(2).approximateTrimming().limit(5l), "foo", "bar");
        assertThat(redis.xlen(key)).isEqualTo(2);
        redis.configSet("stream-node-max-entries", "100");
    }

    @Test
    @EnabledOnCommand("XAUTOCLAIM") // Redis 6.2
    void xaddWithNomkstream() {

        String id = redis.xadd(key, XAddArgs.Builder.nomkstream(), Collections.singletonMap("foo", "bar"));
        assertThat(id).isNull();
        assertThat(redis.exists(key)).isEqualTo(0L);
    }

    @Test
    void xdel() {

        List<String> ids = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            ids.add(redis.xadd(key, Collections.singletonMap("key", "value")));
        }

        Long deleted = redis.xdel(key, ids.get(0), "123456-0");

        assertThat(deleted).isEqualTo(1);

        List<StreamMessage<String, String>> messages = redis.xrange(key, Range.unbounded());
        assertThat(messages).hasSize(1);
    }

    @Test
    void xtrim() {

        List<String> ids = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ids.add(redis.xadd(key, Collections.singletonMap("key", "value")));
        }
        redis.xdel(key, ids.get(0), ids.get(2));
        assertThat(redis.xlen(key)).isBetween(8L, 10L);

        redis.xtrim(key, true, 8);

        assertThat(redis.xlen(key)).isLessThanOrEqualTo(10);

        redis.xtrim(key, XTrimArgs.Builder.maxlen(0).limit(0).approximateTrimming());

        assertThat(redis.xlen(key)).isLessThanOrEqualTo(10);
    }

    @Test
    @EnabledOnCommand("XAUTOCLAIM") // Redis 6.2
    void xtrimMinidLimit() {
        redis.xadd(key, XAddArgs.Builder.maxlen(3).id("3"), "foo", "bar");
        redis.xtrim(key, XTrimArgs.Builder.minId("4"));
        assertThat(redis.xlen(key)).isZero();

        List<String> ids = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ids.add(redis.xadd(key, Collections.singletonMap("key", "value")));
        }

        redis.xtrim(key, XTrimArgs.Builder.maxlen(8));
        assertThat(redis.xlen(key)).isEqualTo(8);
    }

    @Test
    void xrange() {

        List<String> ids = new ArrayList<>();
        for (int i = 0; i < 5; i++) {

            Map<String, String> body = new HashMap<>();
            body.put("key-1", "value-1-" + i);
            body.put("key-2", "value-2-" + i);

            ids.add(redis.xadd(key, body));
        }

        List<StreamMessage<String, String>> messages = redis.xrange(key, Range.unbounded());
        assertThat(messages).hasSize(5);

        StreamMessage<String, String> message = messages.get(0);

        Map<String, String> expectedBody = new HashMap<>();
        expectedBody.put("key-1", "value-1-0");
        expectedBody.put("key-2", "value-2-0");

        assertThat(message.getId()).contains("-");
        assertThat(message.getStream()).isEqualTo(key);
        assertThat(message.getBody()).isEqualTo(expectedBody);

        assertThat(redis.xrange(key, Range.unbounded(), Limit.from(2))).hasSize(2);

        List<StreamMessage<String, String>> range = redis.xrange(key, Range.create(ids.get(0), ids.get(1)));

        assertThat(range).hasSize(2);
        assertThat(range.get(0).getBody()).isEqualTo(expectedBody);
    }

    @Test
    @EnabledOnCommand("XAUTOCLAIM") // Redis 6.2
    void xrangeRanges() {

        String id1 = redis.xadd(key, Collections.singletonMap("key", "value"));
        String id2 = redis.xadd(key, Collections.singletonMap("key", "value"));
        String id3 = redis.xadd(key, Collections.singletonMap("key", "value"));

        assertThat(redis.xrange(key, Range.unbounded())).hasSize(3);
        assertThat(redis.xrange(key, Range.from(Range.Boundary.including(id1), Range.Boundary.excluding(id3)))).hasSize(2);
        assertThat(redis.xrange(key, Range.from(Range.Boundary.excluding(id1), Range.Boundary.excluding(id3)))).hasSize(1);
        assertThat(redis.xrange(key, Range.from(Range.Boundary.excluding(id1), Range.Boundary.including(id3)))).hasSize(2);
    }

    @Test
    void xrevrange() {

        for (int i = 0; i < 5; i++) {

            Map<String, String> body = new HashMap<>();
            body.put("key-1", "value-1-" + i);
            body.put("key-2", "value-2-" + i);

            redis.xadd(key, body);
        }

        List<StreamMessage<String, String>> messages = redis.xrevrange(key, Range.unbounded());
        assertThat(messages).hasSize(5);

        StreamMessage<String, String> message = messages.get(0);

        Map<String, String> expectedBody = new HashMap<>();
        expectedBody.put("key-1", "value-1-4");
        expectedBody.put("key-2", "value-2-4");

        assertThat(message.getId()).contains("-");
        assertThat(message.getStream()).isEqualTo(key);
        assertThat(message.getBody()).isEqualTo(expectedBody);
    }

    @Test
    void xreadSingleStream() {

        redis.xadd("stream-1", Collections.singletonMap("key1", "value1"));
        redis.xadd("stream-1", Collections.singletonMap("key2", "value2"));

        List<StreamMessage<String, String>> messages = redis.xread(XReadArgs.Builder.count(2),
                StreamOffset.from("stream-1", "0-0"));

        assertThat(messages).hasSize(2);
        StreamMessage<String, String> firstMessage = messages.get(0);

        assertThat(firstMessage.getStream()).isEqualTo("stream-1");
        assertThat(firstMessage.getBody()).hasSize(1).containsEntry("key1", "value1");
        assertThat(firstMessage.getMillisElapsedFromDelivery()).isNull();
        assertThat(firstMessage.getDeliveredCount()).isNull();

        StreamMessage<String, String> nextMessage = messages.get(1);

        assertThat(nextMessage.getStream()).isEqualTo("stream-1");
        assertThat(nextMessage.getBody()).hasSize(1).containsEntry("key2", "value2");
    }

    @Test
    void xreadMultipleStreams() {

        Map<String, String> biggerBody = new LinkedHashMap<>();
        biggerBody.put("key4", "value4");
        biggerBody.put("key5", "value5");

        String initial1 = redis.xadd("{s1}stream-1", Collections.singletonMap("key1", "value1"));
        String initial2 = redis.xadd("{s1}stream-2", Collections.singletonMap("key2", "value2"));
        String message1 = redis.xadd("{s1}stream-1", Collections.singletonMap("key3", "value3"));
        String message2 = redis.xadd("{s1}stream-2", biggerBody);

        List<StreamMessage<String, String>> messages = redis.xread(StreamOffset.from("{s1}stream-1", "0-0"),
                StreamOffset.from("{s1}stream-2", "0-0"));

        assertThat(messages).hasSize(4);

        StreamMessage<String, String> firstMessage = messages.get(0);

        assertThat(firstMessage.getId()).isEqualTo(initial1);
        assertThat(firstMessage.getStream()).isEqualTo("{s1}stream-1");
        assertThat(firstMessage.getBody()).hasSize(1).containsEntry("key1", "value1");
        assertThat(firstMessage.getMillisElapsedFromDelivery()).isNull();
        assertThat(firstMessage.getDeliveredCount()).isNull();

        StreamMessage<String, String> secondMessage = messages.get(3);

        assertThat(secondMessage.getId()).isEqualTo(message2);
        assertThat(secondMessage.getStream()).isEqualTo("{s1}stream-2");
        assertThat(secondMessage.getBody()).hasSize(2).containsEntry("key4", "value4");
        assertThat(secondMessage.getMillisElapsedFromDelivery()).isNull();
        assertThat(secondMessage.getDeliveredCount()).isNull();
    }

    @Test
    public void xreadTransactional() {

        String initial1 = redis.xadd("stream-1", Collections.singletonMap("key1", "value1"));
        String initial2 = redis.xadd("stream-2", Collections.singletonMap("key2", "value2"));

        redis.multi();
        redis.xadd("stream-1", Collections.singletonMap("key3", "value3"));
        redis.xadd("stream-2", Collections.singletonMap("key4", "value4"));
        redis.xread(StreamOffset.from("stream-1", initial1), StreamOffset.from("stream-2", initial2));

        TransactionResult exec = redis.exec();

        String message1 = exec.get(0);
        String message2 = exec.get(1);
        List<StreamMessage<String, String>> messages = exec.get(2);

        StreamMessage<String, String> firstMessage = messages.get(0);

        assertThat(firstMessage.getId()).isEqualTo(message1);
        assertThat(firstMessage.getStream()).isEqualTo("stream-1");
        assertThat(firstMessage.getBody()).containsEntry("key3", "value3");
        assertThat(firstMessage.getMillisElapsedFromDelivery()).isNull();
        assertThat(firstMessage.getDeliveredCount()).isNull();

        StreamMessage<String, String> secondMessage = messages.get(1);

        assertThat(secondMessage.getId()).isEqualTo(message2);
        assertThat(secondMessage.getStream()).isEqualTo("stream-2");
        assertThat(secondMessage.getBody()).containsEntry("key4", "value4");
        assertThat(secondMessage.getMillisElapsedFromDelivery()).isNull();
        assertThat(secondMessage.getDeliveredCount()).isNull();
    }

    @Test
    public void xreadLastVsLatest() {
        // Redis 7.4 - you can use the + sign as a special ID to read the last message in the stream.
        assumeTrue(RedisConditions.of(redis).hasVersionGreaterOrEqualsTo("7.4"));

        redis.xadd("stream-1", Collections.singletonMap("key1", "value1"));
        redis.xadd("stream-1", Collections.singletonMap("key2", "value2"));

        List<StreamMessage<String, String>> lastMessages = redis.xread(StreamOffset.last("stream-1"));
        List<StreamMessage<String, String>> latestMessages = redis.xread(StreamOffset.latest("stream-1"));

        assertThat(lastMessages).hasSize(1);
        StreamMessage<String, String> lastMessage = lastMessages.get(0);

        assertThat(lastMessage.getStream()).isEqualTo("stream-1");
        assertThat(lastMessage.getBody()).hasSize(1).containsEntry("key2", "value2");
        assertThat(lastMessage.getMillisElapsedFromDelivery()).isNull();
        assertThat(lastMessage.getDeliveredCount()).isNull();

        assertThat(latestMessages).isEmpty();
    }

    @Test
    void xinfoStream() {

        redis.xadd(key, Collections.singletonMap("key1", "value1"));

        List<Object> objects = redis.xinfoStream(key);

        assertThat(objects).containsSequence("length", 1L);
    }

    @Test
    void xinfoGroups() {

        assertThat(redis.xgroupCreate(StreamOffset.latest(key), "group", XGroupCreateArgs.Builder.mkstream())).isEqualTo("OK");

        List<Object> objects = redis.xinfoGroups(key);
        assertThat((List<Object>) objects.get(0)).containsSequence("name", "group");
    }

    @Test
    void xinfoConsumers() {

        assertThat(redis.xgroupCreate(StreamOffset.from(key, "0-0"), "group", XGroupCreateArgs.Builder.mkstream()))
                .isEqualTo("OK");
        redis.xadd(key, Collections.singletonMap("key1", "value1"));

        redis.xreadgroup(Consumer.from("group", "consumer1"), StreamOffset.lastConsumed(key));

        List<Object> objects = redis.xinfoConsumers(key, "group");
        assertThat((List<Object>) objects.get(0)).containsSequence("name", "consumer1");
    }

    @Test
    void xgroupCreate() {

        assertThat(redis.xgroupCreate(StreamOffset.latest(key), "group", XGroupCreateArgs.Builder.mkstream())).isEqualTo("OK");

        List<Object> groups = redis.dispatch(XINFO, new NestedMultiOutput<>(StringCodec.UTF8),
                new CommandArgs<>(StringCodec.UTF8).add("GROUPS").add(key));

        assertThat(groups).isNotEmpty();
        assertThat(redis.type(key)).isEqualTo("stream");
    }

    @Test
    @EnabledOnCommand("EVAL_RO") // Redis 7.0
    void xgroupCreateEntriesRead_pre822() {
        assumeTrue(RedisConditions.of(redis).getRedisVersion().isLessThan(RedisConditions.Version.parse("8.2.2")),
                "Redis 8.2.2+ has different behavior for entries-read");

        redis.xgroupCreate(StreamOffset.latest(key), "group", XGroupCreateArgs.Builder.entriesRead(5).mkstream(true));

        List<List<Object>> group = (List) redis.xinfoGroups("key");

        assertThat(group.get(0)).containsSequence("entries-read", 5L, "lag");
    }

    @Test
    @EnabledOnCommand("EVAL_RO") // Redis 7.0
    void xgroupCreateEntriesRead_post822() {
        assumeTrue(RedisConditions.of(redis).hasVersionGreaterOrEqualsTo("8.2.2"),
                "Redis 8.2.2+ has different behavior for entries-read");

        redis.xadd(key, Collections.singletonMap("key", "value"));
        redis.xadd(key, Collections.singletonMap("key", "value"));
        redis.xgroupCreate(StreamOffset.latest(key), "group", XGroupCreateArgs.Builder.entriesRead(5).mkstream(true));

        List<List<Object>> group = (List) redis.xinfoGroups("key");

        assertThat(group.get(0)).containsSequence("entries-read", 2L, "lag");
    }

    @Test
    @EnabledOnCommand("XAUTOCLAIM") // Redis 6.2
    void xgroupCreateconsumer() {

        redis.xgroupCreate(StreamOffset.latest(key), "group", XGroupCreateArgs.Builder.mkstream());
        redis.xadd(key, Collections.singletonMap("key", "value"));

        assertThat(redis.xgroupCreateconsumer(key, Consumer.from("group", "consumer1"))).isTrue();
        assertThat(redis.xgroupCreateconsumer(key, Consumer.from("group", "consumer1"))).isFalse();
    }

    @Test
    void xreadgroup() {

        redis.xadd(key, Collections.singletonMap("key", "value"));
        redis.xgroupCreate(StreamOffset.latest(key), "group");
        redis.xadd(key, Collections.singletonMap("key", "value"));

        List<StreamMessage<String, String>> read1 = redis.xreadgroup(Consumer.from("group", "consumer1"),
                StreamOffset.lastConsumed(key));

        assertThat(read1).hasSize(1);
    }

    @Test
    void xreadgroupDeletedMessage() {

        redis.xgroupCreate(StreamOffset.latest(key), "del-group", XGroupCreateArgs.Builder.mkstream());
        redis.xadd(key, Collections.singletonMap("key", "value1"));
        redis.xreadgroup(Consumer.from("del-group", "consumer1"), StreamOffset.lastConsumed(key));

        redis.xadd(key, XAddArgs.Builder.maxlen(1), Collections.singletonMap("key", "value2"));

        List<StreamMessage<String, String>> messages = redis.xreadgroup(Consumer.from("del-group", "consumer1"),
                StreamOffset.from(key, "0-0"));

        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getBody()).isEmpty();
    }

    @Test
    void xreadgroupTrimmedMessage() {

        for (int i = 0; i < 10; i++) {
            redis.xadd(key, Collections.singletonMap("key", "value1"));
        }

        redis.xgroupCreate(StreamOffset.from(key, "0-0"), "del-group", XGroupCreateArgs.Builder.mkstream());

        redis.xreadgroup(Consumer.from("del-group", "consumer1"), XReadArgs.Builder.count(10), StreamOffset.lastConsumed(key));
        redis.xtrim(key, 1);

        List<StreamMessage<String, String>> messages = redis.xreadgroup(Consumer.from("del-group", "consumer1"),
                XReadArgs.Builder.count(10), StreamOffset.from(key, "0-0"));

        assertThat(messages).hasSize(10);
    }

    @Test
    void xpendingWithoutRead() {

        redis.xgroupCreate(StreamOffset.latest(key), "group", XGroupCreateArgs.Builder.mkstream());

        PendingMessages pendingEntries = redis.xpending(key, "group");
        assertThat(pendingEntries.getCount()).isEqualTo(0);
        assertThat(pendingEntries.getConsumerMessageCount()).isEmpty();
    }

    @Test
    void xpendingWithGroup() {

        redis.xgroupCreate(StreamOffset.latest(key), "group", XGroupCreateArgs.Builder.mkstream());
        String id = redis.xadd(key, Collections.singletonMap("key", "value"));

        redis.xreadgroup(Consumer.from("group", "consumer1"), StreamOffset.lastConsumed(key));

        PendingMessages pendingEntries = redis.xpending(key, "group");
        assertThat(pendingEntries.getCount()).isEqualTo(1);
        assertThat(pendingEntries.getMessageIds()).isEqualTo(Range.create(id, id));
    }

    @Test
    void xpending() {

        redis.xgroupCreate(StreamOffset.latest(key), "group", XGroupCreateArgs.Builder.mkstream());
        String id = redis.xadd(key, Collections.singletonMap("key", "value"));

        redis.xreadgroup(Consumer.from("group", "consumer1"), StreamOffset.lastConsumed(key));

        List<PendingMessage> pendingEntries = redis.xpending(key, "group", Range.unbounded(), Limit.from(10));

        PendingMessage message = pendingEntries.get(0);
        assertThat(message.getId()).isEqualTo(id);
        assertThat(message.getConsumer()).isEqualTo("consumer1");
        assertThat(message.getRedeliveryCount()).isEqualTo(1);
    }

    @Test
    @EnabledOnCommand("XAUTOCLAIM") // Redis 6.2
    void xpendingRanges() {

        redis.xgroupCreate(StreamOffset.latest(key), "group", XGroupCreateArgs.Builder.mkstream());
        String id1 = redis.xadd(key, Collections.singletonMap("key", "value"));
        String id2 = redis.xadd(key, Collections.singletonMap("key", "value"));

        redis.xreadgroup(Consumer.from("group", "consumer1"), StreamOffset.lastConsumed(key));

        assertThat(redis.xpending(key, "group", Range.unbounded(), Limit.from(10))).hasSize(2);
        assertThat(redis.xpending(key, "group", Range.from(Range.Boundary.including(id1), Range.Boundary.excluding(id2)),
                Limit.from(10))).hasSize(1);
        assertThat(redis.xpending(key, "group", Range.from(Range.Boundary.including(id1), Range.Boundary.including(id2)),
                Limit.from(10))).hasSize(2);
    }

    @Test
    @EnabledOnCommand("XAUTOCLAIM") // Redis 6.2
    void xpendingWithArgs() {

        redis.xgroupCreate(StreamOffset.latest(key), "group", XGroupCreateArgs.Builder.mkstream());
        String id = redis.xadd(key, Collections.singletonMap("key", "value"));

        redis.xreadgroup(Consumer.from("group", "consumer1"), StreamOffset.lastConsumed(key));

        List<PendingMessage> pendingEntries = redis.xpending(key,
                XPendingArgs.Builder.xpending(Consumer.from("group", "consumer1"), Range.unbounded(), Limit.from(10)));

        PendingMessage message = pendingEntries.get(0);
        assertThat(message.getId()).isEqualTo(id);
        assertThat(message.getConsumer()).isEqualTo("consumer1");
        assertThat(message.getRedeliveryCount()).isEqualTo(1);

        pendingEntries = redis.xpending(key,
                XPendingArgs.Builder.xpending("group", Range.unbounded(), Limit.from(10)).idle(Duration.ofMinutes(1)));

        assertThat(pendingEntries).isEmpty();

        pendingEntries = redis.xpending(key,
                XPendingArgs.Builder.xpending("group", Range.unbounded(), Limit.from(10)).idle(Duration.ZERO));

        assertThat(pendingEntries).hasSize(1);
        message = pendingEntries.get(0);
        assertThat(message.getId()).isEqualTo(id);
        assertThat(message.getConsumer()).isEqualTo("consumer1");
        assertThat(message.getRedeliveryCount()).isEqualTo(1);
    }

    @Test
    @EnabledOnCommand("XAUTOCLAIM") // Redis 6.2
    void xpendingWithIdle() {

        redis.xgroupCreate(StreamOffset.latest(key), "group", XGroupCreateArgs.Builder.mkstream());
        String id = redis.xadd(key, Collections.singletonMap("key", "value"));

        redis.xpending(key,
                XPendingArgs.Builder.xpending(Consumer.from("group", "consumer1"), Range.unbounded(), Limit.unlimited())
                        .idle(Duration.ofMinutes(1)));
    }

    @Test
    void xpendingWithoutMessages() {

        redis.xgroupCreate(StreamOffset.latest(key), "group", XGroupCreateArgs.Builder.mkstream());

        List<PendingMessage> pendingEntries = redis.xpending(key, "group", Range.unbounded(), Limit.from(10));
        assertThat(pendingEntries).isEmpty();
    }

    @Test
    void xpendingGroup() {

        redis.xgroupCreate(StreamOffset.latest(key), "group", XGroupCreateArgs.Builder.mkstream());
        String id = redis.xadd(key, Collections.singletonMap("key", "value"));

        redis.xreadgroup(Consumer.from("group", "consumer1"), StreamOffset.lastConsumed(key));

        PendingMessages pendingMessages = redis.xpending(key, "group");

        assertThat(pendingMessages.getCount()).isEqualTo(1);
        assertThat(pendingMessages.getMessageIds()).isEqualTo(Range.create(id, id));
        assertThat(pendingMessages.getConsumerMessageCount()).containsEntry("consumer1", 1L);
        assertThat(pendingMessages.getCount()).isEqualTo(1);
    }

    @Test
    void xpendingExtended() {

        redis.xgroupCreate(StreamOffset.latest(key), "group", XGroupCreateArgs.Builder.mkstream());
        String id = redis.xadd(key, Collections.singletonMap("key", "value"));

        redis.xreadgroup(Consumer.from("group", "consumer1"), StreamOffset.lastConsumed(key));

        List<PendingMessage> pendingMessages = redis.xpending(key, "group", Range.unbounded(), Limit.unlimited());

        assertThat(pendingMessages).hasSize(1);

        PendingMessage message = pendingMessages.get(0);
        assertThat(message.getId()).isEqualTo(id);
        assertThat(message.getConsumer()).isEqualTo("consumer1");
        assertThat(message.getRedeliveryCount()).isEqualTo(1);
    }

    @Test
    void xack() {

        redis.xgroupCreate(StreamOffset.latest(key), "group", XGroupCreateArgs.Builder.mkstream());
        redis.xadd(key, Collections.singletonMap("key", "value"));

        List<StreamMessage<String, String>> messages = redis.xreadgroup(Consumer.from("group", "consumer1"),
                StreamOffset.lastConsumed(key));

        Long ackd = redis.xack(key, "group", messages.get(0).getId());
        assertThat(ackd).isEqualTo(1);

        List<PendingMessage> pendingEntries = redis.xpending(key, "group", Range.unbounded(), Limit.from(10));
        assertThat(pendingEntries).isEmpty();
    }

    @Test
    @EnabledOnCommand("XAUTOCLAIM") // Redis 6.2
    void xautoclaim() {

        redis.xgroupCreate(StreamOffset.latest(key), "group", XGroupCreateArgs.Builder.mkstream());
        String id1 = redis.xadd(key, Collections.singletonMap("key1", "value1"));
        redis.xadd(key, Collections.singletonMap("key2", "value2"));

        List<StreamMessage<String, String>> messages = redis.xreadgroup(Consumer.from("group", "consumer1"),
                StreamOffset.lastConsumed(key));

        ClaimedMessages<String, String> claimedMessages = redis.xautoclaim(key,
                XAutoClaimArgs.Builder.xautoclaim(Consumer.from("group", "consumer2"), Duration.ZERO, id1).count(20));
        assertThat(claimedMessages.getId()).isNotNull();
        assertThat(claimedMessages.getMessages()).hasSize(2).contains(messages.get(0));
    }

    @Test
    @EnabledOnCommand("XAUTOCLAIM") // Redis 6.2
    void xautoclaimJustId() {

        redis.xgroupCreate(StreamOffset.latest(key), "group", XGroupCreateArgs.Builder.mkstream());
        String id1 = redis.xadd(key, Collections.singletonMap("key1", "value1"));
        redis.xadd(key, Collections.singletonMap("key2", "value2"));

        redis.xreadgroup(Consumer.from("group", "consumer1"), StreamOffset.lastConsumed(key));

        ClaimedMessages<String, String> claimedMessages = redis.xautoclaim(key,
                XAutoClaimArgs.Builder.xautoclaim(Consumer.from("group", "consumer2"), Duration.ZERO, id1).justid().count(20));
        assertThat(claimedMessages.getId()).isNotNull();
        assertThat(claimedMessages.getMessages()).hasSize(2);

        StreamMessage<String, String> message = claimedMessages.getMessages().get(0);
        assertThat(message.getBody()).isNull();
        assertThat(message.getStream()).isEqualTo("key");
        assertThat(message.getId()).isEqualTo(id1);
    }

    @Test
    void xclaim() {

        redis.xgroupCreate(StreamOffset.latest(key), "group", XGroupCreateArgs.Builder.mkstream());
        redis.xadd(key, Collections.singletonMap("key", "value"));

        List<StreamMessage<String, String>> messages = redis.xreadgroup(Consumer.from("group", "consumer1"),
                StreamOffset.lastConsumed(key));

        List<StreamMessage<String, String>> claimedMessages = redis.xclaim(key, Consumer.from("group", "consumer2"), 0,
                messages.get(0).getId());

        assertThat(claimedMessages).hasSize(1).contains(messages.get(0));

        assertThat(redis.xpending(key, Consumer.from("group", "consumer1"), Range.unbounded(), Limit.from(10))).isEmpty();
        assertThat(redis.xpending(key, Consumer.from("group", "consumer2"), Range.unbounded(), Limit.from(10))).hasSize(1);
    }

    @Test
    void xclaimWithArgs() {

        String id1 = redis.xadd(key, Collections.singletonMap("key", "value"));
        redis.xgroupCreate(StreamOffset.latest(key), "group");
        String id2 = redis.xadd(key, Collections.singletonMap("key", "value"));

        List<StreamMessage<String, String>> messages = redis.xreadgroup(Consumer.from("group", "consumer1"),
                StreamOffset.lastConsumed(key));

        List<StreamMessage<String, String>> claimedMessages = redis.xclaim(key, Consumer.from("group", "consumer2"),
                XClaimArgs.Builder.minIdleTime(0).time(Instant.now().minusSeconds(60)), id1, id2);

        assertThat(claimedMessages).hasSize(1).contains(messages.get(0));

        List<PendingMessage> pendingMessages = redis.xpending(key, Consumer.from("group", "consumer2"), Range.unbounded(),
                Limit.from(10));

        PendingMessage message = pendingMessages.get(0);
        assertThat(message.getMsSinceLastDelivery()).isBetween(50000L, 80000L);
    }

    @Test
    void xclaimJustId() {

        String id1 = redis.xadd(key, Collections.singletonMap("key", "value"));
        redis.xgroupCreate(StreamOffset.latest(key), "group");
        String id2 = redis.xadd(key, Collections.singletonMap("key", "value"));
        String id3 = redis.xadd(key, Collections.singletonMap("key", "value"));

        redis.xreadgroup(Consumer.from("group", "consumer1"), StreamOffset.lastConsumed(key));

        List<StreamMessage<String, String>> claimedMessages = redis.xclaim(key, Consumer.from("group", "consumer2"),
                XClaimArgs.Builder.justid(), id1, id2, id3);

        assertThat(claimedMessages).hasSize(2);

        StreamMessage<String, String> message = claimedMessages.get(0);

        assertThat(message.getBody()).isNull();
        assertThat(message.getStream()).isEqualTo("key");
        assertThat(message.getId()).isEqualTo(id2);
    }

    @Test
    void xgroupDestroy() {

        redis.xgroupCreate(StreamOffset.latest(key), "group", XGroupCreateArgs.Builder.mkstream());

        assertThat(redis.xgroupDestroy(key, "group")).isTrue();
        assertThat(redis.xgroupDestroy(key, "group")).isFalse();
    }

    @Test
    void xgroupDelconsumer() {

        redis.xgroupCreate(StreamOffset.latest(key), "group", XGroupCreateArgs.Builder.mkstream());
        redis.xadd(key, Collections.singletonMap("key", "value"));
        redis.xreadgroup(Consumer.from("group", "consumer1"), StreamOffset.lastConsumed(key));

        assertThat(redis.xgroupDelconsumer(key, Consumer.from("group", "consumer1"))).isOne();
        assertThat(redis.xgroupDelconsumer(key, Consumer.from("group", "consumer1"))).isZero();
    }

    @Test
    void xgroupSetid() {

        redis.xgroupCreate(StreamOffset.latest(key), "group", XGroupCreateArgs.Builder.mkstream());

        assertThat(redis.xgroupSetid(StreamOffset.latest(key), "group")).isEqualTo("OK");
    }

    // Redis 8.2 Stream Commands Tests

    @Test
    @EnabledOnCommand("XDELEX") // Redis 8.2
    void xdelex() {
        // Add some entries to the stream
        String id1 = redis.xadd(key, Collections.singletonMap("field1", "value1"));
        String id2 = redis.xadd(key, Collections.singletonMap("field2", "value2"));
        String nonExistentId = "999999-0";

        // Verify initial state
        assertThat(redis.xlen(key)).isEqualTo(2L);

        // Test XDELEX
        List<StreamEntryDeletionResult> results = redis.xdelex(key, id1, id2, nonExistentId);

        assertThat(results).hasSize(3);
        assertThat(results.get(0)).isEqualTo(StreamEntryDeletionResult.DELETED);
        assertThat(results.get(1)).isEqualTo(StreamEntryDeletionResult.DELETED);
        assertThat(results.get(2)).isEqualTo(StreamEntryDeletionResult.NOT_FOUND);

        // Verify entries were deleted
        assertThat(redis.xlen(key)).isEqualTo(0L);
    }

    @Test
    @EnabledOnCommand("XDELEX") // Redis 8.2
    void xdelexWithPolicy() {
        // Add some entries to the stream
        String id1 = redis.xadd(key, Collections.singletonMap("field1", "value1"));
        String id2 = redis.xadd(key, Collections.singletonMap("field2", "value2"));

        // Verify initial state
        assertThat(redis.xlen(key)).isEqualTo(2L);

        // Test XDELEX with KEEP_REFERENCES policy
        List<StreamEntryDeletionResult> results = redis.xdelex(key, StreamDeletionPolicy.KEEP_REFERENCES, id1, id2);

        assertThat(results).hasSize(2);
        assertThat(results.get(0)).isEqualTo(StreamEntryDeletionResult.DELETED);
        assertThat(results.get(1)).isEqualTo(StreamEntryDeletionResult.DELETED);

        // Verify entries were deleted
        assertThat(redis.xlen(key)).isEqualTo(0L);
    }

    @Test
    @EnabledOnCommand("XACKDEL") // Redis 8.2
    void xackdel() {
        // Set up stream with consumer group
        String groupName = "test-group";
        String consumerName = "test-consumer";

        // Add entries to the stream
        String id1 = redis.xadd(key, Collections.singletonMap("field1", "value1"));
        String id2 = redis.xadd(key, Collections.singletonMap("field2", "value2"));

        // Verify initial state
        assertThat(redis.xlen(key)).isEqualTo(2L);

        // Create consumer group
        redis.xgroupCreate(StreamOffset.from(key, "0-0"), groupName, XGroupCreateArgs.Builder.mkstream());

        // Read messages to create pending entries
        List<StreamMessage<String, String>> messages = redis.xreadgroup(Consumer.from(groupName, consumerName),
                StreamOffset.lastConsumed(key));

        assertThat(messages).hasSize(2);

        // Test XACKDEL
        List<StreamEntryDeletionResult> results = redis.xackdel(key, groupName, id1, id2);

        assertThat(results).hasSize(2);
        assertThat(results.get(0)).isEqualTo(StreamEntryDeletionResult.DELETED);
        assertThat(results.get(1)).isEqualTo(StreamEntryDeletionResult.DELETED);

        // Verify no pending messages remain
        List<PendingMessage> pending = redis.xpending(key, groupName, Range.unbounded(), io.lettuce.core.Limit.from(10));
        assertThat(pending).isEmpty();
    }

    @Test
    @EnabledOnCommand("XACKDEL") // Redis 8.2
    void xackdelWithPolicy() {
        // Set up stream with consumer group
        String groupName = "test-group";
        String consumerName = "test-consumer";

        // Add entries to the stream
        String id1 = redis.xadd(key, Collections.singletonMap("field1", "value1"));

        // Verify initial state
        assertThat(redis.xlen(key)).isEqualTo(1L);

        // Create consumer group
        redis.xgroupCreate(StreamOffset.from(key, "0-0"), groupName, XGroupCreateArgs.Builder.mkstream());

        // Read message to create pending entry
        redis.xreadgroup(Consumer.from(groupName, consumerName), StreamOffset.lastConsumed(key));

        // Test XACKDEL with DELETE_REFERENCES policy
        List<StreamEntryDeletionResult> results = redis.xackdel(key, groupName, StreamDeletionPolicy.DELETE_REFERENCES, id1);

        assertThat(results).hasSize(1);
        assertThat(results.get(0)).isEqualTo(StreamEntryDeletionResult.DELETED);
    }

    @Test
    @EnabledOnCommand("XACKDEL") // Redis 8.2
    void xackdelNotFound() {
        String groupName = "test-group";
        String nonExistentId = "999999-0";

        // Create consumer group on empty stream
        redis.xgroupCreate(StreamOffset.from(key, "0-0"), groupName, XGroupCreateArgs.Builder.mkstream());

        // Test XACKDEL with non-existent ID
        List<StreamEntryDeletionResult> results = redis.xackdel(key, groupName, nonExistentId);

        assertThat(results).hasSize(1);
        assertThat(results.get(0)).isEqualTo(StreamEntryDeletionResult.NOT_FOUND);
    }

    @Test
    @EnabledOnCommand("XDELEX") // Redis 8.2
    void xdelexEmptyStream() {
        String nonExistentId = "999999-0";

        // Test XDELEX on empty stream
        List<StreamEntryDeletionResult> results = redis.xdelex(key, nonExistentId);

        assertThat(results).hasSize(1);
        assertThat(results.get(0)).isEqualTo(StreamEntryDeletionResult.NOT_FOUND);
    }

    @Test
    @EnabledOnCommand("XDELEX") // Redis 8.2
    void xdelexWithDelrefPolicy() {
        // Add entries to the stream
        String id1 = redis.xadd(key, Collections.singletonMap("field1", "value1"));
        String id2 = redis.xadd(key, Collections.singletonMap("field2", "value2"));

        // Verify initial state
        assertThat(redis.xlen(key)).isEqualTo(2L);

        // Test XDELEX with DELETE_REFERENCES policy
        List<StreamEntryDeletionResult> results = redis.xdelex(key, StreamDeletionPolicy.DELETE_REFERENCES, id1, id2);

        assertThat(results).hasSize(2);
        assertThat(results.get(0)).isEqualTo(StreamEntryDeletionResult.DELETED);
        assertThat(results.get(1)).isEqualTo(StreamEntryDeletionResult.DELETED);

        // Verify entries were deleted
        assertThat(redis.xlen(key)).isEqualTo(0L);
    }

    @Test
    @EnabledOnCommand("XACKDEL") // Redis 8.2
    void xackdelWithAckedPolicy() {
        // Set up stream with consumer group
        String groupName = "test-group";
        String consumerName = "test-consumer";

        // Add entries to the stream
        String id1 = redis.xadd(key, Collections.singletonMap("field1", "value1"));

        // Verify initial state
        assertThat(redis.xlen(key)).isEqualTo(1L);

        // Create consumer group
        redis.xgroupCreate(StreamOffset.from(key, "0-0"), groupName, XGroupCreateArgs.Builder.mkstream());

        // Read message to create pending entry
        redis.xreadgroup(Consumer.from(groupName, consumerName), StreamOffset.lastConsumed(key));

        // Test XACKDEL with ACKNOWLEDGED policy on pending entry
        // The ACKNOWLEDGED policy behavior: it deletes the entry from the stream and acknowledges it
        List<StreamEntryDeletionResult> results = redis.xackdel(key, groupName, StreamDeletionPolicy.ACKNOWLEDGED, id1);

        assertThat(results).hasSize(1);
        assertThat(results.get(0)).isEqualTo(StreamEntryDeletionResult.DELETED);
    }

    @Test
    @EnabledOnCommand("XDELEX") // Redis 8.2
    void xaddWithTrimmingMode() {
        // Add initial entries to the stream
        redis.xadd(key, Collections.singletonMap("field1", "value1"));
        redis.xadd(key, Collections.singletonMap("field2", "value2"));
        redis.xadd(key, Collections.singletonMap("field3", "value3"));
        redis.xadd(key, Collections.singletonMap("field4", "value4"));
        redis.xadd(key, Collections.singletonMap("field5", "value5"));

        // Verify initial state
        assertThat(redis.xlen(key)).isEqualTo(5L);

        // Create consumer group and read messages to create PEL entries
        redis.xgroupCreate(StreamOffset.from(key, "0-0"), "test-group", XGroupCreateArgs.Builder.mkstream());
        List<StreamMessage<String, String>> messages = redis.xreadgroup(Consumer.from("test-group", "test-consumer"),
                XReadArgs.Builder.count(3), StreamOffset.lastConsumed(key));

        assertThat(messages).hasSize(3);

        // Add new entry with maxLen=3 and KEEP_REFERENCES mode - should preserve PEL references
        String newId = redis.xadd(key, XAddArgs.Builder.maxlen(3).trimmingMode(StreamDeletionPolicy.KEEP_REFERENCES),
                Collections.singletonMap("field6", "value6"));
        assertThat(newId).isNotNull();

        // Stream should be trimmed to 3 entries
        assertThat(redis.xlen(key)).isEqualTo(3L);

        // PEL should still contain references to read messages
        PendingMessages pending = redis.xpending(key, "test-group");
        assertThat(pending.getCount()).isEqualTo(3L);
    }

    @Test
    @EnabledOnCommand("XDELEX") // Redis 8.2
    void xaddWithTrimmingModeDelref() {
        // Add initial entries to the stream
        redis.xadd(key, Collections.singletonMap("field1", "value1"));
        redis.xadd(key, Collections.singletonMap("field2", "value2"));
        redis.xadd(key, Collections.singletonMap("field3", "value3"));
        redis.xadd(key, Collections.singletonMap("field4", "value4"));
        redis.xadd(key, Collections.singletonMap("field5", "value5"));

        // Create consumer group and read messages
        redis.xgroupCreate(StreamOffset.from(key, "0-0"), "test-group", XGroupCreateArgs.Builder.mkstream());
        List<StreamMessage<String, String>> messages = redis.xreadgroup(Consumer.from("test-group", "test-consumer"),
                XReadArgs.Builder.count(3), StreamOffset.lastConsumed(key));

        assertThat(messages).hasSize(3);

        // Add new entry with maxLen=3 and DELETE_REFERENCES mode - should remove PEL references
        String newId = redis.xadd(key, XAddArgs.Builder.maxlen(3).trimmingMode(StreamDeletionPolicy.DELETE_REFERENCES),
                Collections.singletonMap("field6", "value6"));
        assertThat(newId).isNotNull();

        // Stream should be trimmed to 3 entries
        assertThat(redis.xlen(key)).isEqualTo(3L);

        // PEL should have fewer references due to DELREF policy
        PendingMessages pending = redis.xpending(key, "test-group");
        assertThat(pending.getCount()).isLessThan(3L);
    }

    @Test
    @EnabledOnCommand("XDELEX") // Redis 8.2
    void xtrimWithTrimmingMode() {
        // Add initial entries to the stream
        redis.xadd(key, Collections.singletonMap("field1", "value1"));
        redis.xadd(key, Collections.singletonMap("field2", "value2"));
        redis.xadd(key, Collections.singletonMap("field3", "value3"));
        redis.xadd(key, Collections.singletonMap("field4", "value4"));
        redis.xadd(key, Collections.singletonMap("field5", "value5"));

        // Create consumer group and read messages
        redis.xgroupCreate(StreamOffset.from(key, "0-0"), "test-group", XGroupCreateArgs.Builder.mkstream());
        List<StreamMessage<String, String>> messages = redis.xreadgroup(Consumer.from("test-group", "test-consumer"),
                XReadArgs.Builder.count(3), StreamOffset.lastConsumed(key));

        assertThat(messages).hasSize(3);

        // Trim with KEEP_REFERENCES mode
        Long trimmed = redis.xtrim(key, XTrimArgs.Builder.maxlen(3).trimmingMode(StreamDeletionPolicy.KEEP_REFERENCES));
        assertThat(trimmed).isEqualTo(2L);

        // Stream should be trimmed to 3 entries
        assertThat(redis.xlen(key)).isEqualTo(3L);

        // PEL should still contain references
        PendingMessages pending = redis.xpending(key, "test-group");
        assertThat(pending.getCount()).isEqualTo(3L);
    }

    @Test
    @EnabledOnCommand("XDELEX") // Redis 8.2
    void xtrimWithTrimmingModeDelref() {
        // Add initial entries to the stream
        redis.xadd(key, Collections.singletonMap("field1", "value1"));
        redis.xadd(key, Collections.singletonMap("field2", "value2"));
        redis.xadd(key, Collections.singletonMap("field3", "value3"));
        redis.xadd(key, Collections.singletonMap("field4", "value4"));
        redis.xadd(key, Collections.singletonMap("field5", "value5"));

        // Create consumer group and read messages
        redis.xgroupCreate(StreamOffset.from(key, "0-0"), "test-group", XGroupCreateArgs.Builder.mkstream());
        List<StreamMessage<String, String>> messages = redis.xreadgroup(Consumer.from("test-group", "test-consumer"),
                XReadArgs.Builder.count(3), StreamOffset.lastConsumed(key));

        assertThat(messages).hasSize(3);

        // Trim with DELETE_REFERENCES mode
        Long trimmed = redis.xtrim(key, XTrimArgs.Builder.maxlen(3).trimmingMode(StreamDeletionPolicy.DELETE_REFERENCES));
        assertThat(trimmed).isEqualTo(2L);

        // Stream should be trimmed to 3 entries
        assertThat(redis.xlen(key)).isEqualTo(3L);

        // PEL should have fewer references due to DELREF policy
        PendingMessages pending = redis.xpending(key, "test-group");
        assertThat(pending.getCount()).isLessThan(3L);
    }

    // XREADGORUP CLAIM Tests - 8.4 OSS
    // since: 7.1

    private static final String KEY = "it:stream:claim:move:" + UUID.randomUUID();

    private static final String GROUP = "g";

    private static final String C1 = "c1";

    private static final String C2 = "c2";

    private static final Map<String, String> BODY = new HashMap<String, String>() {

        {
            put("f", "v");
        }

    };

    private static final long IDLE_TIME_MS = 5;

    private void beforeEachClaimTest() throws InterruptedException {
        assumeTrue(RedisConditions.of(redis).hasVersionGreaterOrEqualsTo("8.4"), "Redis 8.4+ required for XREADGROUP CLAIM");

        // Produce two entries
        redis.xadd(KEY, BODY);
        redis.xadd(KEY, BODY);

        // Create group and consume with c1 so entries become pending for c1
        redis.xgroupCreate(XReadArgs.StreamOffset.from(KEY, "0-0"), GROUP);
        redis.xreadgroup(Consumer.from(GROUP, C1), XReadArgs.Builder.count(10), XReadArgs.StreamOffset.lastConsumed(KEY));

        // Ensure idle time so entries are claimable
        Thread.sleep(IDLE_TIME_MS);
    }

    @Test
    void xreadgroupClaim_returnsMetadataOrdered() throws Exception {
        beforeEachClaimTest();

        // Produce fresh entries that are NOT claimed (not pending)
        redis.xadd(KEY, BODY);
        redis.xadd(KEY, BODY);

        List<StreamMessage<String, String>> consumer2 = redis.xreadgroup(Consumer.from(GROUP, C2),
                XReadArgs.Builder.claim(Duration.ofMillis(IDLE_TIME_MS)).count(10), XReadArgs.StreamOffset.lastConsumed(KEY));
        long claimedCount = consumer2.stream().filter(StreamMessage::isClaimed).count();
        long freshCount = consumer2.size() - claimedCount;
        StreamMessage<String, String> first = consumer2.get(0);
        StreamMessage<String, String> second = consumer2.get(1);
        StreamMessage<String, String> third = consumer2.get(2);
        StreamMessage<String, String> fourth = consumer2.get(3);

        // Assertions
        assertThat(consumer2).isNotNull();
        assertThat(consumer2).isNotEmpty();
        assertThat(claimedCount).isEqualTo(2);
        assertThat(freshCount).isEqualTo(2);

        // Assert order: pending entries are first
        assertThat(first.isClaimed()).isTrue();
        assertThat(second.isClaimed()).isTrue();
        assertThat(third.isClaimed()).isFalse();
        assertThat(fourth.isClaimed()).isFalse();

        // Assert claimed message structure
        assertThat(first.getMillisElapsedFromDelivery()).isGreaterThanOrEqualTo(5);
        assertThat(first.getDeliveredCount()).isGreaterThanOrEqualTo(1);
        assertThat(first.getBody()).containsEntry("f", "v");
        assertThat(fourth.getMillisElapsedFromDelivery()).isEqualTo(0);
        assertThat(fourth.getDeliveredCount()).isEqualTo(0);
        assertThat(fourth.getBody()).containsEntry("f", "v");
    }

    @Test
    void xreadgroupClaim_movesPendingFromC1ToC2AndRemainsPendingUntilAck() throws Exception {
        beforeEachClaimTest();

        PendingMessages before = redis.xpending(KEY, GROUP);
        List<StreamMessage<String, String>> res = redis.xreadgroup(Consumer.from(GROUP, C2),
                XReadArgs.Builder.claim(Duration.ofMillis(IDLE_TIME_MS)).count(10), XReadArgs.StreamOffset.lastConsumed(KEY));
        PendingMessages afterClaim = redis.xpending(KEY, GROUP);
        long acked = redis.xack(KEY, GROUP, res.get(0).getId(), res.get(1).getId());
        PendingMessages afterAck = redis.xpending(KEY, GROUP);

        // Verify pending belongs to c1
        assertThat(before.getCount()).isEqualTo(2);
        assertThat(before.getConsumerMessageCount().getOrDefault(C1, 0L)).isEqualTo(2);

        // Verify claim withv c2
        assertThat(res).isNotNull();
        assertThat(res).isNotEmpty();
        long claimed = res.stream().filter(StreamMessage::isClaimed).count();
        assertThat(claimed).isEqualTo(2);

        // After claim: entries are pending for c2 (moved), not acked yet
        assertThat(afterClaim.getCount()).isEqualTo(2);
        assertThat(afterClaim.getConsumerMessageCount().getOrDefault(C1, 0L)).isEqualTo(0);
        assertThat(afterClaim.getConsumerMessageCount().getOrDefault(C2, 0L)).isEqualTo(2);

        // XACK the claimed entries -> PEL should become empty
        assertThat(acked).isEqualTo(2);
        assertThat(afterAck.getCount()).isEqualTo(0);
    }

    @Test
    void xreadgroupClaim_claimWithNoackDoesNotCreatePendingAndRemovesClaimedFromPel() throws Exception {
        beforeEachClaimTest();

        PendingMessages before = redis.xpending(KEY, GROUP);

        // Also produce fresh entries that should not be added to PEL when NOACK is set
        redis.xadd(KEY, BODY);
        redis.xadd(KEY, BODY);

        // Claim with NOACK using c2
        List<StreamMessage<String, String>> res = redis.xreadgroup(Consumer.from(GROUP, C2),
                XReadArgs.Builder.claim(Duration.ofMillis(IDLE_TIME_MS)).noack(true).count(10),
                XReadArgs.StreamOffset.lastConsumed(KEY));
        PendingMessages afterNoack = redis.xpending(KEY, GROUP);

        assertThat(res).isNotNull();
        assertThat(res).isNotEmpty();

        long claimedCount = res.stream().filter(StreamMessage::isClaimed).count();
        long freshCount = res.size() - claimedCount;
        assertThat(claimedCount).isEqualTo(2);
        assertThat(freshCount).isEqualTo(2);

        // After NOACK read, previously pending entries remain pending (NOACK does not remove them)
        assertThat(afterNoack.getCount()).isEqualTo(2);

        // Before claim: entries are pending for c1
        assertThat(before.getCount()).isEqualTo(2);
        assertThat(before.getConsumerMessageCount().getOrDefault(C1, 0L)).isEqualTo(2);
        assertThat(before.getConsumerMessageCount().getOrDefault(C2, 0L)).isEqualTo(0);

        // Claimed entries remain pending and are now owned by c2 (CLAIM reassigns ownership). Fresh entries were not added
        // to PEL.
        assertThat(afterNoack.getConsumerMessageCount().getOrDefault(C1, 0L)).isEqualTo(0);
        assertThat(afterNoack.getConsumerMessageCount().getOrDefault(C2, 0L)).isEqualTo(2);
    }

}
