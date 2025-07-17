/*
 * Copyright 2011-2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.search;

import io.lettuce.core.output.ComplexData;
import io.lettuce.core.output.ComplexDataParser;

import java.util.ArrayList;
import java.util.List;

/**
 * Parser for Redis FT.SUGGET command output.
 * <p>
 * This parser converts the response from the Redis FT.SUGGET command into a list of {@link Suggestion} objects. The FT.SUGGET
 * command can return different formats depending on the options used:
 * </p>
 * <ul>
 * <li><strong>Basic format:</strong> Just the suggestion strings</li>
 * <li><strong>With WITHSCORES:</strong> Alternating suggestion strings and scores</li>
 * <li><strong>With WITHPAYLOADS:</strong> Alternating suggestion strings and payloads</li>
 * <li><strong>With both WITHSCORES and WITHPAYLOADS:</strong> Suggestion strings, scores, and payloads in sequence</li>
 * </ul>
 *
 * @param <V> Value type.
 * @author Tihomir Mateev
 * @since 6.8
 */
public class SuggestionParser<V> implements ComplexDataParser<List<Suggestion<V>>> {

    private final boolean withScores;

    private final boolean withPayloads;

    /**
     * Create a new suggestion parser.
     *
     * @param withScores whether the response includes scores
     * @param withPayloads whether the response includes payloads
     */
    public SuggestionParser(boolean withScores, boolean withPayloads) {
        this.withScores = withScores;
        this.withPayloads = withPayloads;
    }

    /**
     * Parse the output of the Redis FT.SUGGET command and convert it to a list of {@link Suggestion} objects.
     * <p>
     * The parsing logic depends on the options used with the FT.SUGGET command:
     * </p>
     * <ul>
     * <li><strong>No options:</strong> Each element is a suggestion string</li>
     * <li><strong>WITHSCORES only:</strong> Elements alternate between suggestion string and score</li>
     * <li><strong>WITHPAYLOADS only:</strong> Elements alternate between suggestion string and payload</li>
     * <li><strong>Both WITHSCORES and WITHPAYLOADS:</strong> Elements are in groups of 3: suggestion, score, payload</li>
     * </ul>
     *
     * @param data output of FT.SUGGET command
     * @return a list of {@link Suggestion} objects
     * @throws IllegalArgumentException if the input data is null or has an invalid format
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<Suggestion<V>> parse(ComplexData data) {
        List<Suggestion<V>> suggestions = new ArrayList<>();

        if (data == null) {
            return suggestions;
        }

        List<Object> elements = data.getDynamicList();
        if (elements == null || elements.isEmpty()) {
            return suggestions;
        }

        for (int i = 0; i < elements.size();) {

            V value = (V) elements.get(i++);
            Suggestion<V> suggestion = new Suggestion<>(value);

            if (withScores) {
                Double score = parseScore(elements.get(i++));
                suggestion.setScore(score);
            }

            if (withPayloads) {
                V payload = (V) elements.get(i++);
                suggestion.setPayload(payload);
            }

            suggestions.add(suggestion);
        }

        return suggestions;
    }

    /**
     * Parse a score value from the response.
     *
     * @param scoreObj the score object from the response
     * @return the parsed score as a Double
     */
    private Double parseScore(Object scoreObj) {
        if (scoreObj == null) {
            return null;
        }

        if (scoreObj instanceof Double) {
            return (Double) scoreObj;
        }

        if (scoreObj instanceof Number) {
            return ((Number) scoreObj).doubleValue();
        }

        return 0.0;
    }

}
