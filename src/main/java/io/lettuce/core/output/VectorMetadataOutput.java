/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 */

package io.lettuce.core.output;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.vector.VectorMetadata;

public class VectorMetadataOutput<K, V> extends CommandOutput<K, V, VectorMetadata> {

    public VectorMetadataOutput(RedisCodec<K, V> codec) {
        super(codec, new VectorMetadata());
    }

}
