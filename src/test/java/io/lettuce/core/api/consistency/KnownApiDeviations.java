/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.api.consistency;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

/**
 * Registry of all known, intentional deviations between the per-flavor Redis command interfaces. The API consistency test suite
 * consults this registry; anything not listed here must follow the regular mapping rules (see
 * {@code .agents/docs/api-consistency.md}).
 * <p>
 * The tables were ported verbatim from the former {@code io.lettuce.apigenerator} generators
 * ({@code CreateSyncApi#FILTER_METHODS}, {@code CreateAsyncApi#KEEP_METHOD_RESULT_TYPE},
 * {@code CreateReactiveApi#KEEP_METHOD_RESULT_TYPE/FORCE_FLUX_RESULT/VALUE_WRAP/RESULT_SPEC},
 * {@code Create*NodeSelectionClusterApi#FILTER_METHODS} and {@code KotlinCompilationUnitFactory}).
 * <p>
 * Keys may take three forms, checked from most to least specific:
 * <ul>
 * <li>{@code methodName(ErasedParamSimpleName, ...)} — a specific overload, e.g. {@code aclCat(AclCategory)}</li>
 * <li>{@code SyncInterfaceSimpleName.methodName} — all overloads within one group, e.g. {@code BaseRedisCommands.reset}</li>
 * <li>{@code methodName} — all overloads in all groups</li>
 * </ul>
 * A deviation entry must never be used to paper over a sync/async signature mismatch: the sync API is a runtime proxy over the
 * async API, so such a mismatch fails at runtime.
 */
public final class KnownApiDeviations {

    /**
     * Methods that exist on the async, reactive and coroutine APIs but intentionally not on the sync API (flushing makes no
     * sense for synchronously dispatched commands). From {@code CreateSyncApi#FILTER_METHODS}.
     */
    public static final Set<String> NOT_ON_SYNC_API = setOf("setAutoFlushCommands", "flushCommands");

    /**
     * Async methods that keep the sync return type instead of wrapping it in {@code RedisFuture}. From
     * {@code CreateAsyncApi#KEEP_METHOD_RESULT_TYPE}.
     */
    public static final Set<String> KEEP_SYNC_RESULT_TYPE_ASYNC = setOf("shutdown", "debugOom", "debugSegfault", "digest",
            "close", "isOpen", "getStatefulConnection", "setAutoFlushCommands", "flushCommands", "setTimeout", "getJsonParser");

    /**
     * Reactive methods that keep the sync return type instead of wrapping it in {@code Mono}/{@code Flux}. From
     * {@code CreateReactiveApi#KEEP_METHOD_RESULT_TYPE}.
     */
    public static final Set<String> KEEP_SYNC_RESULT_TYPE_REACTIVE = setOf("digest", "close", "isOpen", "getStatefulConnection",
            "setAutoFlushCommands", "flushCommands", "setTimeout", "getJsonParser");

    /**
     * Aggregate-declared methods whose return type is flavor-specific by design (connection and node-selection accessors, e.g.
     * {@code getConnection} returns {@code RedisClusterCommands} on the sync aggregate and {@code RedisClusterAsyncCommands} on
     * the async one). Presence is still verified; the return-type check is skipped.
     */
    public static final Set<String> AGGREGATE_FLAVOR_SPECIFIC_RETURN = setOf("getConnection", "getStatefulConnection",
            "masters", "upstream", "slaves", "replicas", "all", "readonly", "nodes");

    /**
     * Aggregate-declared methods without a reactive counterpart: the node-selection API exists for the sync and async flavors
     * only.
     */
    public static final Set<String> NOT_ON_REACTIVE_AGGREGATE = setOf("masters", "upstream", "slaves", "replicas", "all",
            "readonly", "nodes");

    /**
     * Methods that exist only on the reactive API (reactive-specific accessors used by the coroutine implementations).
     */
    public static final Set<String> REACTIVE_ONLY = setOf("getJsonParser");

    /**
     * Methods whose reactive parameter generics intentionally differ from the sync flavor: {@code dispatch} declares
     * {@code CommandOutput<K, V, ?>} instead of {@code CommandOutput<K, V, T>} (the reactive result is emitted by the
     * publisher, not by the output's type argument). From {@code CreateReactiveApi#methodMutator()}.
     */
    public static final Set<String> REACTIVE_PARAMETER_FLAVOR_SPECIFIC = setOf("dispatch");

    /**
     * Reactive methods returning {@code Flux} although the sync return type is not a {@code List}/{@code Set}. From
     * {@code CreateReactiveApi#FORCE_FLUX_RESULT}.
     */
    public static final Set<String> FORCE_FLUX = setOf("eval", "evalsha", "evalReadOnly", "evalshaReadOnly", "fcall",
            "fcallReadOnly", "dispatch");

    /**
     * Reactive methods whose element type is wrapped in {@code Value} because Redis may return null elements. From
     * {@code CreateReactiveApi#VALUE_WRAP}.
     */
    public static final Set<String> REACTIVE_VALUE_WRAP = setOf("geopos", "bitfield", "bfInsert", "bfMAdd", "cfInsertNx",
            "topKAdd", "topKIncrBy",
            // calibration 2026-07: RedisArray element reads return null for missing indexes
            "argetrange", "armget");

    /**
     * Reactive methods with a fully overridden return type (normalized, package-less rendering). From
     * {@code CreateReactiveApi#RESULT_SPEC}.
     */
    public static final Map<String, String> REACTIVE_RESULT_OVERRIDES;

    /**
     * Methods deliberately absent from the cluster node-selection interfaces (connection-control and per-node-only commands).
     * From {@code Create(Sync|Async)NodeSelectionClusterApi#FILTER_METHODS}.
     */
    public static final Set<String> NODE_SELECTION_EXCLUDED = setOf("shutdown", "debugOom", "debugSegfault", "digest",
            "readOnly", "readWrite", "setAutoFlushCommands", "flushCommands");

    /**
     * Methods absent from the node-selection <em>sync</em> flavor only: {@code dispatch} exists on
     * {@code BaseNodeSelectionAsyncCommands} (including {@code Supplier}-based overloads without a sync counterpart) but not on
     * the sync node-selection API.
     */
    public static final Set<String> NOT_ON_NODE_SELECTION_SYNC = setOf("dispatch");

    /**
     * Methods deprecated on the async node-selection API although their sync counterpart is not: the {@code CommandOutput}-
     * based {@code dispatch} overloads are deprecated since 6.2 in favor of the {@code Supplier}-based overloads because a
     * single output instance cannot be reused across the responses of multiple nodes.
     */
    public static final Set<String> NODE_SELECTION_EXTRA_DEPRECATED = setOf("dispatch");

    /**
     * Command groups whose node-selection aggregate wiring is known to be incomplete: {@code NodeSelectionCommands} and
     * {@code NodeSelectionAsyncCommands} do not extend the ACL and Array groups, and {@code NodeSelectionAsyncCommands} extends
     * the <em>sync</em> {@code NodeSelectionStreamCommands} instead of the async flavor. Correcting the Stream wiring changes
     * the return types of stream commands on {@code AsyncNodeSelection} from {@code Executions} to {@code AsyncExecutions} — a
     * breaking change scheduled for the 8.0 release.
     */
    public static final Set<String> NODE_SELECTION_AGGREGATE_PENDING = setOf("ACL", "ARRAY", "STREAM");

    /**
     * Sync methods with no coroutine counterpart. From {@code KotlinCompilationUnitFactory#SKIP_METHODS}.
     */
    public static final Set<String> COROUTINES_SKIP = setOf("getStatefulConnection");

    /**
     * Coroutine methods that are plain functions instead of {@code suspend} functions. From
     * {@code KotlinCompilationUnitFactory#NON_SUSPENDABLE_METHODS}.
     */
    public static final Set<String> COROUTINES_NON_SUSPENDABLE = setOf("isOpen", "flushCommands", "setAutoFlushCommands");

    /**
     * Coroutine methods returning {@code Flow} instead of a suspended scalar/collection. From
     * {@code KotlinCompilationUnitFactory#FLOW_METHODS}.
     */
    public static final Set<String> COROUTINES_FLOW = setOf("aclList", "aclLog", "dispatch", "geohash", "georadius",
            "georadiusbymember", "geosearch", "hgetall", "hkeys", "hmget", "hvals", "keys", "mget", "sdiff", "sinter",
            "smembers", "smismember", "sort", "sortReadOnly", "sunion", "xclaim", "xrange", "xread", "xreadgroup", "xrevrange",
            "zdiff", "zdiffWithScores", "zinter", "zinterWithScores", "zrange", "zrangeWithScores", "zrangebylex",
            "zrangebyscore", "zrangebyscoreWithScores", "zrevrange", "zrevrangeWithScores", "zrevrangebylex",
            "zrevrangebyscore", "zrevrangebyscoreWithScores", "zunion", "zunionWithScores",
            // calibration 2026-07: only the multi-element overloads stream; the single-element overloads suspend
            "srandmember(K, long)", "zpopmax(K, long)", "zpopmin(K, long)", "xpending(K, K, Range, Limit)",
            "xpending(K, Consumer, Range, Limit)", "xpending(K, XPendingArgs)",
            // calibration 2026-07: multi-element commands added after the generators stopped being used
            "hgetdel", "hgetex", "xackdel", "xdelex");

    /**
     * Deprecated sync methods that are still exposed on the coroutine API. From
     * {@code KotlinCompilationUnitFactory#KEEP_DEPRECATED_METHODS}.
     */
    public static final Set<String> COROUTINES_KEEP_DEPRECATED = setOf("flushallAsync", "flushdbAsync", "slaveof",
            "slaveofNoOne", "slaves");

    /**
     * Coroutine methods with a fully overridden return type (normalized, package-less Kotlin rendering). From
     * {@code KotlinCompilationUnitFactory#RESULT_SPEC}.
     */
    public static final Map<String, String> COROUTINES_RESULT_OVERRIDES;

    /**
     * Interface methods that do not correspond to any command-builder method: connection control, locally computed values
     * ({@code digest}), generic dispatch and transaction plumbing ({@code exec} builds its command in the transaction
     * machinery, not in the builder).
     */
    public static final Set<String> BUILDER_EXCLUDED = setOf("close", "isOpen", "getStatefulConnection", "setAutoFlushCommands",
            "flushCommands", "digest", "dispatch", "exec");

    /**
     * Interface methods whose builder method has a different name (the builder variant takes a flag or a different argument
     * shape).
     */
    public static final Map<String, String> BUILDER_ALIASES;

    /**
     * Command-producing builder methods that are implementation details of the async/reactive/coroutine layers (streaming and
     * {@code Value}/{@code KeyValue}-wrapping variants, protocol handshake commands) rather than a public command entry point.
     */
    public static final Set<String> BUILDER_INTERNAL = setOf("bitfieldValue", "geoposValues", "hgetallKeyValue",
            "hmgetKeyValue", "mgetKeyValue", "armgetValues", "argetrangeValues", "bfInsertValues", "bfMAddValues",
            "cfInsertNxValues", "topKAddValues", "topKIncrByValues", "hscanStreaming", "hscanNoValuesStreaming",
            "scanStreaming", "sscanStreaming", "zscanStreaming", "hello", "sync", "clusterAddslots", "clusterDelslots",
            // connection-level commands exposed via StatefulRedisConnection, not via the command interfaces
            "select", "swapdb");

    static {
        Map<String, String> reactive = new HashMap<>();
        reactive.put("geopos", "Flux<Value<GeoCoordinates>>");
        reactive.put("aclCat()", "Mono<Set<AclCategory>>");
        reactive.put("aclCat(AclCategory)", "Mono<Set<CommandType>>");
        reactive.put("aclGetuser", "Mono<List<Object>>");
        reactive.put("bitfield", "Flux<Value<Long>>");
        reactive.put("hgetall", "Flux<KeyValue<K, V>>");
        // Redis returns null for elements that were not found, so the result is a Mono of a nullable-element list
        reactive.put("zmscore", "Mono<List<Double>>");
        reactive.put("hgetall(KeyValueStreamingChannel, K)", "Mono<Long>");
        // calibration 2026-07: entrenched aggregate-declared shapes; changing them to Flux would break the API
        reactive.put("clusterLinks", "Mono<List<Map<String, Object>>>");
        reactive.put("clusterShards", "Mono<List<Object>>");
        REACTIVE_RESULT_OVERRIDES = Collections.unmodifiableMap(reactive);

        Map<String, String> coroutines = new HashMap<>();
        coroutines.put("hgetall", "Flow<KeyValue<K, V>>");
        coroutines.put("zmscore", "List<Double?>");
        COROUTINES_RESULT_OVERRIDES = Collections.unmodifiableMap(coroutines);

        Map<String, String> builderAliases = new HashMap<>();
        builderAliases.put("waitForReplication", "wait");
        builderAliases.put("getMasterAddrByName", "getMasterAddrByKey");
        builderAliases.put("evalReadOnly", "eval");
        builderAliases.put("evalshaReadOnly", "evalsha");
        builderAliases.put("fcallReadOnly", "fcall");
        builderAliases.put("zrevrangestore", "zrangestore");
        builderAliases.put("zrevrangestorebylex", "zrangestorebylex");
        builderAliases.put("zrevrangestorebyscore", "zrangestorebyscore");
        builderAliases.put("flushallAsync", "flushall");
        builderAliases.put("flushdbAsync", "flushdb");
        builderAliases.put("vClearAttributes", "vsetattr");
        BUILDER_ALIASES = Collections.unmodifiableMap(builderAliases);
    }

    private KnownApiDeviations() {
    }

    /**
     * Check whether a method matches an entry of a deviation table, by overload signature, {@code Interface.method} or bare
     * method name.
     */
    public static boolean contains(Set<String> table, Method method, Class<?> declaringGroup) {
        return table.contains(signatureKey(method)) || table.contains(qualifiedKey(method, declaringGroup))
                || table.contains(method.getName());
    }

    /**
     * Look up an override for a method, from most specific (overload signature) to least specific (bare method name) key.
     *
     * @return the override value or {@code null} if none applies.
     */
    public static String lookup(Map<String, String> table, Method method, Class<?> declaringGroup) {
        String bySignature = table.get(signatureKey(method));
        if (bySignature != null) {
            return bySignature;
        }
        String byQualifiedName = table.get(qualifiedKey(method, declaringGroup));
        if (byQualifiedName != null) {
            return byQualifiedName;
        }
        return table.get(method.getName());
    }

    /**
     * Overload-specific key, e.g. {@code aclCat(AclCategory)} or {@code hgetall(KeyValueStreamingChannel, K)}. Generic
     * parameters use their type-variable name, other parameters their erased simple class name.
     */
    public static String signatureKey(Method method) {
        StringJoiner params = new StringJoiner(", ", method.getName() + "(", ")");
        java.lang.reflect.Type[] genericTypes = method.getGenericParameterTypes();
        Class<?>[] erased = method.getParameterTypes();
        for (int i = 0; i < erased.length; i++) {
            if (genericTypes[i] instanceof java.lang.reflect.TypeVariable) {
                params.add(((java.lang.reflect.TypeVariable<?>) genericTypes[i]).getName());
            } else {
                params.add(erased[i].getSimpleName());
            }
        }
        return params.toString();
    }

    private static String qualifiedKey(Method method, Class<?> declaringGroup) {
        return declaringGroup.getSimpleName() + "." + method.getName();
    }

    private static Set<String> setOf(String... entries) {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(entries)));
    }

}
