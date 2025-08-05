/*
 * Copyright 2011-2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.search;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.ComplexData;
import io.lettuce.core.output.ComplexDataParser;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Parser for Redis FT.SPELLCHECK command output.
 * <p>
 * This parser converts the response from the Redis FT.SPELLCHECK command into a {@link SpellCheckResult} object. The
 * FT.SPELLCHECK command returns an array where each element represents a misspelled term from the query. Each misspelled term
 * is a 3-element array consisting of:
 * </p>
 * <ol>
 * <li>The constant string "TERM"</li>
 * <li>The misspelled term itself</li>
 * <li>An array of suggestions, where each suggestion is a 2-element array of [score, suggestion]</li>
 * </ol>
 *
 * @param <V> Value type.
 * @author Tihomir Mateev
 * @since 6.8
 */
public class SpellCheckResultParser<K, V> implements ComplexDataParser<SpellCheckResult<V>> {

    private static final InternalLogger LOG = InternalLoggerFactory.getInstance(SpellCheckResultParser.class);

    private final RedisCodec<K, V> codec;

    public SpellCheckResultParser(RedisCodec<K, V> codec) {
        this.codec = codec;
    }

    @Override
    public SpellCheckResult<V> parse(ComplexData data) {
        if (data == null) {
            return new SpellCheckResult<>();
        }

        if (data.isList()) {
            return new SpellCheckResp2Parser().parse(data);
        }

        return new SpellCheckResp3Parser().parse(data);
    }

    /**
     * Parse the RESP3 output of the Redis FT.SPELLCHECK command and convert it to a {@link SpellCheckResult} object.
     * <p>
     * The parsing logic handles the nested array structure returned by FT.SPELLCHECK:
     * </p>
     * 
     * <pre>
     * [
     *  "results" ->  "misspelled_term" -> [ "score1" => "suggestion1", ["score2", "suggestion2"],
     *                "another_term"    -> [ "score3" => "suggestion3"]
     * ]
     * </pre>
     */
    class SpellCheckResp3Parser implements ComplexDataParser<SpellCheckResult<V>> {

        private final ByteBuffer resultsKeyword = StringCodec.UTF8.encodeKey("results");

        @Override
        public SpellCheckResult<V> parse(ComplexData data) {
            SpellCheckResult<V> result = new SpellCheckResult<>();

            if (data == null) {
                return null;
            }

            Map<Object, Object> elements = data.getDynamicMap();
            if (elements == null || elements.isEmpty() || !elements.containsKey(resultsKeyword)) {
                LOG.warn("Failed while parsing FT.SPELLCHECK: data must contain a 'results' key");
                return result;
            }

            ComplexData resultsData = (ComplexData) elements.get(resultsKeyword);
            Map<Object, Object> resultsMap = resultsData.getDynamicMap();

            // Go through each misspelled term, should contain three items itself
            for (Object term : resultsMap.keySet()) {

                // Key of the inner map is the misspelled term
                V misspelledTerm = codec.decodeValue((ByteBuffer) term);

                // Value of the inner map is the suggestions array
                ComplexData termData = (ComplexData) resultsMap.get(term);
                List<Object> suggestionsArray = termData.getDynamicList();

                List<SpellCheckResult.Suggestion<V>> suggestions = parseSuggestions(suggestionsArray);
                result.addMisspelledTerm(new SpellCheckResult.MisspelledTerm<>(misspelledTerm, suggestions));
            }

            return result;
        }

        private List<SpellCheckResult.Suggestion<V>> parseSuggestions(List<Object> suggestionsArray) {
            List<SpellCheckResult.Suggestion<V>> suggestions = new ArrayList<>();

            for (Object suggestionObj : suggestionsArray) {
                Map<Object, Object> suggestionMap = ((ComplexData) suggestionObj).getDynamicMap();

                for (Object suggestion : suggestionMap.keySet()) {
                    double score = (double) suggestionMap.get(suggestion);
                    V suggestionValue = codec.decodeValue((ByteBuffer) suggestion);
                    suggestions.add(new SpellCheckResult.Suggestion<>(score, suggestionValue));
                }
            }

            return suggestions;
        }

    }

    /**
     * Parse the RESP2 output of the Redis FT.SPELLCHECK command and convert it to a {@link SpellCheckResult} object.
     * <p>
     * The parsing logic handles the nested array structure returned by FT.SPELLCHECK:
     * </p>
     * 
     * <pre>
     * [
     *   ["TERM", "misspelled_term", [["score1", "suggestion1"], ["score2", "suggestion2"]]],
     *   ["TERM", "another_term", [["score3", "suggestion3"]]]
     * ]
     * </pre>
     */
    class SpellCheckResp2Parser implements ComplexDataParser<SpellCheckResult<V>> {

        private final ByteBuffer termKeyword = StringCodec.UTF8.encodeKey("TERM");

        @Override
        public SpellCheckResult<V> parse(ComplexData data) {
            SpellCheckResult<V> result = new SpellCheckResult<>();

            List<Object> elements = data.getDynamicList();
            if (elements == null || elements.isEmpty()) {
                return result;
            }

            // Go through each misspelled term, should contain three items itself
            for (Object element : elements) {
                List<Object> termContents = ((ComplexData) element).getDynamicList();

                if (termContents == null || termContents.size() != 3) {
                    LOG.warn("Failed while parsing FT.SPELLCHECK: each term element must have 3 parts");
                    continue;
                }

                // First element should be "TERM"
                Object termMarker = termContents.get(0);
                boolean isValidTermMarker = termKeyword.equals(termMarker) || "TERM".equals(termMarker);
                if (!isValidTermMarker) {
                    LOG.warn("Failed while parsing FT.SPELLCHECK: expected 'TERM' marker, got: " + termMarker);
                    continue;
                }

                // Second element is the misspelled term
                V misspelledTerm = decodeValue(termContents.get(1));

                // Third element is the suggestions array
                ComplexData suggestionsObj = (ComplexData) termContents.get(2);
                List<Object> suggestionsArray = suggestionsObj.getDynamicList();
                List<SpellCheckResult.Suggestion<V>> suggestions = parseSuggestions(suggestionsArray);

                result.addMisspelledTerm(new SpellCheckResult.MisspelledTerm<>(misspelledTerm, suggestions));
            }

            return result;
        }

        private List<SpellCheckResult.Suggestion<V>> parseSuggestions(List<Object> suggestionsArray) {
            List<SpellCheckResult.Suggestion<V>> suggestions = new ArrayList<>();

            for (Object suggestionObj : suggestionsArray) {
                List<Object> suggestionData = ((ComplexData) suggestionObj).getDynamicList();

                if (suggestionData.size() != 2) {
                    LOG.warn("Failed while parsing FT.SPELLCHECK: each suggestion must have 2 parts");
                    continue;
                }

                // First element is the score
                double score = parseScore(suggestionData.get(0));

                // Second element is the suggestion
                V suggestion = decodeValue(suggestionData.get(1));

                suggestions.add(new SpellCheckResult.Suggestion<>(score, suggestion));
            }

            return suggestions;
        }

    }

    /**
     * Helper method to decode values that can be either ByteBuffer or String objects.
     */
    @SuppressWarnings("unchecked")
    private V decodeValue(Object value) {
        if (value instanceof ByteBuffer) {
            return codec.decodeValue((ByteBuffer) value);
        } else if (value instanceof String) {
            // For test scenarios where strings are passed directly
            return (V) value;
        } else {
            // Fallback - try to cast directly
            return (V) value;
        }
    }

    /**
     * Helper method to parse score values that can be either ByteBuffer, String, or Number objects.
     */
    private double parseScore(Object scoreObj) {
        if (scoreObj == null) {
            return 0.0;
        }

        if (scoreObj instanceof Number) {
            return ((Number) scoreObj).doubleValue();
        }

        if (scoreObj instanceof String) {
            try {
                return Double.parseDouble((String) scoreObj);
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }

        if (scoreObj instanceof ByteBuffer) {
            try {
                String scoreStr = StringCodec.UTF8.decodeValue((ByteBuffer) scoreObj);
                return Double.parseDouble(scoreStr);
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }

        return 0.0;
    }

}
