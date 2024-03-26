package io.lettuce.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.Test;

import io.lettuce.core.internal.Futures;

/**
 * @author Mark Paluch
 */
class ConnectionFutureUnitTests {

    @Test
    void shouldComposeTransformToError() {

        CompletableFuture<String> foo = new CompletableFuture<>();

        ConnectionFuture<Object> transformed = ConnectionFuture.from(null, foo).thenCompose((s, t) -> {

            if (t != null) {
                return Futures.failed(new IllegalStateException(t));
            }
            return Futures.failed(new IllegalStateException());
        });

        foo.complete("foo");

        assertThat(transformed.toCompletableFuture()).isDone();
        assertThat(transformed.toCompletableFuture()).isCompletedExceptionally();
        assertThatThrownBy(transformed::join).hasRootCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    void composeTransformShouldFailWhileTransformation() {

        CompletableFuture<String> foo = new CompletableFuture<>();

        ConnectionFuture<Object> transformed = ConnectionFuture.from(null, foo).thenCompose((s, t) -> {
            throw new IllegalStateException();
        });

        foo.complete("foo");

        assertThat(transformed.toCompletableFuture()).isDone();
        assertThat(transformed.toCompletableFuture()).isCompletedExceptionally();
        assertThatThrownBy(transformed::join).hasRootCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    void composeTransformShouldFailWhileTransformationRetainOriginalException() {

        CompletableFuture<String> foo = new CompletableFuture<>();

        ConnectionFuture<Object> transformed = ConnectionFuture.from(null, foo).thenCompose((s, t) -> {
            throw new IllegalStateException();
        });

        Throwable t = new Throwable();
        foo.completeExceptionally(t);


        assertThat(transformed.toCompletableFuture()).isDone();
        assertThat(transformed.toCompletableFuture()).isCompletedExceptionally();

        try {
            transformed.join();
        } catch (CompletionException e) {

            assertThat(e).hasRootCauseInstanceOf(IllegalStateException.class);
            assertThat(e.getCause()).hasSuppressedException(t);
        }
    }

    @Test
    void shouldComposeWithErrorFlow() {

        CompletableFuture<String> foo = new CompletableFuture<>();
        CompletableFuture<String> exceptional = new CompletableFuture<>();

        ConnectionFuture<Object> transformed1 = ConnectionFuture.from(null, foo).thenCompose((s, t) -> {

            if (t != null) {
                return Futures.failed(new IllegalStateException(t));
            }
            return CompletableFuture.completedFuture(s);
        });

        ConnectionFuture<Object> transformed2 = ConnectionFuture.from(null, exceptional).thenCompose((s, t) -> {

            if (t != null) {
                return Futures.failed(new IllegalStateException(t));
            }
            return CompletableFuture.completedFuture(s);
        });

        foo.complete("foo");
        exceptional.completeExceptionally(new IllegalArgumentException("foo"));

        assertThat(transformed1.toCompletableFuture()).isDone();
        assertThat(transformed1.toCompletableFuture()).isCompletedWithValue("foo");

        assertThat(transformed2.toCompletableFuture()).isDone();
        assertThat(transformed2.toCompletableFuture()).isCompletedExceptionally();
        assertThatThrownBy(transformed2::join).hasCauseInstanceOf(IllegalStateException.class).hasRootCauseInstanceOf(
                IllegalArgumentException.class);
    }
}
