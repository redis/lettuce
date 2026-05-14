package io.lettuce.core;

import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;
import io.lettuce.core.protocol.CommandType;

/**
 * Argument list builder for the Redis {@code INCREX} command. Holds bounds, overflow, expiration, and ENX options.
 * <p>
 * The increment value and mode (BYINT/BYFLOAT) are determined by which method is called on the commands interface, not by this
 * args class.
 * <p>
 * Usage:
 *
 * <pre>
 * IncrexArgs.Builder.ubound(100).overflow(Overflow.SAT).ex(60)
 * IncrexArgs.Builder.lbound(0).ubound(100).overflow(Overflow.REJECT)
 * new IncrexArgs().ex(30).enx()
 * </pre>
 *
 * @since 7.6
 */
public class IncrexArgs implements CompositeArgument {

    public enum Overflow {
        FAIL, SAT, REJECT
    }

    private Number lbound;

    private Number ubound;

    private Overflow overflow;

    private Long ex;

    private Long px;

    private Long exAt;

    private Long pxAt;

    private boolean persist = false;

    private boolean enx = false;

    // ── Static Builder ─────────────────────────────

    public static class Builder {

        private Builder() {
        }

        public static IncrexArgs lbound(long lbound) {
            return new IncrexArgs().lbound(lbound);
        }

        public static IncrexArgs lbound(double lbound) {
            return new IncrexArgs().lbound(lbound);
        }

        public static IncrexArgs ubound(long ubound) {
            return new IncrexArgs().ubound(ubound);
        }

        public static IncrexArgs ubound(double ubound) {
            return new IncrexArgs().ubound(ubound);
        }

        public static IncrexArgs overflow(Overflow overflow) {
            return new IncrexArgs().overflow(overflow);
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

    public IncrexArgs lbound(double lbound) {
        this.lbound = lbound;
        return this;
    }

    public IncrexArgs ubound(long ubound) {
        this.ubound = ubound;
        return this;
    }

    public IncrexArgs ubound(double ubound) {
        this.ubound = ubound;
        return this;
    }

    public IncrexArgs overflow(Overflow overflow) {
        this.overflow = overflow;
        return this;
    }

    public IncrexArgs ex(long seconds) {
        this.ex = seconds;
        return this;
    }

    public IncrexArgs px(long milliseconds) {
        this.px = milliseconds;
        return this;
    }

    public IncrexArgs exAt(long timestampSeconds) {
        this.exAt = timestampSeconds;
        return this;
    }

    public IncrexArgs pxAt(long timestampMilliseconds) {
        this.pxAt = timestampMilliseconds;
        return this;
    }

    public IncrexArgs persist() {
        this.persist = true;
        return this;
    }

    public IncrexArgs enx() {
        this.enx = true;
        return this;
    }

    @Override
    public <K, V> void build(CommandArgs<K, V> args) {

        if (lbound != null) {
            args.add(CommandKeyword.LBOUND);
            args.add(lbound);
        }

        if (ubound != null) {
            args.add(CommandKeyword.UBOUND);
            args.add(ubound);
        }

        if (overflow != null) {
            args.add(CommandKeyword.OVERFLOW);
            switch (overflow) {
                case FAIL:
                    args.add(CommandKeyword.FAIL);
                    break;
                case SAT:
                    args.add(CommandKeyword.SAT);
                    break;
                case REJECT:
                    args.add(CommandKeyword.REJECT);
                    break;
            }
        }

        if (ex != null) {
            args.add(CommandKeyword.EX).add(ex);
        }
        if (px != null) {
            args.add(CommandKeyword.PX).add(px);
        }
        if (exAt != null) {
            args.add(CommandKeyword.EXAT).add(exAt);
        }
        if (pxAt != null) {
            args.add(CommandKeyword.PXAT).add(pxAt);
        }
        if (persist) {
            args.add(CommandType.PERSIST);
        }

        if (enx) {
            args.add(CommandKeyword.ENX);
        }
    }

}
