package biz.paluch.redis.extensibility;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.api.push.PushListener;
import io.lettuce.core.api.push.PushMessage;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.event.EventBus;
import io.lettuce.core.proactive.ProactiveWatchdogCommandHandler;
import io.lettuce.core.protocol.CommandExpiryWriter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.NettyCustomizer;
import io.netty.channel.Channel;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class LettuceProactiveDemo {

    public static final Logger logger = Logger.getLogger(LettuceProactiveDemo.class.getName());

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        ProactiveWatchdogCommandHandler<String, String> proactiveHandler = new ProactiveWatchdogCommandHandler<>();

        ClientResources resources = ClientResources.builder().nettyCustomizer(new NettyCustomizer() {

            @Override
            public void afterChannelInitialized(Channel channel) {
                channel.pipeline().addFirst(proactiveHandler);
            }

        }).build();

        TimeoutOptions timeoutOpts = TimeoutOptions.builder().timeoutCommands().fixedTimeout(Duration.ofMillis(1)).build();
        ClientOptions options = ClientOptions.builder().timeoutOptions(timeoutOpts).build();

        CommandExpiryWriter.enableProactive = true;

        RedisClient redisClient = RedisClient.create(resources, RedisURI.Builder.redis("localhost", 6379).build());
        redisClient.setOptions(options);

        // Monitor connection events
        EventBus eventBus = redisClient.getResources().eventBus();
        eventBus.get().subscribe(e -> {
            logger.info(">>> Connection event: " + e);
        });

        // Subscribe to __rebind channel
        StatefulRedisPubSubConnection<String, String> redis = redisClient.connectPubSub();
        RedisPubSubAsyncCommands<String, String> commands = redis.async();
        commands.subscribe("__rebind").get();

        // Used to stop the demo by sending the following command:
        // publish __rebind "type=stop_demo"
        Control control = new Control();
        redis.addListener(control);

        // Used to initiate the proactive rebind by sending the following command
        // publish __rebind "type=rebind;from_ep=localhost:6379;to_ep=localhost:6479;until_s=10"

        // NO LONGER NEEDED, HANDLER REGISTERS ITSELF
        // redis.addListener(proactiveHandler);

        while (control.shouldContinue) {
            try {
                logger.info("Sending PING");
                logger.info(commands.ping().get());
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                logger.severe("InterruptedException: " + e.getMessage());
            }
        }

        redis.close();
        redisClient.shutdown();
    }

    static class Control implements PushListener {

        public boolean shouldContinue = true;

        @Override
        public void onPushMessage(PushMessage message) {
            List<String> content = message.getContent().stream().map(ez -> StringCodec.UTF8.decodeKey((ByteBuffer) ez))
                    .collect(Collectors.toList());

            if (content.stream().anyMatch(c -> c.equals("type=stop_demo"))) {
                logger.info("Control received message to stop the demo");
                shouldContinue = false;
            }
        }

    }

}
