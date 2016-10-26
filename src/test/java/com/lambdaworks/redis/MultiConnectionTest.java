/*
 * Copyright 2011-2016 the original author or authors.
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
package com.lambdaworks.redis;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.concurrent.Future;

import org.junit.Test;

public class MultiConnectionTest extends AbstractRedisClientTest {

    @Test
    public void twoConnections() throws Exception {

        RedisAsyncConnection<String, String> connection1 = client.connectAsync();

        RedisAsyncConnection<String, String> connection2 = client.connectAsync();

        connection1.sadd("key", "member1", "member2").get();

        Future<Set<String>> members = connection2.smembers("key");

        assertThat(members.get()).hasSize(2);

        connection1.close();
        connection2.close();
    }
}
