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

package io.lettuce.core.json;

import io.lettuce.core.CompositeArgument;
import io.lettuce.core.protocol.CommandArgs;

/**
 * Argument list builder for the Redis <a href="https://redis.io/docs/latest/commands/json.mset/">JSON.MSET</a> command.
 * <p>
 * {@link JsonMsetArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author Tihomir Mateev
 * @since 6.5
 */
public class JsonMsetArgs implements CompositeArgument {

        private String key;
        private JsonPath path;
        private JsonElement element;

        /**
         * Builder entry points for {@link io.lettuce.core.json.JsonGetArgs}.
         */
        public static class Builder {

            /**
             * Utility constructor.
             */
            private Builder() {
            }

            /**
             * Creates new {@link io.lettuce.core.json.JsonGetArgs} and sets the string used for indentation.
             *
             * @return new {@link io.lettuce.core.json.JsonGetArgs} with indentation set.
             */
            public static JsonMsetArgs key(String indent) {
                return new JsonMsetArgs().key(indent);
            }

            /**
             * Creates new {@link io.lettuce.core.json.JsonGetArgs} and sets the string used for newline.
             *
             * @return new {@link io.lettuce.core.json.JsonGetArgs} with newline set.
             */
            public static JsonMsetArgs path(JsonPath path) {
                return new JsonMsetArgs().path(path);
            }

            /**
             * Creates new {@link io.lettuce.core.json.JsonGetArgs} and sets the string used for spacing.
             *
             * @return new {@link io.lettuce.core.json.JsonGetArgs} with spacing set.
             */
            public static JsonMsetArgs element(JsonElement element) {
                return new JsonMsetArgs().element(element);
            }

        }

        /**
         * Set the string used for indentation.
         *
         * @return {@code this}.
         */
        public JsonMsetArgs key(String key) {

            this.key = key;
            return this;
        }

        /**
         * Set the string used for newline.
         *
         * @return {@code this}.
         */
        public JsonMsetArgs path(JsonPath path) {

            this.path = path;
            return this;
        }

        /**
         * Set the string used for spacing.
         *
         * @return {@code this}.
         */
        public JsonMsetArgs element(JsonElement element) {

            this.element = element;
            return this;
        }


        @Override
        public <K, V> void build(CommandArgs<K, V> args) {

            if (key != null) {
                args.add(key);
            }

            if (path != null) {
                args.add(path.toString());
            }

            if (element != null) {
                args.add(element.toString());
            }
        }
}
