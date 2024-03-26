package biz.paluch.redis.extensibility;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.api.push.PushListener;
import io.lettuce.core.api.push.PushMessage;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.event.EventBus;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class LettuceRebindDemo {

    public static final Logger logger = Logger.getLogger(LettuceRebindDemo.class.getName());

    public static final String KEY = "rebind:" + UUID.randomUUID().getLeastSignificantBits();

    public static void main(String[] args) throws ExecutionException, InterruptedException {

        // NEW! No need for a custom handler

        TimeoutOptions timeoutOpts = TimeoutOptions.builder()
                .timeoutCommands()
                .fixedTimeout(Duration.ofMillis(50))
                // NEW! control that during timeouts we need to relax the timeouts
                .proactiveTimeoutsRelaxing(Duration.ofMillis(500))
                .build();
        ClientOptions options = ClientOptions.builder().timeoutOptions(timeoutOpts).build();

        RedisClient redisClient = RedisClient.create(RedisURI.Builder.redis("localhost", 6379).build());
        redisClient.setOptions(options);

        // Monitor connection events
        EventBus eventBus = redisClient.getResources().eventBus();
        eventBus.get().subscribe(e -> {
            logger.info(">>> Event bus received: {} " + e);
        });

        // Subscribe to __rebind channel (REMOVE ONCE WE START RECEIVING THESE WITHOUT SUBSCRIPTION)
        StatefulRedisPubSubConnection<String, String> redis = redisClient.connectPubSub();
        RedisPubSubAsyncCommands<String, String> commands = redis.async();
        commands.subscribe("__rebind").get();

        // Used to stop the demo by sending the following command:
        // publish __rebind "type=stop_demo"
        Control control = new Control();
        redis.addListener(control);

        // Used to initiate the proactive rebind by sending the following command
        // publish __rebind "type=rebind;from_ep=localhost:6379;to_ep=localhost:6479;until_s=10"

        ExecutorService executorService = new ThreadPoolExecutor(
                5,                           // core pool size
                10,                                      // maximum pool size
                60, TimeUnit.SECONDS,                    // idle thread keep-alive time
                new ArrayBlockingQueue<>(20),    // work queue size
                new ThreadPoolExecutor.DiscardPolicy()); // rejection policy

        try {
            while (control.shouldContinue) {
                executorService.execute(new DemoWorker(commands));
                Thread.sleep(1);
            }

            if(executorService.awaitTermination(5, TimeUnit.SECONDS)){
                logger.info("Executor service terminated");
            } else {
                logger.warning("Executor service did not terminate in the specified time");
            }

        } finally {
            executorService.shutdownNow();
        }

        redis.close();
        redisClient.shutdown();
    }
    
    static class DemoWorker implements Runnable {
        private final RedisPubSubAsyncCommands<String, String> commands;

        public DemoWorker(RedisPubSubAsyncCommands<String, String> commands) {
            this.commands = commands;
        }

        @Override
        public void run() {
            try {
                commands.incr(KEY).get();
            } catch (InterruptedException | ExecutionException e) {
                logger.severe("ExecutionException: " + e.getMessage());
            }
        }
    }

    static class Control implements PushListener {

        public boolean shouldContinue = true;

        @Override
        public void onPushMessage(PushMessage message) {
            List<String> content = message.getContent().stream()
                    .filter(ez -> ez instanceof ByteBuffer)
                    .map(ez -> StringCodec.UTF8.decodeKey((ByteBuffer) ez))
                    .collect(Collectors.toList());

            if (content.stream().anyMatch(c -> c.contains("type=stop_demo"))) {
                logger.info("Control received message to stop the demo");
                shouldContinue = false;
            }
        }

    }

}
