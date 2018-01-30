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
package io.lettuce.core;

import io.lettuce.core.protocol.CommandArgs;

/**
 * Argument list builder for the improved redis <a href="http://redis.io/commands/zadd">ZADD</a> command starting from Redis
 * 3.0.2. Static import the methods from {@link Builder} and call the methods: {@code xx()} or {@code nx()} .
 *
 * @author Mark Paluch
 */
public class ZAddArgs implements CompositeArgument {

    private boolean nx = false;
    private boolean xx = false;
    private boolean ch = false;

    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {

        }

        public static ZAddArgs nx() {
            return new ZAddArgs().nx();
        }

        public static ZAddArgs xx() {
            return new ZAddArgs().xx();
        }

        public static ZAddArgs ch() {
            return new ZAddArgs().ch();
        }
    }

    public ZAddArgs nx() {
        this.nx = true;
        return this;
    }

    public ZAddArgs ch() {
        this.ch = true;
        return this;
    }

    public ZAddArgs xx() {
        this.xx = true;
        return this;
    }

    public <K, V> void build(CommandArgs<K, V> args) {
        if (nx) {
            args.add("NX");
        }

        if (xx) {
            args.add("XX");
        }

        if (ch) {
            args.add("CH");
        }
    }
}
