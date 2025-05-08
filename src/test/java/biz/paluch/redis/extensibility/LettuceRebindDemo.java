package biz.paluch.redis.extensibility;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.event.EventBus;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class LettuceRebindDemo {

    public static final String ADDRESS = System.getenv("REBIND_DEMO_ADDRESS");

    public static final Logger logger = Logger.getLogger(LettuceRebindDemo.class.getName());

    public static final String KEY = "rebind:" + UUID.randomUUID().getLeastSignificantBits();

    public static void main(String[] args) throws ExecutionException, InterruptedException {

        TimeoutOptions timeoutOpts = TimeoutOptions.builder().timeoutCommands().fixedTimeout(Duration.ofMillis(250))
                // (optional) relax timeouts during re-bind to decrease risk of timeouts
                .proactiveTimeoutsRelaxing(Duration.ofMillis(750)).build();
        // (required) enable proactive re-bind by enabling it in the ClientOptions
        ClientOptions options = ClientOptions.builder().timeoutOptions(timeoutOpts).proactiveRebind(true).build();

        RedisClient redisClient = RedisClient.create(RedisURI.create(ADDRESS == null ? "redis://localhost:6379" : ADDRESS));
        redisClient.setOptions(options);

        // (optional) monitor connection events
        EventBus eventBus = redisClient.getResources().eventBus();
        eventBus.get().subscribe(e -> {
            logger.info(">>> Event bus received: {} " + e);
        });

        StatefulRedisConnection<String, String> redis = redisClient.connect();
        RedisAsyncCommands<String, String> commands = redis.async();

        // Used to stop the demo after 3 minutes
        Control control = new Control(Duration.ofMinutes(3));

        ExecutorService executorService = new ThreadPoolExecutor(5, // core pool size
                10, // maximum pool size
                60, TimeUnit.SECONDS, // idle thread keep-alive time
                new ArrayBlockingQueue<>(20), // work queue size
                new ThreadPoolExecutor.DiscardPolicy()); // rejection policy

        try {
            while (control.shouldContinue()) {
                executorService.execute(new DemoWorker(commands));
                Thread.sleep(1);
            }

            if (executorService.awaitTermination(5, TimeUnit.SECONDS)) {
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

        private final RedisAsyncCommands<String, String> commands;

        public DemoWorker(RedisAsyncCommands<String, String> commands) {
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

    static class Control {

        private final Instant start;

        private final Duration duration;

        public Control(Duration duration) {
            this.duration = duration;
            this.start = Instant.now();
        }

        public boolean shouldContinue() {
            return Instant.now().isBefore(start.plus(duration));
        }

    }

}
