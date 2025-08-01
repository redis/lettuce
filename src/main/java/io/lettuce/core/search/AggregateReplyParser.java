/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.search;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.output.ComplexData;
import io.lettuce.core.output.ComplexDataParser;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.List;

/**
 * Parser for Redis FT.AGGREGATE command output.
 * <p>
 * This parser converts the response from the Redis FT.AGGREGATE command into a list of {@link SearchReply} objects. The
 * FT.AGGREGATE command returns an array where each element represents a separate aggregation result that can be parsed by the
 * {@link SearchReplyParser}.
 * <p>
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Tihomir Mateev
 * @since 6.8
 * @see SearchReplyParser
 * @see SearchReply
 */
public class AggregateReplyParser<K, V> implements ComplexDataParser<AggregationReply<K, V>> {

    private static final InternalLogger LOG = InternalLoggerFactory.getInstance(AggregateReplyParser.class);

    private final SearchReplyParser<K, V> searchReplyParser;

    private final boolean withCursor;

    public AggregateReplyParser(RedisCodec<K, V> codec, boolean withCursor) {
        this.searchReplyParser = new SearchReplyParser<>(codec);
        this.withCursor = withCursor;
    }

    /**
     * Parses the complex data from FT.AGGREGATE command into a list of SearchReply objects.
     * <p>
     * The method expects the data to be an array where each element is itself a complex data structure that can be parsed by
     * {@link SearchReplyParser}. If the input data is null, empty, or cannot be converted to a list, an empty list is returned.
     *
     * @param data the complex data from the FT.AGGREGATE command response
     * @return a list of SearchReply objects, one for each aggregation result
     */
    @Override
    public AggregationReply<K, V> parse(ComplexData data) {
        AggregationReply<K, V> reply = new AggregationReply<>();

        if (data == null) {
            return reply;
        }

        try {
            if (!withCursor) {
                SearchReply<K, V> searchReply = searchReplyParser.parse(data);
                reply.addSearchReply(searchReply);
                return reply;
            }

            List<Object> aggregateResults = data.getDynamicList();
            if (aggregateResults == null || aggregateResults.isEmpty()) {
                return reply;
            }

            boolean replyRead = false;

            for (Object aggregateResult : aggregateResults) {
                if (aggregateResult instanceof Number) {
                    if (replyRead) {
                        reply.setCursorId(((Number) aggregateResult).longValue());
                    } else {
                        reply.setGroupCount(((Number) aggregateResult).longValue());
                    }
                } else if (aggregateResult instanceof ComplexData) {
                    // Each element should be a ComplexData that can be parsed by SearchReplyParser
                    SearchReply<K, V> searchReply = searchReplyParser.parse((ComplexData) aggregateResult);
                    reply.addSearchReply(searchReply);
                    replyRead = true;
                }
            }

            return reply;

        } catch (Exception e) {
            LOG.warn("Error while parsing the result returned from Redis", e);
            return reply;
        }
    }

}
