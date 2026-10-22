/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import java.nio.charset.StandardCharsets;

/**
 * A {@link TsEncoding} with a name that is not a {@link TsEncodingType} constant. The name is sent to the server as given.
 */
final class RawTsEncoding implements TsEncoding {

    private final String name;

    private final byte[] bytes;

    RawTsEncoding(String name) {
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

        if (!(o instanceof RawTsEncoding)) {
            return false;
        }

        return name.equals(((RawTsEncoding) o).name);
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
