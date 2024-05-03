package io.lettuce.core;

import io.lettuce.core.protocol.CommandArgs;

/**
 * Argument list builder for the Redis <a href="https://redis.io/commands/xgroup">XGROUP</a> CREATE command. Static import the
 * methods from {@link Builder} and call the methods: {@code mkstream(…)} .
 * <p/>
 * {@link XGroupCreateArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author Mark Paluch
 * @since 5.2
 */
public class XGroupCreateArgs implements CompositeArgument {

    private boolean mkstream;

    private Long entriesRead;

    /**
     * Builder entry points for {@link XGroupCreateArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates new {@link XGroupCreateArgs} and set {@literal MKSTREAM}.
         *
         * @return new {@link XGroupCreateArgs} with {@literal MKSTREAM} set.
         * @see XGroupCreateArgs#mkstream(boolean)
         */
        public static XGroupCreateArgs mkstream() {
            return mkstream(true);
        }

        /**
         * Creates new {@link XGroupCreateArgs} and set {@literal MKSTREAM}.
         *
         * @param mkstream whether to apply {@literal MKSTREAM}.
         * @return new {@link XGroupCreateArgs} with {@literal MKSTREAM} set.
         * @see XGroupCreateArgs#mkstream(boolean)
         */
        public static XGroupCreateArgs mkstream(boolean mkstream) {
            return new XGroupCreateArgs().mkstream(mkstream);
        }

        /**
         * Creates new {@link XGroupCreateArgs} and set {@literal ENTRIESREAD}.
         *
         * @param entriesRead number of read entries for lag tracking.
         * @return new {@link XGroupCreateArgs} with {@literal ENTRIESREAD} set.
         * @see XGroupCreateArgs#entriesRead(long)
         * @since 6.2
         */
        public static XGroupCreateArgs entriesRead(long entriesRead) {
            return new XGroupCreateArgs().entriesRead(entriesRead);
        }

    }

    /**
     * Make a stream if it does not exists.
     *
     * @param mkstream whether to apply {@literal MKSTREAM}
     * @return {@code this}
     */
    public XGroupCreateArgs mkstream(boolean mkstream) {

        this.mkstream = mkstream;
        return this;
    }

    /**
     * Configure the {@literal ENTRIESREAD} argument.
     *
     * @param entriesRead number of read entries for lag tracking.
     *
     * @return {@code this}
     * @since 6.2
     */
    public XGroupCreateArgs entriesRead(long entriesRead) {

        this.entriesRead = entriesRead;
        return this;
    }

    public <K, V> void build(CommandArgs<K, V> args) {

        if (mkstream) {
            args.add("MKSTREAM");
        }

        if (entriesRead != null) {
            args.add("ENTRIESREAD").add(entriesRead);
        }
    }

}
