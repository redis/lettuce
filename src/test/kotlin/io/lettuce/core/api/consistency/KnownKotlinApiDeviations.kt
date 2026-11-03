/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.api.consistency

/**
 * The Kotlin coroutines interface of this command group, derived from the sync interface by naming convention (`api.sync`
 * package → sibling `api.coroutines` package, `*Commands` → `*CoroutinesCommands`) and resolved reflectively. Everything
 * coroutine-flavored lives on the Kotlin side of the suite, so the Java test sources carry no reference to the Kotlin flavor;
 * the derivation also verifies the naming convention itself — a coroutine interface that exists under an unconventional name
 * fails resolution.
 *
 * @throws IllegalStateException if the coroutine flavor does not exist under the conventional name.
 */
fun CommandInterfaces.coroutines(): Class<*> {
    val name = coroutinesName(sync())
    try {
        return Class.forName(name)
    } catch (e: ClassNotFoundException) {
        throw IllegalStateException("Coroutines flavor of $this not found under conventional name $name", e)
    }
}

/**
 * The Kotlin coroutines interface of this aggregate, derived by the same naming convention as
 * [CommandInterfaces.coroutines] — but `null` rather than an error when it does not exist: `RedisAdvancedClusterCommands` and
 * `RedisPubSubCommands` have no coroutine counterpart at all, which is a gap in the coroutine API rather than a misnamed file.
 */
fun AggregateInterfaces.coroutines(): Class<*>? = try {
    Class.forName(coroutinesName(sync()))
} catch (e: ClassNotFoundException) {
    null
}

private fun coroutinesName(sync: Class<*>) =
    sync.name.replace(".api.sync.", ".api.coroutines.").replace(Regex("Commands$"), "CoroutinesCommands")

/**
 * Registry of all known, intentional deviations of the Kotlin coroutine interfaces from the sync reference — the Kotlin
 * counterpart of [KnownApiDeviations], kept in the Kotlin test sources because only the coroutine flavor consults it.
 * Key forms and matching rules are those of [KnownApiDeviations.contains] and [KnownApiDeviations.lookup].
 *
 * The tables were ported verbatim from the former `io.lettuce.apigenerator.KotlinCompilationUnitFactory` generator.
 */
object KnownKotlinApiDeviations {

    /**
     * Sync methods with no coroutine counterpart: the connection accessors, which hand out the underlying connection rather
     * than issuing a command. From `KotlinCompilationUnitFactory#SKIP_METHODS`, plus `getJsonParser` — the coroutine
     * implementations reach the parser through the reactive API they delegate to.
     */
    val SKIP = setOf("getStatefulConnection", "getJsonParser")

    /**
     * Coroutine methods that are plain functions instead of `suspend` functions. The generator's counterpart
     * (`KotlinCompilationUnitFactory#NON_SUSPENDABLE_METHODS`) listed `isOpen`, `flushCommands` and `setAutoFlushCommands`,
     * all of which live on `StatefulConnection` rather than on a command interface. Every remaining non-suspending coroutine
     * function returns a [FLOW] `Flow`.
     */
    val NON_SUSPENDABLE = emptySet<String>()

    /**
     * Coroutine methods returning `Flow` instead of a suspended scalar/collection. From
     * `KotlinCompilationUnitFactory#FLOW_METHODS`.
     */
    val FLOW = setOf(
        "aclList", "aclLog", "dispatch", "geohash", "georadius", "georadiusbymember", "geosearch", "hgetall", "hkeys",
        "hmget", "hvals", "keys", "mget", "sdiff", "sinter", "smembers", "smismember", "sort", "sortReadOnly", "sunion",
        "xclaim", "xrange", "xread", "xreadgroup", "xrevrange", "zdiff", "zdiffWithScores", "zinter", "zinterWithScores",
        "zrange", "zrangeWithScores", "zrangebylex", "zrangebyscore", "zrangebyscoreWithScores", "zrevrange",
        "zrevrangeWithScores", "zrevrangebylex", "zrevrangebyscore", "zrevrangebyscoreWithScores", "zunion",
        "zunionWithScores",
        // calibration 2026-07: only the multi-element overloads stream; the single-element overloads suspend
        "srandmember(K, long)", "zpopmax(K, long)", "zpopmin(K, long)", "xpending(K, K, Range, Limit)",
        "xpending(K, Consumer, Range, Limit)", "xpending(K, XPendingArgs)",
        // calibration 2026-07: multi-element commands added after the generators stopped being used
        "hgetdel", "hgetex", "xackdel", "xdelex"
    )

    /**
     * Deprecated sync methods that are still exposed on the coroutine API. From
     * `KotlinCompilationUnitFactory#KEEP_DEPRECATED_METHODS`.
     */
    val KEEP_DEPRECATED = setOf("flushallAsync", "flushdbAsync", "slaveof", "slaveofNoOne", "slaves")

    /**
     * Coroutine methods whose result is *not* nullable, although the regular rule makes every non-collection suspended result
     * a `T?`. From `KotlinCompilationUnitFactory#NON_NULLABLE_RESULT_METHODS`.
     *
     * The generator's group-qualified entries are deliberately not carried over. They were keyed by the coroutine interface
     * name (`RedisSentinelCoroutinesCommands.set`) while `KotlinCompilationUnitFactory`'s matcher derives the qualifier from
     * the *template* it reads (`RedisSentinelCommands`) — so those eight entries never matched during generation either, and
     * the committed sentinel interface declares those results nullable. The bare-name entries all took effect and are
     * reflected in the committed code.
     */
    val NON_NULLABLE_RESULT = setOf(
        "discard", "multi", "exec", "watch", "unwatch", "getMasterAddrByName", "master", "failover", "monitor", "remove",
        "reset", "pubsubNumsub", "pubsubShardNumsub", "pubsubNumpat", "echo", "ping", "readOnly", "readWrite"
    )

    /**
     * Coroutine methods that declare a non-nullable result although the command may answer with nil, so that the
     * implementation throws [java.util.NoSuchElementException] instead of returning `null`: `zmpop`/`bzmpop` use
     * `awaitFirst()` where the sibling `zpopmin`/`zpopmax` correctly use `awaitFirstOrNull()`. ZMPOP answers nil when no key
     * holds an element and BZMPOP answers nil on timeout, so these signatures are unsound. Fixing them widens the return type
     * to `T?`, which is a source-breaking change for Kotlin callers and therefore scheduled for a major release.
     */
    val UNSOUND_NON_NULLABLE_RESULT = setOf("zmpop", "bzmpop")

    /**
     * Methods declared on a sync aggregate that its coroutine counterpart is known not to expose yet. The Kotlin coroutine API
     * has no `CLUSTER` management commands at all — `RedisClusterCoroutinesCommands` declares only `auth`, where
     * `RedisClusterCommands` declares the full cluster surface. Filling the gap is an API addition, so the entries document it
     * instead of hiding it; each one disappears as the coroutine API catches up.
     */
    val AGGREGATE_PENDING = setOf(
        "asking", "clusterAddSlots", "clusterAddSlotsRange", "clusterBumpepoch", "clusterCountFailureReports",
        "clusterCountKeysInSlot", "clusterDelSlots", "clusterDelSlotsRange", "clusterFailover", "clusterFlushslots",
        "clusterForget", "clusterGetKeysInSlot", "clusterInfo", "clusterKeyslot", "clusterLinks", "clusterMeet",
        "clusterMyId", "clusterMyShardId", "clusterNodes", "clusterReplicas", "clusterReplicate", "clusterReset",
        "clusterSaveconfig", "clusterSetConfigEpoch", "clusterSetSlotImporting", "clusterSetSlotMigrating",
        "clusterSetSlotNode", "clusterSetSlotStable", "clusterShards", "clusterSlots"
    )

    /**
     * Coroutine methods with a fully overridden return type (normalized, package-less Kotlin rendering). From
     * `KotlinCompilationUnitFactory#RESULT_SPEC`.
     */
    val RESULT_OVERRIDES = mapOf(
        "hgetall" to "Flow<KeyValue<K, V>>",
        "zmscore" to "List<Double?>"
    )

    /**
     * Every table above, by name, so that [KotlinCoroutinesConsistencyUnitTests] can verify none of them has gone stale — the
     * Kotlin half of what `DeviationTableStalenessUnitTests` does for [KnownApiDeviations].
     */
    val ALL_TABLES: Map<String, Set<String>> = mapOf(
        "SKIP" to SKIP,
        "NON_SUSPENDABLE" to NON_SUSPENDABLE,
        "FLOW" to FLOW,
        "KEEP_DEPRECATED" to KEEP_DEPRECATED,
        "NON_NULLABLE_RESULT" to NON_NULLABLE_RESULT,
        "UNSOUND_NON_NULLABLE_RESULT" to UNSOUND_NON_NULLABLE_RESULT,
        "AGGREGATE_PENDING" to AGGREGATE_PENDING,
        "RESULT_OVERRIDES" to RESULT_OVERRIDES.keys
    )

}
