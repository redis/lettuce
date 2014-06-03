package com.lambdaworks.redis.cluster;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 27.05.14 08:43
 */
public class SlotHashTest {
    @Test
    public void testHash() throws Exception {
        int result = SlotHash.getSlot("123456789".getBytes());
        assertEquals(0x31C3, result);

    }

    @Test
    public void testHashWithHash() throws Exception {
        int result = SlotHash.getSlot("key{123456789}a".getBytes());
        assertEquals(0x31C3, result);

    }
}
