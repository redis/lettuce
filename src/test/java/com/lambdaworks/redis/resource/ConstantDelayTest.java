package com.lambdaworks.redis.resource;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

/**
 * @author Mark Paluch
 */
public class ConstantDelayTest {

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotCreateIfDelayIsNegative() throws Exception {
        Delay.constant(-1, TimeUnit.MILLISECONDS);
    }

    @Test
    public void shouldCreateZeroDelay() throws Exception {

        Delay delay = Delay.constant(0, TimeUnit.MILLISECONDS);

        assertThat(delay.createDelay(0)).isEqualTo(0);
        assertThat(delay.createDelay(5)).isEqualTo(0);
    }

    @Test
    public void shouldCreateConstantDelay() throws Exception {

        Delay delay = Delay.constant(100, TimeUnit.MILLISECONDS);

        assertThat(delay.createDelay(0)).isEqualTo(100);
        assertThat(delay.createDelay(5)).isEqualTo(100);
    }
}