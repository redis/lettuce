package com.lambdaworks.redis;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.Rule;

import com.lambdaworks.CapturingLogRule;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class AbstractTest {

    public static final String host = TestSettings.host();
    public static final int port = TestSettings.port();
    public static final String passwd = TestSettings.password();

    @Rule
    public CapturingLogRule capturingLogRule = new CapturingLogRule();

    protected Logger log = Logger.getLogger(getClass());
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
        return new KeyValue<String, String>(key, value);
    }

    public static ScoredValue<String> sv(double score, String value) {
        return new ScoredValue<String>(score, value);
    }

    public static Set<String> set(String... args) {
        return new HashSet<String>(Arrays.asList(args));
    }
}
