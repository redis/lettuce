/*
 * Copyright 2011-2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.search;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.output.ComplexData;
import io.lettuce.core.output.ComplexDataParser;

/**
 * Parser for FT.SYNDUMP command results that handles both RESP2 and RESP3 protocol responses.
 * 
 * <p>
 * This parser automatically detects the Redis protocol version and switches between RESP2 and RESP3 parsing strategies.
 * </p>
 *
 * <p>
 * The result is a map where each key is a synonym term and each value is a list of group IDs that contain that synonym. This
 * structure properly represents the synonym relationships returned by Redis Search.
 * </p>
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Tihomir Mateev
 * @since 6.8
 */
public class SynonymMapParser<K, V> implements ComplexDataParser<Map<V, List<V>>> {

    private final RedisCodec<K, V> codec;

    public SynonymMapParser(RedisCodec<K, V> codec) {
        this.codec = codec;
    }

    /**
     * Parse the FT.SYNDUMP response data, automatically detecting RESP2 vs RESP3 format.
     *
     * @param data the response data from Redis
     * @return a map where keys are terms and values are lists of synonyms for each term
     */
    @Override
    public Map<V, List<V>> parse(ComplexData data) {

        if (data == null) {
            return new LinkedHashMap<>();
        }

        try {
            // Try RESP2 parsing first (array-based)
            return parseResp2(data);
        } catch (UnsupportedOperationException e) {
            // Automatically switch to RESP3 parsing if we encounter a ComplexData type different than an array
            return parseResp3(data);
        }
    }

    /**
     * Parse FT.SYNDUMP response in RESP2 format (array-based with alternating key-value pairs).
     */
    private Map<V, List<V>> parseResp2(ComplexData data) {
        List<Object> synonymArray = data.getDynamicList();
        Map<V, List<V>> synonymMap = new LinkedHashMap<>();

        // RESP2: Parse alternating key-value pairs
        // Structure: [term1, [synonym1, synonym2], term2, [synonym3, synonym4], ...]
        for (int i = 0; i < synonymArray.size();) {
            if (i + 2 > synonymArray.size()) {
                break; // Incomplete pair, skip
            }

            // Decode the term (key)
            V term = codec.decodeValue((ByteBuffer) synonymArray.get(i++));

            // Decode the synonyms (value - should be a list)
            ComplexData synonymsData = (ComplexData) synonymArray.get(i++);
            List<Object> synonims = synonymsData.getDynamicList();

            List<V> decodedSynonyms = synonims.stream().map(synonym -> codec.decodeValue((ByteBuffer) synonym))
                    .collect(Collectors.toList());
            synonymMap.put(term, decodedSynonyms);
        }

        return synonymMap;
    }

    /**
     * Parse FT.SYNDUMP response in RESP3 format (map-based).
     */
    private Map<V, List<V>> parseResp3(ComplexData data) {
        Map<Object, Object> synonymMapRaw = data.getDynamicMap();
        Map<V, List<V>> synonymMap = new LinkedHashMap<>();

        // RESP3: Parse native map structure
        // Structure: {term1: [synonym1, synonym2], term2: [synonym3, synonym4], ...}
        for (Map.Entry<Object, Object> entry : synonymMapRaw.entrySet()) {
            // Decode the term (key)
            V term = codec.decodeValue((ByteBuffer) entry.getKey());

            // Decode the synonyms (value - should be a list)
            List<V> synonyms = new ArrayList<>();
            Object synonymsData = entry.getValue();

            List<Object> synonymsList = ((ComplexData) synonymsData).getDynamicList();
            synonyms = synonymsList.stream().map(synonym -> codec.decodeValue((ByteBuffer) synonym))
                    .collect(Collectors.toList());

            synonymMap.put(term, synonyms);
        }

        return synonymMap;
    }

}
