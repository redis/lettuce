package com.lambdaworks.redis.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

/**
 * @author Mark Paluch
 */
public class DefaultDefaultCommandLatencyCollectorOptionsTest {

    @Test
    public void testDefault() throws Exception {

        DefaultCommandLatencyCollectorOptions sut = DefaultCommandLatencyCollectorOptions.create();

        assertThat(sut.targetPercentiles()).hasSize(5);
        assertThat(sut.targetUnit()).isEqualTo(TimeUnit.MICROSECONDS);
    }

    @Test
    public void testDisabled() throws Exception {

        DefaultCommandLatencyCollectorOptions sut = DefaultCommandLatencyCollectorOptions.disabled();

        assertThat(sut.isEnabled()).isEqualTo(false);
    }

    @Test
    public void testBuilder() throws Exception {

        DefaultCommandLatencyCollectorOptions sut = DefaultCommandLatencyCollectorOptions.builder()
                .targetUnit(TimeUnit.HOURS).targetPercentiles(new double[] { 1, 2, 3 }).build();

        assertThat(sut.targetPercentiles()).hasSize(3);
        assertThat(sut.targetUnit()).isEqualTo(TimeUnit.HOURS);
    }
}
