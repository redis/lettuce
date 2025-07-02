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
package io.lettuce.core.pubsub;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.*;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.lettuce.core.api.StatefulRedisConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.AbstractRedisClientTest;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.KillArgs;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.push.PushMessage;
import io.lettuce.core.internal.LettuceFactories;
import io.lettuce.core.protocol.ProtocolVersion;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;
import io.lettuce.core.support.PubSubTestListener;
import io.lettuce.test.Delay;
import io.lettuce.test.TestFutures;
import io.lettuce.test.Wait;
import io.lettuce.test.WithPassword;
import io.lettuce.test.condition.EnabledOnCommand;
import io.lettuce.test.resource.DefaultRedisClient;
import io.lettuce.test.resource.FastShutdown;
import io.lettuce.test.resource.TestClientResources;

/**
 * Pub/Sub Command tests using protocol version discovery.
 *
 * @author Will Glozer
 * @author Mark Paluch
 * @author Tugdual Grall
 * @author dengliming
 * @author Tihomir Mateev
 * @author Ali Takavci
 * @author Hari Mani
 */
@Tag(INTEGRATION_TEST)
class PubSubCommandIntegrationTests extends AbstractRedisClientTest {

    RedisPubSubAsyncCommands<String, String> pubsub;

    StatefulRedisPubSubConnection<String, String> pubSubConnection;

    PubSubTestListener listener = new PubSubTestListener();

    BlockingQueue<String> channels = listener.getChannels();

    BlockingQueue<String> shardChannels = listener.getShardChannels();

    BlockingQueue<String> patterns = listener.getPatterns();

    BlockingQueue<String> messages = listener.getMessages();

    BlockingQueue<Long> counts = listener.getCounts();

    BlockingQueue<Long> shardCounts = listener.getShardCounts();

    String channel = "channel0";

    String shardChannel = "shard-channel";

    private final String pattern = "channel*";

    String message = "msg!";

    String shardMessage = "shard msg!";

    @BeforeEach
    void openPubSubConnection() {
        try {
            client.setOptions(getOptions());
            pubSubConnection = client.connectPubSub();
            pubSubConnection.addListener(listener);
            pubsub = pubSubConnection.async();
        } finally {
            listener.clear();
        }
    }

    protected ClientOptions getOptions() {
        return ClientOptions.builder().build();
    }

    @AfterEach
    void closePubSubConnection() {
        if (pubSubConnection != null) {
            pubSubConnection.close();
        }
    }

    @Test
    void auth() {
        WithPassword.run(client, () -> {

            client.setOptions(
                    ClientOptions.builder().protocolVersion(ProtocolVersion.RESP2).pingBeforeActivateConnection(false).build());
            try (final StatefulRedisPubSubConnection<String, String> connection = client.connectPubSub()) {
                RedisPubSubAsyncCommands<String, String> commands = connection.async();
                connection.addListener(listener);
                commands.auth(passwd);

                commands.subscribe(channel);
                assertThat(channels.take()).isEqualTo(channel);
            }
        });
    }

    @Test
    @EnabledOnCommand("ACL")
    void authWithUsername() {
        WithPassword.run(client, () -> {

            client.setOptions(
                    ClientOptions.builder().protocolVersion(ProtocolVersion.RESP2).pingBeforeActivateConnection(false).build());
            try (final StatefulRedisPubSubConnection<String, String> connection = client.connectPubSub()) {
                RedisPubSubAsyncCommands<String, String> commands = connection.async();
                connection.addListener(listener);
                commands.auth(username, passwd);

                commands.subscribe(channel);
                assertThat(channels.take()).isEqualTo(channel);
            }
        });
    }

    @Test
    void authWithReconnect() {

        WithPassword.run(client, () -> {

            client.setOptions(
                    ClientOptions.builder().protocolVersion(ProtocolVersion.RESP2).pingBeforeActivateConnection(false).build());

            try (final StatefulRedisPubSubConnection<String, String> connection = client.connectPubSub()) {
                RedisPubSubAsyncCommands<String, String> command = client.connectPubSub().async();
                connection.addListener(listener);
                command.auth(passwd);

                command.clientSetname("authWithReconnect");
                command.subscribe(channel).get();

                assertThat(channels.take()).isEqualTo(channel);

                redis.auth(passwd);
                long id = findNamedClient("authWithReconnect");
                redis.clientKill(KillArgs.Builder.id(id));

                Delay.delay(Duration.ofMillis(100));
                Wait.untilTrue(connection::isOpen).waitOrTimeout();

                assertThat(channels.take()).isEqualTo(channel);
            }
        });
    }

    @Test
    @EnabledOnCommand("ACL")
    void authWithUsernameAndReconnect() {

        WithPassword.run(client, () -> {

            client.setOptions(
                    ClientOptions.builder().protocolVersion(ProtocolVersion.RESP2).pingBeforeActivateConnection(false).build());

            try (final StatefulRedisPubSubConnection<String, String> connection = client.connectPubSub()) {
                RedisPubSubAsyncCommands<String, String> command = connection.async();
                connection.addListener(listener);
                command.auth(username, passwd);
                command.clientSetname("authWithReconnect");
                command.subscribe(channel).get();

                assertThat(channels.take()).isEqualTo(channel);

                long id = findNamedClient("authWithReconnect");
                redis.auth(username, passwd);
                redis.clientKill(KillArgs.Builder.id(id));

                Delay.delay(Duration.ofMillis(100));
                Wait.untilTrue(connection::isOpen).waitOrTimeout();

                assertThat(channels.take()).isEqualTo(channel);
            }
        });
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
    @EnabledOnCommand("SSUBSCRIBE")
    void messageToShardChannel() throws Exception {
        pubsub.ssubscribe(shardChannel);
        Wait.untilEquals(shardChannel, shardChannels::poll).waitOrTimeout();

        redis.spublish(shardChannel, shardMessage);
        Wait.untilEquals(shardChannel, shardChannels::poll).waitOrTimeout();
        Wait.untilEquals(shardMessage, messages::poll).waitOrTimeout();
    }

    @Test
    @EnabledOnCommand("SSUBSCRIBE")
    void messageToShardChannelViaNewClient() throws Exception {
        pubsub.ssubscribe(shardChannel);
        Wait.untilEquals(shardChannel, shardChannels::poll).waitOrTimeout();

        RedisPubSubAsyncCommands<String, String> redis = DefaultRedisClient.get().connectPubSub().async();
        redis.spublish(shardChannel, shardMessage);
        Wait.untilEquals(shardMessage, messages::poll).waitOrTimeout();
        Wait.untilEquals(shardChannel, shardChannels::poll).waitOrTimeout();
    }

    @Test
    @EnabledOnCommand("ACL")
    void messageAsPushMessage() throws Exception {

        pubsub.subscribe(channel);
        assertThat(counts.take()).isNotNull();

        AtomicReference<PushMessage> messageRef = new AtomicReference<>();
        pubSubConnection.addListener(messageRef::set);

        redis.publish(channel, message);
        assertThat(messages.take()).isEqualTo(message);
        Wait.untilTrue(() -> messageRef.get() != null).waitOrTimeout();

        PushMessage pushMessage = messageRef.get();
        assertThat(pushMessage).isNotNull();
        assertThat(pushMessage.getType()).isEqualTo("message");
        assertThat(pushMessage.getContent()).contains(ByteBuffer.wrap("message".getBytes()),
                ByteBuffer.wrap(message.getBytes()));
    }

    @Test
    void pipelinedMessage() throws Exception {
        pubsub.subscribe(channel);
        assertThat(channels.take()).isEqualTo(channel);
        try (final StatefulRedisConnection<String, String> connection = client.connect()) {
            RedisAsyncCommands<String, String> command = connection.async();

            connection.setAutoFlushCommands(false);
            command.publish(channel, message);
            Delay.delay(Duration.ofMillis(100));

            assertThat(channels).isEmpty();
            connection.flushCommands();

            assertThat(channels.take()).isEqualTo(channel);
            assertThat(messages.take()).isEqualTo(message);
        }
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

        pubSubConnection.setAutoFlushCommands(false);
        pubsub.subscribe(channel);
        Delay.delay(Duration.ofMillis(100));
        assertThat(channels).isEmpty();
        pubSubConnection.flushCommands();

        assertThat(channels.take()).isEqualTo(channel);

        redis.publish(channel, message);

        assertThat(channels.take()).isEqualTo(channel);
        assertThat(messages.take()).isEqualTo(message);

    }

    @Test
    void psubscribe() throws Exception {
        RedisFuture<Void> psubscribe = pubsub.psubscribe(pattern);
        assertThat(TestFutures.getOrTimeout(psubscribe)).isNull();
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
        TestFutures.awaitOrTimeout(pubsub.subscribe(channel));
        List<String> result = redis.pubsubChannels();
        assertThat(result).contains(channel);

    }

    @Test
    void pubsubMultipleChannels() {
        TestFutures.awaitOrTimeout(pubsub.subscribe(channel, "channel1", "channel3"));

        List<String> result = redis.pubsubChannels();
        assertThat(result).contains(channel, "channel1", "channel3");

    }

    @Test
    void pubsubChannelsWithArg() {
        TestFutures.awaitOrTimeout(pubsub.subscribe(channel));
        List<String> result = redis.pubsubChannels(pattern);
        assertThat(result).contains(channel);
    }

    @Test
    void pubsubNumsub() {

        TestFutures.awaitOrTimeout(pubsub.subscribe(channel));

        Map<String, Long> result = redis.pubsubNumsub(channel);
        assertThat(result).isNotEmpty();
        assertThat(result).containsKeys(channel);
    }

    @Test
    @EnabledOnCommand("SPUBLISH")
    void pubsubShardChannels() {
        TestFutures.awaitOrTimeout(pubsub.ssubscribe(shardChannel));
        List<String> result = redis.pubsubShardChannels();
        assertThat(result).contains(shardChannel);
    }

    @Test
    @EnabledOnCommand("SPUBLISH")
    void pubsubMultipleShardChannels() {
        TestFutures.awaitOrTimeout(pubsub.ssubscribe(shardChannel, "channel1", "channel3"));
        List<String> result = redis.pubsubShardChannels();
        assertThat(result).contains(shardChannel, "channel1", "channel3");

    }

    @Test
    @EnabledOnCommand("SPUBLISH")
    void pubsubShardChannelsWithArg() {
        TestFutures.awaitOrTimeout(pubsub.ssubscribe(shardChannel));
        List<String> result = redis.pubsubShardChannels(shardChannel);
        assertThat(result).contains(shardChannel);
    }

    @Test
    @EnabledOnCommand("SPUBLISH")
    void pubsubShardNumsub() {
        TestFutures.awaitOrTimeout(pubsub.ssubscribe(shardChannel));

        Map<String, Long> result = redis.pubsubShardNumsub(shardChannel);
        assertThat(result.keySet()).contains(shardChannel);
    }

    @Test
    void pubsubNumpat() {

        TestFutures.awaitOrTimeout(pubsub.psubscribe(pattern));
        Long result = redis.pubsubNumpat();
        assertThat(result.longValue()).isGreaterThan(0); // Redis sometimes keeps old references
    }

    @Test
    void punsubscribe() throws Exception {
        TestFutures.awaitOrTimeout(pubsub.punsubscribe(pattern));
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
        TestFutures.awaitOrTimeout(pubsub.unsubscribe(channel));
        assertThat(channels.take()).isEqualTo(channel);
        assertThat((long) counts.take()).isEqualTo(0);

        RedisFuture<Void> future = pubsub.unsubscribe();

        assertThat(TestFutures.getOrTimeout(future)).isNull();
        assertThat(future.getError()).isNull();

        assertThat(channels).isEmpty();
        assertThat(patterns).isEmpty();
    }

    @Test
    void pubsubCloseOnClientShutdown() {
        final RedisClient redisClient = RedisClient.create(TestClientResources.get(),
                RedisURI.Builder.redis(host, port).build());
        try (final StatefulRedisPubSubConnection<String, String> connection = redisClient.connectPubSub()) {
            FastShutdown.shutdown(redisClient);
            assertThat(connection.isOpen()).isFalse();
        }
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

        Wait.untilTrue(pubSubConnection::isOpen).waitOrTimeout();

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

        Wait.untilTrue(pubSubConnection::isOpen).waitOrTimeout();

        redis.publish(channel, message);
        assertThat(channels.take()).isEqualTo(channel);
        assertThat(messages.take()).isEqualTo(message);
    }

    @Test
    void resubscribeShardChannelsOnReconnect() throws Exception {
        pubsub.ssubscribe(shardChannel);
        assertThat(shardChannels.take()).isEqualTo(shardChannel);
        assertThat((long) shardCounts.take()).isEqualTo(1);

        pubsub.quit();

        assertThat(shardChannels.take()).isEqualTo(shardChannel);
        assertThat((long) shardCounts.take()).isEqualTo(1);

        Wait.untilTrue(pubSubConnection::isOpen).waitOrTimeout();

        redis.spublish(shardChannel, shardMessage);
        assertThat(shardChannels.take()).isEqualTo(shardChannel);
        assertThat(messages.take()).isEqualTo(shardMessage);
    }

    @Test
    void interleaveCommands() throws ExecutionException, InterruptedException {

        // relay Double and Long
        assertThat(pubsub.zadd("myzset", 1.0, "one").get()).isEqualTo(1L);
        assertThat(pubsub.zadd("myzset", 2.0, "two").get()).isEqualTo(1L);
        assertThat(pubsub.zpopmin("myzset", 1).get().get(0).getValue()).isEqualTo("one");
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

        pubSubConnection.addListener(adapter);
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

        pubSubConnection.removeListener(listener);

        redis.publish(channel, message);
        assertThat(channels.poll(10, TimeUnit.MILLISECONDS)).isNull();
        assertThat(messages.poll(10, TimeUnit.MILLISECONDS)).isNull();
    }

    @Test
    void echoAllowedInSubscriptionState() {

        TestFutures.awaitOrTimeout(pubsub.subscribe(channel));

        assertThat(TestFutures.getOrTimeout(pubsub.echo("ping"))).isEqualTo("ping");
        pubsub.unsubscribe(channel);
    }

}
