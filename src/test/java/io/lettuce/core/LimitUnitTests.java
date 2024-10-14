package io.lettuce.core;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * @author Mark Paluch
 */
@Tag(UNIT_TEST)
class LimitUnitTests {

    @Test
    void create() {

        Limit limit = Limit.create(1, 2);

        assertThat(limit.getOffset()).isEqualTo(1);
        assertThat(limit.getCount()).isEqualTo(2);
        assertThat(limit.isLimited()).isTrue();
    }

    @Test
    void unlimited() {

        Limit limit = Limit.unlimited();

        assertThat(limit.getOffset()).isEqualTo(-1);
        assertThat(limit.getCount()).isEqualTo(-1);
        assertThat(limit.isLimited()).isFalse();
    }

}
