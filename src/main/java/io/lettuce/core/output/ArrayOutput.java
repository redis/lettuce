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

import io.lettuce.core.codec.RedisCodec;

/**
 * {@link java.util.List} of objects and lists to support dynamic nested structures (List with mixed content of values and
 * sublists).
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 */
public class ArrayOutput<K, V> extends NestedMultiOutput<K, V> {

    public ArrayOutput(RedisCodec<K, V> codec) {
        super(codec);
    }

}
