/*
 * Copyright 2011-2022 the original author or authors.
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

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandType;

import java.util.ArrayList;
import java.util.List;

import static io.lettuce.core.protocol.CommandKeyword.ID;

/**
 *
 * Argument list builder for the Redis <a href="https://redis.io/commands/client-list">CLIENT LIST</a> command.
 * <p>
 * {@link ClientListArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author Mikhael Sokolov
 * @since 6.3
 */
public class ClientListArgs implements CompositeArgument {

    private enum Type {
        NORMAL, MASTER, SLAVE, PUBSUB
    }

    private List<Long> ids;

    private Type type;

    /**
     * Builder entry points for {@link ClientListArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates new {@link ClientListArgs} setting {@literal client-id}.
         *
         * @param id client ids.
         * @return new {@link ClientListArgs} with {@literal client-id} set.
         * @see ClientListArgs#ids(long...)
         */
        public static ClientListArgs ids(long... id) {
            return new ClientListArgs().ids(id);
        }

        /**
         * Creates new {@link ClientListArgs} setting {@literal TYPE PUBSUB}.
         *
         * @return new {@link ClientListArgs} with {@literal TYPE PUBSUB} set.
         * @see ClientListArgs#type(Type)
         */
        public static ClientListArgs typePubsub() {
            return new ClientListArgs().type(Type.PUBSUB);
        }

        /**
         * Creates new {@link ClientListArgs} setting {@literal TYPE NORMAL}.
         *
         * @return new {@link ClientListArgs} with {@literal TYPE NORMAL} set.
         * @see ClientListArgs#type(Type)
         */
        public static ClientListArgs typeNormal() {
            return new ClientListArgs().type(Type.NORMAL);
        }

        /**
         * Creates new {@link ClientListArgs} setting {@literal TYPE MASTER}.
         *
         * @return new {@link ClientListArgs} with {@literal TYPE MASTER} set.
         * @see ClientListArgs#type(Type)
         * @since 5.0.4
         */
        public static ClientListArgs typeMaster() {
            return new ClientListArgs().type(Type.MASTER);
        }

        /**
         * Creates new {@link ClientListArgs} setting {@literal TYPE SLAVE}.
         *
         * @return new {@link ClientListArgs} with {@literal TYPE SLAVE} set.
         * @see ClientListArgs#type(Type)
         */
        public static ClientListArgs typeSlave() {
            return new ClientListArgs().type(Type.SLAVE);
        }
    }

    /**
     * Filter the clients with its client {@code ids}.
     *
     * @param ids client ids
     * @return {@code this} {@link ClientListArgs}.
     */
    public ClientListArgs ids(long... ids) {
        LettuceAssert.notNull(ids, "Ids must not be null");

        this.ids = new ArrayList<>(ids.length);

        for (long id : ids) {
            this.ids.add(id);
        }
        return this;
    }

    /**
     * This filters the connections of all the clients in the specified {@link ClientListArgs.Type}. Note that clients blocked into the
     * {@literal MONITOR} command are considered to belong to the normal class.
     *
     * @param type must not be {@code null}.
     * @return {@code this} {@link ClientListArgs}.
     */
    public ClientListArgs type(Type type) {

        LettuceAssert.notNull(type, "Type must not be null");

        this.type = type;
        return this;
    }

    @Override
    public <K, V> void build(CommandArgs<K, V> args) {
        if (ids != null && !ids.isEmpty()) {
            args.add(ID);
            for (Long id : ids) {
                args.add(id);
            }
        }

        if (type != null) {
            args.add(CommandType.TYPE).add(type.name().toLowerCase());
        }
    }
}
