package io.lettuce.core.dynamic.segment;

import io.lettuce.core.dynamic.CommandMethod;

/**
 * Strategy interface to create {@link CommandSegments} for a {@link CommandMethod}.
 *
 * @author Mark Paluch
 * @since 5.0
 */
public interface CommandSegmentFactory {

    /**
     * Create {@link CommandSegments} for a {@link CommandMethod}.
     *
     * @param commandMethod must not be {@code null}.
     * @return the {@link CommandSegments}.
     */
    CommandSegments createCommandSegments(CommandMethod commandMethod);

}
