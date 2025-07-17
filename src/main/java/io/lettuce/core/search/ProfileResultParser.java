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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.output.ComplexData;
import io.lettuce.core.output.ComplexDataParser;

/**
 * Parser for FT.PROFILE command results that handles both RESP2 and RESP3 protocol responses.
 *
 * <p>
 * This parser automatically detects the Redis protocol version and switches between RESP2 and RESP3 parsing strategies:
 * </p>
 * <ul>
 * <li><strong>RESP2:</strong> Uses array-based parsing with key-value pairs</li>
 * <li><strong>RESP3:</strong> Uses map-based parsing with native map structures</li>
 * </ul>
 *
 * @param <V> Value type.
 * @author Tihomir Mateev
 * @since 6.8
 */
public class ProfileResultParser<V> implements ComplexDataParser<ProfileResult> {

    /**
     * Parse the FT.PROFILE response data, automatically detecting RESP2 vs RESP3 format.
     *
     * @param data the response data from Redis
     * @return parsed ProfileResult
     * @throws IllegalArgumentException if the data format is invalid
     */
    public ProfileResult parse(ComplexData data) {
        LettuceAssert.notNull(data, "Failed while parsing FT.PROFILE: data must not be null");

        try {
            try {
                // Try RESP2 parsing first (array-based)
                return parseResp2(data);
            } catch (UnsupportedOperationException e) {
                // Automatically switch to RESP3 parsing if we encounter a ComplexData type different than an array
                return parseResp3(data);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed while parsing FT.PROFILE: " + e.getMessage(), e);
        }
    }

    /**
     * Parse FT.PROFILE response in RESP2 format (array-based).
     */
    private ProfileResult parseResp2(ComplexData data) {
        List<Object> mainArray = data.getDynamicList();
        if (mainArray.size() != 2) {
            throw new IllegalArgumentException("FT.PROFILE response must have exactly 2 elements");
        }

        // First element: search results
        Object searchResults = mainArray.get(0);

        // Second element: profile information
        Object profileData = mainArray.get(1);
        ProfileResult.ProfileInfo profileInfo = parseProfileInfoResp2(profileData);

        return new ProfileResult(searchResults, profileInfo);
    }

    /**
     * Parse FT.PROFILE response in RESP3 format (map-based).
     */
    private ProfileResult parseResp3(ComplexData data) {
        Map<Object, Object> mainMap = data.getDynamicMap();
        if (mainMap.size() != 2) {
            throw new IllegalArgumentException("FT.PROFILE response must have exactly 2 elements");
        }

        // First element: search results (under "Results" key)
        Object searchResults = mainMap.get("Results");

        // Second element: profile information (under "Profile" key)
        Object profileData = mainMap.get("Profile");
        ProfileResult.ProfileInfo profileInfo = parseProfileInfoResp3(profileData);

        return new ProfileResult(searchResults, profileInfo);
    }

    /**
     * Parse profile information in RESP2 format (array-based with key-value pairs).
     */
    private ProfileResult.ProfileInfo parseProfileInfoResp2(Object profileData) {
        if (!(profileData instanceof ComplexData)) {
            throw new IllegalArgumentException("Profile data must be ComplexData");
        }

        List<Object> profileArray = ((ComplexData) profileData).getDynamicList();

        String totalProfileTime = null;
        String parsingTime = null;
        String pipelineCreationTime = null;
        List<String> warnings = new ArrayList<>();
        List<ProfileResult.IteratorProfile> iteratorProfiles = new ArrayList<>();
        List<ProfileResult.ResultProcessorProfile> resultProcessorProfiles = new ArrayList<>();
        ProfileResult.CoordinatorProfile coordinatorProfile = null;

        // RESP2: Parse profile data pairs (key-value format)
        // The structure is: ["Shards", [shard_data], "Coordinator", coordinator_data]
        for (int i = 0; i < profileArray.size(); i += 2) {
            if (i + 1 >= profileArray.size()) {
                break;
            }

            String key = profileArray.get(i).toString();
            Object value = profileArray.get(i + 1);

            switch (key) {
                case "Shards":
                    if (value instanceof ComplexData) {
                        List<Object> shardsList = ((ComplexData) value).getDynamicList();
                        if (!shardsList.isEmpty() && shardsList.get(0) instanceof ComplexData) {
                            // Parse the first shard's data
                            ProfileResult.ProfileInfo shardInfo = parseShardDataResp2(
                                    ((ComplexData) shardsList.get(0)).getDynamicList());
                            totalProfileTime = shardInfo.getTotalProfileTime();
                            parsingTime = shardInfo.getParsingTime();
                            pipelineCreationTime = shardInfo.getPipelineCreationTime();
                            warnings = shardInfo.getWarnings();
                            iteratorProfiles = shardInfo.getIteratorProfiles();
                            resultProcessorProfiles = shardInfo.getResultProcessorProfiles();
                        }
                    }
                    break;
                case "Coordinator":
                    if (value instanceof ComplexData) {
                        List<Object> coordinatorData = ((ComplexData) value).getDynamicList();
                        if (!coordinatorData.isEmpty()) {
                            coordinatorProfile = parseCoordinatorProfileResp2(coordinatorData);
                        }
                    }
                    break;
            }
        }

        return new ProfileResult.ProfileInfo(totalProfileTime, parsingTime, pipelineCreationTime, warnings, iteratorProfiles,
                resultProcessorProfiles, coordinatorProfile);
    }

    /**
     * Parse profile information in RESP3 format (map-based).
     */
    private ProfileResult.ProfileInfo parseProfileInfoResp3(Object profileData) {
        if (!(profileData instanceof ComplexData)) {
            throw new IllegalArgumentException("Profile data must be ComplexData");
        }

        Map<Object, Object> profileMap = ((ComplexData) profileData).getDynamicMap();

        String totalProfileTime = null;
        String parsingTime = null;
        String pipelineCreationTime = null;
        List<String> warnings = new ArrayList<>();
        List<ProfileResult.IteratorProfile> iteratorProfiles = new ArrayList<>();
        List<ProfileResult.ResultProcessorProfile> resultProcessorProfiles = new ArrayList<>();
        ProfileResult.CoordinatorProfile coordinatorProfile = null;

        // RESP3: Parse profile data from map structure
        Object shardsData = profileMap.get("Shards");
        if (shardsData instanceof ComplexData) {
            List<Object> shardsList = ((ComplexData) shardsData).getDynamicList();
            if (!shardsList.isEmpty() && shardsList.get(0) instanceof ComplexData) {
                // Parse the first shard's data
                ProfileResult.ProfileInfo shardInfo = parseShardDataResp3(((ComplexData) shardsList.get(0)).getDynamicMap());
                totalProfileTime = shardInfo.getTotalProfileTime();
                parsingTime = shardInfo.getParsingTime();
                pipelineCreationTime = shardInfo.getPipelineCreationTime();
                warnings = shardInfo.getWarnings();
                iteratorProfiles = shardInfo.getIteratorProfiles();
                resultProcessorProfiles = shardInfo.getResultProcessorProfiles();
            }
        }

        Object coordinatorData = profileMap.get("Coordinator");
        if (coordinatorData instanceof ComplexData) {
            Map<Object, Object> coordinatorMap = ((ComplexData) coordinatorData).getDynamicMap();
            if (!coordinatorMap.isEmpty()) {
                coordinatorProfile = parseCoordinatorProfileResp3(coordinatorMap);
            }
        }

        return new ProfileResult.ProfileInfo(totalProfileTime, parsingTime, pipelineCreationTime, warnings, iteratorProfiles,
                resultProcessorProfiles, coordinatorProfile);
    }

    /**
     * Parse shard data in RESP2 format (array-based with key-value pairs).
     */
    private ProfileResult.ProfileInfo parseShardDataResp2(List<Object> shardData) {
        String totalProfileTime = null;
        String parsingTime = null;
        String pipelineCreationTime = null;
        List<String> warnings = new ArrayList<>();
        List<ProfileResult.IteratorProfile> iteratorProfiles = new ArrayList<>();
        List<ProfileResult.ResultProcessorProfile> resultProcessorProfiles = new ArrayList<>();

        // Parse shard data pairs (key-value format)
        for (int i = 0; i < shardData.size(); i += 2) {
            if (i + 1 >= shardData.size()) {
                break;
            }

            String key = shardData.get(i).toString();
            Object value = shardData.get(i + 1);

            switch (key) {
                case "Total profile time":
                    totalProfileTime = value.toString();
                    break;
                case "Parsing time":
                    parsingTime = value.toString();
                    break;
                case "Pipeline creation time":
                    pipelineCreationTime = value.toString();
                    break;
                case "Warning":
                    if (value instanceof ComplexData) {
                        List<Object> warningList = ((ComplexData) value).getDynamicList();
                        for (Object warning : warningList) {
                            warnings.add(warning.toString());
                        }
                    } else if (!"None".equals(value.toString())) {
                        warnings.add(value.toString());
                    }
                    break;
                case "Iterators profile":
                    if (value instanceof ComplexData) {
                        iteratorProfiles.addAll(parseIteratorProfilesResp2(((ComplexData) value).getDynamicList()));
                    }
                    break;
                case "Result processors profile":
                    if (value instanceof ComplexData) {
                        resultProcessorProfiles
                                .addAll(parseResultProcessorProfilesResp2(((ComplexData) value).getDynamicList()));
                    }
                    break;
            }
        }

        return new ProfileResult.ProfileInfo(totalProfileTime, parsingTime, pipelineCreationTime, warnings, iteratorProfiles,
                resultProcessorProfiles, null);
    }

    /**
     * Parse shard data in RESP3 format (map-based).
     */
    private ProfileResult.ProfileInfo parseShardDataResp3(Map<Object, Object> shardMap) {
        String totalProfileTime = getString(shardMap.get("Total profile time"));
        String parsingTime = getString(shardMap.get("Parsing time"));
        String pipelineCreationTime = getString(shardMap.get("Pipeline creation time"));

        List<String> warnings = new ArrayList<>();
        Object warningData = shardMap.get("Warning");
        if (warningData instanceof ComplexData) {
            List<Object> warningList = ((ComplexData) warningData).getDynamicList();
            for (Object warning : warningList) {
                warnings.add(warning.toString());
            }
        } else if (warningData != null && !"None".equals(warningData.toString())) {
            warnings.add(warningData.toString());
        }

        List<ProfileResult.IteratorProfile> iteratorProfiles = new ArrayList<>();
        Object iteratorData = shardMap.get("Iterators profile");
        if (iteratorData instanceof ComplexData) {
            iteratorProfiles.addAll(parseIteratorProfilesResp3(((ComplexData) iteratorData).getDynamicMap()));
        }

        List<ProfileResult.ResultProcessorProfile> resultProcessorProfiles = new ArrayList<>();
        Object processorData = shardMap.get("Result processors profile");
        if (processorData instanceof ComplexData) {
            resultProcessorProfiles.addAll(parseResultProcessorProfilesResp3(((ComplexData) processorData).getDynamicList()));
        }

        return new ProfileResult.ProfileInfo(totalProfileTime, parsingTime, pipelineCreationTime, warnings, iteratorProfiles,
                resultProcessorProfiles, null);
    }

    private String getString(Object value) {
        return value != null ? value.toString() : null;
    }

    private List<ProfileResult.IteratorProfile> parseIteratorProfilesResp2(List<Object> iteratorData) {
        List<ProfileResult.IteratorProfile> profiles = new ArrayList<>();

        // Parse iterator profile data (key-value pairs)
        String type = null;
        String queryType = null;
        String term = null;
        String time = null;
        Long counter = null;
        Long size = null;
        List<ProfileResult.IteratorProfile> childIterators = new ArrayList<>();

        for (int i = 0; i < iteratorData.size(); i += 2) {
            if (i + 1 >= iteratorData.size()) {
                break;
            }

            String key = iteratorData.get(i).toString();
            Object value = iteratorData.get(i + 1);

            switch (key) {
                case "Type":
                    type = value.toString();
                    break;
                case "Query type":
                    queryType = value.toString();
                    break;
                case "Term":
                    term = value.toString();
                    break;
                case "Time":
                    time = value.toString();
                    break;
                case "Counter":
                    counter = parseLong(value);
                    break;
                case "Size":
                    size = parseLong(value);
                    break;
                case "Child iterators":
                    if (value instanceof ComplexData) {
                        List<Object> childData = ((ComplexData) value).getDynamicList();
                        for (Object child : childData) {
                            if (child instanceof ComplexData) {
                                childIterators.addAll(parseIteratorProfilesResp2(((ComplexData) child).getDynamicList()));
                            }
                        }
                    }
                    break;
            }
        }

        if (type != null) {
            profiles.add(new ProfileResult.IteratorProfile(type, queryType, term, time, counter, size, childIterators));
        }

        return profiles;
    }

    private List<ProfileResult.IteratorProfile> parseIteratorProfilesResp3(Map<Object, Object> iteratorMap) {
        List<ProfileResult.IteratorProfile> profiles = new ArrayList<>();

        String type = getString(iteratorMap.get("Type"));
        String queryType = getString(iteratorMap.get("Query type"));
        String term = getString(iteratorMap.get("Term"));
        String time = getString(iteratorMap.get("Time"));
        Long counter = parseLong(iteratorMap.get("Counter"));
        Long size = parseLong(iteratorMap.get("Size"));

        List<ProfileResult.IteratorProfile> childIterators = new ArrayList<>();
        Object childData = iteratorMap.get("Child iterators");
        if (childData instanceof ComplexData) {
            List<Object> childList = ((ComplexData) childData).getDynamicList();
            for (Object child : childList) {
                if (child instanceof ComplexData) {
                    childIterators.addAll(parseIteratorProfilesResp3(((ComplexData) child).getDynamicMap()));
                }
            }
        }

        if (type != null) {
            profiles.add(new ProfileResult.IteratorProfile(type, queryType, term, time, counter, size, childIterators));
        }

        return profiles;
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private List<ProfileResult.ResultProcessorProfile> parseResultProcessorProfilesResp2(List<Object> processorData) {
        List<ProfileResult.ResultProcessorProfile> profiles = new ArrayList<>();

        // Each processor is represented as a sub-array
        for (Object processor : processorData) {
            if (processor instanceof ComplexData) {
                List<Object> processorArray = ((ComplexData) processor).getDynamicList();

                String type = null;
                String time = null;
                Long counter = null;

                for (int i = 0; i < processorArray.size(); i += 2) {
                    if (i + 1 >= processorArray.size()) {
                        break;
                    }

                    String key = processorArray.get(i).toString();
                    Object value = processorArray.get(i + 1);

                    switch (key) {
                        case "Type":
                            type = value.toString();
                            break;
                        case "Time":
                            time = value.toString();
                            break;
                        case "Counter":
                            counter = parseLong(value);
                            break;
                    }
                }

                if (type != null) {
                    profiles.add(new ProfileResult.ResultProcessorProfile(type, time, counter));
                }
            }
        }

        return profiles;
    }

    private List<ProfileResult.ResultProcessorProfile> parseResultProcessorProfilesResp3(List<Object> processorData) {
        List<ProfileResult.ResultProcessorProfile> profiles = new ArrayList<>();

        // Each processor is represented as a map in RESP3
        for (Object processor : processorData) {
            if (processor instanceof ComplexData) {
                Map<Object, Object> processorMap = ((ComplexData) processor).getDynamicMap();

                String type = getString(processorMap.get("Type"));
                String time = getString(processorMap.get("Time"));
                Long counter = parseLong(processorMap.get("Counter"));

                if (type != null) {
                    profiles.add(new ProfileResult.ResultProcessorProfile(type, time, counter));
                }
            }
        }

        return profiles;
    }

    private ProfileResult.CoordinatorProfile parseCoordinatorProfileResp2(List<Object> coordinatorData) {
        String totalCoordinatorTime = null;
        String postProcessingTime = null;

        for (int i = 0; i < coordinatorData.size(); i += 2) {
            if (i + 1 >= coordinatorData.size()) {
                break;
            }

            String key = coordinatorData.get(i).toString();
            Object value = coordinatorData.get(i + 1);

            switch (key) {
                case "Total coordinator time":
                    totalCoordinatorTime = value.toString();
                    break;
                case "Post-processing time":
                    postProcessingTime = value.toString();
                    break;
            }
        }

        return new ProfileResult.CoordinatorProfile(totalCoordinatorTime, postProcessingTime);
    }

    private ProfileResult.CoordinatorProfile parseCoordinatorProfileResp3(Map<Object, Object> coordinatorMap) {
        String totalCoordinatorTime = getString(coordinatorMap.get("Total coordinator time"));
        String postProcessingTime = getString(coordinatorMap.get("Post-processing time"));

        return new ProfileResult.CoordinatorProfile(totalCoordinatorTime, postProcessingTime);
    }

}
