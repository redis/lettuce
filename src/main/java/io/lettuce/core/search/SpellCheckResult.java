/*
 * Copyright 2011-2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents the result of a Redis FT.SPELLCHECK command.
 * <p>
 * Contains a list of misspelled terms from the query, each with their spelling suggestions. The misspelled terms are ordered by
 * their order of appearance in the query.
 * </p>
 *
 * @param <V> Value type.
 * @author Tihomir Mateev
 * @since 6.8
 */
public class SpellCheckResult<V> {

    private final List<MisspelledTerm<V>> misspelledTerms = new ArrayList<>();

    public SpellCheckResult() {
    }

    /**
     * Get the list of misspelled terms with their suggestions.
     *
     * @return the list of misspelled terms
     */
    public List<MisspelledTerm<V>> getMisspelledTerms() {
        return misspelledTerms;
    }

    /**
     * Check if there are any misspelled terms.
     *
     * @return {@code true} if there are misspelled terms
     */
    public boolean hasMisspelledTerms() {
        return !misspelledTerms.isEmpty();
    }

    /**
     * Get the number of misspelled terms.
     *
     * @return the number of misspelled terms
     */
    public int getMisspelledTermCount() {
        return misspelledTerms.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        SpellCheckResult<?> that = (SpellCheckResult<?>) o;
        return Objects.equals(misspelledTerms, that.misspelledTerms);
    }

    @Override
    public int hashCode() {
        return Objects.hash(misspelledTerms);
    }

    @Override
    public String toString() {
        return "SpellCheckResult{" + "misspelledTerms=" + misspelledTerms + '}';
    }

    void addMisspelledTerm(MisspelledTerm<V> vMisspelledTerm) {
        misspelledTerms.add(vMisspelledTerm);
    }

    /**
     * Represents a misspelled term and its spelling suggestions.
     *
     * @param <V> Value type.
     */
    public static class MisspelledTerm<V> {

        private final V term;

        private final List<Suggestion<V>> suggestions;

        /**
         * Create a new misspelled term.
         *
         * @param term the misspelled term
         * @param suggestions the list of spelling suggestions
         */
        public MisspelledTerm(V term, List<Suggestion<V>> suggestions) {
            this.term = term;
            this.suggestions = suggestions;
        }

        /**
         * Get the misspelled term.
         *
         * @return the misspelled term
         */
        public V getTerm() {
            return term;
        }

        /**
         * Get the list of spelling suggestions.
         *
         * @return the list of suggestions
         */
        public List<Suggestion<V>> getSuggestions() {
            return suggestions;
        }

        /**
         * Check if there are any suggestions for this term.
         *
         * @return {@code true} if there are suggestions
         */
        public boolean hasSuggestions() {
            return suggestions != null && !suggestions.isEmpty();
        }

        /**
         * Get the number of suggestions for this term.
         *
         * @return the number of suggestions
         */
        public int getSuggestionCount() {
            return suggestions != null ? suggestions.size() : 0;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            MisspelledTerm<?> that = (MisspelledTerm<?>) o;
            return Objects.equals(term, that.term) && Objects.equals(suggestions, that.suggestions);
        }

        @Override
        public int hashCode() {
            return Objects.hash(term, suggestions);
        }

        @Override
        public String toString() {
            return "MisspelledTerm{" + "term=" + term + ", suggestions=" + suggestions + '}';
        }

    }

    /**
     * Represents a spelling suggestion with its score.
     *
     * @param <V> Value type.
     */
    public static class Suggestion<V> {

        private final double score;

        private final V suggestion;

        /**
         * Create a new spelling suggestion.
         *
         * @param score the suggestion score
         * @param suggestion the suggested term
         */
        public Suggestion(double score, V suggestion) {
            this.score = score;
            this.suggestion = suggestion;
        }

        /**
         * Get the suggestion score.
         * <p>
         * The score is calculated by dividing the number of documents in which the suggested term exists by the total number of
         * documents in the index. Results can be normalized by dividing scores by the highest score.
         * </p>
         *
         * @return the suggestion score
         */
        public double getScore() {
            return score;
        }

        /**
         * Get the suggested term.
         *
         * @return the suggested term
         */
        public V getSuggestion() {
            return suggestion;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            Suggestion<?> that = (Suggestion<?>) o;
            return Double.compare(that.score, score) == 0 && Objects.equals(suggestion, that.suggestion);
        }

        @Override
        public int hashCode() {
            return Objects.hash(score, suggestion);
        }

        @Override
        public String toString() {
            return "Suggestion{" + "score=" + score + ", suggestion=" + suggestion + '}';
        }

    }

}
