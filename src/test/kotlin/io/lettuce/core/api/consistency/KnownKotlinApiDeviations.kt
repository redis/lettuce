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
    val name = sync().name.replace(".api.sync.", ".api.coroutines.").replace(Regex("Commands$"), "CoroutinesCommands")
    try {
        return Class.forName(name)
    } catch (e: ClassNotFoundException) {
        throw IllegalStateException("Coroutines flavor of $this not found under conventional name $name", e)
    }
}

/**
 * Registry of all known, intentional deviations of the Kotlin coroutine interfaces from the sync reference — the Kotlin
 * counterpart of [KnownApiDeviations], kept in the Kotlin test sources because only the coroutine flavor consults it.
 * Key forms and matching rules are those of [KnownApiDeviations.contains] and [KnownApiDeviations.lookup].
 *
 * The tables were ported verbatim from the former `io.lettuce.apigenerator.KotlinCompilationUnitFactory` generator.
 */
object KnownKotlinApiDeviations {

    /**
     * Sync methods with no coroutine counterpart. From `KotlinCompilationUnitFactory#SKIP_METHODS`.
     */
    val SKIP = setOf("getStatefulConnection")

    /**
     * Coroutine methods that are plain functions instead of `suspend` functions. From
     * `KotlinCompilationUnitFactory#NON_SUSPENDABLE_METHODS`.
     */
    val NON_SUSPENDABLE = setOf("isOpen", "flushCommands", "setAutoFlushCommands")

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
     * Coroutine methods with a fully overridden return type (normalized, package-less Kotlin rendering). From
     * `KotlinCompilationUnitFactory#RESULT_SPEC`.
     */
    val RESULT_OVERRIDES = mapOf(
        "hgetall" to "Flow<KeyValue<K, V>>",
        "zmscore" to "List<Double?>"
    )

}
