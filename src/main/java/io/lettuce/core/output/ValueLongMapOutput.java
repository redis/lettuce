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

import java.util.HashMap;
import java.util.Map;

public class ValueLongMapOutput<K, V> extends CommandOutput<K, V, Map<V, Long>> {

    public ValueLongMapOutput(RedisCodec<K, V> codec) {
        super(codec, new HashMap<>());
    }

}
