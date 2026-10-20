/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import java.nio.charset.StandardCharsets;

/**
 * A {@link TsReducer} with a name that is not a {@link TsReducerType} constant. The name is sent to the server as given.
 */
final class RawTsReducer implements TsReducer {

    private final String name;

    private final byte[] bytes;

    RawTsReducer(String name) {
        this.name = name;
        this.bytes = name.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] getBytes() {
        return bytes;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (!(o instanceof RawTsReducer)) {
            return false;
        }

        return name.equals(((RawTsReducer) o).name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }

}
