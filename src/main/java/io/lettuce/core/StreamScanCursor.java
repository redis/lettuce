package io.lettuce.core;

/**
 * Cursor result using the Streaming API. Provides the count of retrieved elements.
 *
 * @author Mark Paluch
 * @since 3.0
 */
public class StreamScanCursor extends ScanCursor {

    private long count;

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

}
