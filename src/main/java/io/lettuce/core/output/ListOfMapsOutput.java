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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.lettuce.core.codec.RedisCodec;

/**
 * {@link java.util.List} of maps output.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Will Glozer
 */
public class ListOfMapsOutput<K, V> extends CommandOutput<K, V, List<Map<K, V>>> {

    private MapOutput<K, V> nested;

    private int mapCount = -1;

    private final List<Integer> counts = new ArrayList<>();

    public ListOfMapsOutput(RedisCodec<K, V> codec) {
        super(codec, new ArrayList<>());
        nested = new MapOutput<>(codec);
    }

    @Override
    public void set(ByteBuffer bytes) {
        nested.set(bytes);
    }

    @Override
    public void complete(int depth) {

        if (!counts.isEmpty()) {
            int expectedSize = counts.get(0);

            if (nested.get().size() == expectedSize) {
                counts.remove(0);
                output.add(new LinkedHashMap<>(nested.get()));
                nested.get().clear();
            }
        }
    }

    @Override
    public void multi(int count) {

        nested.multi(count);

        if (mapCount == -1) {
            mapCount = count;
        } else {
            // div 2 because of key value pair counts twice
            counts.add(count / 2);
        }
    }

}
