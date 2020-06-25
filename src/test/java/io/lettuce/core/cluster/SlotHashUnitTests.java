/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.cluster;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

/**
 * @author Mark Paluch
 * @since 3.0
 */
class SlotHashUnitTests {

    private static final byte[] BYTES = "123456789".getBytes();

    private static final byte[] TAGGED = "key{123456789}a".getBytes();

    @Test
    void shouldGetSlotHeap() {

        int result = SlotHash.getSlot(BYTES);
        assertThat(result).isEqualTo(0x31C3);
    }

    @Test
    void shouldGetTaggedSlotHeap() {

        int result = SlotHash.getSlot(TAGGED);
        assertThat(result).isEqualTo(0x31C3);
    }

    @Test
    void shouldGetSlotDirect() {

        int result = SlotHash.getSlot((ByteBuffer) ByteBuffer.allocateDirect(BYTES.length).put(BYTES).flip());
        assertThat(result).isEqualTo(0x31C3);
    }

    @Test
    void testHashWithHash() {

        int result = SlotHash.getSlot((ByteBuffer) ByteBuffer.allocateDirect(TAGGED.length).put(TAGGED).flip());
        assertThat(result).isEqualTo(0x31C3);
    }

}
