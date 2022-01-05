/*
 * Copyright 2011-2022 the original author or authors.
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
package io.lettuce.core.sentinel;

import static org.assertj.core.api.Assertions.*;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.TestSupport;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.sentinel.api.StatefulRedisSentinelConnection;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.WithPassword;
import io.lettuce.test.condition.EnabledOnCommand;
import io.lettuce.test.settings.TestSettings;

/**
 * Integration tests for Redis Sentinel using ACL authentication.
 *
 * @author Mark Paluch
 */
@ExtendWith(LettuceExtension.class)
@EnabledOnCommand("ACL")
public class SentinelAclIntegrationTests extends TestSupport {

    private final RedisClient redisClient;

    private final RedisURI redisURI = RedisURI.Builder.sentinel(TestSettings.host(), 26381, SentinelTestSettings.MASTER_ID)
            // data node auth
            .withAuthentication("default", TestSettings.password()).build();

    private final RedisURI sentinelWithAcl = RedisURI.Builder
            .sentinel(TestSettings.host(), 26381, SentinelTestSettings.MASTER_ID)
            // data node auth
            .withAuthentication("default", TestSettings.password()).build();

    @Inject
    public SentinelAclIntegrationTests(RedisClient redisClient) {
        this.redisClient = redisClient;

        // sentinel node auth
        for (RedisURI sentinel : redisURI.getSentinels()) {
            sentinel.setPassword(TestSettings.password());
        }

        // sentinel node auth
        for (RedisURI sentinel : sentinelWithAcl.getSentinels()) {
            sentinel.setUsername(TestSettings.aclUsername());
            sentinel.setPassword(TestSettings.aclPassword());
        }
    }

    @BeforeEach
    void setUp() {
        StatefulRedisConnection<String, String> connection = redisClient.connect(redisURI.getSentinels().get(0));
        WithPassword.enableAuthentication(connection.sync());
        connection.close();
    }

    @Test
    void sentinelWithAuthentication() {

        StatefulRedisSentinelConnection<String, String> connection = redisClient.connectSentinel(sentinelWithAcl);

        assertThat(connection.sync().ping()).isEqualTo("PONG");

        connection.close();
    }

    @Test
    void connectThroughSentinel() {

        StatefulRedisConnection<String, String> connection = redisClient.connect(sentinelWithAcl);

        assertThat(connection.sync().ping()).isEqualTo("PONG");

        connection.close();
    }

}
