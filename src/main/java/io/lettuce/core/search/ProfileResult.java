/*
 * Copyright 2011-2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.search;

import java.util.List;

/**
 * Represents the result of an FT.PROFILE command, containing both search results and profiling information.
 *
 * @author Tihomir Mateev
 * @since 6.8
 */
public class ProfileResult {

    private final Object searchResults;

    private final ProfileInfo profileInfo;

    public ProfileResult(Object searchResults, ProfileInfo profileInfo) {
        this.searchResults = searchResults;
        this.profileInfo = profileInfo;
    }

    /**
     * Get the search results from the FT.SEARCH or FT.AGGREGATE command.
     *
     * @return the search results
     */
    public Object getSearchResults() {
        return searchResults;
    }

    /**
     * Get the profiling information.
     *
     * @return the profile information
     */
    public ProfileInfo getProfileInfo() {
        return profileInfo;
    }

    /**
     * Represents profiling information for a query execution.
     */
    public static class ProfileInfo {

        private final String totalProfileTime;

        private final String parsingTime;

        private final String pipelineCreationTime;

        private final List<String> warnings;

        private final List<IteratorProfile> iteratorProfiles;

        private final List<ResultProcessorProfile> resultProcessorProfiles;

        private final CoordinatorProfile coordinatorProfile;

        public ProfileInfo(String totalProfileTime, String parsingTime, String pipelineCreationTime, List<String> warnings,
                List<IteratorProfile> iteratorProfiles, List<ResultProcessorProfile> resultProcessorProfiles,
                CoordinatorProfile coordinatorProfile) {
            this.totalProfileTime = totalProfileTime;
            this.parsingTime = parsingTime;
            this.pipelineCreationTime = pipelineCreationTime;
            this.warnings = warnings;
            this.iteratorProfiles = iteratorProfiles;
            this.resultProcessorProfiles = resultProcessorProfiles;
            this.coordinatorProfile = coordinatorProfile;
        }

        /**
         * Get the total profile time in milliseconds.
         *
         * @return total profile time
         */
        public String getTotalProfileTime() {
            return totalProfileTime;
        }

        /**
         * Get the parsing time in milliseconds.
         *
         * @return parsing time
         */
        public String getParsingTime() {
            return parsingTime;
        }

        /**
         * Get the pipeline creation time in milliseconds.
         *
         * @return pipeline creation time
         */
        public String getPipelineCreationTime() {
            return pipelineCreationTime;
        }

        /**
         * Get any warnings that occurred during query execution.
         *
         * @return list of warnings
         */
        public List<String> getWarnings() {
            return warnings;
        }

        /**
         * Get the iterator profiles.
         *
         * @return list of iterator profiles
         */
        public List<IteratorProfile> getIteratorProfiles() {
            return iteratorProfiles;
        }

        /**
         * Get the result processor profiles.
         *
         * @return list of result processor profiles
         */
        public List<ResultProcessorProfile> getResultProcessorProfiles() {
            return resultProcessorProfiles;
        }

        /**
         * Get the coordinator profile (only present in multi-shard environments).
         *
         * @return coordinator profile, or null if not present
         */
        public CoordinatorProfile getCoordinatorProfile() {
            return coordinatorProfile;
        }

        /**
         * Check if there are any warnings.
         *
         * @return true if warnings exist
         */
        public boolean hasWarnings() {
            return warnings != null && !warnings.isEmpty();
        }

        /**
         * Check if coordinator profile is present.
         *
         * @return true if coordinator profile exists
         */
        public boolean hasCoordinatorProfile() {
            return coordinatorProfile != null;
        }

    }

    /**
     * Represents an iterator profile with execution details.
     */
    public static class IteratorProfile {

        private final String type;

        private final String queryType;

        private final String term;

        private final String time;

        private final Long counter;

        private final Long size;

        private final List<IteratorProfile> childIterators;

        public IteratorProfile(String type, String queryType, String term, String time, Long counter, Long size,
                List<IteratorProfile> childIterators) {
            this.type = type;
            this.queryType = queryType;
            this.term = term;
            this.time = time;
            this.counter = counter;
            this.size = size;
            this.childIterators = childIterators;
        }

        /**
         * Get the iterator type (e.g., INTERSECT, UNION, TEXT, TAG, etc.).
         *
         * @return iterator type
         */
        public String getType() {
            return type;
        }

        /**
         * Get the query type (when applicable).
         *
         * @return query type
         */
        public String getQueryType() {
            return queryType;
        }

        /**
         * Get the term (when applicable).
         *
         * @return term
         */
        public String getTerm() {
            return term;
        }

        /**
         * Get the execution time in milliseconds.
         *
         * @return execution time
         */
        public String getTime() {
            return time;
        }

        /**
         * Get the counter (number of interactions).
         *
         * @return counter
         */
        public Long getCounter() {
            return counter;
        }

        /**
         * Get the size of the document set.
         *
         * @return size
         */
        public Long getSize() {
            return size;
        }

        /**
         * Get child iterators.
         *
         * @return list of child iterators
         */
        public List<IteratorProfile> getChildIterators() {
            return childIterators;
        }

        /**
         * Check if this iterator has child iterators.
         *
         * @return true if child iterators exist
         */
        public boolean hasChildIterators() {
            return childIterators != null && !childIterators.isEmpty();
        }

    }

    /**
     * Represents a result processor profile with execution details.
     */
    public static class ResultProcessorProfile {

        private final String type;

        private final String time;

        private final Long counter;

        public ResultProcessorProfile(String type, String time, Long counter) {
            this.type = type;
            this.time = time;
            this.counter = counter;
        }

        /**
         * Get the processor type (e.g., Index, Scorer, Sorter, Loader, etc.).
         *
         * @return processor type
         */
        public String getType() {
            return type;
        }

        /**
         * Get the execution time in milliseconds.
         *
         * @return execution time
         */
        public String getTime() {
            return time;
        }

        /**
         * Get the counter (number of invocations).
         *
         * @return counter
         */
        public Long getCounter() {
            return counter;
        }

    }

    /**
     * Represents coordinator profile information (multi-shard environments only).
     */
    public static class CoordinatorProfile {

        private final String totalCoordinatorTime;

        private final String postProcessingTime;

        public CoordinatorProfile(String totalCoordinatorTime, String postProcessingTime) {
            this.totalCoordinatorTime = totalCoordinatorTime;
            this.postProcessingTime = postProcessingTime;
        }

        /**
         * Get the total coordinator time in milliseconds.
         *
         * @return total coordinator time
         */
        public String getTotalCoordinatorTime() {
            return totalCoordinatorTime;
        }

        /**
         * Get the post-processing time in milliseconds.
         *
         * @return post-processing time
         */
        public String getPostProcessingTime() {
            return postProcessingTime;
        }

    }

}
