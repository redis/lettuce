/*
 * Copyright 2020 the original author or authors.
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
package io.lettuce.core.masterreplica;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import reactor.core.publisher.Mono;
import io.lettuce.core.internal.AsyncCloseable;

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
