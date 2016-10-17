package com.lambdaworks.redis.dynamic.segment;

import com.lambdaworks.redis.dynamic.CommandMethod;

/**
 * Strategy interface to create {@link CommandSegments} for a {@link CommandMethod}.l
 * 
 * @author Mark Paluch
 * @since 5.0
 */
public interface CommandSegmentFactory {

    /**
     * Create {@link CommandSegments} for a {@link CommandMethod}.
     * 
     * @param commandMethod must not be {@literal null}.
     * @return the {@link CommandSegments}.
     */
    CommandSegments createCommandSegments(CommandMethod commandMethod);
}
