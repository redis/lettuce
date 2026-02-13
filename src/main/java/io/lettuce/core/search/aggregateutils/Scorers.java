/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.search.aggregateutils;

import java.util.Collections;
import java.util.List;

import io.lettuce.core.annotations.Experimental;

/**
 * Factory class for creating {@link Scorer} instances for text search.
 *
 * @author Aleksandar Todorov
 * @since 7.2
 * @see Scorer
 */
@Experimental
public class Scorers {

    private static Scorer simpleScorer(String name) {
        return new Scorer(name) {

            @Override
            protected List<Object> getOwnArgs() {
                return Collections.emptyList();
            }

        };
    }

    public static Scorer tfidf() {
        return simpleScorer("TFIDF");
    }

    public static Scorer tfidfDocNorm() {
        return simpleScorer("TFIDF.DOCNORM");
    }

    public static Scorer bm25() {
        return simpleScorer("BM25STD");
    }

    public static Scorer bm25Norm() {
        return simpleScorer("BM25STD_NORM");
    }

    public static Scorer dismax() {
        return simpleScorer("DISMAX");
    }

    public static Scorer docscore() {
        return simpleScorer("DOCSCORE");
    }

    public static Scorer hamming() {
        return simpleScorer("HAMMING");
    }

}
