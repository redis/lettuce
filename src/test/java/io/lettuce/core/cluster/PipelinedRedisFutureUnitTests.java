package io.lettuce.core.cluster;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;

import org.junit.jupiter.api.Test;

import io.lettuce.test.TestFutures;

/**
 * @author Mark Paluch
 */
class PipelinedRedisFutureUnitTests {

    private PipelinedRedisFuture<String> sut;

    @Test
    void testComplete() {

        String other = "other";

        sut = new PipelinedRedisFuture<>(new HashMap<>(), o -> other);

        sut.complete("");
        assertThat(TestFutures.getOrTimeout(sut.toCompletableFuture())).isEqualTo(other);
        assertThat(sut.getError()).isNull();
    }

    @Test
    void testCompleteExceptionally() {

        String other = "other";

        sut = new PipelinedRedisFuture<>(new HashMap<>(), o -> other);

        sut.completeExceptionally(new Exception());
        assertThat(TestFutures.getOrTimeout(sut.toCompletableFuture())).isEqualTo(other);
        assertThat(sut.getError()).isNull();
    }

}
