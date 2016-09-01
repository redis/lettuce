package com.lambdaworks.redis.pubsub;

import static com.google.code.tempusfugit.temporal.Duration.millis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.lambdaworks.redis.reactive.TestSubscriber;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.lambdaworks.Delay;
import com.lambdaworks.TestClientResources;
import com.lambdaworks.Wait;
import com.lambdaworks.redis.AbstractRedisClientTest;
import com.lambdaworks.redis.FastShutdown;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.internal.LettuceFactories;
import com.lambdaworks.redis.pubsub.api.reactive.ChannelMessage;
import com.lambdaworks.redis.pubsub.api.reactive.PatternMessage;
import com.lambdaworks.redis.pubsub.api.reactive.RedisPubSubReactiveCommands;
import com.lambdaworks.redis.pubsub.api.sync.RedisPubSubCommands;

import reactor.core.Cancellation;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Mark Paluch
 */
public class PubSubReactiveTest extends AbstractRedisClientTest implements RedisPubSubListener<String, String> {

    private RedisPubSubReactiveCommands<String, String> pubsub;
    private RedisPubSubReactiveCommands<String, String> pubsub2;

    private BlockingQueue<String> channels;
    private BlockingQueue<String> patterns;
    private BlockingQueue<String> messages;
    private BlockingQueue<Long> counts;

    private String channel = "channel0";
    private String pattern = "channel*";
    private String message = "msg!";

    @Before
    public void openPubSubConnection() throws Exception {

        pubsub = client.connectPubSub().reactive();
        pubsub2 = client.connectPubSub().reactive();
        pubsub.addListener(this);
        channels = LettuceFactories.newBlockingQueue();
        patterns = LettuceFactories.newBlockingQueue();
        messages = LettuceFactories.newBlockingQueue();
        counts = LettuceFactories.newBlockingQueue();
    }

    @After
    public void closePubSubConnection() throws Exception {
        pubsub.getStatefulConnection().close();
        pubsub2.getStatefulConnection().close();
    }

    @Test
    public void observeChannels() throws Exception {

        block(pubsub.subscribe(channel));

        BlockingQueue<ChannelMessage<String, String>> channelMessages = LettuceFactories.newBlockingQueue();
        TestSubscriber<ChannelMessage<String, String>> testSubscriber = TestSubscriber.create();

        pubsub.observeChannels().doOnNext(channelMessages::add).subscribe(testSubscriber);

        redis.publish(channel, message);
        redis.publish(channel, message);
        redis.publish(channel, message);

        testSubscriber.awaitAndAssertNextValueCount(3);
        Wait.untilEquals(3, () -> channelMessages.size()).waitOrTimeout();
        assertThat(channelMessages).hasSize(3);

        testSubscriber.cancel();
        redis.publish(channel, message);
        Delay.delay(millis(500));
        assertThat(channelMessages).hasSize(3);

        ChannelMessage<String, String> channelMessage = channelMessages.take();
        assertThat(channelMessage.getChannel()).isEqualTo(channel);
        assertThat(channelMessage.getMessage()).isEqualTo(message);
    }

    @Test
    public void observeChannelsUnsubscribe() throws Exception {

        block(pubsub.subscribe(channel));

        BlockingQueue<ChannelMessage<String, String>> channelMessages = LettuceFactories.newBlockingQueue();

        pubsub.observeChannels().doOnNext(channelMessages::add).subscribe().dispose();

        block(redis.getStatefulConnection().reactive().publish(channel, message));
        block(redis.getStatefulConnection().reactive().publish(channel, message));

        Delay.delay(millis(500));
        assertThat(channelMessages).isEmpty();
    }

    @Test
    public void observePatterns() throws Exception {

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
    public void observePatternsWithUnsubscribe() throws Exception {

        block(pubsub.psubscribe(pattern));

        BlockingQueue<PatternMessage<String, String>> patternMessages = LettuceFactories.newBlockingQueue();

        Cancellation subscription = pubsub.observePatterns().doOnNext(patternMessages::add).subscribe();

        redis.publish(channel, message);
        redis.publish(channel, message);
        redis.publish(channel, message);

        Wait.untilTrue(() -> patternMessages.size() == 3).waitOrTimeout();
        assertThat(patternMessages).hasSize(3);
        subscription.dispose();

        redis.publish(channel, message);
        redis.publish(channel, message);
        redis.publish(channel, message);

        Delay.delay(millis(500));

        assertThat(patternMessages).hasSize(3);
    }

    @Test(timeout = 2000)
    public void message() throws Exception {

        block(pubsub.subscribe(channel));
        assertThat(channels.take()).isEqualTo(channel);

        redis.publish(channel, message);
        assertThat(channels.take()).isEqualTo(channel);
        assertThat(messages.take()).isEqualTo(message);
    }

    @Test(timeout = 2000)
    public void pmessage() throws Exception {

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

    @Test(timeout = 2000)
    public void psubscribe() throws Exception {

        block(pubsub.psubscribe(pattern));

        assertThat(patterns.take()).isEqualTo(pattern);
        assertThat((long) counts.take()).isEqualTo(1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void pubsubEmptyChannels() throws Exception {

        pubsub.subscribe();
        fail("Missing IllegalArgumentException: channels must not be empty");
    }

    @Test
    public void pubsubChannels() throws Exception {

        block(pubsub.subscribe(channel));
        List<String> result = block(pubsub2.pubsubChannels().collectList());
        assertThat(result).contains(channel);
    }

    @Test
    public void pubsubMultipleChannels() throws Exception {

        block(pubsub.subscribe(channel, "channel1", "channel3"));

        List<String> result = all(pubsub2.pubsubChannels());
        assertThat(result).contains(channel, "channel1", "channel3");
    }

    @Test
    public void pubsubChannelsWithArg() throws Exception {

        pubsub.subscribe(channel).subscribe();
        Wait.untilTrue(() -> mono(pubsub2.pubsubChannels(pattern).filter(s -> channel.equals(s))) != null).waitOrTimeout();

        String result = mono(pubsub2.pubsubChannels(pattern).filter(s -> channel.equals(s)));
        assertThat(result).isEqualToIgnoringCase(channel);
    }

    @Test
    public void pubsubNumsub() throws Exception {

        pubsub.subscribe(channel).subscribe();
        Wait.untilEquals(1, () -> block(pubsub2.pubsubNumsub(channel)).size()).waitOrTimeout();

        Map<String, Long> result = block(pubsub2.pubsubNumsub(channel));
        assertThat(result).hasSize(1);
        assertThat(result.get(channel)).isGreaterThan(0);
    }

    @Test
    public void pubsubNumpat() throws Exception {

        Wait.untilEquals(0L, () -> block(pubsub2.pubsubNumpat())).waitOrTimeout();

        pubsub.psubscribe(pattern).subscribe();
        Wait.untilEquals(1L, () -> redis.pubsubNumpat()).waitOrTimeout();

        Long result = block(pubsub2.pubsubNumpat());
        assertThat(result.longValue()).isGreaterThan(0);
    }

    @Test(timeout = 2000)
    public void punsubscribe() throws Exception {

        pubsub.punsubscribe(pattern).subscribe();
        assertThat(patterns.take()).isEqualTo(pattern);
        assertThat((long) counts.take()).isEqualTo(0);

    }

    @Test(timeout = 2000)
    public void subscribe() throws Exception {

        pubsub.subscribe(channel).subscribe();
        assertThat(channels.take()).isEqualTo(channel);
        assertThat((long) counts.take()).isGreaterThan(0);
    }

    @Test(timeout = 2000)
    public void unsubscribe() throws Exception {

        pubsub.unsubscribe(channel).subscribe();
        assertThat(channels.take()).isEqualTo(channel);
        assertThat((long) counts.take()).isEqualTo(0);

        block(pubsub.unsubscribe());

        assertThat(channels).isEmpty();
        assertThat(patterns).isEmpty();

    }

    @Test
    public void pubsubCloseOnClientShutdown() throws Exception {

        RedisClient redisClient = RedisClient.create(TestClientResources.get(), RedisURI.Builder.redis(host, port).build());

        RedisPubSubCommands<String, String> connection = redisClient.connectPubSub().sync();
        FastShutdown.shutdown(redisClient);

        assertThat(connection.isOpen()).isFalse();
    }

    @Test(timeout = 2000)
    public void utf8Channel() throws Exception {

        String channel = "channelλ";
        String message = "αβγ";

        block(pubsub.subscribe(channel));
        assertThat(channels.take()).isEqualTo(channel);

        pubsub2.publish(channel, message).subscribe();
        assertThat(channels.take()).isEqualTo(channel);
        assertThat(messages.take()).isEqualTo(message);
    }

    @Test(timeout = 2000)
    public void resubscribeChannelsOnReconnect() throws Exception {

        pubsub.subscribe(channel).subscribe();
        assertThat(channels.take()).isEqualTo(channel);
        assertThat((long) counts.take()).isEqualTo(1);

        block(pubsub.quit());
        assertThat(channels.take()).isEqualTo(channel);
        assertThat((long) counts.take()).isEqualTo(1);

        Wait.untilTrue(pubsub::isOpen).waitOrTimeout();

        redis.publish(channel, message);
        assertThat(channels.take()).isEqualTo(channel);
        assertThat(messages.take()).isEqualTo(message);
    }

    @Test(timeout = 2000)
    public void resubscribePatternsOnReconnect() throws Exception {

        pubsub.psubscribe(pattern).subscribe();
        assertThat(patterns.take()).isEqualTo(pattern);
        assertThat((long) counts.take()).isEqualTo(1);

        block(pubsub.quit());

        assertThat(patterns.take()).isEqualTo(pattern);
        assertThat((long) counts.take()).isEqualTo(1);

        Wait.untilTrue(pubsub::isOpen).waitOrTimeout();

        pubsub2.publish(channel, message).subscribe();
        assertThat(channels.take()).isEqualTo(channel);
        assertThat(messages.take()).isEqualTo(message);
    }

    @Test(timeout = 2000)
    public void adapter() throws Exception {

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

        pubsub.addListener(adapter);
        pubsub.subscribe(channel).subscribe();
        pubsub.psubscribe(pattern).subscribe();

        assertThat((long) localCounts.take()).isEqualTo(1L);

        pubsub2.publish(channel, message).subscribe();
        pubsub.punsubscribe(pattern).subscribe();
        pubsub.unsubscribe(channel).subscribe();

        assertThat((long) localCounts.take()).isEqualTo(0L);
    }

    @Test(timeout = 2000)
    public void removeListener() throws Exception {

        pubsub.subscribe(channel).subscribe();
        assertThat(channels.take()).isEqualTo(channel);

        pubsub2.publish(channel, message).subscribe();
        assertThat(channels.take()).isEqualTo(channel);
        assertThat(messages.take()).isEqualTo(message);

        pubsub.removeListener(this);

        pubsub2.publish(channel, message).subscribe();
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

    protected <T> T block(Mono<T> mono) {
        return mono.block();
    }

    protected <T> T mono(Flux<T> flux) {
        return flux.next().block();
    }

    protected <T> List<T> all(Flux<T> flux) {
        return flux.collectList().block();
    }
}
