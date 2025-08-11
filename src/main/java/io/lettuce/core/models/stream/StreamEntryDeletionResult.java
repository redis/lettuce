/*
 * Copyright 2025-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.models.stream;

/**
 * Result of stream entry deletion operations for XDELEX and XACKDEL commands.
 * <p>
 * Represents the outcome of attempting to delete a specific stream entry:
 * <ul>
 * <li>NOT_FOUND (-1): ID doesn't exist in stream</li>
 * <li>DELETED (1): Entry was deleted/acknowledged and deleted</li>
 * <li>NOT_DELETED_UNACKNOWLEDGED_OR_STILL_REFERENCED (2): Entry wasn't deleted.</li>
 * </ul>
 *
 * @since 6.8
 */
public enum StreamEntryDeletionResult {

    UNKNOWN(-2),

    /**
     * The stream entry ID was not found in the stream.
     */
    NOT_FOUND(-1),

    /**
     * The entry was successfully deleted from the stream.
     */
    DELETED(1),

    /**
     * The entry was not deleted due to one of the following reasons:
     * <ul>
     * <li>For XDELEX: The entry was not acknowledged by any consumer group</li>
     * <li>For XACKDEL: The entry still has pending references in other consumer groups</li>
     * </ul>
     */
    NOT_DELETED_UNACKNOWLEDGED_OR_STILL_REFERENCED(2);

    private final int code;

    StreamEntryDeletionResult(int code) {
        this.code = code;
    }

    /**
     * Returns the numeric code associated with this result.
     *
     * @return the numeric code.
     */
    public int getCode() {
        return code;
    }

    /**
     * Create a {@link StreamEntryDeletionResult} from its numeric code.
     *
     * @param code the numeric code.
     * @return the {@link StreamEntryDeletionResult}.
     * @throws IllegalArgumentException if the code is unknown.
     */
    public static StreamEntryDeletionResult fromCode(int code) {
        switch (code) {
            case -1:
                return NOT_FOUND;
            case 1:
                return DELETED;
            case 2:
                return NOT_DELETED_UNACKNOWLEDGED_OR_STILL_REFERENCED;
            default:
                return UNKNOWN;
        }
    }

    /**
     * Create a {@link StreamEntryDeletionResult} from a {@link Long} value.
     *
     * @param value the Long value, may be {@code null}.
     * @return the {@link StreamEntryDeletionResult}, or {@code null} if the input is {@code null}.
     * @throws IllegalArgumentException if the code is unknown.
     */
    public static StreamEntryDeletionResult fromLong(Long value) {
        if (value == null) {
            return null;
        }
        return fromCode(value.intValue());
    }

    @Override
    public String toString() {
        return name() + "(" + code + ")";
    }

}
