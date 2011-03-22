// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

/**
 * A value and its associated score from a ZSET.
 *
 * @author Will Glozer
 */
public class ScoredValue<V> {
    public final double score;
    public final V value;

    public ScoredValue(double score, V value) {
        this.score = score;
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ScoredValue that = (ScoredValue) o;
        return Double.compare(that.score, score) == 0 && value.equals(that.value);
    }

    @Override
    public String toString() {
        return String.format("(%f, %s)", score, value.toString());
    }
}
