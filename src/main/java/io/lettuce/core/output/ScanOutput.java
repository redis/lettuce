/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.output;

import java.nio.ByteBuffer;

import io.lettuce.core.LettuceStrings;
import io.lettuce.core.ScanCursor;
import io.lettuce.core.codec.RedisCodec;

/**
 * Cursor handling output.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @param <T> Cursor type.
 * @author Mark Paluch
 */
public abstract class ScanOutput<K, V, T extends ScanCursor> extends CommandOutput<K, V, T> {

    public ScanOutput(RedisCodec<K, V> codec, T cursor) {
        super(codec, cursor);
    }

    @Override
    public void set(ByteBuffer bytes) {

        if (output.getCursor() == null) {
            output.setCursor(decodeAscii(bytes));
            if (LettuceStrings.isNotEmpty(output.getCursor()) && "0".equals(output.getCursor())) {
                output.setFinished(true);
            }
            return;
        }

        setOutput(bytes);

    }

    protected abstract void setOutput(ByteBuffer bytes);

}
