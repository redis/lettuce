/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.output;

import io.lettuce.core.GCRAResponse;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.List;

/**
 * Parser for Redis <a href="https://redis.io/commands/gcra">GCRA</a> command output.
 * <p>
 * The GCRA command returns an array of 5 values:
 * <ol>
 * <li>{@code limited} - integer 0 or 1 indicating if the request was rate-limited</li>
 * <li>{@code max_requests} - integer, the maximum number of requests allowed</li>
 * <li>{@code available_requests} - integer, the number of requests still available</li>
 * <li>{@code retry_after} - integer, seconds until the request should be retried (-1 if not limited)</li>
 * <li>{@code full_burst_after} - integer, seconds until full burst capacity is restored</li>
 * </ol>
 *
 * @author Aleksandar Todorov
 * @since 7.6
 */
public class GCRAResponseParser implements ComplexDataParser<GCRAResponse> {

    private static final InternalLogger LOG = InternalLoggerFactory.getInstance(GCRAResponseParser.class);

    public static final GCRAResponseParser INSTANCE = new GCRAResponseParser();

    private static final int EXPECTED_SIZE = 5;

    /**
     * Utility constructor.
     */
    private GCRAResponseParser() {
    }

    /**
     * Parse the output of the Redis GCRA command and convert it to a {@link GCRAResponse} object.
     *
     * @param data output of GCRA command
     * @return a {@link GCRAResponse} instance containing the parsed information
     */
    @Override
    public GCRAResponse parse(ComplexData data) {
        if (data == null) {
            return null;
        }

        List<Object> list;
        try {
            list = data.getDynamicList();
        } catch (UnsupportedOperationException e) {
            LOG.warn("Failed while parsing GCRA: unexpected data structure", e);
            return null;
        }

        if (list == null || list.isEmpty()) {
            LOG.warn("Failed while parsing GCRA: data must not be null or empty");
            return null;
        }

        if (list.size() != EXPECTED_SIZE) {
            LOG.warn("Failed while parsing GCRA: expected {} elements but got {}", EXPECTED_SIZE, list.size());
            return null;
        }

        GCRAResponse response = new GCRAResponse();
        response.setLimited(parseLong(list.get(0)) != 0);
        response.setMaxRequests(parseLong(list.get(1)));
        response.setAvailableRequests(parseLong(list.get(2)));
        response.setRetryAfter(parseLong(list.get(3)));
        response.setFullBurstAfter(parseLong(list.get(4)));

        return response;
    }

    private long parseLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        } else if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                LOG.warn("Failed to parse long value: {}", value);
                return 0;
            }
        }
        return 0;
    }

}
