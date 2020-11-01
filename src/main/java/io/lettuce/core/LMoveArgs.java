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

import static io.lettuce.core.protocol.CommandKeyword.LEFT;
import static io.lettuce.core.protocol.CommandKeyword.RIGHT;

/**
 * Argument list builder for the Redis <a href="http://redis.io/commands/blmove">BLMOVE</a> and
 * <a href="http://redis.io/commands/lmove">LMOVE</a> commands. Static import the methods from {@link Builder} and
 * chain the method calls: {@code leftRight()}.
 *
 * @author Mikhael Sokolov
 * @since 6.1
 */
public class LMoveArgs implements CompositeArgument {

    private enum Direction {
        LEFT, RIGHT
    }

    private final Direction source;

    private final Direction destination;

    private LMoveArgs(Direction source, Direction destination) {
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
         * Creates new {@link LMoveArgs} setting with LEFT LEFT directions.
         *
         * @return new {@link LMoveArgs} with args set.
         */
        public static LMoveArgs leftLeft() {
            return new LMoveArgs(Direction.LEFT, Direction.LEFT);
        }

        /**
         * Creates new {@link LMoveArgs} setting with LEFT RIGHT directions.
         *
         * @return new {@link LMoveArgs} with args set.
         */
        public static LMoveArgs leftRight() {
            return new LMoveArgs(Direction.LEFT, Direction.RIGHT);
        }

        /**
         * Creates new {@link LMoveArgs} setting with RIGHT LEFT directions.
         *
         * @return new {@link LMoveArgs} with args set.
         */
        public static LMoveArgs rightLeft() {
            return new LMoveArgs(Direction.RIGHT, Direction.LEFT);
        }

        /**
         * Creates new {@link LMoveArgs} setting with RIGHT RIGHT directions.
         *
         * @return new {@link LMoveArgs} with args set.
         */
        public static LMoveArgs rightRight() {
            return new LMoveArgs(Direction.RIGHT, Direction.RIGHT);
        }
    }

    public <K, V> void build(CommandArgs<K, V> args) {
        passDirection(source, args);
        passDirection(destination, args);
    }

    private static <K, V> void passDirection(Direction direction, CommandArgs<K, V> args) {
        switch (direction) {
            case LEFT:
                args.add(LEFT);
                break;
            case RIGHT:
                args.add(RIGHT);
                break;
            default:
                throw new IllegalArgumentException(String.format("Direction %s not supported", direction));
        }
    }
}
