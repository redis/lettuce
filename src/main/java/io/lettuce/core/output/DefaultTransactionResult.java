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
package io.lettuce.core.output;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import io.lettuce.core.TransactionResult;
import io.lettuce.core.internal.LettuceAssert;

/**
 * Result of a {@code MULTI} transaction.
 *
 * @author Mark Paluch
 * @since 5.0
 */
class DefaultTransactionResult implements Iterable<Object>, TransactionResult {

    private final boolean discarded;

    private final List<Object> result;

    /**
     * Creates a new {@link DefaultTransactionResult}.
     *
     * @param discarded {@code true} if the transaction is discarded.
     * @param result the transaction result, must not be {@code null}.
     */
    public DefaultTransactionResult(boolean discarded, List<Object> result) {

        LettuceAssert.notNull(result, "Result must not be null");

        this.discarded = discarded;
        this.result = result;
    }

    @Override
    public boolean wasDiscarded() {
        return discarded;
    }

    @Override
    public Iterator<Object> iterator() {
        return result.iterator();
    }

    @Override
    public int size() {
        return result.size();
    }

    @Override
    public boolean isEmpty() {
        return result.isEmpty();
    }

    @Override
    public <T> T get(int index) {
        return (T) result.get(index);
    }

    @Override
    public Stream<Object> stream() {
        return result.stream();
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [wasRolledBack=").append(discarded);
        sb.append(", responses=").append(size());
        sb.append(']');
        return sb.toString();
    }

}
