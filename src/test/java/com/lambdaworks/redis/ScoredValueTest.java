// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class ScoredValueTest {
    @Test
    public void equals() throws Exception {
        ScoredValue<String> sv1 = new ScoredValue<String>(1.0, "a");
        assertThat(sv1.equals(new ScoredValue<String>(1.0, "a"))).isTrue();
        assertThat(sv1.equals(null)).isFalse();
        assertThat(sv1.equals(new ScoredValue<String>(1.1, "a"))).isFalse();
        assertThat(sv1.equals(new ScoredValue<String>(1.0, "b"))).isFalse();
    }

    @Test
    public void testToString() throws Exception {
        ScoredValue<String> sv1 = new ScoredValue<String>(1.0, "a");
        assertThat(sv1.toString()).isEqualTo(String.format("(%f, %s)", sv1.score, sv1.value));
    }

    @Test
    public void testHashCode() throws Exception {
        assertThat(new ScoredValue<String>(1.0, "a").hashCode() != 0).isTrue();
        assertThat(new ScoredValue<String>(0.0, "a").hashCode() != 0).isTrue();
        assertThat(new ScoredValue<String>(0.0, null).hashCode() == 0).isTrue();
    }
}
