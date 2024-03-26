package io.lettuce.core.dynamic;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import io.lettuce.core.protocol.RedisCommand;

/**
 * Result of a batching request. Contains references to the batched {@link RedisCommand}s.
 *
 * @author Mark Paluch
 * @since 5.0
 */
class BatchTasks implements Iterable<RedisCommand<?, ?, ?>> {

    public static final BatchTasks EMPTY = new BatchTasks(Collections.emptyList());

    private final List<RedisCommand<?, ?, ?>> futures;

    BatchTasks(List<RedisCommand<?, ?, ?>> futures) {
        this.futures = futures;
    }

    @Override
    public Iterator<RedisCommand<?, ?, ?>> iterator() {
        return futures.iterator();
    }

    @SuppressWarnings("rawtypes")
    public RedisCommand<?, ?, ?>[] toArray() {
        return futures.toArray(new RedisCommand[0]);
    }

}
