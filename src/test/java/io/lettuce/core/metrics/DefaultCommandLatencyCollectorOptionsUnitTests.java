package io.lettuce.core.metrics;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * @author Mark Paluch
 */
@Tag(UNIT_TEST)
class DefaultCommandLatencyCollectorOptionsUnitTests {

    @Test
    void testDefault() {

        DefaultCommandLatencyCollectorOptions sut = DefaultCommandLatencyCollectorOptions.create();

        assertThat(sut.targetPercentiles()).hasSize(5);
        assertThat(sut.targetUnit()).isEqualTo(TimeUnit.MICROSECONDS);
    }

    @Test
    void testDisabled() {

        DefaultCommandLatencyCollectorOptions sut = DefaultCommandLatencyCollectorOptions.disabled();

        assertThat(sut.isEnabled()).isEqualTo(false);
    }

    @Test
    void testBuilder() {

        DefaultCommandLatencyCollectorOptions sut = DefaultCommandLatencyCollectorOptions.builder().targetUnit(TimeUnit.HOURS)
                .targetPercentiles(new double[] { 1, 2, 3 }).build();

        assertThat(sut.targetPercentiles()).hasSize(3);
        assertThat(sut.targetUnit()).isEqualTo(TimeUnit.HOURS);
    }

}
