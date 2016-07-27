package com.lambdaworks.redis.output;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import com.lambdaworks.redis.TransactionResult;
import com.lambdaworks.redis.internal.LettuceAssert;

/**
 * Result of a {@code MULTI} transaction.
 * 
 * @author Mark Paluch
 * @since 5.0
 */
class DefaultTransactionResult implements Iterable<Object>, TransactionResult {

    private final boolean wasRolledBack;
    private final List<Object> result;

    /**
     * Creates a new {@link DefaultTransactionResult}.
     * 
     * @param wasRolledBack {@literal true} if the transaction was rolled back.
     * @param result the transaction result, must not be {@literal null}.
     */
    public DefaultTransactionResult(boolean wasRolledBack, List<Object> result) {

        LettuceAssert.notNull(result, "Result must not be null");

        this.wasRolledBack = wasRolledBack;
        this.result = result;
    }

    /**
     *
     * @return {@literal true} if the transaction was rolled back
     */
    @Override
    public boolean wasRolledBack() {
        return wasRolledBack;
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
}
