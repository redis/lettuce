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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lambdaworks.redis.RedisCommandTimeoutException;
import com.lambdaworks.redis.api.sync.RedisHashCommands;
import com.lambdaworks.redis.codec.Utf8StringCodec;

/**
 * Base for simulating slow connections/commands running into timeouts.
 */
public abstract class BreakClientBase {

    public static int TIMEOUT = 1;

    public static final String TEST_KEY = "taco";
    public volatile boolean sleep = false;

    protected Logger log = LogManager.getLogger(getClass());

    public void testSingle(RedisHashCommands<String, String> client) throws InterruptedException {
        populateTest(0, client);

        assertThat(client.hvals(TEST_KEY)).hasSize(16385);

        breakClient(client);

        assertThat(client.hvals(TEST_KEY)).hasSize(16385);
    }

    public void testLoop(RedisHashCommands<String, String> client) throws InterruptedException {
        populateTest(100, client);

        assertThat(client.hvals(TEST_KEY)).hasSize(16485);

        breakClient(client);

        assertExtraKeys(100, client);
    }

    public void assertExtraKeys(int howmany, RedisHashCommands<String, String> target) {
        for (int x = 0; x < howmany; x++) {
            int i = Integer.parseInt(target.hget(TEST_KEY, "GET-" + x));
            assertThat(x).isEqualTo(i);
        }
    }

    protected void breakClient(RedisHashCommands<String, String> target) throws InterruptedException {
        try {
            this.sleep = true;
            log.info("This should timeout");
            target.hgetall(TEST_KEY);
            fail();
        } catch (RedisCommandTimeoutException expected) {
            log.info("got expected timeout");
        }
    }

    protected void populateTest(int loopFor, RedisHashCommands<String, String> target) {
        log.info("populating hash");
        target.hset(TEST_KEY, TEST_KEY, TEST_KEY);

        for (int x = 0; x < loopFor; x++) {
            target.hset(TEST_KEY, "GET-" + x, Integer.toString(x));
        }

        for (int i = 0; i < 16384; i++) {
            target.hset(TEST_KEY, Integer.toString(i), TEST_KEY);
        }
        assertThat(target.hvals(TEST_KEY)).hasSize(16385 + loopFor);
        log.info("done");

    }

    public Utf8StringCodec slowCodec = new Utf8StringCodec() {
        public String decodeValue(ByteBuffer bytes) {

            if (sleep) {
                log.info("Sleeping for " + (TIMEOUT + 2) + " seconds in slowCodec");
                sleep = false;
                try {
                    TimeUnit.SECONDS.sleep(TIMEOUT + 2);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                log.info("Done sleeping in slowCodec");
            }

            return super.decodeValue(bytes);
        }
    };
}
