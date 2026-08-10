/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.api.consistency;

import io.lettuce.core.api.async.BaseRedisAsyncCommands;
import io.lettuce.core.api.async.RedisAclAsyncCommands;
import io.lettuce.core.api.async.RedisArrayAsyncCommands;
import io.lettuce.core.api.async.RedisBloomFilterAsyncCommands;
import io.lettuce.core.api.async.RedisCMSAsyncCommands;
import io.lettuce.core.api.async.RedisCuckooFilterAsyncCommands;
import io.lettuce.core.api.async.RediSearchAsyncCommands;
import io.lettuce.core.api.async.RedisFunctionAsyncCommands;
import io.lettuce.core.api.async.RedisGeoAsyncCommands;
import io.lettuce.core.api.async.RedisHLLAsyncCommands;
import io.lettuce.core.api.async.RedisHashAsyncCommands;
import io.lettuce.core.api.async.RedisJsonAsyncCommands;
import io.lettuce.core.api.async.RedisKeyAsyncCommands;
import io.lettuce.core.api.async.RedisListAsyncCommands;
import io.lettuce.core.api.async.RedisScriptingAsyncCommands;
import io.lettuce.core.api.async.RedisServerAsyncCommands;
import io.lettuce.core.api.async.RedisSetAsyncCommands;
import io.lettuce.core.api.async.RedisSortedSetAsyncCommands;
import io.lettuce.core.api.async.RedisStreamAsyncCommands;
import io.lettuce.core.api.async.RedisStringAsyncCommands;
import io.lettuce.core.api.async.RedisTopKAsyncCommands;
import io.lettuce.core.api.async.RedisTransactionalAsyncCommands;
import io.lettuce.core.api.async.RedisVectorSetAsyncCommands;
import io.lettuce.core.api.reactive.BaseRedisReactiveCommands;
import io.lettuce.core.api.reactive.RedisAclReactiveCommands;
import io.lettuce.core.api.reactive.RedisArrayReactiveCommands;
import io.lettuce.core.api.reactive.RedisBloomFilterReactiveCommands;
import io.lettuce.core.api.reactive.RedisCMSReactiveCommands;
import io.lettuce.core.api.reactive.RedisCuckooFilterReactiveCommands;
import io.lettuce.core.api.reactive.RediSearchReactiveCommands;
import io.lettuce.core.api.reactive.RedisFunctionReactiveCommands;
import io.lettuce.core.api.reactive.RedisGeoReactiveCommands;
import io.lettuce.core.api.reactive.RedisHLLReactiveCommands;
import io.lettuce.core.api.reactive.RedisHashReactiveCommands;
import io.lettuce.core.api.reactive.RedisJsonReactiveCommands;
import io.lettuce.core.api.reactive.RedisKeyReactiveCommands;
import io.lettuce.core.api.reactive.RedisListReactiveCommands;
import io.lettuce.core.api.reactive.RedisScriptingReactiveCommands;
import io.lettuce.core.api.reactive.RedisServerReactiveCommands;
import io.lettuce.core.api.reactive.RedisSetReactiveCommands;
import io.lettuce.core.api.reactive.RedisSortedSetReactiveCommands;
import io.lettuce.core.api.reactive.RedisStreamReactiveCommands;
import io.lettuce.core.api.reactive.RedisStringReactiveCommands;
import io.lettuce.core.api.reactive.RedisTopKReactiveCommands;
import io.lettuce.core.api.reactive.RedisTransactionalReactiveCommands;
import io.lettuce.core.api.reactive.RedisVectorSetReactiveCommands;
import io.lettuce.core.api.sync.BaseRedisCommands;
import io.lettuce.core.api.sync.RedisAclCommands;
import io.lettuce.core.api.sync.RedisArrayCommands;
import io.lettuce.core.api.sync.RedisBloomFilterCommands;
import io.lettuce.core.api.sync.RedisCMSCommands;
import io.lettuce.core.api.sync.RedisCuckooFilterCommands;
import io.lettuce.core.api.sync.RediSearchCommands;
import io.lettuce.core.api.sync.RedisFunctionCommands;
import io.lettuce.core.api.sync.RedisGeoCommands;
import io.lettuce.core.api.sync.RedisHLLCommands;
import io.lettuce.core.api.sync.RedisHashCommands;
import io.lettuce.core.api.sync.RedisJsonCommands;
import io.lettuce.core.api.sync.RedisKeyCommands;
import io.lettuce.core.api.sync.RedisListCommands;
import io.lettuce.core.api.sync.RedisScriptingCommands;
import io.lettuce.core.api.sync.RedisServerCommands;
import io.lettuce.core.api.sync.RedisSetCommands;
import io.lettuce.core.api.sync.RedisSortedSetCommands;
import io.lettuce.core.api.sync.RedisStreamCommands;
import io.lettuce.core.api.sync.RedisStringCommands;
import io.lettuce.core.api.sync.RedisTopKCommands;
import io.lettuce.core.api.sync.RedisTransactionalCommands;
import io.lettuce.core.api.sync.RedisVectorSetCommands;
import io.lettuce.core.cluster.api.async.BaseNodeSelectionAsyncCommands;
import io.lettuce.core.cluster.api.async.NodeSelectionAclAsyncCommands;
import io.lettuce.core.cluster.api.async.NodeSelectionArrayAsyncCommands;
import io.lettuce.core.cluster.api.async.NodeSelectionBloomFilterAsyncCommands;
import io.lettuce.core.cluster.api.async.NodeSelectionCMSAsyncCommands;
import io.lettuce.core.cluster.api.async.NodeSelectionCuckooFilterAsyncCommands;
import io.lettuce.core.cluster.api.async.NodeSelectionFunctionAsyncCommands;
import io.lettuce.core.cluster.api.async.NodeSelectionGeoAsyncCommands;
import io.lettuce.core.cluster.api.async.NodeSelectionHLLAsyncCommands;
import io.lettuce.core.cluster.api.async.NodeSelectionHashAsyncCommands;
import io.lettuce.core.cluster.api.async.NodeSelectionJsonAsyncCommands;
import io.lettuce.core.cluster.api.async.NodeSelectionKeyAsyncCommands;
import io.lettuce.core.cluster.api.async.NodeSelectionListAsyncCommands;
import io.lettuce.core.cluster.api.async.NodeSelectionScriptingAsyncCommands;
import io.lettuce.core.cluster.api.async.NodeSelectionSearchAsyncCommands;
import io.lettuce.core.cluster.api.async.NodeSelectionServerAsyncCommands;
import io.lettuce.core.cluster.api.async.NodeSelectionSetAsyncCommands;
import io.lettuce.core.cluster.api.async.NodeSelectionSortedSetAsyncCommands;
import io.lettuce.core.cluster.api.async.NodeSelectionStreamAsyncCommands;
import io.lettuce.core.cluster.api.async.NodeSelectionStringAsyncCommands;
import io.lettuce.core.cluster.api.async.NodeSelectionTopKAsyncCommands;
import io.lettuce.core.cluster.api.async.NodeSelectionVectorSetAsyncCommands;
import io.lettuce.core.cluster.api.sync.BaseNodeSelectionCommands;
import io.lettuce.core.cluster.api.sync.NodeSelectionAclCommands;
import io.lettuce.core.cluster.api.sync.NodeSelectionArrayCommands;
import io.lettuce.core.cluster.api.sync.NodeSelectionBloomFilterCommands;
import io.lettuce.core.cluster.api.sync.NodeSelectionCMSCommands;
import io.lettuce.core.cluster.api.sync.NodeSelectionCuckooFilterCommands;
import io.lettuce.core.cluster.api.sync.NodeSelectionFunctionCommands;
import io.lettuce.core.cluster.api.sync.NodeSelectionGeoCommands;
import io.lettuce.core.cluster.api.sync.NodeSelectionHLLCommands;
import io.lettuce.core.cluster.api.sync.NodeSelectionHashCommands;
import io.lettuce.core.cluster.api.sync.NodeSelectionJsonCommands;
import io.lettuce.core.cluster.api.sync.NodeSelectionKeyCommands;
import io.lettuce.core.cluster.api.sync.NodeSelectionListCommands;
import io.lettuce.core.cluster.api.sync.NodeSelectionScriptingCommands;
import io.lettuce.core.cluster.api.sync.NodeSelectionSearchCommands;
import io.lettuce.core.cluster.api.sync.NodeSelectionServerCommands;
import io.lettuce.core.cluster.api.sync.NodeSelectionSetCommands;
import io.lettuce.core.cluster.api.sync.NodeSelectionSortedSetCommands;
import io.lettuce.core.cluster.api.sync.NodeSelectionStreamCommands;
import io.lettuce.core.cluster.api.sync.NodeSelectionStringCommands;
import io.lettuce.core.cluster.api.sync.NodeSelectionTopKCommands;
import io.lettuce.core.cluster.api.sync.NodeSelectionVectorSetCommands;
import io.lettuce.core.sentinel.api.async.RedisSentinelAsyncCommands;
import io.lettuce.core.sentinel.api.reactive.RedisSentinelReactiveCommands;
import io.lettuce.core.sentinel.api.sync.RedisSentinelCommands;

/**
 * Catalog of all Redis command interface groups and their per-flavor interfaces. Each entry represents one command group
 * (formerly one template in {@code src/main/templates}) and lists the sync, async, reactive, Kotlin coroutine and cluster
 * node-selection interfaces that must stay consistent with each other.
 * <p>
 * This catalog is the single place where a new command group must be registered so that the API consistency test suite covers
 * it.
 * <p>
 * The Kotlin coroutine flavor is owned entirely by the Kotlin side of the suite so that the Java test sources carry no
 * reference to it: the {@code CommandInterfaces.coroutines()} extension function (see {@code KnownKotlinApiDeviations.kt})
 * derives the coroutine interface from the sync interface by naming convention.
 */
public enum CommandInterfaces {

    BASE(BaseRedisCommands.class, BaseRedisAsyncCommands.class, BaseRedisReactiveCommands.class,
            BaseNodeSelectionCommands.class, BaseNodeSelectionAsyncCommands.class),

    ACL(RedisAclCommands.class, RedisAclAsyncCommands.class, RedisAclReactiveCommands.class, NodeSelectionAclCommands.class,
            NodeSelectionAclAsyncCommands.class),

    ARRAY(RedisArrayCommands.class, RedisArrayAsyncCommands.class, RedisArrayReactiveCommands.class,
            NodeSelectionArrayCommands.class, NodeSelectionArrayAsyncCommands.class),

    BLOOM_FILTER(RedisBloomFilterCommands.class, RedisBloomFilterAsyncCommands.class, RedisBloomFilterReactiveCommands.class,
            NodeSelectionBloomFilterCommands.class, NodeSelectionBloomFilterAsyncCommands.class),

    CMS(RedisCMSCommands.class, RedisCMSAsyncCommands.class, RedisCMSReactiveCommands.class, NodeSelectionCMSCommands.class,
            NodeSelectionCMSAsyncCommands.class),

    CUCKOO_FILTER(RedisCuckooFilterCommands.class, RedisCuckooFilterAsyncCommands.class,
            RedisCuckooFilterReactiveCommands.class, NodeSelectionCuckooFilterCommands.class,
            NodeSelectionCuckooFilterAsyncCommands.class),

    FUNCTION(RedisFunctionCommands.class, RedisFunctionAsyncCommands.class, RedisFunctionReactiveCommands.class,
            NodeSelectionFunctionCommands.class, NodeSelectionFunctionAsyncCommands.class),

    GEO(RedisGeoCommands.class, RedisGeoAsyncCommands.class, RedisGeoReactiveCommands.class, NodeSelectionGeoCommands.class,
            NodeSelectionGeoAsyncCommands.class),

    HASH(RedisHashCommands.class, RedisHashAsyncCommands.class, RedisHashReactiveCommands.class,
            NodeSelectionHashCommands.class, NodeSelectionHashAsyncCommands.class),

    HLL(RedisHLLCommands.class, RedisHLLAsyncCommands.class, RedisHLLReactiveCommands.class, NodeSelectionHLLCommands.class,
            NodeSelectionHLLAsyncCommands.class),

    JSON(RedisJsonCommands.class, RedisJsonAsyncCommands.class, RedisJsonReactiveCommands.class,
            NodeSelectionJsonCommands.class, NodeSelectionJsonAsyncCommands.class),

    KEY(RedisKeyCommands.class, RedisKeyAsyncCommands.class, RedisKeyReactiveCommands.class, NodeSelectionKeyCommands.class,
            NodeSelectionKeyAsyncCommands.class),

    LIST(RedisListCommands.class, RedisListAsyncCommands.class, RedisListReactiveCommands.class,
            NodeSelectionListCommands.class, NodeSelectionListAsyncCommands.class),

    SCRIPTING(RedisScriptingCommands.class, RedisScriptingAsyncCommands.class, RedisScriptingReactiveCommands.class,
            NodeSelectionScriptingCommands.class, NodeSelectionScriptingAsyncCommands.class),

    SEARCH(RediSearchCommands.class, RediSearchAsyncCommands.class, RediSearchReactiveCommands.class,
            NodeSelectionSearchCommands.class, NodeSelectionSearchAsyncCommands.class),

    SENTINEL(RedisSentinelCommands.class, RedisSentinelAsyncCommands.class, RedisSentinelReactiveCommands.class, null, null),

    SERVER(RedisServerCommands.class, RedisServerAsyncCommands.class, RedisServerReactiveCommands.class,
            NodeSelectionServerCommands.class, NodeSelectionServerAsyncCommands.class),

    SET(RedisSetCommands.class, RedisSetAsyncCommands.class, RedisSetReactiveCommands.class, NodeSelectionSetCommands.class,
            NodeSelectionSetAsyncCommands.class),

    SORTED_SET(RedisSortedSetCommands.class, RedisSortedSetAsyncCommands.class, RedisSortedSetReactiveCommands.class,
            NodeSelectionSortedSetCommands.class, NodeSelectionSortedSetAsyncCommands.class),

    STREAM(RedisStreamCommands.class, RedisStreamAsyncCommands.class, RedisStreamReactiveCommands.class,
            NodeSelectionStreamCommands.class, NodeSelectionStreamAsyncCommands.class),

    STRING(RedisStringCommands.class, RedisStringAsyncCommands.class, RedisStringReactiveCommands.class,
            NodeSelectionStringCommands.class, NodeSelectionStringAsyncCommands.class),

    TOP_K(RedisTopKCommands.class, RedisTopKAsyncCommands.class, RedisTopKReactiveCommands.class,
            NodeSelectionTopKCommands.class, NodeSelectionTopKAsyncCommands.class),

    TRANSACTIONAL(RedisTransactionalCommands.class, RedisTransactionalAsyncCommands.class,
            RedisTransactionalReactiveCommands.class, null, null),

    VECTOR_SET(RedisVectorSetCommands.class, RedisVectorSetAsyncCommands.class, RedisVectorSetReactiveCommands.class,
            NodeSelectionVectorSetCommands.class, NodeSelectionVectorSetAsyncCommands.class);

    private final Class<?> sync;

    private final Class<?> async;

    private final Class<?> reactive;

    private final Class<?> nodeSelectionSync;

    private final Class<?> nodeSelectionAsync;

    CommandInterfaces(Class<?> sync, Class<?> async, Class<?> reactive, Class<?> nodeSelectionSync,
            Class<?> nodeSelectionAsync) {
        this.sync = sync;
        this.async = async;
        this.reactive = reactive;
        this.nodeSelectionSync = nodeSelectionSync;
        this.nodeSelectionAsync = nodeSelectionAsync;
    }

    public Class<?> sync() {
        return sync;
    }

    public Class<?> async() {
        return async;
    }

    public Class<?> reactive() {
        return reactive;
    }

    /**
     * @return the cluster node-selection sync interface, or {@code null} if the group has no node-selection flavor.
     */
    public Class<?> nodeSelectionSync() {
        return nodeSelectionSync;
    }

    /**
     * @return the cluster node-selection async interface, or {@code null} if the group has no node-selection flavor.
     */
    public Class<?> nodeSelectionAsync() {
        return nodeSelectionAsync;
    }

    public boolean hasNodeSelection() {
        return nodeSelectionSync != null;
    }

    /**
     * @return the fully qualified name of the command builder that constructs this group's commands. The builder classes are
     *         package-private, hence the name instead of a {@code Class} literal.
     */
    public String commandBuilderClassName() {
        switch (this) {
            case ARRAY:
                return "io.lettuce.core.RedisArrayCommandBuilder";
            case BLOOM_FILTER:
                return "io.lettuce.core.RedisBloomFilterCommandBuilder";
            case CMS:
                return "io.lettuce.core.RedisCMSCommandBuilder";
            case CUCKOO_FILTER:
                return "io.lettuce.core.RedisCuckooFilterCommandBuilder";
            case JSON:
                return "io.lettuce.core.RedisJsonCommandBuilder";
            case SEARCH:
                return "io.lettuce.core.RediSearchCommandBuilder";
            case TOP_K:
                return "io.lettuce.core.RedisTopKCommandBuilder";
            case VECTOR_SET:
                return "io.lettuce.core.RedisVectorSetCommandBuilder";
            case SENTINEL:
                return "io.lettuce.core.sentinel.SentinelCommandBuilder";
            default:
                return "io.lettuce.core.RedisCommandBuilder";
        }
    }

}
