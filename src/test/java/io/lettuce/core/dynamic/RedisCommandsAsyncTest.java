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
package io.lettuce.core.dynamic;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.junit.Test;

import io.lettuce.core.AbstractRedisClientTest;

/**
 * @author Mark Paluch
 */
public class RedisCommandsAsyncTest extends AbstractRedisClientTest {

    @Test
    public void async() throws Exception {

        RedisCommandFactory factory = new RedisCommandFactory(redis.getStatefulConnection());

        MultipleExecutionModels api = factory.getCommands(MultipleExecutionModels.class);

        Future<String> set = api.set(key, value);
        assertThat(set).isInstanceOf(CompletableFuture.class);
        set.get();
    }

    static interface MultipleExecutionModels extends Commands {
        Future<String> set(String key, String value);
    }
}
