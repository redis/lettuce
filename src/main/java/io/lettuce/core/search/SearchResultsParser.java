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
import io.lettuce.core.output.VectorMetadataParser;
import io.lettuce.core.search.arguments.SearchArgs;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SearchResultsParser<K, V> implements ComplexDataParser<SearchResults<K, V>> {

    private static final InternalLogger LOG = InternalLoggerFactory.getInstance(VectorMetadataParser.class);

    private final RedisCodec<K, V> codec;

    private final boolean withScores;

    private final boolean withContent;

    public SearchResultsParser(RedisCodec<K, V> codec, SearchArgs<K, V> args) {
        this.codec = codec;
        this.withScores = args != null && args.isWithScores();
        // this.withPayloads = args != null && args.isWithPayloads();
        // this.withSortKeys = args != null && args.isWithSortKeys();
        this.withContent = args == null || !args.isNoContent();
    }

    @Override
    public SearchResults<K, V> parse(ComplexData data) {
        try {
            data.getDynamicList();
            return new Resp2SearchResultsParser().parse(data);
        } catch (UnsupportedOperationException e) {
            return new Resp3SearchResultsParser().parse(data);
        }
    }

    class Resp2SearchResultsParser implements ComplexDataParser<SearchResults<K, V>> {

        @Override
        public SearchResults<K, V> parse(ComplexData data) {
            final SearchResults<K, V> searchResults = new SearchResults<>();

            final List<Object> resultsList = data.getDynamicList();

            if (resultsList == null || resultsList.isEmpty()) {
                return searchResults;
            }

            searchResults.setCount((Long) resultsList.get(0));

            if (resultsList.size() == 1) {
                return searchResults;
            }

            for (int i = 1; i < resultsList.size(); i++) {

                final K id = codec.decodeKey((ByteBuffer) resultsList.get(i));
                final SearchResults.SearchResult<K, V> searchResult = new SearchResults.SearchResult<>(id);

                if (withScores) {
                    searchResult.setScore(Double.parseDouble(StringCodec.UTF8.decodeKey((ByteBuffer) resultsList.get(i + 1))));
                    i++;
                }

                if (withContent) {
                    ComplexData resultData = (ComplexData) resultsList.get(i + 1);
                    List<Object> resultEntries = resultData.getDynamicList();

                    Map<K, V> resultEntriesProcessed = IntStream.range(0, resultEntries.size() / 2).boxed()
                            .collect(Collectors.toMap(idx -> codec.decodeKey((ByteBuffer) resultEntries.get(idx * 2)),
                                    idx -> codec.decodeValue((ByteBuffer) resultEntries.get(idx * 2 + 1))));

                    searchResult.addFields(resultEntriesProcessed);
                    i++;
                }

                searchResults.addResult(searchResult);
            }

            return searchResults;
        }

    }

    class Resp3SearchResultsParser implements ComplexDataParser<SearchResults<K, V>> {

        private final ByteBuffer ATTRIBUTES_KEY = StringCodec.UTF8.encodeKey("attributes");

        private final ByteBuffer FORMAT_KEY = StringCodec.UTF8.encodeKey("format");

        private final ByteBuffer RESULTS_KEY = StringCodec.UTF8.encodeKey("results");

        private final ByteBuffer TOTAL_RESULTS_KEY = StringCodec.UTF8.encodeKey("total_results");

        private final ByteBuffer WARNING_KEY = StringCodec.UTF8.encodeKey("warning");

        private final ByteBuffer SCORE_KEY = StringCodec.UTF8.encodeKey("score");

        private final ByteBuffer ID_KEY = StringCodec.UTF8.encodeKey("id");

        private final ByteBuffer EXTRA_ATTRIBUTES_KEY = StringCodec.UTF8.encodeKey("extra_attributes");

        private final ByteBuffer VALUES_KEY = StringCodec.UTF8.encodeKey("values");

        @Override
        public SearchResults<K, V> parse(ComplexData data) {
            final SearchResults<K, V> searchResults = new SearchResults<>();

            final Map<Object, Object> resultsMap = data.getDynamicMap();

            if (resultsMap == null || resultsMap.isEmpty()) {
                return searchResults;
            }

            // FIXME Parse attributes? ATTRIBUTES_KEY

            // FIXME Parse format? FORMAT_KEY

            if (resultsMap.containsKey(RESULTS_KEY)) {
                ComplexData results = (ComplexData) resultsMap.get(RESULTS_KEY);

                List<String> a = resultsMap.keySet().stream().map(o -> (ByteBuffer) o).map(StringCodec.UTF8::decodeKey)
                        .collect(Collectors.toList());

                results.getDynamicList().forEach(result -> {
                    ComplexData resultData = (ComplexData) result;
                    Map<Object, Object> resultEntry = resultData.getDynamicMap();

                    final K id = codec.decodeKey((ByteBuffer) resultEntry.get(ID_KEY));
                    final SearchResults.SearchResult<K, V> searchResult = new SearchResults.SearchResult<>(id);

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
                            K decodedKey = codec.decodeKey((ByteBuffer) key);
                            V decodedValue = codec.decodeValue((ByteBuffer) value);
                            searchResult.addFields(decodedKey, decodedValue);
                        });
                    }
                    searchResults.addResult(searchResult);
                });
            }

            if (resultsMap.containsKey(TOTAL_RESULTS_KEY)) {
                searchResults.setCount((Long) resultsMap.get(TOTAL_RESULTS_KEY));
            }

            if (resultsMap.containsKey(WARNING_KEY)) {
                ComplexData warning = (ComplexData) resultsMap.get(WARNING_KEY);
                warning.getDynamicList().forEach(warningEntry -> {
                    LOG.warn("Warning while parsing search results: {}", warningEntry);
                });
            }

            return searchResults;
        }

    }

}
