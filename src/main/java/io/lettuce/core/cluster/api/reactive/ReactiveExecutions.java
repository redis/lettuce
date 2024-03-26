package io.lettuce.core.cluster.api.reactive;

import java.util.Collection;

import reactor.core.publisher.Flux;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;

/**
 * Execution holder for a reactive command to be executed on multiple nodes.
 *
 * @author Mark Paluch
 * @since 4.4
 */
public interface ReactiveExecutions<T> {

    /**
     * Return a {@link Flux} that contains a combined stream of the multi-node execution.
     *
     * @return
     */
    Flux<T> flux();

    /**
     * @return collection of nodes on which the command was executed.
     */
    Collection<RedisClusterNode> nodes();

}
