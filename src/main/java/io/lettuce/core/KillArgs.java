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

import static io.lettuce.core.protocol.CommandKeyword.ADDR;
import static io.lettuce.core.protocol.CommandKeyword.ID;
import static io.lettuce.core.protocol.CommandKeyword.SKIPME;
import static io.lettuce.core.protocol.CommandType.TYPE;

import io.lettuce.core.protocol.CommandArgs;

/**
 *
 * Argument list builder for the redis <a href="http://redis.io/commands/client-kill">CLIENT KILL</a> command. Static import the
 * methods from {@link Builder} and chain the method calls: {@code id(1).skipme()}.
 *
 * @author Mark Paluch
 * @since 3.0
 */
public class KillArgs implements CompositeArgument {

    private static enum Type {
        NORMAL, SLAVE, PUBSUB
    }

    private Boolean skipme;
    private String addr;
    private Long id;
    private Type type;

    /**
     * Static builder methods.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {

        }

        public static KillArgs skipme() {
            return new KillArgs().skipme();
        }

        public static KillArgs addr(String addr) {
            return new KillArgs().addr(addr);
        }

        public static KillArgs id(long id) {
            return new KillArgs().id(id);
        }

        public static KillArgs typePubsub() {
            return new KillArgs().type(Type.PUBSUB);
        }

        public static KillArgs typeNormal() {
            return new KillArgs().type(Type.NORMAL);
        }

        public static KillArgs typeSlave() {
            return new KillArgs().type(Type.SLAVE);
        }

    }

    public KillArgs skipme() {
        return this.skipme(true);
    }

    public KillArgs skipme(boolean state) {
        this.skipme = state;
        return this;
    }

    public KillArgs addr(String addr) {
        this.addr = addr;
        return this;
    }

    public KillArgs id(long id) {
        this.id = id;
        return this;
    }

    public KillArgs type(Type type) {
        this.type = type;
        return this;
    }

    public <K, V> void build(CommandArgs<K, V> args) {

        if (skipme != null) {
            args.add(SKIPME).add(skipme.booleanValue() ? "yes" : "no");
        }

        if (id != null) {
            args.add(ID).add(id);
        }

        if (addr != null) {
            args.add(ADDR).add(addr);
        }

        if (type != null) {
            args.add(TYPE).add(type.name().toLowerCase());
        }

    }
}
