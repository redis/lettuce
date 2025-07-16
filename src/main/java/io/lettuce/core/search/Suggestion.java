/*
 * Copyright 2011-2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.search;

import java.util.Objects;

/**
 * Represents a suggestion returned by the FT.SUGGET command.
 * <p>
 * A suggestion contains the suggestion string itself, and optionally a score and payload
 * when the FT.SUGGET command is called with WITHSCORES and/or WITHPAYLOADS options.
 * </p>
 *
 * @param <V> Value type.
 * @author Tihomir Mateev
 * @since 6.8
 */
public class Suggestion<V> {

    private final V value;

    private Double score;

    private V payload;

    /**
     * Create a new suggestion with only the value.
     *
     * @param value the suggestion string
     */
    public Suggestion(V value) {
        this.value = value;
    }

    void setScore(Double score) {
        this.score = score;
    }

    void setPayload(V payload) {
        this.payload = payload;
    }

    /**
     * Get the suggestion string.
     *
     * @return the suggestion value
     */
    public V getValue() {
        return value;
    }

    /**
     * Get the suggestion score.
     *
     * @return the suggestion score, or {@code null} if not available
     */
    public Double getScore() {
        return score;
    }

    /**
     * Get the suggestion payload.
     *
     * @return the suggestion payload, or {@code null} if not available
     */
    public V getPayload() {
        return payload;
    }

    /**
     * Check if this suggestion has a score.
     *
     * @return {@code true} if the suggestion has a score
     */
    public boolean hasScore() {
        return score != null;
    }

    /**
     * Check if this suggestion has a payload.
     *
     * @return {@code true} if the suggestion has a payload
     */
    public boolean hasPayload() {
        return payload != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Suggestion<?> that = (Suggestion<?>) o;
        return Objects.equals(value, that.value) &&
                Objects.equals(score, that.score) &&
                Objects.equals(payload, that.payload);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, score, payload);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Suggestion{");
        sb.append("value=").append(value);
        if (score != null) {
            sb.append(", score=").append(score);
        }
        if (payload != null) {
            sb.append(", payload=").append(payload);
        }
        sb.append('}');
        return sb.toString();
    }

}
