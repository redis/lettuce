package io.lettuce.core;

import static io.lettuce.core.protocol.CommandKeyword.*;

import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.ProtocolKeyword;

/**
 * Argument list builder for the ZMPOP <a href="https://redis.io/commands/zmpop">ZMPOP</a> and
 * <a href="https://redis.io/commands/bzmpop">BZMPOP</a> command starting. {@link ZPopArgs} is a mutable object and instances
 * should be used only once to avoid shared mutable state.
 *
 * @author Mark Paluch
 * @since 6.3
 */
public class ZPopArgs implements CompositeArgument {

    private ProtocolKeyword modifier;

    /**
     * Builder entry points for {@link ScanArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates new {@link ZPopArgs} and enabling {@literal MIN}.
         *
         * @return new {@link ZPopArgs} with {@literal MIN} enabled.
         * @see ZPopArgs#min()
         */
        public static ZPopArgs min() {
            return new ZPopArgs().min();
        }

        /**
         * Creates new {@link ZPopArgs} and enabling {@literal MAX}.
         *
         * @return new {@link ZPopArgs} with {@literal MAX} enabled.
         * @see ZPopArgs#min()
         */
        public static ZPopArgs max() {
            return new ZPopArgs().max();
        }

    }

    /**
     * Elements popped are those with the lowest scores from the first non-empty sorted set
     *
     * @return {@code this} {@link ZPopArgs}.
     */
    public ZPopArgs min() {

        this.modifier = MIN;
        return this;
    }

    /**
     * Elements popped are those with the highest scores from the first non-empty sorted set
     *
     * @return {@code this} {@link ZPopArgs}.
     */
    public ZPopArgs max() {

        this.modifier = MAX;
        return this;
    }

    @Override
    public <K, V> void build(CommandArgs<K, V> args) {
        args.add(modifier);
    }

}
