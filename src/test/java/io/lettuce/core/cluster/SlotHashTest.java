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
package io.lettuce.core.cluster;

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;

/**
 * @author Mark Paluch
 * @since 3.0
 */
public class SlotHashTest {

    @Test
    public void testHash() throws Exception {
        int result = SlotHash.getSlot("123456789".getBytes());
        assertThat(result).isEqualTo(0x31C3);

    }

    @Test
    public void testHashWithHash() throws Exception {
        int result = SlotHash.getSlot("key{123456789}a".getBytes());
        assertThat(result).isEqualTo(0x31C3);

    }
}
