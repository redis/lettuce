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

/**
 * Parser for Redis <a href="https://redis.io/commands/hotkeys">HOTKEYS GET</a> command output.
 *
 * @author Aleksandar Todorov
 * @since 7.3
 */
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

        Map<Object, Object> data = dynamicData.getDynamicMap();
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("Failed while parsing HOTKEYS GET: data must not be null or empty");
        }

        // Extract fields from the response
        boolean trackingActive = getLongValue(data, "tracking-active") == 1;
        int sampleRatio = getLongValue(data, "sample-ratio").intValue();

        List<Integer> selectedSlots = parseIntegerList(data.get("selected-slots"));

        Long sampledCommandSelectedSlotsMs = getLongValueOrNull(data, "sampled-command-selected-slots-ms");
        Long allCommandsSelectedSlotsMs = getLongValueOrNull(data, "all-commands-selected-slots-ms");
        Long allCommandsAllSlotsMs = getLongValueOrNull(data, "all-commands-all-slots-ms");

        Long netBytesSampledCommandsSelectedSlots = getLongValueOrNull(data, "net-bytes-sampled-commands-selected-slots");
        Long netBytesAllCommandsSelectedSlots = getLongValueOrNull(data, "net-bytes-all-commands-selected-slots");
        Long netBytesAllCommandsAllSlots = getLongValueOrNull(data, "net-bytes-all-commands-all-slots");

        Long collectionStartTimeUnixMs = getLongValueOrNull(data, "collection-start-time-unix-ms");
        Long collectionDurationMs = getLongValueOrNull(data, "collection-duration-ms");
        Long totalCpuTimeUserMs = getLongValueOrNull(data, "total-cpu-time-user-ms");
        Long totalCpuTimeSysMs = getLongValueOrNull(data, "total-cpu-time-sys-ms");
        Long totalNetBytes = getLongValueOrNull(data, "total-net-bytes");

        Map<String, Long> byCpuTime = parseKeyValueMap(data.get("by-cpu-time"));
        Map<String, Long> byNetBytes = parseKeyValueMap(data.get("by-net-bytes"));

        return new HotkeysReply(trackingActive, sampleRatio, selectedSlots, sampledCommandSelectedSlotsMs,
                allCommandsSelectedSlotsMs, allCommandsAllSlotsMs, netBytesSampledCommandsSelectedSlots,
                netBytesAllCommandsSelectedSlots, netBytesAllCommandsAllSlots, collectionStartTimeUnixMs, collectionDurationMs,
                totalCpuTimeUserMs, totalCpuTimeSysMs, totalNetBytes, byCpuTime, byNetBytes);
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

    private List<Integer> parseIntegerList(Object value) {
        if (value == null) {
            return new ArrayList<>();
        }

        if (value instanceof ComplexData) {
            List<?> list = ((ComplexData) value).getDynamicList();
            List<Integer> result = new ArrayList<>(list.size());
            for (Object item : list) {
                result.add(((Long) item).intValue());
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
