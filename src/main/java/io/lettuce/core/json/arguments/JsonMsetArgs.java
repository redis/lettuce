/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.json.arguments;

import io.lettuce.core.CompositeArgument;
import io.lettuce.core.json.JsonValue;
import io.lettuce.core.json.JsonPath;
import io.lettuce.core.protocol.CommandArgs;

/**
 * Argument list builder for the Redis <a href="https://redis.io/docs/latest/commands/json.mset/">JSON.MSET</a> command.
 * <p>
 *
 * @author Tihomir Mateev
 * @since 6.5
 */
public class JsonMsetArgs<K, V> implements CompositeArgument {

    private final K key;

    private final JsonPath path;

    private final JsonValue<V> element;

    /**
     * Creates a new {@link JsonMsetArgs} given a {@code key}, {@code path} and {@code element}.
     *
     * @param key the key to set the value for
     * @param path the path to set the value for
     * @param element the value to set
     */
    public JsonMsetArgs(K key, JsonPath path, JsonValue<V> element) {
        this.key = key;
        this.path = path;
        this.element = element;
    }

    /**
     * Return the key associated with this {@link JsonMsetArgs}.
     */
    public K getKey() {
        return key;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <K, V> void build(CommandArgs<K, V> args) {

        if (key != null) {
            args.addKey((K) key);
        }

        if (path != null) {
            args.add(path.toString());
        }

        if (element != null) {
            args.add(element.asByteBuffer().array());
        }
    }

}
