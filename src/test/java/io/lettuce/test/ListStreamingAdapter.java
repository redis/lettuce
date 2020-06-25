/*
 * Copyright 2018-2020 the original author or authors.
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
package io.lettuce.test;

import java.util.List;
import java.util.Vector;

import io.lettuce.core.ScoredValue;
import io.lettuce.core.output.KeyStreamingChannel;
import io.lettuce.core.output.ScoredValueStreamingChannel;
import io.lettuce.core.output.ValueStreamingChannel;

/**
 * Streaming adapter which stores every key or/and value in a list. This adapter can be used in KeyStreamingChannels and
 * ValueStreamingChannels.
 *
 * @author Mark Paluch
 * @param <T> Value-Type.
 * @since 3.0
 */
public class ListStreamingAdapter<T>
        implements KeyStreamingChannel<T>, ValueStreamingChannel<T>, ScoredValueStreamingChannel<T> {

    private final List<T> list = new Vector<>();

    @Override
    public void onKey(T key) {
        list.add(key);

    }

    @Override
    public void onValue(T value) {
        list.add(value);
    }

    public List<T> getList() {
        return list;
    }

    @Override
    public void onValue(ScoredValue<T> value) {
        list.add(value.getValue());
    }

}
