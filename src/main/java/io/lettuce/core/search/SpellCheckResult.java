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
 * @author Tihomir Mateev
 * @since 6.8
 */
public class SpellCheckResult {

    private final List<MisspelledTerm> misspelledTerms = new ArrayList<>();

    public SpellCheckResult() {
    }

    /**
     * Get the list of misspelled terms with their suggestions.
     *
     * @return the list of misspelled terms
     */
    public List<MisspelledTerm> getMisspelledTerms() {
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
        SpellCheckResult that = (SpellCheckResult) o;
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

    void addMisspelledTerm(MisspelledTerm misspelledTerm) {
        misspelledTerms.add(misspelledTerm);
    }

    /**
     * Represents a misspelled term and its spelling suggestions.
     */
    public static class MisspelledTerm {

        private final String term;

        private final List<Suggestion> suggestions;

        /**
         * Create a new misspelled term.
         *
         * @param term the misspelled term
         * @param suggestions the list of spelling suggestions
         */
        public MisspelledTerm(String term, List<Suggestion> suggestions) {
            this.term = term;
            this.suggestions = suggestions;
        }

        /**
         * Get the misspelled term.
         *
         * @return the misspelled term
         */
        public String getTerm() {
            return term;
        }

        /**
         * Get the list of spelling suggestions.
         *
         * @return the list of suggestions
         */
        public List<Suggestion> getSuggestions() {
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
            MisspelledTerm that = (MisspelledTerm) o;
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
     */
    public static class Suggestion {

        private final double score;

        private final String suggestion;

        /**
         * Create a new spelling suggestion.
         *
         * @param score the suggestion score
         * @param suggestion the suggested term
         */
        public Suggestion(double score, String suggestion) {
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
        public String getSuggestion() {
            return suggestion;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            Suggestion that = (Suggestion) o;
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
