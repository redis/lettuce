package io.lettuce.core;

import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;

/**
 * Argument list builder for the integer-bounded form of the Redis {@code INCREX} command. Accepts {@code long} lower/upper
 * bounds; paired with the {@code increx(K, long, IncrexArgs)} overload on the commands interface (sends {@code BYINT} on the
 * wire).
 * <p>
 * For float-bounded INCREX use {@link IncrexFloatArgs}.
 * <p>
 * Usage:
 *
 * <pre>
 * IncrexArgs.Builder.ubound(100).saturate().ex(60)
 * IncrexArgs.Builder.lbound(0).ubound(100).saturate()
 * new IncrexArgs().ex(30).enx()
 * </pre>
 *
 * @since 7.6
 */
public class IncrexArgs extends BaseIncrexArgs<IncrexArgs> {

    private Long lbound;

    private Long ubound;

    // ── Static Builder ─────────────────────────────

    public static class Builder {

        private Builder() {
        }

        public static IncrexArgs lbound(long lbound) {
            return new IncrexArgs().lbound(lbound);
        }

        public static IncrexArgs ubound(long ubound) {
            return new IncrexArgs().ubound(ubound);
        }

        public static IncrexArgs saturate() {
            return new IncrexArgs().saturate();
        }

        public static IncrexArgs ex(long seconds) {
            return new IncrexArgs().ex(seconds);
        }

        public static IncrexArgs px(long milliseconds) {
            return new IncrexArgs().px(milliseconds);
        }

        public static IncrexArgs exAt(long timestampSeconds) {
            return new IncrexArgs().exAt(timestampSeconds);
        }

        public static IncrexArgs pxAt(long timestampMilliseconds) {
            return new IncrexArgs().pxAt(timestampMilliseconds);
        }

        public static IncrexArgs persist() {
            return new IncrexArgs().persist();
        }

        public static IncrexArgs enx() {
            return new IncrexArgs().enx();
        }

    }

    // ── Fluent Setters ─────────────────────────────

    public IncrexArgs lbound(long lbound) {
        this.lbound = lbound;
        return this;
    }

    public IncrexArgs ubound(long ubound) {
        this.ubound = ubound;
        return this;
    }

    @Override
    protected <K, V> void buildBounds(CommandArgs<K, V> args) {

        if (lbound != null) {
            args.add(CommandKeyword.LBOUND);
            args.add(lbound);
        }

        if (ubound != null) {
            args.add(CommandKeyword.UBOUND);
            args.add(ubound);
        }
    }

}
