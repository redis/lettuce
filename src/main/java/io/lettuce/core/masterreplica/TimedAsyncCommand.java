package io.lettuce.core.masterreplica;

import io.lettuce.core.protocol.AsyncCommand;
import io.lettuce.core.protocol.RedisCommand;
import io.netty.buffer.ByteBuf;

/**
 * Timed command that records the time at which the command was encoded and completed.
 *
 * @param <K> Key type
 * @param <V> Value type
 * @param <T> Result type
 * @author Mark Paluch
 */
class TimedAsyncCommand<K, V, T> extends AsyncCommand<K, V, T> {

    long encodedAtNs = -1;

    long completedAtNs = -1;

    public TimedAsyncCommand(RedisCommand<K, V, T> command) {
        super(command);
    }

    @Override
    public void encode(ByteBuf buf) {
        completedAtNs = -1;
        encodedAtNs = -1;

        super.encode(buf);
        encodedAtNs = System.nanoTime();
    }

    @Override
    public void complete() {
        completedAtNs = System.nanoTime();
        super.complete();
    }

    public long duration() {
        if (completedAtNs == -1 || encodedAtNs == -1) {
            return -1;
        }
        return completedAtNs - encodedAtNs;
    }

}
