package io.lettuce.core.event;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * @author Mark Paluch
 */
@Tag(UNIT_TEST)
class DefaultEventPublisherOptionsUnitTests {

    @Test
    void testDefault() {

        DefaultEventPublisherOptions sut = DefaultEventPublisherOptions.create();

        assertThat(sut.eventEmitInterval()).isEqualTo(Duration.ofMinutes(10));
    }

    @Test
    void testDisabled() {

        DefaultEventPublisherOptions sut = DefaultEventPublisherOptions.disabled();

        assertThat(sut.eventEmitInterval()).isEqualTo(Duration.ZERO);
    }

    @Test
    void testBuilder() {

        DefaultEventPublisherOptions sut = DefaultEventPublisherOptions.builder().eventEmitInterval(1, TimeUnit.SECONDS)
                .build();

        assertThat(sut.eventEmitInterval()).isEqualTo(Duration.ofSeconds(1));
    }

}
