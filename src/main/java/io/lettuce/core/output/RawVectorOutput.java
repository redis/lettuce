/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 */

package io.lettuce.core.output;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.vector.RawVector;

public class RawVectorOutput <K, V> extends CommandOutput<K, V, RawVector> {

    public RawVectorOutput(RedisCodec<K, V> codec)  {
        super(codec, new RawVector());
    }

}
