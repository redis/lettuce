package com.lambdaworks.redis;

/**
 * Generic Cursor data structure.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.0
 */
public class ScanCursor {
    private String cursor;
    private boolean finished;

    /**
     * 
     * @return cursor
     */
    public String getCursor() {
        return cursor;
    }

    /**
     * Set the cursor
     * 
     * @param cursor
     */
    public void setCursor(String cursor) {
        this.cursor = cursor;
    }

    /**
     * 
     * @return true if the scan operation of this cursor is finished.
     */
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
