/*
 * Copyright 2017-2020 the original author or authors.
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
package io.lettuce.core.internal;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import io.netty.channel.ChannelFuture;

/**
 * Utility methods for {@link java.util.concurrent.Future} handling. This class is part of the internal API and may change
 * without further notice.
 *
 * @author Mark Paluch
 * @since 5.1
 */
public abstract class Futures {

    private Futures() {
        // no instances allowed
    }

    /**
     * Create a composite {@link CompletableFuture} is composed from the given {@code futures}.
     *
     * @param futures must not be {@code null}.
     * @return the composed {@link CompletableFuture}.
     * @since 5.1.1
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static CompletableFuture<Void> allOf(Collection<? extends CompletableFuture<?>> futures) {

        LettuceAssert.notNull(futures, "Futures must not be null");

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    /**
     * Create a {@link CompletableFuture} that is completed exceptionally with {@code throwable}.
     *
     * @param throwable must not be {@code null}.
     * @return the exceptionally completed {@link CompletableFuture}.
     */
    public static <T> CompletableFuture<T> failed(Throwable throwable) {

        LettuceAssert.notNull(throwable, "Throwable must not be null");

        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(throwable);

        return future;
    }

    /**
     * Adapt Netty's {@link ChannelFuture} emitting a {@link Void} result.
     *
     * @param future the {@link ChannelFuture} to adapt.
     * @return the {@link CompletableFuture}.
     */
    public static CompletableFuture<Void> from(ChannelFuture future) {

        LettuceAssert.notNull(future, "ChannelFuture must not be null");

        CompletableFuture<Void> result = new CompletableFuture<>();
        adapt(future, result);

        return result;
    }

    /**
     * Adapt Netty's {@link ChannelFuture} emitting a {@link Void} result into a {@link CompletableFuture}.
     *
     * @param future
     */
    public static void adapt(ChannelFuture future, CompletableFuture<Void> target) {

        future.addListener(f -> {
            if (f.isSuccess()) {
                target.complete(null);
            } else {
                target.completeExceptionally(f.cause());
            }
        });

        if (future.isSuccess()) {
            target.complete(null);
        } else if (future.isCancelled()) {
            target.cancel(false);
        } else if (future.isDone() && !future.isSuccess()) {
            target.completeExceptionally(future.cause());
        }
    }

}
