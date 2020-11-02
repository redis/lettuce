/*
 * Copyright 2020 the original author or authors.
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
package io.lettuce.core.support.caching;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

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

/**
 * Integration tests for server-side assisted cache invalidation.
 *
 * @author Mark Paluch
 */
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

}
