package io.lettuce.core;

import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;

/**
 * Argument list builder for the floating-point-bounded form of the Redis {@code INCREX} command. Accepts {@code double}
 * lower/upper bounds; paired with the {@code increx(K, double, IncrexFloatArgs)} overload on the commands interface (sends
 * {@code BYFLOAT} on the wire).
 * <p>
 * For integer-bounded INCREX use {@link IncrexArgs}.
 * <p>
 * Usage:
 *
 * <pre>
 * IncrexFloatArgs.Builder.ubound(100.0).saturate().ex(60)
 * IncrexFloatArgs.Builder.lbound(0.0).ubound(100.0).saturate()
 * new IncrexFloatArgs().ex(30).enx()
 * </pre>
 *
 * @since 7.6
 */
public class IncrexFloatArgs extends BaseIncrexArgs<IncrexFloatArgs> {

    private Double lbound;

    private Double ubound;

    // ── Static Builder ─────────────────────────────

    public static class Builder {

        private Builder() {
        }

        public static IncrexFloatArgs lbound(double lbound) {
            return new IncrexFloatArgs().lbound(lbound);
        }

        public static IncrexFloatArgs ubound(double ubound) {
            return new IncrexFloatArgs().ubound(ubound);
        }

        public static IncrexFloatArgs saturate() {
            return new IncrexFloatArgs().saturate();
        }

        public static IncrexFloatArgs ex(long seconds) {
            return new IncrexFloatArgs().ex(seconds);
        }

        public static IncrexFloatArgs px(long milliseconds) {
            return new IncrexFloatArgs().px(milliseconds);
        }

        public static IncrexFloatArgs exAt(long timestampSeconds) {
            return new IncrexFloatArgs().exAt(timestampSeconds);
        }

        public static IncrexFloatArgs pxAt(long timestampMilliseconds) {
            return new IncrexFloatArgs().pxAt(timestampMilliseconds);
        }

        public static IncrexFloatArgs persist() {
            return new IncrexFloatArgs().persist();
        }

        public static IncrexFloatArgs enx() {
            return new IncrexFloatArgs().enx();
        }

    }

    // ── Fluent Setters ─────────────────────────────

    public IncrexFloatArgs lbound(double lbound) {
        this.lbound = lbound;
        return this;
    }

    public IncrexFloatArgs ubound(double ubound) {
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
