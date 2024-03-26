package io.lettuce.core;

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;
import io.lettuce.core.protocol.ProtocolKeyword;

/**
 * Argument list builder for the Redis <a href="https://redis.io/commands/blmpop">BLMPOP</a> and
 * <a href="https://redis.io/commands/lpop">LPOP</a> commands. Static import the methods from {@link Builder} and chain the
 * method calls: {@code left().count(…)}.
 *
 * @author Mark Paluch
 * @since 6.2
 */
public class LMPopArgs implements CompositeArgument {

    private final ProtocolKeyword direction;

    private Long count;

    private LMPopArgs(ProtocolKeyword source, Long count) {
        this.direction = source;
        this.count = count;
    }

    /**
     * Builder entry points for {@link LMPopArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates new {@link LMPopArgs} setting with {@code LEFT} direction.
         *
         * @return new {@link LMPopArgs} with args set.
         */
        public static LMPopArgs left() {
            return new LMPopArgs(CommandKeyword.LEFT, null);
        }

        /**
         * Creates new {@link LMPopArgs} setting with {@code RIGHT} direction.
         *
         * @return new {@link LMPopArgs} with args set.
         */
        public static LMPopArgs right() {
            return new LMPopArgs(CommandKeyword.RIGHT, null);
        }

    }

    /**
     * Set the {@code count} of entries to return.
     *
     * @param count number greater 0.
     * @return {@code this}
     */
    public LMPopArgs count(long count) {

        LettuceAssert.isTrue(count > 0, "Count must be greater 0");

        this.count = count;
        return this;
    }

    @Override
    public <K, V> void build(CommandArgs<K, V> args) {
        args.add(direction);

        if (count != null) {
            args.add(CommandKeyword.COUNT).add(count);
        }
    }

}
