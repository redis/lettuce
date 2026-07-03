/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.search;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.ComplexData;
import io.lettuce.core.output.ComplexDataParser;
import io.lettuce.core.search.arguments.SearchArgs;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

/**
 * Parser for Redis Search (RediSearch) command responses that converts raw Redis data into structured {@link SearchReply}
 * objects. This parser handles both RESP2 and RESP3 protocol responses and supports various search result formats including
 * results with scores, content, IDs, and cursor-based pagination.
 *
 * <p>
 * The parser automatically detects the Redis protocol version and switches between RESP2 and RESP3 parsing strategies. It
 * supports the following search result features:
 * </p>
 * <ul>
 * <li>Document IDs and content fields</li>
 * <li>Search scores when requested with WITHSCORES</li>
 * <li>Cursor-based pagination for large result sets</li>
 * <li>Warning messages from Redis</li>
 * <li>Total result counts</li>
 * </ul>
 *
 * <p>
 * Document ids are decoded through the connection's key codec. Field names are schema identifiers (hash field names, JSONPath
 * expressions or aliases) and are decoded as raw UTF-8; field values are kept as raw bytes so that binary content (for example
 * vector embeddings) survives the round-trip.
 * </p>
 *
 * @param <K> the type of the document id in the search results
 * @author Redis Ltd.
 * @since 6.8
 */
public class SearchReplyParser<K> implements ComplexDataParser<SearchReply<K>> {

    private static final InternalLogger LOG = InternalLoggerFactory.getInstance(SearchReplyParser.class);

    private final RedisCodec<K, ?> codec;

    private final boolean withScores;

    private final boolean withContent;

    private final boolean withIds;

    /**
     * Creates a new SearchReplyParser configured based on the provided search arguments. This constructor analyzes the search
     * arguments to determine which components of the search results should be parsed and included in the final
     * {@link SearchReply}.
     *
     * @param codec the Redis codec used for decoding document ids. Must not be {@code null}.
     * @param args the search arguments that determine parsing behavior. If {@code null}, default parsing behavior is used (with
     *        content, without scores, with IDs).
     *        <ul>
     *        <li>If {@code args.isWithScores()} is {@code true}, search scores will be parsed and included</li>
     *        <li>If {@code args.isNoContent()} is {@code true}, document content will be excluded from parsing</li>
     *        <li>Document IDs are always parsed when using this constructor</li>
     *        </ul>
     */
    public SearchReplyParser(RedisCodec<K, ?> codec, SearchArgs<K> args) {
        this.codec = codec;
        this.withScores = args != null && args.isWithScores();
        this.withContent = args == null || !args.isNoContent();
        this.withIds = true;
    }

    /**
     * Creates a new SearchReplyParser with default parsing configuration. This constructor is typically used for aggregation
     * results or other search operations where specific search arguments are not available.
     *
     * <p>
     * Default configuration:
     * </p>
     * <ul>
     * <li>Scores are not parsed ({@code withScores = false})</li>
     * <li>Content is parsed ({@code withContent = true})</li>
     * <li>IDs are not parsed ({@code withIds = false})</li>
     * </ul>
     *
     * @param codec the Redis codec used for decoding document ids. Must not be {@code null}.
     */
    public SearchReplyParser(RedisCodec<K, ?> codec) {
        this.codec = codec;
        this.withScores = false;
        this.withContent = true;
        this.withIds = false;
    }

    /**
     * Parses Redis Search command response data into a structured {@link SearchReply} object. This method automatically detects
     * the Redis protocol version (RESP2 or RESP3) and uses the appropriate parsing strategy.
     *
     * @param data the complex data structure returned by Redis containing the search results. Must not be {@code null}.
     * @return a {@link SearchReply} containing the parsed search results. Never {@code null}. Returns an empty
     *         {@link SearchReply} if parsing fails.
     */
    @Override
    public SearchReply<K> parse(ComplexData data) {
        try {
            if (data.isList()) {
                return new Resp2SearchResultsParser().parse(data);
            }

            return new Resp3SearchResultsParser().parse(data);
        } catch (Exception e) {
            LOG.warn("Unable to parse the result from Redis", e);
            return new SearchReply<>();
        }
    }

    private static byte[] toBytes(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.duplicate().get(bytes);
        return bytes;
    }

    class Resp2SearchResultsParser implements ComplexDataParser<SearchReply<K>> {

        @Override
        public SearchReply<K> parse(ComplexData data) {
            final SearchReply<K> searchReply = new SearchReply<>();

            final List<Object> resultsList = data.getDynamicList();

            if (resultsList == null || resultsList.isEmpty()) {
                return searchReply;
            }

            // Check if this is a cursor response (has 2 elements: results array and cursor id)
            if (resultsList.size() == 2 && resultsList.get(1) instanceof Long) {
                // This is a cursor response: [results_array, cursor_id]
                List<Object> actualResults = ((ComplexData) resultsList.get(0)).getDynamicList();
                Long cursorId = (Long) resultsList.get(1);

                searchReply.setCursorId(cursorId);

                if (actualResults == null || actualResults.isEmpty()) {
                    return searchReply;
                }

                searchReply.setCount((Long) actualResults.get(0));

                if (actualResults.size() == 1) {
                    return searchReply;
                }

                // Parse the actual results
                parseResults(searchReply, actualResults);
            } else {
                // Regular search response
                searchReply.setCount((Long) resultsList.get(0));

                if (resultsList.size() == 1) {
                    return searchReply;
                }

                // Parse the results
                parseResults(searchReply, resultsList);
            }

            return searchReply;
        }

        private void parseResults(SearchReply<K> searchReply, List<Object> resultsList) {
            for (int i = 1; i < resultsList.size();) {

                K id = codec.decodeKey(StringCodec.UTF8.encodeKey("0"));
                if (withIds) {
                    id = codec.decodeKey((ByteBuffer) resultsList.get(i));
                    i++;
                }

                final SearchReply.SearchResult<K> searchResult = new SearchReply.SearchResult<>(id);

                if (withScores) {
                    searchResult.setScore(Double.parseDouble(StringCodec.UTF8.decodeKey((ByteBuffer) resultsList.get(i))));
                    i++;
                }

                if (withContent) {
                    ComplexData resultData = (ComplexData) resultsList.get(i);
                    List<Object> resultEntries = resultData.getDynamicList();

                    for (int idx = 0; idx < resultEntries.size(); idx += 2) {
                        String fieldName = StringCodec.UTF8.decodeKey((ByteBuffer) resultEntries.get(idx));
                        Object value = resultEntries.get(idx + 1);
                        byte[] fieldValue = value == null ? null : toBytes((ByteBuffer) value);
                        searchResult.addField(fieldName, fieldValue);
                    }

                    i++;
                }

                searchReply.addResult(searchResult);
            }
        }

    }

    class Resp3SearchResultsParser implements ComplexDataParser<SearchReply<K>> {

        private final ByteBuffer ATTRIBUTES_KEY = StringCodec.UTF8.encodeKey("attributes");

        private final ByteBuffer FORMAT_KEY = StringCodec.UTF8.encodeKey("format");

        private final ByteBuffer RESULTS_KEY = StringCodec.UTF8.encodeKey("results");

        private final ByteBuffer TOTAL_RESULTS_KEY = StringCodec.UTF8.encodeKey("total_results");

        private final ByteBuffer WARNING_KEY = StringCodec.UTF8.encodeKey("warning");

        private final ByteBuffer SCORE_KEY = StringCodec.UTF8.encodeKey("score");

        private final ByteBuffer ID_KEY = StringCodec.UTF8.encodeKey("id");

        private final ByteBuffer EXTRA_ATTRIBUTES_KEY = StringCodec.UTF8.encodeKey("extra_attributes");

        private final ByteBuffer VALUES_KEY = StringCodec.UTF8.encodeKey("values");

        private final ByteBuffer CURSOR_KEY = StringCodec.UTF8.encodeKey("cursor");

        @Override
        public SearchReply<K> parse(ComplexData data) {
            final SearchReply<K> searchReply = new SearchReply<>();

            final Map<Object, Object> resultsMap = data.getDynamicMap();

            if (resultsMap == null || resultsMap.isEmpty()) {
                return searchReply;
            }

            // FIXME Parse attributes? ATTRIBUTES_KEY

            // FIXME Parse format? FORMAT_KEY

            if (resultsMap.containsKey(RESULTS_KEY)) {
                ComplexData results = (ComplexData) resultsMap.get(RESULTS_KEY);

                results.getDynamicList().forEach(result -> {
                    ComplexData resultData = (ComplexData) result;
                    Map<Object, Object> resultEntry = resultData.getDynamicMap();

                    SearchReply.SearchResult<K> searchResult;
                    if (resultEntry.containsKey(ID_KEY)) {
                        final K id = codec.decodeKey((ByteBuffer) resultEntry.get(ID_KEY));
                        searchResult = new SearchReply.SearchResult<>(id);
                    } else {
                        searchResult = new SearchReply.SearchResult<>();
                    }

                    if (resultEntry.containsKey(SCORE_KEY)) {
                        if (resultEntry.get(SCORE_KEY) instanceof Double) {
                            searchResult.setScore((Double) resultEntry.get(SCORE_KEY));
                        } else {
                            ComplexData scores = (ComplexData) resultEntry.get(SCORE_KEY);
                            List<Object> scoresList = scores.getDynamicList();
                            searchResult.setScore((Double) scoresList.get(0));
                        }
                    }

                    if (resultEntry.containsKey(EXTRA_ATTRIBUTES_KEY)) {
                        ComplexData extraAttributes = (ComplexData) resultEntry.get(EXTRA_ATTRIBUTES_KEY);
                        extraAttributes.getDynamicMap().forEach((key, value) -> {
                            String fieldName = StringCodec.UTF8.decodeKey((ByteBuffer) key);
                            byte[] fieldValue = value == null ? null : toBytes((ByteBuffer) value);
                            searchResult.addField(fieldName, fieldValue);
                        });
                    }
                    searchReply.addResult(searchResult);
                });
            }

            if (resultsMap.containsKey(TOTAL_RESULTS_KEY)) {
                searchReply.setCount((Long) resultsMap.get(TOTAL_RESULTS_KEY));
            }

            if (resultsMap.containsKey(CURSOR_KEY)) {
                searchReply.setCursorId((Long) resultsMap.get(CURSOR_KEY));
            }

            if (resultsMap.containsKey(WARNING_KEY)) {
                ComplexData warning = (ComplexData) resultsMap.get(WARNING_KEY);
                warning.getDynamicList().forEach(warningEntry -> {
                    searchReply.addWarning(StringCodec.UTF8.decodeValue((ByteBuffer) warningEntry));
                });
            }

            return searchReply;
        }

    }

}
