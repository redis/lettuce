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
package io.lettuce.core.output;

/**
 * Streaming API for multiple keys and values (tuples). You can implement this interface in order to receive a call to
 * {@code onKeyValue} on every key-value.
 *
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 5.0
 */
@FunctionalInterface
public interface KeyValueStreamingChannel<K, V> extends StreamingChannel {

    /**
     * Called on every incoming key/value pair.
     *
     * @param key the key
     * @param value the value
     */
    void onKeyValue(K key, V value);
}
