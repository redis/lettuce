package com.lambdaworks.redis;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class ScanCursorTest {

    @Test
    public void testFactory() throws Exception {
        ScanCursor scanCursor = ScanCursor.of("dummy");
        assertThat(scanCursor.getCursor()).isEqualTo("dummy");
        assertThat(scanCursor.isFinished()).isFalse();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void setCursorOnImmutableInstance() throws Exception {
        ScanCursor.INITIAL.setCursor("");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void setFinishedOnImmutableInstance() throws Exception {
        ScanCursor.INITIAL.setFinished(false);
    }
}
