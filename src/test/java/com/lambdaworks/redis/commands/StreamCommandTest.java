/*
 * Copyright 2017 the original author or authors.
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
package com.lambdaworks.redis.commands;

import static com.lambdaworks.redis.protocol.CommandType.XINFO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.util.*;

import org.junit.Ignore;
import org.junit.Test;

import com.lambdaworks.redis.*;
import com.lambdaworks.redis.XReadArgs.StreamOffset;
import com.lambdaworks.redis.codec.StringCodec;
import com.lambdaworks.redis.models.stream.PendingMessage;
import com.lambdaworks.redis.models.stream.PendingParser;
import com.lambdaworks.redis.output.NestedMultiOutput;
import com.lambdaworks.redis.protocol.CommandArgs;

/**
 * @author Mark Paluch
 */
public class StreamCommandTest extends AbstractRedisClientTest {

    @Test
    public void xadd() {

        assertThat(redis.xadd(key, Collections.singletonMap("key", "value"))).endsWith("-0");
        assertThat(redis.xadd(key, "foo", "bar")).isNotEmpty();

        assertThat(redis.xlen(key)).isEqualTo(2);
    }

    @Test
    public void xaddMaxLen() {

        String id = redis.xadd(key, XAddArgs.Builder.maxlen(5), "foo", "bar");

        for (int i = 0; i < 5; i++) {
            redis.xadd(key, XAddArgs.Builder.maxlen(5), "foo", "bar");
        }

        List<StreamMessage<String, String>> messages = redis.xrange(key,
                Range.from(Range.Boundary.including(id), Range.Boundary.unbounded()));

        assertThat(messages).hasSize(5);
    }

    @Test
    public void xrange() {

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
    public void xrevrange() {

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
    public void xread() {

        String initial1 = redis.xadd("stream-1", Collections.singletonMap("key1", "value1"));
        String initial2 = redis.xadd("stream-2", Collections.singletonMap("key2", "value2"));
        String message1 = redis.xadd("stream-1", Collections.singletonMap("key3", "value3"));
        String message2 = redis.xadd("stream-2", Collections.singletonMap("key4", "value4"));

        List<StreamMessage<String, String>> messages = redis.xread(StreamOffset.from("stream-1", initial1),
                StreamOffset.from("stream-2", initial2));

        StreamMessage<String, String> firstMessage = messages.get(0);

        assertThat(firstMessage.getId()).isEqualTo(message1);
        assertThat(firstMessage.getStream()).isEqualTo("stream-1");
        assertThat(firstMessage.getBody()).containsEntry("key3", "value3");

        StreamMessage<String, String> secondMessage = messages.get(1);

        assertThat(secondMessage.getId()).isEqualTo(message2);
        assertThat(secondMessage.getStream()).isEqualTo("stream-2");
        assertThat(secondMessage.getBody()).containsEntry("key4", "value4");
    }

    @Test
    public void xreadTransactional() {

        String initial1 = redis.xadd("stream-1", Collections.singletonMap("key1", "value1"));
        String initial2 = redis.xadd("stream-2", Collections.singletonMap("key2", "value2"));

        redis.multi();
        redis.xadd("stream-1", Collections.singletonMap("key3", "value3"));
        redis.xadd("stream-2", Collections.singletonMap("key4", "value4"));
        redis.xread(StreamOffset.from("stream-1", initial1), XReadArgs.StreamOffset.from("stream-2", initial2));

        List<Object> exec = redis.exec();

        String message1 = (String) exec.get(0);
        String message2 = (String) exec.get(1);
        List<StreamMessage<String, String>> messages = (List) exec.get(2);

        StreamMessage<String, String> firstMessage = messages.get(0);

        assertThat(firstMessage.getId()).isEqualTo(message1);
        assertThat(firstMessage.getStream()).isEqualTo("stream-1");
        assertThat(firstMessage.getBody()).containsEntry("key3", "value3");

        StreamMessage<String, String> secondMessage = messages.get(1);

        assertThat(secondMessage.getId()).isEqualTo(message2);
        assertThat(secondMessage.getStream()).isEqualTo("stream-2");
        assertThat(secondMessage.getBody()).containsEntry("key4", "value4");
    }

    @Test
    public void xgroupCreate() {

        redis.xadd(key, Collections.singletonMap("key", "value"));

        assertThat(redis.xgroupCreate(key, "group", "$")).isEqualTo("OK");

        List<Object> groups = redis.dispatch(XINFO, new NestedMultiOutput<>(StringCodec.UTF8), new CommandArgs<>(
                StringCodec.UTF8).add("GROUPS").add(key));

        assertThat(groups).isNotEmpty();
    }

    @Test
    public void xgroupread() {

        redis.xadd(key, Collections.singletonMap("key", "value"));
        redis.xgroupCreate(key, "group", "$");
        redis.xadd(key, Collections.singletonMap("key", "value"));

        List<StreamMessage<String, String>> read1 = redis.xreadgroup(Consumer.from("group", "consumer1"),
                StreamOffset.latestConsumer(key));

        assertThat(read1).hasSize(1);
    }

    @Test
    public void xpendingWithGroup() {

        redis.xadd(key, Collections.singletonMap("key", "value"));
        redis.xgroupCreate(key, "group", "$");
        String id = redis.xadd(key, Collections.singletonMap("key", "value"));

        redis.xreadgroup(Consumer.from("group", "consumer1"), StreamOffset.latestConsumer(key));

        List<Object> pendingEntries = redis.xpending(key, "group");
        assertThat(pendingEntries).hasSize(4).containsSequence(1L, id, id);
    }

    @Test
    public void xpending() {

        redis.xadd(key, Collections.singletonMap("key", "value"));
        redis.xgroupCreate(key, "group", "$");
        String id = redis.xadd(key, Collections.singletonMap("key", "value"));

        redis.xreadgroup(Consumer.from("group", "consumer1"),
                StreamOffset.latestConsumer(key));

        List<Object> pendingEntries = redis.xpending(key, "group", Range.unbounded(), Limit.from(10));

        List<PendingMessage> pendingMessages = PendingParser.parseRange(pendingEntries);
        assertThat(pendingMessages).hasSize(1);

        PendingMessage message = pendingMessages.get(0);
        assertThat(message.getId()).isEqualTo(id);
        assertThat(message.getConsumer()).isEqualTo("consumer1");
        assertThat(message.getRedeliveryCount()).isEqualTo(1);
    }

    @Test
    public void xack() {

        redis.xadd(key, Collections.singletonMap("key", "value"));
        redis.xgroupCreate(key, "group", "$");
        redis.xadd(key, Collections.singletonMap("key", "value"));

        List<StreamMessage<String, String>> messages = redis.xreadgroup(Consumer.from("group", "consumer1"),
                StreamOffset.latestConsumer(key));

        Long ackd = redis.xack(key, "group", messages.get(0).getId());
        assertThat(ackd).isEqualTo(1);

        List<Object> pendingEntries = redis.xpending(key, "group", Range.unbounded(), Limit.from(10));
        assertThat(pendingEntries).isEmpty();
    }

    @Test
    @Ignore("Not yet implemented in Redis")
    public void xgroupDelgroup() {

        redis.xadd(key, Collections.singletonMap("key", "value"));

        fail("Not yet implemented in Redis");
    }

    @Test
    @Ignore("Not yet implemented in Redis")
    public void xgroupSetid() {

        redis.xadd(key, Collections.singletonMap("key", "value"));

        fail("Not yet implemented in Redis");
    }
}
