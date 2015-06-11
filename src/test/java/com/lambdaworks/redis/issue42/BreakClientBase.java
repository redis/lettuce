package com.lambdaworks.redis.issue42;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import com.lambdaworks.redis.api.sync.RedisHashCommands;
import org.apache.log4j.Logger;
import org.junit.Assert;

import com.lambdaworks.redis.RedisCommandTimeoutException;
import com.lambdaworks.redis.codec.Utf8StringCodec;

/**
 * Base for simulating slow connections/commands running into timeouts.
 */
public abstract class BreakClientBase {

    public static int TIMEOUT = 5;

    public static final String TEST_KEY = "taco";
    public volatile boolean sleep = false;

    protected Logger log = Logger.getLogger(getClass());

    public void testSingle(RedisHashCommands<String, String> client) throws InterruptedException {
        populateTest(0, client);

        assertEquals(16385, client.hvals(TEST_KEY).size());

        breakClient(client);

        assertEquals(16385, client.hvals(TEST_KEY).size());
    }

    public void testLoop(RedisHashCommands<String, String> client) throws InterruptedException {
        populateTest(100, client);
        assertEquals(16385 + 100, client.hvals(TEST_KEY).size());

        breakClient(client);

        assertExtraKeys(100, client);
    }

    public void assertExtraKeys(int howmany, RedisHashCommands<String, String> target) {
        for (int x = 0; x < howmany; x++) {
            int i = Integer.parseInt(target.hget(TEST_KEY, "GET-" + x));
            Assert.assertEquals(x, i);
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

        TimeUnit.SECONDS.sleep(5);
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

        assertEquals(16385 + loopFor, target.hvals(TEST_KEY).size());
        log.info("done");

    }

    public Utf8StringCodec slowCodec = new Utf8StringCodec() {
        public String decodeValue(ByteBuffer bytes) {

            if (sleep) {
                log.info("Sleeping for " + (TIMEOUT + 3) + " seconds in slowCodec");
                sleep = false;
                try {
                    TimeUnit.SECONDS.sleep(TIMEOUT + 3);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                log.info("Done sleeping in slowCodec");
            }

            return super.decodeValue(bytes);
        }
    };
}
