/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core;

import io.lettuce.core.codec.StringCodec;
import org.junit.jupiter.api.Tag;

import static io.lettuce.TestTags.UNIT_TEST;

/**
 * Unit tests for {@link RedisTopKCommandBuilder}.
 *
 * @author Yordan Tsintsov
 */
@Tag(UNIT_TEST)
public class RedisTopKCommandBuilderUnitTests {

    private final RedisTopKCommandBuilder<String, String> builder = new RedisTopKCommandBuilder<>(StringCodec.UTF8);

}
