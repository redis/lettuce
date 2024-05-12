package io.lettuce.core.masterreplica;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import reactor.core.publisher.Mono;
import io.lettuce.core.api.AsyncCloseable;

/**
 * Utility to resume a {@link org.reactivestreams.Publisher} after termination.
 *
 * @author Mark Paluch
 */
class ResumeAfter {

    private static final AtomicIntegerFieldUpdater<ResumeAfter> UPDATER = AtomicIntegerFieldUpdater
            .newUpdater(ResumeAfter.class, "closed");

    private final AsyncCloseable closeable;

    private static final int ST_OPEN = 0;

    private static final int ST_CLOSED = 1;

    @SuppressWarnings("unused")
    private volatile int closed = ST_OPEN;

    private ResumeAfter(AsyncCloseable closeable) {
        this.closeable = closeable;
    }

    public static ResumeAfter close(AsyncCloseable closeable) {
        return new ResumeAfter(closeable);
    }

    public <T> Mono<T> thenEmit(T value) {

        return Mono.defer(() -> {

            if (firstCloseLatch()) {
                return Mono.fromCompletionStage(closeable.closeAsync());
            }

            return Mono.empty();

        }).then(Mono.just(value)).doFinally(s -> {

            if (firstCloseLatch()) {
                closeable.closeAsync();
            }
        });
    }

    public <T> Mono<T> thenError(Throwable t) {

        return Mono.defer(() -> {

            if (firstCloseLatch()) {
                return Mono.fromCompletionStage(closeable.closeAsync());
            }

            return Mono.empty();

        }).then(Mono.<T> error(t)).doFinally(s -> {

            if (firstCloseLatch()) {
                closeable.closeAsync();
            }
        });
    }

    private boolean firstCloseLatch() {
        return UPDATER.compareAndSet(ResumeAfter.this, ST_OPEN, ST_CLOSED);
    }

}
