/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.output;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.lettuce.core.HotkeysReply;
import io.lettuce.core.Range;
import io.lettuce.core.annotations.Experimental;

/**
 * Parser for Redis <a href="https://redis.io/commands/hotkeys">HOTKEYS GET</a> command output.
 *
 * @author Aleksandar Todorov
 * @since 7.4
 */
@Experimental
public class HotkeysReplyParser implements ComplexDataParser<HotkeysReply> {

    public static final HotkeysReplyParser INSTANCE = new HotkeysReplyParser();

    /**
     * Utility constructor.
     */
    private HotkeysReplyParser() {
    }

    /**
     * Parse the output of the Redis HOTKEYS GET command and convert it to a {@link HotkeysReply}.
     *
     * @param dynamicData output of HOTKEYS GET command.
     * @return a {@link HotkeysReply} instance.
     */
    @Override
    public HotkeysReply parse(ComplexData dynamicData) {
        if (dynamicData == null) {
            return null;
        }

        Map<Object, Object> data = extractDataMap(dynamicData);
        if (data == null || data.isEmpty()) {
            // HOTKEYS GET returns nil (empty response) when hotkeys has never been started
            return null;
        }

        // Extract fields from the response
        boolean trackingActive = getLongValue(data, "tracking-active") == 1;
        int sampleRatio = getLongValue(data, "sample-ratio").intValue();

        List<Range<Integer>> selectedSlots = parseSlotRanges(data.get("selected-slots"));

        // CPU time fields use microseconds (us) suffix
        Long sampledCommandSelectedSlotsUs = getLongValueOrNull(data, "sampled-command-selected-slots-us");
        Long allCommandsSelectedSlotsUs = getLongValueOrNull(data, "all-commands-selected-slots-us");
        Long allCommandsAllSlotsUs = getLongValueOrNull(data, "all-commands-all-slots-us");

        Long netBytesSampledCommandsSelectedSlots = getLongValueOrNull(data, "net-bytes-sampled-commands-selected-slots");
        Long netBytesAllCommandsSelectedSlots = getLongValueOrNull(data, "net-bytes-all-commands-selected-slots");
        Long netBytesAllCommandsAllSlots = getLongValueOrNull(data, "net-bytes-all-commands-all-slots");

        Long collectionStartTimeUnixMs = getLongValueOrNull(data, "collection-start-time-unix-ms");
        Long collectionDurationMs = getLongValueOrNull(data, "collection-duration-ms");
        Long totalCpuTimeUserMs = getLongValueOrNull(data, "total-cpu-time-user-ms");
        Long totalCpuTimeSysMs = getLongValueOrNull(data, "total-cpu-time-sys-ms");
        Long totalNetBytes = getLongValueOrNull(data, "total-net-bytes");

        // by-cpu-time uses microseconds (us) suffix
        Map<String, Long> byCpuTimeUs = parseKeyValueMap(data.get("by-cpu-time-us"));
        Map<String, Long> byNetBytes = parseKeyValueMap(data.get("by-net-bytes"));

        return new HotkeysReply(trackingActive, sampleRatio, selectedSlots, sampledCommandSelectedSlotsUs,
                allCommandsSelectedSlotsUs, allCommandsAllSlotsUs, netBytesSampledCommandsSelectedSlots,
                netBytesAllCommandsSelectedSlots, netBytesAllCommandsAllSlots, collectionStartTimeUnixMs, collectionDurationMs,
                totalCpuTimeUserMs, totalCpuTimeSysMs, totalNetBytes, byCpuTimeUs, byNetBytes);
    }

    /**
     * Extract data map from ComplexData. Handles both RESP2 (array of key-value pairs) and RESP3 (map) formats. Also handles
     * the case where the response is wrapped in an outer array.
     */
    private Map<Object, Object> extractDataMap(ComplexData dynamicData) {
        // Try RESP3 map format first
        if (dynamicData.isMap()) {
            return dynamicData.getDynamicMap();
        }

        // RESP2 format: array of alternating key-value pairs
        if (dynamicData.isList()) {
            List<?> list = dynamicData.getDynamicList();
            if (list == null || list.isEmpty()) {
                return null;
            }

            // Check if the response is wrapped in an outer array (single element that is ComplexData)
            // This happens when Redis returns [[key1, val1, key2, val2, ...]]
            if (list.size() == 1 && list.get(0) instanceof ComplexData) {
                ComplexData innerData = (ComplexData) list.get(0);
                if (innerData.isMap()) {
                    return innerData.getDynamicMap();
                }
                if (innerData.isList()) {
                    list = innerData.getDynamicList();
                    if (list == null || list.isEmpty()) {
                        return null;
                    }
                }
            }

            Map<Object, Object> result = new LinkedHashMap<>();
            for (int i = 0; i < list.size(); i += 2) {
                if (i + 1 < list.size()) {
                    result.put(list.get(i), list.get(i + 1));
                }
            }
            return result;
        }

        return null;
    }

    private Long getLongValue(Map<Object, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Failed while parsing HOTKEYS GET: missing required field '" + key + "'");
        }
        return (Long) value;
    }

    private Long getLongValueOrNull(Map<Object, Object> data, String key) {
        Object value = data.get(key);
        return value != null ? (Long) value : null;
    }

    /**
     * Parse selected-slots which is an array of [start, end] ranges. Example: [[0, 16383]] means all slots.
     */
    private List<Range<Integer>> parseSlotRanges(Object value) {
        if (value == null) {
            return new ArrayList<>();
        }

        if (value instanceof ComplexData) {
            List<?> list = ((ComplexData) value).getDynamicList();
            List<Range<Integer>> result = new ArrayList<>(list.size());
            for (Object item : list) {
                if (item instanceof ComplexData) {
                    List<?> range = ((ComplexData) item).getDynamicList();
                    if (range.size() == 2) {
                        int start = ((Long) range.get(0)).intValue();
                        int end = ((Long) range.get(1)).intValue();
                        result.add(Range.create(start, end));
                    }
                }
            }
            return result;
        }

        return new ArrayList<>();
    }

    private Map<String, Long> parseKeyValueMap(Object value) {
        if (value == null) {
            return new LinkedHashMap<>();
        }

        if (value instanceof ComplexData) {
            ComplexData complexData = (ComplexData) value;
            Map<String, Long> result = new LinkedHashMap<>();

            // Handle both RESP2/RESP3 array format and RESP3 map format
            if (complexData.isMap()) {
                // RESP3 map format - direct map access
                Map<Object, Object> map = complexData.getDynamicMap();
                for (Map.Entry<Object, Object> entry : map.entrySet()) {
                    result.put((String) entry.getKey(), (Long) entry.getValue());
                }
            } else {
                // RESP2/RESP3 array format - alternating key-value pairs
                List<?> list = complexData.getDynamicList();
                for (int i = 0; i < list.size(); i += 2) {
                    String key = (String) list.get(i);
                    Long val = (Long) list.get(i + 1);
                    result.put(key, val);
                }
            }
            return result;
        }

        return new LinkedHashMap<>();
    }

}
