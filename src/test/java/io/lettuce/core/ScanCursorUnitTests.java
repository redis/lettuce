package io.lettuce.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * @author Mark Paluch
 */
class ScanCursorUnitTests {

    @Test
    void testFactory() {
        ScanCursor scanCursor = ScanCursor.of("dummy");
        assertThat(scanCursor.getCursor()).isEqualTo("dummy");
        assertThat(scanCursor.isFinished()).isFalse();
    }

    @Test
    void setCursorOnImmutableInstance() {
        assertThatThrownBy(() -> ScanCursor.INITIAL.setCursor("")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void setFinishedOnImmutableInstance() {
        assertThatThrownBy(() -> ScanCursor.INITIAL.setFinished(false)).isInstanceOf(UnsupportedOperationException.class);
    }
}
