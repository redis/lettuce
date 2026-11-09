/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

/**
 * Lenient lookups of the TimeSeries value types, for names that come from the server. Unlike the public {@code of(String)}
 * factories, they accept any name, so a reply value with a comma or a space can't fail decoding. A known name gives the enum
 * constant, any other name a raw value that is kept as given.
 *
 * @author Aleksandar Todorov
 * @since 8.0
 */
final class TsValueTypes {

    private TsValueTypes() {
    }

    /**
     * @return the aggregation named {@code name}, or {@code null} if {@code name} is {@code null} or empty.
     */
    static TsAggregation aggregation(String name) {

        if (name == null || name.isEmpty()) {
            return null;
        }

        for (TsAggregationType type : TsAggregationType.values()) {
            if (type.getToken().equalsIgnoreCase(name)) {
                return type;
            }
        }

        return new RawTsAggregation(name);
    }

    /**
     * @return the encoding named {@code name}, or {@code null} if {@code name} is {@code null} or empty.
     */
    static TsEncoding encoding(String name) {

        if (name == null || name.isEmpty()) {
            return null;
        }

        for (TsEncodingType type : TsEncodingType.values()) {
            if (type.getToken().equalsIgnoreCase(name)) {
                return type;
            }
        }

        return new RawTsEncoding(name);
    }

    /**
     * @return the duplicate policy named {@code name}, or {@code null} if {@code name} is {@code null} or empty.
     */
    static TsDuplicatePolicy duplicatePolicy(String name) {

        if (name == null || name.isEmpty()) {
            return null;
        }

        for (TsDuplicatePolicyType type : TsDuplicatePolicyType.values()) {
            if (type.getToken().equalsIgnoreCase(name)) {
                return type;
            }
        }

        return new RawTsDuplicatePolicy(name);
    }

}
