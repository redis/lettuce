package io.lettuce.core.resource;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * @author Mark Paluch
 */
@Tag(UNIT_TEST)
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
