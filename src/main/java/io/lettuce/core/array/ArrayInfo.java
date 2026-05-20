/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.array;

import java.util.Collections;
import java.util.Map;

/**
 * Immutable representation of metadata about a Redis array, returned by the {@code ARINFO} command.
 * <p>
 * Known fields are available via typed getters. The raw server map is accessible via {@link #getInfo()} so callers can read
 * fields added in future server versions without requiring a client release.
 *
 * @author Aleksandar Todorov
 * @since 7.6
 * @see <a href="https://redis.io/docs/latest/commands/arinfo/">Redis Documentation: ARINFO</a>
 */
public class ArrayInfo {

    public static final String COUNT = "count";

    public static final String LEN = "len";

    public static final String NEXT_INSERT_INDEX = "next-insert-index";

    public static final String SLICES = "slices";

    public static final String DIRECTORY_SIZE = "directory-size";

    public static final String SUPER_DIR_ENTRIES = "super-dir-entries";

    public static final String SLICE_SIZE = "slice-size";

    private final Long count;

    private final Long len;

    private final Long nextInsertIndex;

    private final Long slices;

    private final Long directorySize;

    private final Long superDirEntries;

    private final Long sliceSize;

    private final Map<String, Object> info;

    /**
     * Creates a new {@link ArrayInfo} from the raw server response map.
     *
     * @param map the raw key-value pairs from the ARINFO response.
     */
    public ArrayInfo(Map<String, Object> map) {
        this.info = Collections.unmodifiableMap(map);
        this.count = parseLong(map.get(COUNT));
        this.len = parseLong(map.get(LEN));
        this.nextInsertIndex = parseLong(map.get(NEXT_INSERT_INDEX));
        this.slices = parseLong(map.get(SLICES));
        this.directorySize = parseLong(map.get(DIRECTORY_SIZE));
        this.superDirEntries = parseLong(map.get(SUPER_DIR_ENTRIES));
        this.sliceSize = parseLong(map.get(SLICE_SIZE));
    }

    /**
     * Returns the raw server response map. Useful for accessing fields added in future server versions.
     *
     * @return an unmodifiable map of all fields returned by the server.
     */
    public Map<String, Object> getInfo() {
        return info;
    }

    /**
     * @return the number of populated (non-empty) slots in the array, or {@code null} if not available.
     */
    public Long getCount() {
        return count;
    }

    /**
     * @return the logical length of the array (max index + 1), or {@code null} if not available.
     */
    public Long getLen() {
        return len;
    }

    /**
     * @return the next index that will be used by {@code ARINSERT}, or {@code null} if not available.
     */
    public Long getNextInsertIndex() {
        return nextInsertIndex;
    }

    /**
     * @return the number of internal slices, or {@code null} if not available.
     */
    public Long getSlices() {
        return slices;
    }

    /**
     * @return the size of the internal directory, or {@code null} if not available.
     */
    public Long getDirectorySize() {
        return directorySize;
    }

    /**
     * @return the number of super-directory entries, or {@code null} if not available.
     */
    public Long getSuperDirEntries() {
        return superDirEntries;
    }

    /**
     * @return the size of each internal slice (default 4096), or {@code null} if not available.
     */
    public Long getSliceSize() {
        return sliceSize;
    }

    static Long parseLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        } else if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

}
