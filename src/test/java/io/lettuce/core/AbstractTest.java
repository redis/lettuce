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
package io.lettuce.core;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Rule;

import io.lettuce.LoggingTestRule;
import io.lettuce.core.internal.LettuceSets;

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
