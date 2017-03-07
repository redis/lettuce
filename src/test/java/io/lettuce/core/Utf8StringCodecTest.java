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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.Test;

/**
 * @author Will Glozer
 * @author Mark Paluch
 */
public class Utf8StringCodecTest extends AbstractRedisClientTest {
    @Test
    public void decodeHugeBuffer() throws Exception {
        char[] huge = new char[8192];
        Arrays.fill(huge, 'A');
        String value = new String(huge);
        redis.set(key, value);
        assertThat(redis.get(key)).isEqualTo(value);
    }
}
