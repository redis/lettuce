package com.lambdaworks.redis.issue42;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.lambdaworks.redis.RedisCommandTimeoutException;
import com.lambdaworks.redis.RedisHashesConnection;
import com.lambdaworks.redis.codec.Utf8StringCodec;

/**
 * Base for simulating slow connections/commands running into timeouts.
 */
public abstract class BreakClientBase {

    public static int TIMEOUT = 5;

    public static final String TEST_KEY = "taco";
    public volatile boolean sleep = false;

    protected Logger log = Logger.getLogger(getClass());

    public void testSingle(RedisHashesConnection<String, String> client) throws InterruptedException {
        populateTest(0, client);

        assertThat(client.hvals(TEST_KEY)).hasSize(16385);

        breakClient(client);

        assertThat(client.hvals(TEST_KEY)).hasSize(16385);
    }

    public void testLoop(RedisHashesConnection<String, String> client) throws InterruptedException {
        populateTest(100, client);

        assertThat(client.hvals(TEST_KEY)).hasSize(16485);

        breakClient(client);

        assertExtraKeys(100, client);
    }

    public void assertExtraKeys(int howmany, RedisHashesConnection<String, String> target) {
        for (int x = 0; x < howmany; x++) {
            int i = Integer.parseInt(target.hget(TEST_KEY, "GET-" + x));
            assertThat(x).isEqualTo(i);
        }
    }

    protected void breakClient(RedisHashesConnection<String, String> target) throws InterruptedException {
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

    protected void populateTest(int loopFor, RedisHashesConnection<String, String> target) {
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
