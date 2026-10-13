package io.lettuce.core.protocol;

import java.util.function.LongConsumer;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

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

    @SuppressWarnings("rawtypes")
    private static final AtomicReferenceFieldUpdater<TransactionalCommand, LongConsumer> QUEUE_LATENCY_COMPLETION = AtomicReferenceFieldUpdater
            .newUpdater(TransactionalCommand.class, LongConsumer.class, "queueLatencyCompletion");

    private volatile LongConsumer queueLatencyCompletion;

    public TransactionalCommand(RedisCommand<K, V, T> command) {
        super(command, 2);
    }

    void onQueueLatencyCompletion(LongConsumer completion) {
        this.queueLatencyCompletion = completion;
    }

    @Override
    public void complete() {
        super.complete();
        if (queueLatencyCompletion != null && (isDone() || isCancelled())) {
            queueLatencyCompletion = null;
        }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        queueLatencyCompletion = null;
        return super.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean completeExceptionally(Throwable ex) {
        queueLatencyCompletion = null;
        return super.completeExceptionally(ex);
    }

    /**
     * Complete a command whose result has been decoded from an {@literal EXEC} response.
     *
     * @since 7.9
     */
    public void completeResponse() {
        if (queueLatencyCompletion == null) {
            complete();
            return;
        }
        LongConsumer completion = QUEUE_LATENCY_COMPLETION.getAndSet(this, null);
        long completedAt = completion == null ? 0 : System.nanoTime();
        boolean cancelled = isCancelled() || getDelegate().isCancelled();
        complete();
        if (completion != null && isDone() && !cancelled && !AsyncCommand.hasCancelledFuture(this)) {
            completion.accept(completedAt);
        }
    }

}
