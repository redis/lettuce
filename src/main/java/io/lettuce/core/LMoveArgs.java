/*
 * Copyright 2020 the original author or authors.
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

import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;
import io.lettuce.core.protocol.ProtocolKeyword;

/**
 * Argument list builder for the Redis <a href="http://redis.io/commands/blmove">BLMOVE</a> and
 * <a href="http://redis.io/commands/lmove">LMOVE</a> commands. Static import the methods from {@link Builder} and chain the
 * method calls: {@code leftRight()}.
 *
 * @author Mikhael Sokolov
 * @author Mark Paluch
 * @since 6.1
 */
public class LMoveArgs implements CompositeArgument {

    private final ProtocolKeyword source;

    private final ProtocolKeyword destination;

    private LMoveArgs(ProtocolKeyword source, ProtocolKeyword destination) {
        this.source = source;
        this.destination = destination;
    }

    /**
     * Builder entry points for {@link LMoveArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates new {@link LMoveArgs} setting with {@code LEFT} {@code LEFT} directions.
         *
         * @return new {@link LMoveArgs} with args set.
         */
        public static LMoveArgs leftLeft() {
            return new LMoveArgs(CommandKeyword.LEFT, CommandKeyword.LEFT);
        }

        /**
         * Creates new {@link LMoveArgs} setting with {@code LEFT} {@code RIGHT} directions.
         *
         * @return new {@link LMoveArgs} with args set.
         */
        public static LMoveArgs leftRight() {
            return new LMoveArgs(CommandKeyword.LEFT, CommandKeyword.RIGHT);
        }

        /**
         * Creates new {@link LMoveArgs} setting with {@code RIGHT} {@code LEFT} directions.
         *
         * @return new {@link LMoveArgs} with args set.
         */
        public static LMoveArgs rightLeft() {
            return new LMoveArgs(CommandKeyword.RIGHT, CommandKeyword.LEFT);
        }

        /**
         * Creates new {@link LMoveArgs} setting with {@code RIGHT} {@code RIGHT} directions.
         *
         * @return new {@link LMoveArgs} with args set.
         */
        public static LMoveArgs rightRight() {
            return new LMoveArgs(CommandKeyword.RIGHT, CommandKeyword.RIGHT);
        }
    }

    @Override
    public <K, V> void build(CommandArgs<K, V> args) {
        args.add(source).add(destination);
    }
}
