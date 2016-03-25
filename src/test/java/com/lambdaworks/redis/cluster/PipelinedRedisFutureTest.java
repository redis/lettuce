package com.lambdaworks.redis.cluster;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import com.google.common.collect.Maps;

/**
 * @author Mark Paluch
 */
public class PipelinedRedisFutureTest {

    private PipelinedRedisFuture<String> sut;

    @Test
    public void testComplete() throws Exception {

        String other = "other";

        sut = new PipelinedRedisFuture<>(Maps.newHashMap(), o -> other);

        sut.complete("");
        assertThat(sut.get()).isEqualTo(other);
        assertThat(sut.getError()).isNull();

    }

    @Test
    public void testCompleteExceptionally() throws Exception {

        String other = "other";

        sut = new PipelinedRedisFuture<>(Maps.newHashMap(), o -> other);

        sut.completeExceptionally(new Exception());
        assertThat(sut.get()).isEqualTo(other);
        assertThat(sut.getError()).isNull();

    }
}
