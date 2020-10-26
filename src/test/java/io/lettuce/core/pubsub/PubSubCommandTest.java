/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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
package io.lettuce.core.pubsub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.lettuce.core.*;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.internal.LettuceFactories;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;
import io.lettuce.test.Delay;
import io.lettuce.test.Futures;
import io.lettuce.test.Wait;
import io.lettuce.test.resource.FastShutdown;
import io.lettuce.test.resource.TestClientResources;

/**
 * @author Will Glozer
 * @author Mark Paluch
 */
class PubSubCommandTest extends AbstractRedisClientTest implements RedisPubSubListener<String, String> {

    private RedisPubSubAsyncCommands<String, String> pubsub;

    private BlockingQueue<String> channels;

    private BlockingQueue<String> patterns;

    private BlockingQueue<String> messages;

    private BlockingQueue<Long> counts;

    private String channel = "channel0";

    private String pattern = "channel*";

    private String message = "msg!";

    @BeforeEach
    void openPubSubConnection() {
        try {
            pubsub = client.connectPubSub().async();
            pubsub.getStatefulConnection().addListener(this);
        } finally {
            channels = LettuceFactories.newBlockingQueue();
            patterns = LettuceFactories.newBlockingQueue();
            messages = LettuceFactories.newBlockingQueue();
            counts = LettuceFactories.newBlockingQueue();
        }
    }

    @AfterEach
    void closePubSubConnection() {
        if (pubsub != null) {
            pubsub.getStatefulConnection().close();
        }
    }

    @Test
    void auth() {
        new WithPasswordRequired() {

            @Override
            protected void run(RedisClient client) throws Exception {
                RedisPubSubAsyncCommands<String, String> connection = client.connectPubSub().async();
                connection.getStatefulConnection().addListener(PubSubCommandTest.this);
                connection.auth(passwd);

                connection.subscribe(channel);
                assertThat(channels.take()).isEqualTo(channel);
            }

        };
    }

    @Test
    void authWithReconnect() {

        new WithPasswordRequired() {

            @Override
            protected void run(RedisClient client) throws Exception {

                RedisPubSubAsyncCommands<String, String> connection = client.connectPubSub().async();
                connection.getStatefulConnection().addListener(PubSubCommandTest.this);
                connection.auth(passwd);
                connection.clientSetname("authWithReconnect");
                connection.subscribe(channel);

                assertThat(channels.take()).isEqualTo(channel);

                long id = findNamedClient("authWithReconnect");
                redis.clientKill(KillArgs.Builder.id(id));

                Delay.delay(Duration.ofMillis(100));
                Wait.untilTrue(connection::isOpen).waitOrTimeout();

                assertThat(channels.take()).isEqualTo(channel);
            }

        };
    }

    private long findNamedClient(String name) {

        Pattern pattern = Pattern.compile(".*id=(\\d+).*name=" + name + ".*", Pattern.MULTILINE);
        String clients = redis.clientList();
        Matcher matcher = pattern.matcher(clients);

        if (!matcher.find()) {
            throw new IllegalStateException("Cannot find PubSub client in: " + clients);
        }

        return Long.parseLong(matcher.group(1));
    }

    @Test
    void message() throws Exception {
        pubsub.subscribe(channel);
        assertThat(channels.take()).isEqualTo(channel);

        redis.publish(channel, message);
        assertThat(channels.take()).isEqualTo(channel);
        assertThat(messages.take()).isEqualTo(message);
    }

    @Test
    void pipelinedMessage() throws Exception {
        pubsub.subscribe(channel);
        assertThat(channels.take()).isEqualTo(channel);
        RedisAsyncCommands<String, String> connection = client.connect().async();

        connection.setAutoFlushCommands(false);
        connection.publish(channel, message);
        Delay.delay(Duration.ofMillis(100));

        assertThat(channels).isEmpty();
        connection.flushCommands();

        assertThat(channels.take()).isEqualTo(channel);
        assertThat(messages.take()).isEqualTo(message);

        connection.getStatefulConnection().close();
    }

    @Test
    void pmessage() throws Exception {
        pubsub.psubscribe(pattern).await(1, TimeUnit.MINUTES);
        assertThat(patterns.take()).isEqualTo(pattern);

        redis.publish(channel, message);
        assertThat(patterns.take()).isEqualTo(pattern);
        assertThat(channels.take()).isEqualTo(channel);
        assertThat(messages.take()).isEqualTo(message);

        redis.publish("channel2", "msg 2!");
        assertThat(patterns.take()).isEqualTo(pattern);
        assertThat(channels.take()).isEqualTo("channel2");
        assertThat(messages.take()).isEqualTo("msg 2!");
    }

    @Test
    void pipelinedSubscribe() throws Exception {

        pubsub.setAutoFlushCommands(false);
        pubsub.subscribe(channel);
        Delay.delay(Duration.ofMillis(100));
        assertThat(channels).isEmpty();
        pubsub.flushCommands();

        assertThat(channels.take()).isEqualTo(channel);

        redis.publish(channel, message);

        assertThat(channels.take()).isEqualTo(channel);
        assertThat(messages.take()).isEqualTo(message);

    }

    @Test
    void psubscribe() throws Exception {
        RedisFuture<Void> psubscribe = pubsub.psubscribe(pattern);
        assertThat(Futures.get(psubscribe)).isNull();
        assertThat(psubscribe.getError()).isNull();
        assertThat(psubscribe.isCancelled()).isFalse();
        assertThat(psubscribe.isDone()).isTrue();

        assertThat(patterns.take()).isEqualTo(pattern);
        assertThat((long) counts.take()).isEqualTo(1);
    }

    @Test
    void psubscribeWithListener() throws Exception {
        RedisFuture<Void> psubscribe = pubsub.psubscribe(pattern);
        final List<Object> listener = new ArrayList<>();

        psubscribe.thenAccept(aVoid -> listener.add("done"));
        psubscribe.await(1, TimeUnit.MINUTES);

        assertThat(patterns.take()).isEqualTo(pattern);
        assertThat((long) counts.take()).isEqualTo(1);
        assertThat(listener).hasSize(1);
    }

    @Test
    void pubsubEmptyChannels() {
        assertThatThrownBy(() -> pubsub.subscribe()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void pubsubChannels() {
        Futures.await(pubsub.subscribe(channel));
        List<String> result = redis.pubsubChannels();
        assertThat(result).contains(channel);

    }

    @Test
    void pubsubMultipleChannels() {
        Futures.await(pubsub.subscribe(channel, "channel1", "channel3"));

        List<String> result = redis.pubsubChannels();
        assertThat(result).contains(channel, "channel1", "channel3");

    }

    @Test
    void pubsubChannelsWithArg() {
        Futures.await(pubsub.subscribe(channel));
        List<String> result = redis.pubsubChannels(pattern);
        assertThat(result).contains(channel);
    }

    @Test
    void pubsubNumsub() {

        Futures.await(pubsub.subscribe(channel));

        Map<String, Long> result = redis.pubsubNumsub(channel);
        assertThat(result).isNotEmpty();
        assertThat(result).containsKeys(channel);
    }

    @Test
    void pubsubNumpat() {

        Futures.await(pubsub.psubscribe(pattern));
        Long result = redis.pubsubNumpat();
        assertThat(result.longValue()).isGreaterThan(0); // Redis sometimes keeps old references
    }

    @Test
    void punsubscribe() throws Exception {
        Futures.await(pubsub.punsubscribe(pattern));
        assertThat(patterns.take()).isEqualTo(pattern);
        assertThat((long) counts.take()).isEqualTo(0);

    }

    @Test
    void subscribe() throws Exception {
        pubsub.subscribe(channel);
        assertThat(channels.take()).isEqualTo(channel);
        assertThat((long) counts.take()).isEqualTo(1);
    }

    @Test
    void unsubscribe() throws Exception {
        Futures.await(pubsub.unsubscribe(channel));
        assertThat(channels.take()).isEqualTo(channel);
        assertThat((long) counts.take()).isEqualTo(0);

        RedisFuture<Void> future = pubsub.unsubscribe();

        assertThat(Futures.get(future)).isNull();
        assertThat(future.getError()).isNull();

        assertThat(channels).isEmpty();
        assertThat(patterns).isEmpty();

    }

    @Test
    void pubsubCloseOnClientShutdown() {

        RedisClient redisClient = RedisClient.create(TestClientResources.get(), RedisURI.Builder.redis(host, port).build());

        RedisPubSubAsyncCommands<String, String> connection = redisClient.connectPubSub().async();

        FastShutdown.shutdown(redisClient);

        assertThat(connection.isOpen()).isFalse();
    }

    @Test
    void utf8Channel() throws Exception {
        String channel = "channelλ";
        String message = "αβγ";

        pubsub.subscribe(channel);
        assertThat(channels.take()).isEqualTo(channel);

        redis.publish(channel, message);
        assertThat(channels.take()).isEqualTo(channel);
        assertThat(messages.take()).isEqualTo(message);
    }

    @Test
    void resubscribeChannelsOnReconnect() throws Exception {
        pubsub.subscribe(channel);
        assertThat(channels.take()).isEqualTo(channel);
        assertThat((long) counts.take()).isEqualTo(1);

        pubsub.quit();

        assertThat(channels.take()).isEqualTo(channel);
        assertThat((long) counts.take()).isEqualTo(1);

        Wait.untilTrue(pubsub::isOpen).waitOrTimeout();

        redis.publish(channel, message);
        assertThat(channels.take()).isEqualTo(channel);
        assertThat(messages.take()).isEqualTo(message);
    }

    @Test
    void resubscribePatternsOnReconnect() throws Exception {
        pubsub.psubscribe(pattern);
        assertThat(patterns.take()).isEqualTo(pattern);
        assertThat((long) counts.take()).isEqualTo(1);

        pubsub.quit();

        assertThat(patterns.take()).isEqualTo(pattern);
        assertThat((long) counts.take()).isEqualTo(1);

        Wait.untilTrue(pubsub::isOpen).waitOrTimeout();

        redis.publish(channel, message);
        assertThat(channels.take()).isEqualTo(channel);
        assertThat(messages.take()).isEqualTo(message);
    }

    @Test
    void adapter() throws Exception {
        final BlockingQueue<Long> localCounts = LettuceFactories.newBlockingQueue();

        RedisPubSubAdapter<String, String> adapter = new RedisPubSubAdapter<String, String>() {

            @Override
            public void subscribed(String channel, long count) {
                super.subscribed(channel, count);
                localCounts.add(count);
            }

            @Override
            public void unsubscribed(String channel, long count) {
                super.unsubscribed(channel, count);
                localCounts.add(count);
            }

        };

        pubsub.getStatefulConnection().addListener(adapter);
        pubsub.subscribe(channel);
        pubsub.psubscribe(pattern);

        assertThat((long) localCounts.take()).isEqualTo(1L);

        redis.publish(channel, message);
        pubsub.punsubscribe(pattern);
        pubsub.unsubscribe(channel);

        assertThat((long) localCounts.take()).isEqualTo(0L);
    }

    @Test
    void removeListener() throws Exception {
        pubsub.subscribe(channel);
        assertThat(channels.take()).isEqualTo(channel);

        redis.publish(channel, message);
        assertThat(channels.take()).isEqualTo(channel);
        assertThat(messages.take()).isEqualTo(message);

        pubsub.getStatefulConnection().removeListener(this);

        redis.publish(channel, message);
        assertThat(channels.poll(10, TimeUnit.MILLISECONDS)).isNull();
        assertThat(messages.poll(10, TimeUnit.MILLISECONDS)).isNull();
    }

    @Test
    void pingNotAllowedInSubscriptionState() {

        Futures.await(pubsub.subscribe(channel));

        assertThatThrownBy(() -> Futures.get(pubsub.ping())).isInstanceOf(RedisException.class)
                .hasMessageContaining("not allowed");
        pubsub.unsubscribe(channel);

        Wait.untilTrue(() -> channels.size() == 2).waitOrTimeout();

        assertThat(Futures.get(pubsub.ping())).isEqualTo("PONG");
    }

    // RedisPubSubListener implementation

    @Override
    public void message(String channel, String message) {
        channels.add(channel);
        messages.add(message);
    }

    @Override
    public void message(String pattern, String channel, String message) {
        patterns.add(pattern);
        channels.add(channel);
        messages.add(message);
    }

    @Override
    public void subscribed(String channel, long count) {
        channels.add(channel);
        counts.add(count);
    }

    @Override
    public void psubscribed(String pattern, long count) {
        patterns.add(pattern);
        counts.add(count);
    }

    @Override
    public void unsubscribed(String channel, long count) {
        channels.add(channel);
        counts.add(count);
    }

    @Override
    public void punsubscribed(String pattern, long count) {
        patterns.add(pattern);
        counts.add(count);
    }

}
