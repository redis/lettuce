/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.search.arguments;

/**
 * Scoring function for search queries.
 * <p/>
 * The scoring function determines how the relevance of a document is calculated.
 * <p/>
 * The default scoring function is {@link ScoringFunction#TF_IDF}.
 * 
 * @see <a href="https://redis.io/docs/latest/develop/interact/search-and-query/advanced-concepts/scoring/">Scoring</a>
 * @since 6.8
 * @author Tihomir Mateev
 */
public enum ScoringFunction {

    /**
     * Term Frequency - Inverse Document Frequency.
     * <p/>
     * This is the default setting.
     * 
     * @see <a href="https://en.wikipedia.org/wiki/Tf%E2%80%93idf">Wikipedia</a>
     */
    TF_IDF("TFIDF"),

    /**
     * Term Frequency - Inverse Document Frequency with document normalization.
     * <p/>
     * Identical to the default TFIDF scorer, with one important distinction - term frequencies are normalized by the length of
     * the document, expressed as the total number of terms. The length is weighted, so that if a document contains two terms,
     * one in a field that has a weight 1 and one in a field with a weight of 5, the total frequency is 6, not 2.
     * 
     * @see <a href="https://en.wikipedia.org/wiki/Tf%E2%80%93idf">Wikipedia</a>
     */
    TF_IDF_NORMALIZED("TFIDF.DOCNORM"),

    /**
     * A variation on the basic TFIDF scorer. The relevance score for each document is multiplied by the presumptive document
     * score, and a penalty is applied based on slop as in TFIDF.
     *
     * @see <a href="https://en.wikipedia.org/wiki/Okapi_BM25">Wikipedia</a>
     */
    BM25("BM25"),

    /**
     * A simple scorer that sums up the frequencies of matched terms. In the case of union clauses, it will give the maximum
     * value of those matches. No other penalties or factors are applied.
     *
     * @see <a href="https://wiki.apache.org/solr/DisMax">DisMax</a>
     */
    DIS_MAX("DISMAX"),

    /**
     * A scoring function that just returns the presumptive score of the document without applying any calculations to it. Since
     * document scores can be updated, this can be useful if you'd like to use an external score and nothing further.
     */
    DOCUMENT_SCORE("DOCSCORE"),

    /**
     * Scoring by the inverse Hamming distance between the document's payload and the query payload is performed. Since the
     * nearest neighbors are of interest, the inverse Hamming distance (1/(1+d)) is used so that a distance of 0 gives a perfect
     * score of 1 and is the highest rank.
     * <p/>
     * This only works if:
     * <ul/>
     * <li>The document has a payload.
     * <li/>
     * <li>The query has a payload.
     * <li/>
     * <li>Both are exactly the same length.
     * <li/>
     * </ul>
     * Payloads are binary-safe, and having payloads with a length that is a multiple of 64 bits yields slightly faster results.
     * <p/>
     * 
     * @see <a href="https://en.wikipedia.org/wiki/Hamming_distance">Wikipedia</a>
     */
    HAMMING_DISTANCE("HAMMING");

    private final String name;

    ScoringFunction(String function) {
        this.name = function;
    }

    @Override
    public String toString() {
        return name;
    }

}
