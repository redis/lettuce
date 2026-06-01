package io.lettuce.core;

import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;
import io.lettuce.core.protocol.CommandType;

/**
 * Abstract base for {@code INCREX} arguments. Owns options common to both the integer-bounded variant ({@link IncrexArgs}) and
 * the float-bounded variant ({@link IncrexFloatArgs}): {@code SATURATE}, the
 * {@code EX}/{@code PX}/{@code EXAT}/{@code PXAT}/{@code PERSIST} expiration block, and {@code ENX}.
 * <p>
 * Bounds ({@code LBOUND}/{@code UBOUND}) live on the concrete subclasses so the bound type matches the increment type at
 * compile time &mdash; integer-mode {@code increx} only accepts {@link IncrexArgs}, float-mode {@code increx} only accepts
 * {@link IncrexFloatArgs}.
 *
 * @param <T> the concrete subtype, for fluent method chaining
 * @since 7.6
 */
abstract class BaseIncrexArgs<T extends BaseIncrexArgs<T>> implements CompositeArgument {

    private boolean saturate = false;

    private Long ex;

    private Long px;

    private Long exAt;

    private Long pxAt;

    private boolean persist = false;

    private boolean enx = false;

    @SuppressWarnings("unchecked")
    private T self() {
        return (T) this;
    }

    /**
     * Saturate the result on out-of-bounds: the value is clamped to the violated bound (or the implicit numeric-type limit when
     * no explicit bound is set), and the second element of the reply reflects the actual applied increment. Without
     * {@code SATURATE}, an out-of-bounds operation is silently rejected: the key value and TTL are unchanged and the reply is
     * {@code [current_value, 0]}.
     */
    public T saturate() {
        this.saturate = true;
        return self();
    }

    public T ex(long seconds) {
        this.ex = seconds;
        return self();
    }

    public T px(long milliseconds) {
        this.px = milliseconds;
        return self();
    }

    public T exAt(long timestampSeconds) {
        this.exAt = timestampSeconds;
        return self();
    }

    public T pxAt(long timestampMilliseconds) {
        this.pxAt = timestampMilliseconds;
        return self();
    }

    public T persist() {
        this.persist = true;
        return self();
    }

    /**
     * Apply the expiry only if the key does not already have one ({@code Expiry only if Not eXists}). If the key already has a
     * TTL, the existing TTL is preserved.
     */
    public T enx() {
        this.enx = true;
        return self();
    }

    @Override
    public <K, V> void build(CommandArgs<K, V> args) {

        buildBounds(args);

        if (saturate) {
            args.add(CommandKeyword.SATURATE);
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

    /**
     * Subclasses emit their typed {@code LBOUND}/{@code UBOUND} here.
     */
    protected abstract <K, V> void buildBounds(CommandArgs<K, V> args);

}
