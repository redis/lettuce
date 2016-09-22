package com.lambdaworks.redis;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

/**
 * @author Mark Paluch
 */
public class LimitTest {

    @Test
    public void create() {

        Limit limit = Limit.create(1, 2);

        assertThat(limit.getOffset()).isEqualTo(1);
        assertThat(limit.getCount()).isEqualTo(2);
        assertThat(limit.isLimited()).isTrue();
    }

    @Test
    public void unlimited() {

        Limit limit = Limit.unlimited();

        assertThat(limit.getOffset()).isEqualTo(-1);
        assertThat(limit.getCount()).isEqualTo(-1);
        assertThat(limit.isLimited()).isFalse();
    }
}
