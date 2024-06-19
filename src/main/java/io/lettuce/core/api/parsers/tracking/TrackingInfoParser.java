/*
 * Copyright 2024, Redis Ltd. and Contributors
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

package io.lettuce.core.api.parsers.tracking;

import io.lettuce.core.output.data.DynamicAggregateData;
import io.lettuce.core.protocol.CommandKeyword;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Parser for Redis <a href="https://redis.io/docs/latest/commands/client-trackinginfo/">CLIENT TRACKINGINFO</a> command output.
 *
 * @author Tihomir Mateev
 * @since 7.0
 */
public class TrackingInfoParser {

    /**
     * Utility constructor.
     */
    private TrackingInfoParser() {
    }

    /**
     * Parse the output of the Redis CLIENT TRACKINGINFO command and convert it to a {@link TrackingInfo}
     *
     * @param trackinginfoOutput output of CLIENT TRACKINGINFO command
     * @return an {@link TrackingInfo} instance
     */
    public static TrackingInfo parse(DynamicAggregateData trackinginfoOutput) {

        verifyStructure(trackinginfoOutput);

        Map<Object, Object> data = trackinginfoOutput.getDynamicMap();
        Set<?> flags = ((DynamicAggregateData) data.get(CommandKeyword.FLAGS.toString().toLowerCase())).getDynamicSet();
        Long clientId = (Long) data.get(CommandKeyword.REDIRECT.toString().toLowerCase());
        List<?> prefixes = ((DynamicAggregateData) data.get(CommandKeyword.PREFIXES.toString().toLowerCase())).getDynamicList();

        Set<TrackingInfo.TrackingFlag> parsedFlags = new HashSet<>();
        List<String> parsedPrefixes = new ArrayList<>();

        for (Object flag : flags) {
            String toParse = (String) flag;
            parsedFlags.add(TrackingInfo.TrackingFlag.from(toParse));
        }

        for (Object prefix : prefixes) {
            parsedPrefixes.add((String) prefix);
        }

        return new TrackingInfo(parsedFlags, clientId, parsedPrefixes);
    }

    private static void verifyStructure(DynamicAggregateData trackinginfoOutput) {

        if (trackinginfoOutput == null || trackinginfoOutput.getDynamicMap().isEmpty()) {
            throw new IllegalArgumentException("trackinginfoOutput must not be null or empty");
        }

        Map<Object, Object> data = trackinginfoOutput.getDynamicMap();

        if (!data.containsKey(CommandKeyword.FLAGS.toString().toLowerCase())
                || !data.containsKey(CommandKeyword.REDIRECT.toString().toLowerCase())
                || !data.containsKey(CommandKeyword.PREFIXES.toString().toLowerCase())) {
            throw new IllegalArgumentException("trackinginfoOutput has missing flags");
        }
    }

}
