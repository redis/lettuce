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

import org.junit.Test;

public class KeyValueTest {
    protected String key = "key";
    protected String value = "value";

    @Test
    public void equals() throws Exception {
        KeyValue<String, String> kv = kv(key, value);
        assertThat(kv.equals(kv(key, value))).isTrue();
        assertThat(kv.equals(null)).isFalse();
        assertThat(kv.equals(kv("a", value))).isFalse();
        assertThat(kv.equals(kv(key, "b"))).isFalse();
    }

    @Test
    public void testToString() throws Exception {
        KeyValue<String, String> kv = kv(key, value);
        assertThat(kv.toString()).isEqualTo(String.format("(%s, %s)", kv.key, kv.value));
    }

    @Test
    public void testHashCode() throws Exception {
        assertThat(kv(key, value).hashCode() != 0).isTrue();
    }

    protected KeyValue<String, String> kv(String key, String value) {
        return new KeyValue<>(key, value);
    }
}
