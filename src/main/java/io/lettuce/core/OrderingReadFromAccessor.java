package io.lettuce.core;

/**
 * Accessor for {@link ReadFrom} ordering. Internal utility class.
 *
 * @author Mark Paluch
 * @since 5.2
 */
public abstract class OrderingReadFromAccessor {

    /**
     * Utility constructor.
     */
    private OrderingReadFromAccessor() {
    }

    /**
     * Returns whether this {@link ReadFrom} requires ordering of the resulting
     * {@link io.lettuce.core.models.role.RedisNodeDescription nodes}.
     *
     * @return {@code true} if code using {@link ReadFrom} should retain ordering or {@code false} to allow reordering of
     *         {@link io.lettuce.core.models.role.RedisNodeDescription nodes}.
     * @since 5.2
     * @see ReadFrom#isOrderSensitive()
     */
    public static boolean isOrderSensitive(ReadFrom readFrom) {
        return readFrom.isOrderSensitive();
    }

}
