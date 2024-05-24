package io.lettuce.core.cluster.models.tracking;

import io.lettuce.core.protocol.CommandKeyword;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
    public static TrackingInfo parse(List<?> trackinginfoOutput) {

        verifyStructure(trackinginfoOutput);

        List<?> flags = (List<?>) trackinginfoOutput.get(1);
        Long clientId = (Long) trackinginfoOutput.get(3);
        List<?> prefixes = (List<?>) trackinginfoOutput.get(5);

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

    private static void verifyStructure(List<?> trackinginfoOutput) {

        if (trackinginfoOutput == null || trackinginfoOutput.isEmpty()) {
            throw new IllegalArgumentException("trackinginfoOutput must not be null or empty");
        }

        if (trackinginfoOutput.size() != 6) {
            throw new IllegalArgumentException("trackinginfoOutput has wrong number of elements");
        }

        if (!CommandKeyword.FLAGS.toString().equalsIgnoreCase(trackinginfoOutput.get(0).toString())
                || !CommandKeyword.REDIRECT.toString().equalsIgnoreCase(trackinginfoOutput.get(2).toString())
                || !CommandKeyword.PREFIXES.toString().equalsIgnoreCase(trackinginfoOutput.get(4).toString())) {
            throw new IllegalArgumentException("trackinginfoOutput has unsupported argument order");
        }

        if (!(trackinginfoOutput.get(1) instanceof List) || !(trackinginfoOutput.get(3) instanceof Long)
                || !(trackinginfoOutput.get(5) instanceof List)) {
            throw new IllegalArgumentException("trackinginfoOutput has unsupported argument format");
        }
    }

}
