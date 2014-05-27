package com.lambdaworks.redis;

/**
 * Generic Cursor data structure.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 20.05.14 14:35
 */
public class ScanCursor {
    private String cursor;
    private boolean finished;

    public String getCursor() {
        return cursor;
    }

    public void setCursor(String cursor) {
        this.cursor = cursor;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    /**
     * Creates a Scan-Cursor reference.
     * 
     * @param cursor
     * @return ScanCursor
     */
    public static ScanCursor of(String cursor) {
        ScanCursor scanCursor = new ScanCursor();
        scanCursor.setCursor(cursor);
        return scanCursor;
    }
}
