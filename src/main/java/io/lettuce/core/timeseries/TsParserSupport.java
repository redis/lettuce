/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.LettuceStrings;
import io.lettuce.core.output.ComplexData;

/**
 * Decoding helpers shared by the TimeSeries reply parsers. Values come from an {@code EncodedComplexOutput}, so strings arrive
 * as {@link ByteBuffer}, integers as {@link Long}, RESP3 doubles as {@link Double} and aggregates as {@link ComplexData}.
 */
final class TsParserSupport {

    private TsParserSupport() {
    }

    /**
     * Decode a string value as UTF-8.
     *
     * @return the string, or {@code null} for a nil value.
     */
    static String string(Object value) {

        if (value == null) {
            return null;
        }

        if (value instanceof ByteBuffer) {
            return StandardCharsets.UTF_8.decode(((ByteBuffer) value).duplicate()).toString();
        }

        return value.toString();
    }

    /**
     * Decode a key with the connection codec.
     *
     * @return the key, or {@code null} for a nil value.
     */
    static <K> K key(RedisCodec<K, ?> codec, Object value) {

        if (value == null) {
            return null;
        }

        return codec.decodeKey(((ByteBuffer) value).duplicate());
    }

    /**
     * Decode an integer value.
     *
     * @return the value, or {@code null} for a nil or absent value.
     */
    static Long longValue(Object value) {

        if (value == null) {
            return null;
        }

        if (value instanceof Number) {
            return ((Number) value).longValue();
        }

        return Long.parseLong(string(value));
    }

    /**
     * Decode an integer value, using {@code 0} for a nil or absent value.
     */
    static long longOrZero(Object value) {

        Long result = longValue(value);
        return result == null ? 0 : result;
    }

    /**
     * Decode a double: a RESP3 double, or a RESP2 string such as {@code 4.5}, {@code NaN}, {@code Infinity}, {@code -Infinity},
     * {@code inf} or {@code nan}.
     *
     * @return the value, or {@code null} for a nil or absent value.
     */
    static Double doubleValue(Object value) {

        if (value == null) {
            return null;
        }

        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }

        return LettuceStrings.toDouble(string(value));
    }

    /**
     * Decode labels: a RESP3 map, or a RESP2 array of {@code [name, value]} pairs.
     *
     * @return the labels in server order, never {@code null}; empty for a nil value.
     */
    static Map<String, String> labels(Object value) {

        Map<String, String> labels = new LinkedHashMap<>();

        if (!(value instanceof ComplexData)) {
            return labels;
        }

        ComplexData data = (ComplexData) value;
        if (data.isMap()) {
            data.getDynamicMap().forEach((name, labelValue) -> labels.put(string(name), string(labelValue)));
            return labels;
        }

        for (Object pair : data.getDynamicList()) {
            List<Object> nameAndValue = ((ComplexData) pair).getDynamicList();
            labels.put(string(nameAndValue.get(0)), nameAndValue.size() > 1 ? string(nameAndValue.get(1)) : null);
        }

        return labels;
    }

    /**
     * Decode the fields of a RESP3 map or a RESP2 flat name/value array, with UTF-8 field names.
     */
    static Map<String, Object> fields(ComplexData data) {

        Map<String, Object> fields = new LinkedHashMap<>();
        data.getDynamicMap().forEach((name, value) -> fields.put(string(name), value));
        return fields;
    }

    /**
     * Convert a reply value into plain Java values: strings as UTF-8 {@link String}, aggregates as unmodifiable {@link List},
     * {@link Map} or {@link Set}; numbers, booleans and {@code null} unchanged.
     */
    static Object plain(Object value) {

        if (value instanceof ByteBuffer) {
            return string(value);
        }

        if (!(value instanceof ComplexData)) {
            return value;
        }

        ComplexData data = (ComplexData) value;

        if (data.isMap()) {
            Map<Object, Object> map = new LinkedHashMap<>();
            data.getDynamicMap().forEach((k, v) -> map.put(plain(k), plain(v)));
            return Collections.unmodifiableMap(map);
        }

        if (data.isSet()) {
            Set<Object> set = new LinkedHashSet<>();
            data.getDynamicSet().forEach(element -> set.add(plain(element)));
            return Collections.unmodifiableSet(set);
        }

        List<Object> list = new ArrayList<>();
        data.getDynamicList().forEach(element -> list.add(plain(element)));
        return Collections.unmodifiableList(list);
    }

}
