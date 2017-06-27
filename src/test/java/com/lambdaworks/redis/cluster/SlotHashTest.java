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
package com.lambdaworks.redis.cluster;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;

import org.junit.Test;

/**
 * @author Mark Paluch
 * @since 3.0
 */
public class SlotHashTest {

    static final byte[] BYTES = "123456789".getBytes();
    static final byte[] TAGGED = "key{123456789}a".getBytes();

    @Test
    public void shouldGetSlotHeap() {

        int result = SlotHash.getSlot(BYTES);
        assertThat(result).isEqualTo(0x31C3);
    }

    @Test
    public void shouldGetTaggedSlotHeap() {

        int result = SlotHash.getSlot(TAGGED);
        assertThat(result).isEqualTo(0x31C3);
    }

    @Test
    public void shouldGetSlotDirect() {

        int result = SlotHash.getSlot((ByteBuffer) ByteBuffer.allocateDirect(BYTES.length).put(BYTES).flip());
        assertThat(result).isEqualTo(0x31C3);
    }

    @Test
    public void testHashWithHash() {

        int result = SlotHash.getSlot((ByteBuffer) ByteBuffer.allocateDirect(TAGGED.length).put(TAGGED).flip());
        assertThat(result).isEqualTo(0x31C3);
    }
}
