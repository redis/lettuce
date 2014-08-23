package com.lambdaworks.redis;

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;

public class ScanCursorTest {

    @Test
    public void testFactory() throws Exception {
        ScanCursor scanCursor = ScanCursor.of("dummy");
        assertThat(scanCursor.getCursor()).isEqualTo("dummy");
        assertThat(scanCursor.isFinished()).isFalse();
    }
}
