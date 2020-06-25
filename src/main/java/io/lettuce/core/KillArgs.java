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
package io.lettuce.core;

import static io.lettuce.core.protocol.CommandKeyword.ADDR;
import static io.lettuce.core.protocol.CommandKeyword.ID;
import static io.lettuce.core.protocol.CommandKeyword.SKIPME;
import static io.lettuce.core.protocol.CommandType.TYPE;

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.CommandArgs;

/**
 * Argument list builder for the Redis <a href="http://redis.io/commands/client-kill">CLIENT KILL</a> command. Static import the
 * methods from {@link Builder} and chain the method calls: {@code id(1).skipme()}.
 * <p>
 * {@link KillArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author Mark Paluch
 * @since 3.0
 */
public class KillArgs implements CompositeArgument {

    private enum Type {
        NORMAL, MASTER, SLAVE, PUBSUB
    }

    private Boolean skipme;

    private String addr;

    private Long id;

    private Type type;

    /**
     * Builder entry points for {@link KillArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates new {@link KillArgs} and enabling {@literal SKIPME YES}.
         *
         * @return new {@link KillArgs} with {@literal SKIPME YES} enabled.
         * @see KillArgs#skipme()
         */
        public static KillArgs skipme() {
            return new KillArgs().skipme();
        }

        /**
         * Creates new {@link KillArgs} setting {@literal ADDR}.
         *
         * @param addr must not be {@code null}.
         * @return new {@link KillArgs} with {@literal ADDR} set.
         * @see KillArgs#addr(String)
         */
        public static KillArgs addr(String addr) {
            return new KillArgs().addr(addr);
        }

        /**
         * Creates new {@link KillArgs} setting {@literal ID}.
         *
         * @param id client id.
         * @return new {@link KillArgs} with {@literal ID} set.
         * @see KillArgs#id(long)
         */
        public static KillArgs id(long id) {
            return new KillArgs().id(id);
        }

        /**
         * Creates new {@link KillArgs} setting {@literal TYPE PUBSUB}.
         *
         * @return new {@link KillArgs} with {@literal TYPE PUBSUB} set.
         * @see KillArgs#type(Type)
         */
        public static KillArgs typePubsub() {
            return new KillArgs().type(Type.PUBSUB);
        }

        /**
         * Creates new {@link KillArgs} setting {@literal TYPE NORMAL}.
         *
         * @return new {@link KillArgs} with {@literal TYPE NORMAL} set.
         * @see KillArgs#type(Type)
         */
        public static KillArgs typeNormal() {
            return new KillArgs().type(Type.NORMAL);
        }

        /**
         * Creates new {@link KillArgs} setting {@literal TYPE MASTER}.
         *
         * @return new {@link KillArgs} with {@literal TYPE MASTER} set.
         * @see KillArgs#type(Type)
         * @since 5.0.4
         */
        public static KillArgs typeMaster() {
            return new KillArgs().type(Type.MASTER);
        }

        /**
         * Creates new {@link KillArgs} setting {@literal TYPE SLAVE}.
         *
         * @return new {@link KillArgs} with {@literal TYPE SLAVE} set.
         * @see KillArgs#type(Type)
         */
        public static KillArgs typeSlave() {
            return new KillArgs().type(Type.SLAVE);
        }

    }

    /**
     * By default this option is enabled, that is, the client calling the command will not get killed, however setting this
     * option to no will have the effect of also killing the client calling the command.
     *
     * @return {@code this} {@link MigrateArgs}.
     */
    public KillArgs skipme() {
        return this.skipme(true);
    }

    /**
     * By default this option is enabled, that is, the client calling the command will not get killed, however setting this
     * option to no will have the effect of also killing the client calling the command.
     *
     * @param state
     * @return {@code this} {@link KillArgs}.
     */
    public KillArgs skipme(boolean state) {

        this.skipme = state;
        return this;
    }

    /**
     * Kill the client at {@code addr}.
     *
     * @param addr must not be {@code null}.
     * @return {@code this} {@link KillArgs}.
     */
    public KillArgs addr(String addr) {

        LettuceAssert.notNull(addr, "Client address must not be null");

        this.addr = addr;
        return this;
    }

    /**
     * Kill the client with its client {@code id}.
     *
     * @param id
     * @return {@code this} {@link KillArgs}.
     */
    public KillArgs id(long id) {

        this.id = id;
        return this;
    }

    /**
     * This closes the connections of all the clients in the specified {@link Type class}. Note that clients blocked into the
     * {@literal MONITOR} command are considered to belong to the normal class.
     *
     * @param type must not be {@code null}.
     * @return {@code this} {@link KillArgs}.
     */
    public KillArgs type(Type type) {

        LettuceAssert.notNull(type, "Type must not be null");

        this.type = type;
        return this;
    }

    public <K, V> void build(CommandArgs<K, V> args) {

        if (skipme != null) {
            args.add(SKIPME).add(skipme ? "YES" : "NO");
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
