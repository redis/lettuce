package io.lettuce.core.metrics;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * @author Mark Paluch
 * @author shariorfarhan07 (Sharior Hossain Farhan)
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
    void testDefaultMaxCommandLatencyIds() {

        DefaultCommandLatencyCollectorOptions sut = DefaultCommandLatencyCollectorOptions.create();

        assertThat(sut.maxCommandLatencyIds()).isEqualTo(DefaultCommandLatencyCollectorOptions.DEFAULT_MAX_COMMAND_LATENCY_IDS)
                .isEqualTo(500);
    }

    @Test
    void testMaxCommandLatencyIds() {

        DefaultCommandLatencyCollectorOptions sut = DefaultCommandLatencyCollectorOptions.builder().maxCommandLatencyIds(42)
                .build();

        assertThat(sut.maxCommandLatencyIds()).isEqualTo(42);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> DefaultCommandLatencyCollectorOptions.builder().maxCommandLatencyIds(0));
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
