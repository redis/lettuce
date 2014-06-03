// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

import org.junit.Test;

import static org.junit.Assert.*;

public class ScoredValueTest {
    @Test
    public void equals() throws Exception {
        ScoredValue<String> sv1 = new ScoredValue<String>(1.0, "a");
        assertTrue(sv1.equals(new ScoredValue<String>(1.0, "a")));
        assertFalse(sv1.equals(null));
        assertFalse(sv1.equals(new ScoredValue<String>(1.1, "a")));
        assertFalse(sv1.equals(new ScoredValue<String>(1.0, "b")));
    }

    @Test
    public void testToString() throws Exception {
        ScoredValue<String> sv1 = new ScoredValue<String>(1.0, "a");
        assertEquals(String.format("(%f, %s)", sv1.score, sv1.value), sv1.toString());
    }

    @Test
    public void testHashCode() throws Exception {
        assertTrue(new ScoredValue<String>(1.0, "a").hashCode() != 0);
        assertTrue(new ScoredValue<String>(0.0, "a").hashCode() != 0);
        assertTrue(new ScoredValue<String>(0.0, null).hashCode() == 0);
    }
}
