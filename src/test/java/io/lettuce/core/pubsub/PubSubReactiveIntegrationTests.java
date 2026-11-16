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

import io.lettuce.core.pubsub.PubSubOutput.Type;
import io.lettuce.core.pubsub.api.reactive.SubscriptionMessage;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import io.lettuce.core.AbstractRedisClientTest;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.internal.LettuceFactories;
import io.lettuce.core.pubsub.api.reactive.ChannelMessage;
import io.lettuce.core.pubsub.api.reactive.PatternMessage;
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands;
import io.lettuce.test.Delay;
import io.lettuce.test.Wait;
import io.lettuce.test.condition.EnabledOnCommand;
import io.lettuce.test.resource.FastShutdown;
import io.lettuce.test.resource.TestClientResources;

/**
 * @author Mark Paluch
 * @author Ali Takavci
 * @author Hari Mani
 */
@Tag(INTEGRATION_TEST)
class PubSubReactiveIntegrationTests extends AbstractRedisClientTest implements RedisPubSubListener<String, String> {

    private RedisPubSubReactiveCommands<String, String> pubsub;

    private RedisPubSubReactiveCommands<String, String> pubsub2;

    private StatefulRedisPubSubConnection<String, String> pubSubConnection;

    private StatefulRedisPubSubConnection<String, String> pubSubConnection2;

    private BlockingQueue<String> channels;

    private BlockingQueue<String> shardChannels;

    private BlockingQueue<String> patterns;

    private BlockingQueue<String> messages;

    private BlockingQueue<Long> counts;

    private final String shardChannel = "shard-channel";

    private final String channel = "channel0";

    private final String pattern = "channel*";

    private final String message = "msg!";

    @BeforeEach
    void openPubSubConnection() {
        pubSubConnection = client.connectPubSub();
        pubSubConnection2 = client.connectPubSub();
        pubsub = pubSubConnection.reactive();
        pubsub2 = pubSubConnection2.reactive();
        pubSubConnection.addListener(this);
        channels = LettuceFactories.newBlockingQueue();
        shardChannels = LettuceFactories.newBlockingQueue();
        patterns = LettuceFactories.newBlockingQueue();
        messages = LettuceFactories.newBlockingQueue();
        counts = LettuceFactories.newBlockingQueue();
    }

    @AfterEach
    void closePubSubConnection() {
        pubSubConnection.close();
        pubSubConnection2.close();
    }

    @Test
    void observeChannels() throws Exception {

        block(pubsub.subscribe(channel));

        BlockingQueue<ChannelMessage<String, String>> channelMessages = LettuceFactories.newBlockingQueue();

        Disposable disposable = pubsub.observeChannels().doOnNext(channelMessages::add).subscribe();

        redis.publish(channel, message);
        redis.publish(channel, message);
        redis.publish(channel, message);

        Wait.untilEquals(3, channelMessages::size).waitOrTimeout();
        assertThat(channelMessages).hasSize(3);

        disposable.dispose();
        redis.publish(channel, message);
        Delay.delay(Duration.ofMillis(500));
        assertThat(channelMessages).hasSize(3);

        ChannelMessage<String, String> channelMessage = channelMessages.take();
        assertThat(channelMessage.getChannel()).isEqualTo(channel);
        assertThat(channelMessage.getMessage()).isEqualTo(message);
    }

    @Test
    void observeChannelsUnsubscribe() {

        block(pubsub.subscribe(channel));

        BlockingQueue<ChannelMessage<String, String>> channelMessages = LettuceFactories.newBlockingQueue();

        pubsub.observeChannels().doOnNext(channelMessages::add).subscribe().dispose();

        block(statefulRedisConnection.reactive().publish(channel, message));
        block(statefulRedisConnection.reactive().publish(channel, message));

        Delay.delay(Duration.ofMillis(500));
        assertThat(channelMessages).isEmpty();
    }

    @Test
    void observePatterns() throws Exception {

        block(pubsub.psubscribe(pattern));

        BlockingQueue<PatternMessage<String, String>> patternMessages = LettuceFactories.newBlockingQueue();

        pubsub.observePatterns().doOnNext(patternMessages::add).subscribe();

        redis.publish(channel, message);
        redis.publish(channel, message);
        redis.publish(channel, message);

        Wait.untilTrue(() -> patternMessages.size() == 3).waitOrTimeout();
        assertThat(patternMessages).hasSize(3);

        PatternMessage<String, String> patternMessage = patternMessages.take();
        assertThat(patternMessage.getChannel()).isEqualTo(channel);
        assertThat(patternMessage.getMessage()).isEqualTo(message);
        assertThat(patternMessage.getPattern()).isEqualTo(pattern);
    }

    @Test
    void observePatternsWithUnsubscribe() {

        block(pubsub.psubscribe(pattern));

        BlockingQueue<PatternMessage<String, String>> patternMessages = LettuceFactories.newBlockingQueue();

        Disposable subscription = pubsub.observePatterns().doOnNext(patternMessages::add).subscribe();

        redis.publish(channel, message);
        redis.publish(channel, message);
        redis.publish(channel, message);

        Wait.untilTrue(() -> patternMessages.size() == 3).waitOrTimeout();
        assertThat(patternMessages).hasSize(3);
        subscription.dispose();

        redis.publish(channel, message);
        redis.publish(channel, message);
        redis.publish(channel, message);

        Delay.delay(Duration.ofMillis(500));

        assertThat(patternMessages).hasSize(3);
    }

    @Test
    void observeMessage() throws Exception {

        block(pubsub.subscribe(channel));

        BlockingQueue<SubscriptionMessage<String, String>> channelMessages = LettuceFactories.newBlockingQueue();

        Disposable disposable = pubsub.observe(sink -> new RedisPubSubAdapter<String, String>() {

            @Override
            public void message(String channel, String message) {
                sink.next(new SubscriptionMessage<>(channel, message));
            }

        }).doOnNext(channelMessages::add).subscribe();

        redis.publish(channel, message);
        redis.publish(channel, message);
        redis.publish(channel, message);

        Wait.untilEquals(3, channelMessages::size).waitOrTimeout();
        assertThat(channelMessages).hasSize(3);

        disposable.dispose();
        redis.publish(channel, message);
        Delay.delay(Duration.ofMillis(500));
        assertThat(channelMessages).hasSize(3);

        SubscriptionMessage<String, String> channelMessage = channelMessages.take();
        assertThat(channelMessage.getChannel()).isEqualTo(channel);
        assertThat(channelMessage.getMessage()).isEqualTo(message);
    }

    @Test
    void observeMessageUnsubscribe() {

        block(pubsub.subscribe(channel));

        BlockingQueue<SubscriptionMessage<String, String>> channelMessages = LettuceFactories.newBlockingQueue();

        pubsub.observe(sink -> new RedisPubSubAdapter<String, String>() {

            @Override
            public void message(String channel, String message) {
                sink.next(new SubscriptionMessage<>(channel, message));
            }

        }).doOnNext(channelMessages::add).subscribe().dispose();

        block(statefulRedisConnection.reactive().publish(channel, message));
        block(statefulRedisConnection.reactive().publish(channel, message));

        Delay.delay(Duration.ofMillis(500));
        assertThat(channelMessages).isEmpty();
    }

    @Test
    void observePMessage() throws Exception {

        block(pubsub.psubscribe(pattern));

        BlockingQueue<SubscriptionMessage<String, String>> patternMessages = LettuceFactories.newBlockingQueue();

        pubsub.observe(sink -> new RedisPubSubAdapter<String, String>() {

            @Override
            public void message(String pattern, String channel, String message) {
                sink.next(new SubscriptionMessage<>(pattern, channel, message));
            }

        }).doOnNext(patternMessages::add).subscribe();

        redis.publish(channel, message);
        redis.publish(channel, message);
        redis.publish(channel, message);

        Wait.untilTrue(() -> patternMessages.size() == 3).waitOrTimeout();
        assertThat(patternMessages).hasSize(3);

        SubscriptionMessage<String, String> patternMessage = patternMessages.take();
        assertThat(patternMessage.getChannel()).isEqualTo(channel);
        assertThat(patternMessage.getMessage()).isEqualTo(message);
        assertThat(patternMessage.getPattern()).isEqualTo(pattern);
    }

    @Test
    void observePMessageWithUnsubscribe() {

        block(pubsub.psubscribe(pattern));

        BlockingQueue<SubscriptionMessage<String, String>> patternMessages = LettuceFactories.newBlockingQueue();

        Disposable subscription = pubsub.observe(sink -> new RedisPubSubAdapter<String, String>() {

            @Override
            public void message(String pattern, String channel, String message) {
                sink.next(new SubscriptionMessage<>(pattern, channel, message));
            }

        }).doOnNext(patternMessages::add).subscribe();

        redis.publish(channel, message);
        redis.publish(channel, message);
        redis.publish(channel, message);

        Wait.untilTrue(() -> patternMessages.size() == 3).waitOrTimeout();
        assertThat(patternMessages).hasSize(3);
        subscription.dispose();

        redis.publish(channel, message);
        redis.publish(channel, message);
        redis.publish(channel, message);

        Delay.delay(Duration.ofMillis(500));

        assertThat(patternMessages).hasSize(3);
    }

    @Test
    void observeSMessage() throws Exception {

        block(pubsub.ssubscribe(shardChannel));

        BlockingQueue<SubscriptionMessage<String, String>> sharedMessages = LettuceFactories.newBlockingQueue();

        pubsub.observe(sink -> new RedisPubSubAdapter<String, String>() {

            @Override
            public void smessage(String shardChannel, String message) {
                sink.next(new SubscriptionMessage<>(shardChannel, message));
            }

        }).doOnNext(sharedMessages::add).subscribe();

        redis.spublish(shardChannel, message);
        redis.spublish(shardChannel, message);
        redis.spublish(shardChannel, message);

        Wait.untilTrue(() -> sharedMessages.size() == 3).waitOrTimeout();
        assertThat(sharedMessages).hasSize(3);

        SubscriptionMessage<String, String> shardMessage = sharedMessages.take();
        assertThat(shardMessage.getChannel()).isEqualTo(shardChannel);
        assertThat(shardMessage.getMessage()).isEqualTo(message);
    }

    @Test
    void observeSMessageWithUnsubscribe() {

        block(pubsub.ssubscribe(shardChannel));

        BlockingQueue<SubscriptionMessage<String, String>> shardMessages = LettuceFactories.newBlockingQueue();

        Disposable subscription = pubsub.observe(sink -> new RedisPubSubAdapter<String, String>() {

            @Override
            public void smessage(String shardChannel, String message) {
                sink.next(new SubscriptionMessage<>(shardChannel, message));
            }

        }).doOnNext(shardMessages::add).subscribe();

        redis.spublish(shardChannel, message);
        redis.spublish(shardChannel, message);
        redis.spublish(shardChannel, message);

        Wait.untilTrue(() -> shardMessages.size() == 3).waitOrTimeout();
        assertThat(shardMessages).hasSize(3);
        subscription.dispose();

        redis.spublish(shardChannel, message);
        redis.spublish(shardChannel, message);
        redis.spublish(shardChannel, message);

        Delay.delay(Duration.ofMillis(500));

        assertThat(shardMessages).hasSize(3);
    }

    @Test
    void message() throws Exception {

        block(pubsub.subscribe(channel));
        assertThat(channels.take()).isEqualTo(channel);

        redis.publish(channel, message);
        assertThat(channels.take()).isEqualTo(channel);
        assertThat(messages.take()).isEqualTo(message);
    }

    @Test
    void pmessage() throws Exception {

        block(pubsub.psubscribe(pattern));
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
    void psubscribe() throws Exception {

        block(pubsub.psubscribe(pattern));

        assertThat(patterns.take()).isEqualTo(pattern);
        assertThat((long) counts.take()).isEqualTo(1);
    }

    @Test
    void pubsubEmptyChannels() {
        assertThatThrownBy(() -> pubsub.subscribe()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void pubsubChannels() {

        block(pubsub.subscribe(channel));
        List<String> result = block(pubsub2.pubsubChannels().collectList());
        assertThat(result).contains(channel);
    }

    @Test
    void pubsubMultipleChannels() {

        StepVerifier.create(pubsub.subscribe(channel, "channel1", "channel3")).verifyComplete();

        StepVerifier.create(pubsub2.pubsubChannels().collectList())
                .consumeNextWith(actual -> assertThat(actual).contains(channel, "channel1", "channel3")).verifyComplete();
    }

    @Test
    void pubsubChannelsWithArg() {

        StepVerifier.create(pubsub.subscribe(channel)).verifyComplete();
        Wait.untilTrue(() -> mono(pubsub2.pubsubChannels(pattern).filter(channel::equals)) != null).waitOrTimeout();

        String result = mono(pubsub2.pubsubChannels(pattern).filter(channel::equals));
        assertThat(result).isEqualToIgnoringCase(channel);
    }

    @Test
    void pubsubNumsub() {

        StepVerifier.create(pubsub.subscribe(channel)).verifyComplete();

        Wait.untilEquals(1, () -> block(pubsub2.pubsubNumsub(channel)).size()).waitOrTimeout();

        Map<String, Long> result = block(pubsub2.pubsubNumsub(channel));
        assertThat(result).hasSize(1);
        assertThat(result).containsKeys(channel);
    }

    @Test
    @EnabledOnCommand("SPUBLISH")
    void pubsubShardChannels() {
        block(pubsub.ssubscribe(shardChannel));
        List<String> result = block(pubsub2.pubsubShardChannels().collectList());
        assertThat(result).contains(shardChannel);
    }

    @Test
    @EnabledOnCommand("SPUBLISH")
    void pubsubShardMultipleChannels() {
        StepVerifier.create(pubsub.ssubscribe(shardChannel, "channel1", "channel3")).verifyComplete();

        StepVerifier.create(pubsub2.pubsubShardChannels().collectList())
                .consumeNextWith(actual -> assertThat(actual).contains(shardChannel, "channel1", "channel3")).verifyComplete();
    }

    @Test
    @EnabledOnCommand("SPUBLISH")
    void pubsubShardChannelsWithArg() {

        StepVerifier.create(pubsub.ssubscribe(shardChannel)).verifyComplete();
        Wait.untilTrue(() -> mono(pubsub2.pubsubShardChannels(shardChannel).filter(shardChannel::equals)) != null)
                .waitOrTimeout();

        String result = mono(pubsub2.pubsubShardChannels(shardChannel).filter(shardChannel::equals));
        assertThat(result).isEqualToIgnoringCase(shardChannel);
    }

    @Test
    @EnabledOnCommand("SPUBLISH")
    void pubsubShardNumsub() {
        StepVerifier.create(pubsub.ssubscribe(shardChannel)).verifyComplete();

        Wait.untilEquals(1, () -> block(pubsub2.pubsubShardNumsub(shardChannel)).size()).waitOrTimeout();

        Map<String, Long> result = block(pubsub2.pubsubShardNumsub(shardChannel));
        assertThat(result.getOrDefault(shardChannel, 0L)).isEqualTo(1);
    }

    @Test
    void pubsubNumpat() {

        Wait.untilEquals(0L, () -> block(pubsub2.pubsubNumpat())).waitOrTimeout();

        StepVerifier.create(pubsub.psubscribe(pattern)).verifyComplete();
        Wait.untilEquals(1L, () -> redis.pubsubNumpat()).waitOrTimeout();

        Long result = block(pubsub2.pubsubNumpat());
        assertThat(result.longValue()).isGreaterThan(0);
    }

    @Test
    void punsubscribe() throws Exception {

        StepVerifier.create(pubsub.punsubscribe(pattern)).verifyComplete();
        assertThat(patterns.take()).isEqualTo(pattern);
        assertThat((long) counts.take()).isEqualTo(0);

    }

    @Test
    void subscribe() throws Exception {

        StepVerifier.create(pubsub.subscribe(channel)).verifyComplete();
        assertThat(channels.take()).isEqualTo(channel);
        assertThat((long) counts.take()).isGreaterThan(0);
    }

    @Test
    void unsubscribe() throws Exception {

        StepVerifier.create(pubsub.unsubscribe(channel)).verifyComplete();
        assertThat(channels.take()).isEqualTo(channel);
        assertThat((long) counts.take()).isEqualTo(0);

        block(pubsub.unsubscribe());

        assertThat(channels).isEmpty();
        assertThat(patterns).isEmpty();

    }

    @Test
    void ssubscribe() throws Exception {
        StepVerifier.create(pubsub.ssubscribe(channel)).verifyComplete();
        assertThat(shardChannels.take()).isEqualTo(channel);
        assertThat((long) counts.take()).isGreaterThan(0);
    }

    @Test
    void sunsubscribe() throws Exception {
        StepVerifier.create(pubsub.sunsubscribe(channel)).verifyComplete();
        assertThat(shardChannels.take()).isEqualTo(channel);
        assertThat((long) counts.take()).isEqualTo(0);
    }

    @Test
    void pubsubCloseOnClientShutdown() {
        final RedisURI uri = RedisURI.Builder.redis(host, port).build();
        RedisClient redisClient = RedisClient.create(TestClientResources.get(), uri);
        final StatefulRedisPubSubConnection<String, String> connection = redisClient.connectPubSub();
        FastShutdown.shutdown(redisClient);
        assertThat(connection.isOpen()).isFalse();
    }

    @Test
    void utf8Channel() throws Exception {

        String channel = "channelλ";
        String message = "αβγ";

        block(pubsub.subscribe(channel));
        assertThat(channels.take()).isEqualTo(channel);

        StepVerifier.create(pubsub2.publish(channel, message)).expectNextCount(1).verifyComplete();
        assertThat(channels.take()).isEqualTo(channel);
        assertThat(messages.take()).isEqualTo(message);
    }

    @Test
    void resubscribeChannelsOnReconnect() throws Exception {

        StepVerifier.create(pubsub.subscribe(channel)).verifyComplete();
        assertThat(channels.take()).isEqualTo(channel);
        assertThat((long) counts.take()).isEqualTo(1);

        block(pubsub.quit());
        assertThat(channels.take()).isEqualTo(channel);
        assertThat((long) counts.take()).isEqualTo(1);

        Wait.untilTrue(pubSubConnection::isOpen).waitOrTimeout();

        redis.publish(channel, message);
        assertThat(channels.take()).isEqualTo(channel);
        assertThat(messages.take()).isEqualTo(message);
    }

    @Test
    void resubscribePatternsOnReconnect() throws Exception {

        StepVerifier.create(pubsub.psubscribe(pattern)).verifyComplete();
        assertThat(patterns.take()).isEqualTo(pattern);
        assertThat((long) counts.take()).isEqualTo(1);

        block(pubsub.quit());

        assertThat(patterns.take()).isEqualTo(pattern);
        assertThat((long) counts.take()).isEqualTo(1);

        Wait.untilTrue(pubSubConnection::isOpen).waitOrTimeout();

        StepVerifier.create(pubsub2.publish(channel, message)).expectNextCount(1).verifyComplete();
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

        pubSubConnection.addListener(adapter);
        StepVerifier.create(pubsub.subscribe(channel)).verifyComplete();
        StepVerifier.create(pubsub.psubscribe(pattern)).verifyComplete();

        assertThat((long) localCounts.take()).isEqualTo(1L);

        StepVerifier.create(pubsub2.publish(channel, message)).expectNextCount(1).verifyComplete();
        StepVerifier.create(pubsub.punsubscribe(pattern)).verifyComplete();
        StepVerifier.create(pubsub.unsubscribe(channel)).verifyComplete();

        assertThat((long) localCounts.take()).isEqualTo(0L);
    }

    @Test
    void removeListener() throws Exception {

        StepVerifier.create(pubsub.subscribe(channel)).verifyComplete();
        assertThat(channels.take()).isEqualTo(channel);

        StepVerifier.create(pubsub2.publish(channel, message)).expectNextCount(1).verifyComplete();
        assertThat(channels.take()).isEqualTo(channel);
        assertThat(messages.take()).isEqualTo(message);

        pubSubConnection.removeListener(this);

        StepVerifier.create(pubsub2.publish(channel, message)).expectNextCount(1).verifyComplete();
        assertThat(channels.poll(10, TimeUnit.MILLISECONDS)).isNull();
        assertThat(messages.poll(10, TimeUnit.MILLISECONDS)).isNull();
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

    @Override
    public void ssubscribed(String shardChannel, long count) {
        shardChannels.add(shardChannel);
        counts.add(count);
    }

    @Override
    public void sunsubscribed(String shardChannel, long count) {
        shardChannels.add(shardChannel);
        counts.add(count);
    }

    <T> T block(Mono<T> mono) {
        return mono.block();
    }

    <T> T mono(Flux<T> flux) {
        return flux.next().block();
    }

    <T> List<T> all(Flux<T> flux) {
        return flux.collectList().block();
    }

}
