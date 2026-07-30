package io.lettuce.core;

/**
 * Accessor for the source-node association of a {@link ScanCursor}. Internal utility class that lets the driver (e.g.
 * Master/Replica connection routing) associate a scan cursor with the node that issued it across package boundaries, without
 * widening the public {@link ScanCursor} API.
 *
 * @author Sanghun Lee
 * @since 7.7
 */
public abstract class ScanCursorAccessor {

    /**
     * Utility constructor.
     */
    private ScanCursorAccessor() {
    }

    /**
     * Returns the {@link RedisURI} of the node that issued {@code cursor}, if known.
     *
     * @param cursor the scan cursor.
     * @return the node that issued the cursor or {@code null} if unknown.
     * @since 7.7
     */
    public static RedisURI getSource(ScanCursor cursor) {
        return cursor.getSource();
    }

    /**
     * Associates {@code cursor} with the node that issued it.
     *
     * @param cursor the scan cursor.
     * @param source the node that issued the cursor, may be {@code null}.
     * @since 7.7
     */
    public static void setSource(ScanCursor cursor, RedisURI source) {
        cursor.setSource(source);
    }

}
