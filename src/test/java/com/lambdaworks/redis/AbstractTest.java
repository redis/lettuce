package com.lambdaworks.redis;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Rule;

import com.lambdaworks.LoggingTestRule;
import com.lambdaworks.redis.internal.LettuceSets;

/**
 * @author Mark Paluch
 */
public class AbstractTest {

    public static final String host = TestSettings.host();
    public static final int port = TestSettings.port();
    public static final String passwd = TestSettings.password();

    @Rule
    public LoggingTestRule loggingTestRule = new LoggingTestRule(false);

    protected Logger log = LogManager.getLogger(getClass());
    protected String key = "key";
    protected String value = "value";

    public static List<String> list(String... args) {
        return Arrays.asList(args);
    }

    public static List<Object> list(Object... args) {
        return Arrays.asList(args);
    }

    public static List<ScoredValue<String>> svlist(ScoredValue<String>... args) {
        return Arrays.asList(args);
    }

    public static KeyValue<String, String> kv(String key, String value) {
        return KeyValue.fromNullable(key, value);
    }

    public static ScoredValue<String> sv(double score, String value) {
        return ScoredValue.fromNullable(score, value);
    }

    public static Set<String> set(String... args) {
        return LettuceSets.newHashSet(args);
    }
}
