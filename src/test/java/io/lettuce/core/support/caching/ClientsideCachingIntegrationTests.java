package io.lettuce.core.support.caching;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.TestSupport;
import io.lettuce.core.TrackingArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.ProtocolVersion;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.Wait;
import io.lettuce.test.condition.EnabledOnCommand;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import javax.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Integration tests for server-side assisted cache invalidation.
 *
 * @author Mark Paluch
 * @author Yoobin Yoon
 */
@Tag(INTEGRATION_TEST)
@ExtendWith(LettuceExtension.class)
@EnabledOnCommand("ACL")
public class ClientsideCachingIntegrationTests extends TestSupport {

    private final RedisClient redisClient;

    @Inject
    public ClientsideCachingIntegrationTests(RedisClient redisClient) {
        this.redisClient = redisClient;
    }

    @BeforeEach
    void setUp() {

        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            connection.sync().flushdb();
        }
    }

    @Test
    void clientCachingResp2() {

        ClientOptions resp2 = ClientOptions.builder().protocolVersion(ProtocolVersion.RESP2).build();

        redisClient.setOptions(resp2);

        StatefulRedisConnection<String, String> data = redisClient.connect();
        RedisCommands<String, String> commands = data.sync();
        StatefulRedisPubSubConnection<String, String> pubSub = redisClient.connectPubSub();

        List<String> invalidations = new CopyOnWriteArrayList<>();

        commands.clientTracking(TrackingArgs.Builder.enabled().redirect(pubSub.sync().clientId()));

        pubSub.addListener(new RedisPubSubAdapter<String, String>() {

            @Override
            public void message(String channel, String message) {
                if (channel.equals("__redis__:invalidate")) {
                    invalidations.add(message);
                }
            }

        });

        pubSub.sync().subscribe("__redis__:invalidate");

        commands.get("key1");
        commands.get("key2");

        assertThat(invalidations).isEmpty();

        Map<String, String> keys = new HashMap<>();
        keys.put("key1", "value1");
        keys.put("key2", "value2");

        commands.mset(keys);

        Wait.untilEquals(2, invalidations::size).waitOrTimeout();

        assertThat(invalidations).contains("key1", "key2");

        data.close();
        pubSub.close();
    }

    @Test
    void clientCachingResp3() {

        ClientOptions resp2 = ClientOptions.builder().protocolVersion(ProtocolVersion.RESP3).build();

        redisClient.setOptions(resp2);

        StatefulRedisConnection<String, String> data = redisClient.connect();
        RedisCommands<String, String> commands = data.sync();

        List<String> invalidations = new CopyOnWriteArrayList<>();

        commands.clientTracking(TrackingArgs.Builder.enabled());

        data.addListener(message -> {

            if (message.getType().equals("invalidate")) {
                invalidations.addAll((List) message.getContent(StringCodec.UTF8::decodeKey).get(1));
            }
        });

        commands.get("key1");
        commands.get("key2");

        assertThat(invalidations).isEmpty();

        Map<String, String> keys = new HashMap<>();
        keys.put("key1", "value1");
        keys.put("key2", "value2");

        commands.mset(keys);

        Wait.untilEquals(2, invalidations::size).waitOrTimeout();

        assertThat(invalidations).contains("key1", "key2");

        data.close();
    }

    @Test
    void serverAssistedCachingShouldFetchValueFromRedis() {

        Map<String, String> clientCache = new ConcurrentHashMap<>();

        StatefulRedisConnection<String, String> otherParty = redisClient.connect();
        RedisCommands<String, String> commands = otherParty.sync();

        commands.set(key, value);

        StatefulRedisConnection<String, String> connection = redisClient.connect();
        CacheFrontend<String, String> frontend = ClientSideCaching.enable(CacheAccessor.forMap(clientCache), connection,
                TrackingArgs.Builder.enabled().noloop());

        assertThat(clientCache).isEmpty();
        String shouldExist = frontend.get(key);
        assertThat(shouldExist).isNotNull();
        assertThat(clientCache).hasSize(1);

        otherParty.close();
        frontend.close();
    }

    @Test
    void serverAssistedCachingShouldExpireValueFromRedis() throws InterruptedException {

        Map<String, String> clientCache = new ConcurrentHashMap<>();

        StatefulRedisConnection<String, String> otherParty = redisClient.connect();
        RedisCommands<String, String> commands = otherParty.sync();

        StatefulRedisConnection<String, String> connection = redisClient.connect();
        CacheFrontend<String, String> frontend = ClientSideCaching.enable(CacheAccessor.forMap(clientCache), connection,
                TrackingArgs.Builder.enabled());

        // make sure value exists in Redis
        // client-side cache is empty
        commands.set(key, value);

        // Read-through into Redis
        String cachedValue = frontend.get(key);
        assertThat(cachedValue).isNotNull();

        // client-side cache holds the same value
        assertThat(clientCache).hasSize(1);

        // now, the key expires
        commands.pexpire(key, 1);

        // a while later
        Thread.sleep(200);

        // the expiration reflects in the client-side cache
        assertThat(clientCache).isEmpty();

        assertThat(frontend.get(key)).isNull();

        otherParty.close();
        frontend.close();
    }

    @Test
    void serverAssistedCachingShouldUseValueLoader() throws InterruptedException {

        Map<String, String> clientCache = new ConcurrentHashMap<>();

        StatefulRedisConnection<String, String> otherParty = redisClient.connect();
        RedisCommands<String, String> commands = otherParty.sync();

        StatefulRedisConnection<String, String> connection = redisClient.connect();
        CacheFrontend<String, String> frontend = ClientSideCaching.enable(CacheAccessor.forMap(clientCache), connection,
                TrackingArgs.Builder.enabled().noloop());

        String shouldLoad = frontend.get(key, () -> "myvalue");
        assertThat(shouldLoad).isEqualTo("myvalue");
        assertThat(clientCache).hasSize(1);
        assertThat(commands.get(key)).isEqualTo("myvalue");

        commands.set(key, value);
        Thread.sleep(100);
        assertThat(clientCache).isEmpty();

        otherParty.close();
        frontend.close();
    }

    @Test
    void valueLoaderShouldBeInvokedOnceForConcurrentRequests() throws Exception {

        Map<String, String> clientCache = new ConcurrentHashMap<>();

        StatefulRedisConnection<String, String> connection = redisClient.connect();

        final String testKey = "concurrent-loader-key";
        connection.sync().del(testKey);

        AtomicInteger loaderCallCount = new AtomicInteger(0);

        CacheFrontend<String, String> frontend = ClientSideCaching.enable(CacheAccessor.forMap(clientCache), connection,
                TrackingArgs.Builder.enabled());

        try {
            int threadCount = 10;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch finishLatch = new CountDownLatch(threadCount);
            List<String> results = new CopyOnWriteArrayList<>();

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();

                        String result = frontend.get(testKey, () -> {
                            loaderCallCount.incrementAndGet();

                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }

                            return "loaded-value";
                        });

                        results.add(result);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        finishLatch.countDown();
                    }
                });
            }

            startLatch.countDown();

            finishLatch.await(5, TimeUnit.SECONDS);
            executor.shutdown();

            assertThat(loaderCallCount.get()).isEqualTo(1);

            assertThat(results).hasSize(threadCount);
            assertThat(results).containsOnly("loaded-value");

            assertThat(connection.sync().get(testKey)).isEqualTo("loaded-value");

            assertThat(clientCache).containsEntry(testKey, "loaded-value");
        } finally {
            frontend.close();
            connection.close();
        }
    }

    @Test
    void locksShouldBeProperlyCleanedUp() throws Exception {

        Map<String, String> clientCache = new ConcurrentHashMap<>();

        StatefulRedisConnection<String, String> connection = redisClient.connect();
        StatefulRedisConnection<String, String> otherClient = redisClient.connect();

        final String testKey1 = "lock-test-key1";
        final String testKey2 = "lock-test-key2";
        final String initialValue = "initial-value";
        final String updatedValue = "updated-value";

        connection.sync().del(testKey1, testKey2);
        connection.sync().set(testKey1, initialValue);
        connection.sync().set(testKey2, initialValue);

        ClientSideCaching<String, String> frontend = (ClientSideCaching<String, String>) ClientSideCaching.enable(
                CacheAccessor.forMap(clientCache), connection, TrackingArgs.Builder.enabled());

        Field keyLocksField = ClientSideCaching.class.getDeclaredField("keyLocks");
        keyLocksField.setAccessible(true);
        ConcurrentHashMap<String, ReentrantLock> keyLocks = (ConcurrentHashMap<String, ReentrantLock>) keyLocksField.get(
                frontend);

        try {
            frontend.get(testKey1);
            frontend.get(testKey2);

            assertThat(keyLocks).containsKey(testKey1);
            assertThat(keyLocks).containsKey(testKey2);
            assertThat(keyLocks).hasSize(2);

            otherClient.sync().set(testKey1, updatedValue);

            Thread.sleep(200);

            assertThat(keyLocks).doesNotContainKey(testKey1);
            assertThat(keyLocks).containsKey(testKey2);
            assertThat(keyLocks).hasSize(1);

            frontend.get(testKey1);

            assertThat(keyLocks).containsKey(testKey1);
            assertThat(keyLocks).hasSize(2);

            frontend.close();

            assertThat(keyLocks).isEmpty();

        } finally {
            connection.close();
            otherClient.close();
        }
    }

}
