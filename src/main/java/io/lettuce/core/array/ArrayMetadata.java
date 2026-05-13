/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.array;

/**
 * Represents metadata about a Redis array, returned by the {@code ARINFO} command.
 * <p>
 * Contains 7 top-level fields describing the array's size, structure, and internal configuration.
 *
 * @author Aleksandar Todorov
 * @since 7.6
 * @see <a href="https://redis.io/docs/latest/commands/arinfo/">Redis Documentation: ARINFO</a>
 */
public class ArrayMetadata {

    private Long count;

    private Long len;

    private Long nextInsertIndex;

    private Long slices;

    private Long directorySize;

    private Long superDirEntries;

    private Long sliceSize;

    /**
     * Creates a new empty {@link ArrayMetadata} instance.
     */
    public ArrayMetadata() {
        // Default constructor
    }

    /**
     * Gets the number of populated (non-empty) slots in the array.
     *
     * @return the count of populated slots, or {@code null} if not available
     */
    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    /**
     * Gets the logical length of the array (max index + 1).
     *
     * @return the logical length, or {@code null} if not available
     */
    public Long getLen() {
        return len;
    }

    public void setLen(Long len) {
        this.len = len;
    }

    /**
     * Gets the next index that will be used by {@code ARINSERT}.
     *
     * @return the next insert index, or {@code null} if not available
     */
    public Long getNextInsertIndex() {
        return nextInsertIndex;
    }

    public void setNextInsertIndex(Long nextInsertIndex) {
        this.nextInsertIndex = nextInsertIndex;
    }

    /**
     * Gets the number of internal slices used to store the array.
     *
     * @return the slice count, or {@code null} if not available
     */
    public Long getSlices() {
        return slices;
    }

    public void setSlices(Long slices) {
        this.slices = slices;
    }

    /**
     * Gets the size of the internal directory.
     *
     * @return the directory size, or {@code null} if not available
     */
    public Long getDirectorySize() {
        return directorySize;
    }

    public void setDirectorySize(Long directorySize) {
        this.directorySize = directorySize;
    }

    /**
     * Gets the number of super-directory entries (used for very large or high-index arrays).
     *
     * @return the super-directory entry count, or {@code null} if not available
     */
    public Long getSuperDirEntries() {
        return superDirEntries;
    }

    public void setSuperDirEntries(Long superDirEntries) {
        this.superDirEntries = superDirEntries;
    }

    /**
     * Gets the size of each internal slice (default 4096).
     *
     * @return the slice size, or {@code null} if not available
     */
    public Long getSliceSize() {
        return sliceSize;
    }

    public void setSliceSize(Long sliceSize) {
        this.sliceSize = sliceSize;
    }

}
