/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.output;

import io.lettuce.core.TrackingInfo;
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
 * @since 6.5
 */
public class TrackingInfoParser implements ComplexDataParser<TrackingInfo> {

    public static final TrackingInfoParser INSTANCE = new TrackingInfoParser();

    /**
     * Utility constructor.
     */
    private TrackingInfoParser() {
    }

    /**
     * Parse the output of the Redis CLIENT TRACKINGINFO command and convert it to a {@link TrackingInfo}
     *
     * @param dynamicData output of CLIENT TRACKINGINFO command
     * @return an {@link TrackingInfo} instance
     */
    public TrackingInfo parse(ComplexData dynamicData) {
        Map<Object, Object> data = verifyStructure(dynamicData);
        Set<?> flags = ((ComplexData) data.get(CommandKeyword.FLAGS.toString().toLowerCase())).getDynamicSet();
        Long clientId = (Long) data.get(CommandKeyword.REDIRECT.toString().toLowerCase());
        List<?> prefixes = ((ComplexData) data.get(CommandKeyword.PREFIXES.toString().toLowerCase())).getDynamicList();

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

    private Map<Object, Object> verifyStructure(ComplexData trackinginfoOutput) {

        if (trackinginfoOutput == null) {
            throw new IllegalArgumentException("Failed while parsing CLIENT TRACKINGINFO: trackinginfoOutput must not be null");
        }

        Map<Object, Object> data = trackinginfoOutput.getDynamicMap();
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("Failed while parsing CLIENT TRACKINGINFO: data must not be null or empty");
        }

        if (!data.containsKey(CommandKeyword.FLAGS.toString().toLowerCase())
                || !data.containsKey(CommandKeyword.REDIRECT.toString().toLowerCase())
                || !data.containsKey(CommandKeyword.PREFIXES.toString().toLowerCase())) {
            throw new IllegalArgumentException(
                    "Failed while parsing CLIENT TRACKINGINFO: trackinginfoOutput has missing flags");
        }

        return data;
    }

}
