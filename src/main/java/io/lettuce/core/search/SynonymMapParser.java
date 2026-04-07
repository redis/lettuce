/*
 * Copyright 2011-2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.search;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.lettuce.core.codec.StringCodec;
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
 * @author Tihomir Mateev
 * @since 6.8
 */
public class SynonymMapParser implements ComplexDataParser<Map<String, List<String>>> {

    public SynonymMapParser() {
    }

    /**
     * Parse the FT.SYNDUMP response data, automatically detecting RESP2 vs RESP3 format.
     *
     * @param data the response data from Redis
     * @return a map where keys are terms and values are lists of synonym group IDs for each term
     */
    @Override
    public Map<String, List<String>> parse(ComplexData data) {

        if (data == null) {
            return new LinkedHashMap<>();
        }

        if (data.isList()) {
            return parseResp2(data);
        }

        return parseResp3(data);
    }

    /**
     * Parse FT.SYNDUMP response in RESP2 format (array-based with alternating key-value pairs).
     */
    private Map<String, List<String>> parseResp2(ComplexData data) {
        List<Object> synonymArray = data.getDynamicList();
        Map<String, List<String>> synonymMap = new LinkedHashMap<>();

        // RESP2: Parse alternating key-value pairs
        // Structure: [term1, [synonym1, synonym2], term2, [synonym3, synonym4], ...]
        for (int i = 0; i < synonymArray.size();) {
            if (i + 2 > synonymArray.size()) {
                break; // Incomplete pair, skip
            }

            // Decode the term (key)
            String term = StringCodec.UTF8.decodeValue((ByteBuffer) synonymArray.get(i++));

            // Decode the synonyms (value - should be a list)
            ComplexData synonymsData = (ComplexData) synonymArray.get(i++);
            List<Object> synonims = synonymsData.getDynamicList();

            List<String> decodedSynonyms = synonims.stream().map(synonym -> StringCodec.UTF8.decodeValue((ByteBuffer) synonym))
                    .collect(Collectors.toList());
            synonymMap.put(term, decodedSynonyms);
        }

        return synonymMap;
    }

    /**
     * Parse FT.SYNDUMP response in RESP3 format (map-based).
     */
    private Map<String, List<String>> parseResp3(ComplexData data) {
        Map<Object, Object> synonymMapRaw = data.getDynamicMap();
        Map<String, List<String>> synonymMap = new LinkedHashMap<>();

        // RESP3: Parse native map structure
        // Structure: {term1: [synonym1, synonym2], term2: [synonym3, synonym4], ...}
        for (Map.Entry<Object, Object> entry : synonymMapRaw.entrySet()) {
            // Decode the term (key)
            String term = StringCodec.UTF8.decodeValue((ByteBuffer) entry.getKey());

            // Decode the synonyms (value - should be a list)
            Object synonymsData = entry.getValue();

            List<Object> synonymsList = ((ComplexData) synonymsData).getDynamicList();
            List<String> synonyms = synonymsList.stream().map(synonym -> StringCodec.UTF8.decodeValue((ByteBuffer) synonym))
                    .collect(Collectors.toList());

            synonymMap.put(term, synonyms);
        }

        return synonymMap;
    }

}
