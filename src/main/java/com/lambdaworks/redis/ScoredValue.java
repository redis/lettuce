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

/**
 * A value and its associated score from a ZSET.
 * 
 * @param <V> Value type.
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
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ScoredValue<?> that = (ScoredValue<?>) o;
        return Double.compare(that.score, score) == 0 && value.equals(that.value);
    }

    @Override
    public int hashCode() {

        long temp = Double.doubleToLongBits(score);
        int result = (int) (temp ^ (temp >>> 32));
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return String.format("(%f, %s)", score, value);
    }
}
