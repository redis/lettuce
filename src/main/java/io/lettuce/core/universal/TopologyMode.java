package io.lettuce.core.universal;

/**
 * Deployment topology detected by {@link UniversalClient} at connect time.
 *
 * @since 7.x (PoC)
 */
public enum TopologyMode {

    /** A single server (or a proxied deployment that presents as one, e.g. Redis Enterprise). */
    STANDALONE,

    /** An OSS Redis Cluster. */
    CLUSTER
}
