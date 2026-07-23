package io.lettuce.core;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * @author Mark Paluch
 * @author Sanghun Lee
 */
@Tag(UNIT_TEST)
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

    @Test
    void setSourceOnImmutableInstance() {
        assertThatThrownBy(() -> ScanCursor.INITIAL.setSource(RedisURI.create("localhost", 6379)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

}
