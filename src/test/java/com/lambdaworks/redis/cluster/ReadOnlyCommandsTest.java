package com.lambdaworks.redis.cluster;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class ReadOnlyCommandsTest {

    @Test
    public void testCount() throws Exception {
        assertThat(ReadOnlyCommands.READ_ONLY_COMMANDS).hasSize(62);
    }
}
