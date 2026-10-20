/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries.arguments;

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.timeseries.TsEncoding;

/**
 * Base for the TimeSeries arguments of commands that can create a series. Adds the chunk encoding to the series options.
 *
 * @param <T> the concrete subtype, for fluent method chaining.
 * @since 8.0
 */
abstract class BaseTsCreateArgs<T extends BaseTsCreateArgs<T>> extends BaseTsSeriesArgs<T> {

    /**
     * Set the encoding of the data chunks of the series ({@code ENCODING}).
     *
     * @param encoding the chunk encoding, must not be {@code null}.
     * @return {@code this}.
     * @throws IllegalArgumentException if {@code encoding} is {@code null}.
     * @since 8.0
     */
    public T encoding(TsEncoding encoding) {

        LettuceAssert.notNull(encoding, "Encoding must not be null");

        this.encoding = encoding;
        return self();
    }

}
