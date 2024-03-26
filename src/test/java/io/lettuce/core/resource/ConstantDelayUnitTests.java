package io.lettuce.core.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

/**
 * @author Mark Paluch
 */
class ConstantDelayUnitTests {

    @Test
    void shouldNotCreateIfDelayIsNegative() {
        assertThatThrownBy(() -> Delay.constant(-1, TimeUnit.MILLISECONDS)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldCreateZeroDelay() {

        Delay delay = Delay.constant(0, TimeUnit.MILLISECONDS);

        assertThat(delay.createDelay(0)).isEqualTo(Duration.ZERO);
        assertThat(delay.createDelay(5)).isEqualTo(Duration.ZERO);
    }

    @Test
    void shouldCreateConstantDelay() {

        Delay delay = Delay.constant(100, TimeUnit.MILLISECONDS);

        assertThat(delay.createDelay(0)).isEqualTo(Duration.ofMillis(100));
        assertThat(delay.createDelay(5)).isEqualTo(Duration.ofMillis(100));
    }
}
