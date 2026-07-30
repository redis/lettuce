package io.lettuce.core;

import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;

/**
 * Argument list builder for the Redis <a href="https://redis.io/commands/sunioncard">SUNIONCARD</a> command. Static import the
 * methods from {@link SUnionCardArgs.Builder} and call the methods: {@code approx(…)} {@code limit(…)}.
 *
 * {@link SUnionCardArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @since 7.7
 */
public class SUnionCardArgs implements CompositeArgument {

    private boolean approx;

    private Long limit;

    /**
     * Builder entry points for {@link SUnionCardArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates new {@link SUnionCardArgs} and sets {@literal APPROX}.
         *
         * @return new {@link SUnionCardArgs} with {@literal APPROX} set.
         */
        public static SUnionCardArgs approx() {
            return new SUnionCardArgs().approx(true);
        }

        /**
         * Creates new {@link SUnionCardArgs} and sets {@literal LIMIT}.
         *
         * @param limit cap on the returned cardinality, {@code 0} means no limit.
         * @return new {@link SUnionCardArgs} with {@literal LIMIT} set.
         */
        public static SUnionCardArgs limit(long limit) {
            return new SUnionCardArgs().limit(limit);
        }

    }

    /**
     * Return an approximate cardinality using HyperLogLog instead of computing the exact cardinality.
     *
     * @param approx {@code true} to send {@literal APPROX}.
     * @return {@code this}.
     */
    public SUnionCardArgs approx(boolean approx) {

        this.approx = approx;
        return this;
    }

    /**
     * Cap the returned cardinality at {@code limit}.
     *
     * @param limit cap on the returned cardinality, {@code 0} means no limit.
     * @return {@code this}.
     */
    public SUnionCardArgs limit(long limit) {

        this.limit = limit;
        return this;
    }

    @Override
    public <K, V> void build(CommandArgs<K, V> args) {

        if (approx) {
            args.add(CommandKeyword.APPROX);
        }

        if (limit != null) {
            args.add(CommandKeyword.LIMIT).add(limit);
        }
    }

}
