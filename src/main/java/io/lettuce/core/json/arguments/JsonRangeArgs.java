/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
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

package io.lettuce.core.json.arguments;

import io.lettuce.core.CompositeArgument;
import io.lettuce.core.protocol.CommandArgs;

/**
 * Argument list builder for the RedisJSON commands that require ranges. By default, start and end indexes are set to 0.
 * Modifying these values might have different effects depending on the command they are supplied to.
 * <p>
 * {@link JsonRangeArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author Tihomir Mateev
 * @since 6.5
 * @see <a href="https://redis.io/docs/latest/commands/json.arrindex/">JSON.ARRINDEX</a>
 * @see <a href="https://redis.io/docs/latest/commands/json.arrtrim/">JSON.ARRTRIM</a>
 */
public class JsonRangeArgs implements CompositeArgument {

    private long start = 0;

    private long stop = 0;

    /**
     * Builder entry points for {@link JsonRangeArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates new {@link JsonRangeArgs} and sets the start index.
         *
         * @return new {@link JsonRangeArgs} with the start index set.
         */
        public static JsonRangeArgs start(long start) {
            return new JsonRangeArgs().start(start);
        }

        /**
         * Creates new {@link JsonRangeArgs} and sets the end index.
         *
         * @return new {@link JsonRangeArgs} with the end index set.
         */
        public static JsonRangeArgs stop(long stop) {
            return new JsonRangeArgs().stop(stop);
        }

    }

    /**
     * Set the start index.
     *
     * @return {@code this}.
     */
    public JsonRangeArgs start(long start) {

        this.start = start;
        return this;
    }

    /**
     * Set the end index.
     *
     * @return {@code this}.
     */
    public JsonRangeArgs stop(long stop) {

        this.stop = stop;
        return this;
    }

    @Override
    public <K, V> void build(CommandArgs<K, V> args) {

        if (start != 0) {
            args.add(start);
        }

        if (stop != 0) {
            args.add(stop);
        }
    }

}