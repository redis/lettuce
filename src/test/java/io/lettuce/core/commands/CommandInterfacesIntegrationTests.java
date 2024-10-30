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
package io.lettuce.core.commands;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisContainerIntegrationTests;
import io.lettuce.core.RedisURI;
import io.lettuce.core.TestSupport;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.dynamic.Commands;
import io.lettuce.core.dynamic.RedisCommandFactory;
import io.lettuce.core.dynamic.annotation.Command;
import io.lettuce.core.dynamic.annotation.Param;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.condition.EnabledOnCommand;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import reactor.core.publisher.Flux;

import javax.inject.Inject;
import java.lang.reflect.Proxy;
import java.util.List;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static io.lettuce.core.SetArgs.Builder.ex;
import static io.lettuce.core.SetArgs.Builder.exAt;
import static io.lettuce.core.SetArgs.Builder.px;
import static io.lettuce.core.SetArgs.Builder.pxAt;
import static io.lettuce.core.StringMatchResult.Position;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for {@link io.lettuce.core.dynamic.annotation.Command}.
 *
 * @author Tihomir Mateev
 */
@Tag(INTEGRATION_TEST)
public class CommandInterfacesIntegrationTests extends RedisContainerIntegrationTests {

    protected static RedisClient client;

    protected static RedisCommands<String, String> redis;

    protected CommandInterfacesIntegrationTests() {
        RedisURI redisURI = RedisURI.Builder.redis("127.0.0.1").withPort(16379).build();

        client = RedisClient.create(redisURI);
        redis = client.connect().sync();
    }

    @BeforeEach
    void setUp() {
        this.redis.flushall();
    }

    @Test
    void issue2612() {
        CustomCommands commands = CustomCommands.instance(getConnection());

        Flux<Boolean> flux = commands.insert("myBloomFilter", 10000, 0.01, new String[] { "1", "2", "3", });
        List<Boolean> res = flux.collectList().block();

        assertThat(res).hasSize(3);
        assertThat(res).contains(true, true, true);
    }

    protected StatefulConnection<String, String> getConnection() {
        StatefulRedisConnection<String, String> src = redis.getStatefulConnection();
        Assumptions.assumeFalse(Proxy.isProxyClass(src.getClass()), "Redis connection is proxy, skipping.");
        return src;
    }

    private interface CustomCommands extends Commands {

        @Command("BF.INSERT :filter CAPACITY :capacity ERROR :error ITEMS :items ")
        Flux<Boolean> insert(@Param("filter") String filter, @Param("capacity") long capacity, @Param("error") double error,
                @Param("items") String[] items);

        static CustomCommands instance(StatefulConnection<String, String> conn) {
            RedisCommandFactory factory = new RedisCommandFactory(conn);
            return factory.getCommands(CustomCommands.class);
        }

    }

}
