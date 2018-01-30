/*
 * Copyright 2011-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.pubsub;

import java.nio.ByteBuffer;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.protocol.CommandArgs;

/**
 *
 * Command args for Pub/Sub connections. This implementation hides the first key as PubSub keys are not keys from the key-space.
 *
 * @author Mark Paluch
 * @since 4.2
 */
class PubSubCommandArgs<K, V> extends CommandArgs<K, V> {

    /**
     * @param codec Codec used to encode/decode keys and values, must not be {@literal null}.
     */
    public PubSubCommandArgs(RedisCodec<K, V> codec) {
        super(codec);
    }

    /**
     *
     * @return always {@literal null}.
     */
    @Override
    public ByteBuffer getFirstEncodedKey() {
        return null;
    }
}
