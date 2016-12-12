/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lambdaworks.redis.dynamic;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import com.lambdaworks.redis.AbstractRedisClientTest;
import com.lambdaworks.redis.dynamic.annotation.Command;

import reactor.core.publisher.Mono;

/**
 * @author Mark Paluch
 */
public class RedisCommandsReactiveTest extends AbstractRedisClientTest {

    @Test
    public void reactive() throws Exception {

        RedisCommandFactory factory = new RedisCommandFactory(redis.getStatefulConnection());

        MultipleExecutionModels api = factory.getCommands(MultipleExecutionModels.class);

        Mono<String> set = api.setReactive(key, value);
        assertThat(set.block()).isEqualTo("OK");
    }

    static interface MultipleExecutionModels extends Commands {
        @Command("SET")
        Mono<String> setReactive(String key, String value);
    }
}
