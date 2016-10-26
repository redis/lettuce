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
package com.lambdaworks.redis.issue42;

import java.util.concurrent.TimeUnit;

import com.lambdaworks.category.SlowTests;
import com.lambdaworks.redis.DefaultRedisClient;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.api.sync.RedisCommands;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@SlowTests
@Ignore("Run me manually")
public class BreakClientTest extends BreakClientBase {

    protected static RedisClient client = DefaultRedisClient.get();

    protected RedisCommands<String, String> redis;

    @Before
    public void setUp() throws Exception {
        client.setDefaultTimeout(TIMEOUT, TimeUnit.SECONDS);
        redis = client.connect(this.slowCodec).sync();
        redis.flushall();
        redis.flushdb();
    }

    @After
    public void tearDown() throws Exception {
        redis.close();
    }

    @Test
    public void testStandAlone() throws Exception {
        testSingle(redis);
    }

    @Test
    public void testLooping() throws Exception {
        testLoop(redis);
    }

}
