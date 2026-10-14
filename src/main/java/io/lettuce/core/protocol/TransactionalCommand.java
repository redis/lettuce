package io.lettuce.core.protocol;

/**
 * A wrapper for commands within a {@literal MULTI} transaction. Commands triggered within a transaction will be completed
 * twice. Once on the submission and once during {@literal EXEC}. Only the second completion will complete the underlying
 * command.
 *
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @param <T> Command output type.
 *
 * @author Mark Paluch
 */
public class TransactionalCommand<K, V, T> extends AsyncCommand<K, V, T> implements RedisCommand<K, V, T> {

    public TransactionalCommand(RedisCommand<K, V, T> command) {
        super(command, 2);
    }

}
