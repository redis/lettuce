package io.lettuce.core.support;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import io.lettuce.core.internal.LettuceAssert;

/**
 * Utility class to adapt Commons Pool 2 configuration to {@link BoundedPoolConfig}.
 *
 * @author Mark Paluch
 * @since 5.1
 */
public class CommonsPool2ConfigConverter {

    private CommonsPool2ConfigConverter() {
    }

    /**
     * Converts {@link GenericObjectPoolConfig} properties to an immutable {@link BoundedPoolConfig}. Applies max total, min/max
     * idle and test on borrow/create/release configuration.
     *
     * @param config must not be {@code null}.
     * @return the converted {@link BoundedPoolConfig}.
     */
    public static BoundedPoolConfig bounded(GenericObjectPoolConfig<?> config) {

        LettuceAssert.notNull(config, "GenericObjectPoolConfig must not be null");

        return BoundedPoolConfig.builder() //
                .maxTotal(config.getMaxTotal() > 0 ? config.getMaxTotal() : Integer.MAX_VALUE)
                .maxIdle(config.getMaxIdle() > 0 ? config.getMaxIdle() : Integer.MAX_VALUE) //
                .minIdle(config.getMinIdle()) //
                .testOnAcquire(config.getTestOnBorrow()) //
                .testOnCreate(config.getTestOnCreate()) //
                .testOnRelease(config.getTestOnReturn()) //
                .build();
    }

}
