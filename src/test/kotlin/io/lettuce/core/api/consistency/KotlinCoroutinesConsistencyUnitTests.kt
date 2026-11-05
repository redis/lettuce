/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.api.consistency

import io.lettuce.TestTags.UNIT_TEST
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import io.lettuce.core.cluster.api.coroutines.RedisClusterCoroutinesCommands
import java.lang.reflect.Method
import java.util.EnumSet
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.javaMethod
import kotlinx.coroutines.flow.Flow
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

/**
 * Verify that the Kotlin coroutine command interfaces mirror the sync interfaces: every sync method (except deprecated,
 * streaming-channel and explicitly skipped methods) must exist as a `suspend fun`, or as a plain function when it is
 * non-suspendable or returns a [Flow], with the sync parameter and result types translated to their Kotlin counterparts.
 *
 * Comparison is textual over a canonical rendering ([canonical]) that folds the differences the two languages express
 * differently — boxing (`Long` / `long`), arrays (`Array<out K>` / `K[]`), variance (`out K` / `? extends K`) and
 * `Any` / `Object`. Top-level nullability is asserted separately against [KnownKotlinApiDeviations.NON_NULLABLE_RESULT];
 * nullability *inside* a generic argument (`List<Double?>`) is not verified.
 *
 * Also verifies that the coroutine aggregate interfaces extend the coroutine interface of every command group they are
 * supposed to cover, that the methods declared directly on the aggregates reach the coroutine aggregates, and that no entry of
 * [KnownKotlinApiDeviations] has gone stale — the coroutine counterparts of `AggregateInterfaceConsistencyUnitTests` and
 * `DeviationTableStalenessUnitTests`, kept here so the Java test sources stay free of compile-time references to Kotlin types.
 */
@OptIn(io.lettuce.core.ExperimentalLettuceCoroutinesApi::class)
@Tag(UNIT_TEST)
class KotlinCoroutinesConsistencyUnitTests {

    @ParameterizedTest
    @EnumSource(CommandInterfaces::class)
    fun syncMethodsExistOnCoroutinesApiWithMatchingShape(group: CommandInterfaces) {

        val softly = SoftAssertions()
        val coroutines = group.coroutines()
        val coroutineFunctions = commandFunctions(coroutines)

        for (syncMethod in TypeSignatures.apiMethods(group.sync())) {

            if (isSkippedOnCoroutinesApi(syncMethod, group.sync())) {
                continue
            }

            val function = coroutineFunctions[erasedKey(syncMethod.name, erasedParameters(syncMethod))]
            if (function == null) {
                softly.fail<Any>(
                    "%s is missing on %s", TypeSignatures.describe(group.sync(), syncMethod), coroutines.simpleName
                )
                continue
            }

            verifyShape(softly, group.sync(), coroutines, syncMethod, function)
        }

        softly.assertAll()
    }

    @ParameterizedTest
    @EnumSource(CommandInterfaces::class)
    fun coroutinesMethodsExistOnSyncOrAsyncApi(group: CommandInterfaces) {

        val softly = SoftAssertions()

        val knownKeys = TypeSignatures.apiMethods(group.sync()).map { erasedKey(it.name, erasedParameters(it)) } +
            TypeSignatures.apiMethods(group.async())
                .filter { KnownApiDeviations.contains(KnownApiDeviations.NOT_ON_SYNC_API, it, group.sync()) }
                .map { erasedKey(it.name, erasedParameters(it)) }

        for ((key, function) in commandFunctions(group.coroutines())) {
            if (key !in knownKeys) {
                softly.fail<Any>(
                    "%s.%s has no sync/async counterpart", group.coroutines().simpleName, function.name
                )
            }
        }

        softly.assertAll()
    }

    @Test
    fun standaloneAggregateCoversAllGroups() {

        val softly = SoftAssertions()

        for (group in EnumSet.complementOf(EnumSet.of(CommandInterfaces.SENTINEL))) {
            assertExtends(softly, RedisCoroutinesCommands::class.java, group.coroutines())
        }

        softly.assertAll()
    }

    @Test
    fun clusterAggregateCoversAllClusterGroups() {

        val softly = SoftAssertions()

        for (group in EnumSet.complementOf(EnumSet.of(CommandInterfaces.SENTINEL, CommandInterfaces.TRANSACTIONAL))) {
            assertExtends(softly, RedisClusterCoroutinesCommands::class.java, group.coroutines())
        }

        softly.assertAll()
    }

    private fun assertExtends(softly: SoftAssertions, aggregate: Class<*>, groupInterface: Class<*>) {
        softly.assertThat(groupInterface.isAssignableFrom(aggregate))
            .describedAs("%s must extend %s", aggregate.simpleName, groupInterface.simpleName).isTrue
    }

    /**
     * The aggregate interfaces declare command methods of their own ([AggregateInterfaces]); those must reach the coroutine
     * aggregates too. Methods the coroutine API does not expose yet are recorded in
     * [KnownKotlinApiDeviations.AGGREGATE_PENDING] rather than skipped silently.
     */
    @ParameterizedTest
    @EnumSource(AggregateInterfaces::class)
    fun aggregateDeclaredMethodsExistOnCoroutinesAggregate(aggregate: AggregateInterfaces) {

        val coroutines = aggregate.coroutines() ?: return // no coroutine flavor at all; recorded on the enum
        val softly = SoftAssertions()
        val coroutineFunctions = commandFunctions(coroutines, declaredOnly = false)

        for (syncMethod in TypeSignatures.apiMethods(aggregate.sync())) {

            if (isSkippedOnCoroutinesApi(syncMethod, aggregate.sync()) ||
                KnownApiDeviations.contains(KnownKotlinApiDeviations.AGGREGATE_PENDING, syncMethod, aggregate.sync())
            ) {
                continue
            }

            val function = coroutineFunctions[erasedKey(syncMethod.name, erasedParameters(syncMethod))]
            if (function == null) {
                softly.fail<Any>(
                    "%s is missing on %s", TypeSignatures.describe(aggregate.sync(), syncMethod), coroutines.simpleName
                )
                continue
            }

            verifyShape(softly, aggregate.sync(), coroutines, syncMethod, function)
        }

        softly.assertAll()
    }

    /**
     * Every key of every [KnownKotlinApiDeviations] table must still match a sync method — a stale entry keeps claiming a
     * deviation that no longer exists and, because the loosest key form is a bare method name, silently exempts the next
     * command that happens to share that name.
     */
    @Test
    fun deviationTableEntriesAreNotStale() {

        val syncInterfaces = CommandInterfaces.values().map { it.sync() } + AggregateInterfaces.values().map { it.sync() }
        val softly = SoftAssertions()

        for ((table, keys) in KnownKotlinApiDeviations.ALL_TABLES) {
            val unmatched = keys.filterNot { key ->
                syncInterfaces.any { syncGroup ->
                    TypeSignatures.apiMethods(syncGroup).any { KnownApiDeviations.contains(setOf(key), it, syncGroup) }
                }
            }
            softly.assertThat(unmatched)
                .describedAs("stale KnownKotlinApiDeviations.%s entries — they match no sync method", table)
                .isEmpty()
        }

        softly.assertAll()
    }

    private fun verifyShape(
        softly: SoftAssertions,
        syncGroup: Class<*>,
        coroutines: Class<*>,
        syncMethod: Method,
        function: KFunction<*>
    ) {

        val override = KnownApiDeviations.lookup(KnownKotlinApiDeviations.RESULT_OVERRIDES, syncMethod, syncGroup)
        val expectFlow = override?.startsWith("Flow<") ?: KnownApiDeviations.contains(
            KnownKotlinApiDeviations.FLOW, syncMethod, syncGroup
        )
        val expectSuspend = !expectFlow &&
            !KnownApiDeviations.contains(KnownKotlinApiDeviations.NON_SUSPENDABLE, syncMethod, syncGroup)

        val description = "${coroutines.simpleName}.${KnownApiDeviations.signatureKey(syncMethod)}"

        softly.assertThat(function.isSuspend).describedAs("suspend modifier of %s", description).isEqualTo(expectSuspend)

        softly.assertThat(parameterSignature(function)).describedAs("parameter types of %s", description)
            .isEqualTo(parameterSignature(syncMethod))

        if (expectFlow) {
            softly.assertThat(function.returnType.classifier).describedAs("return type of %s", description)
                .isEqualTo(Flow::class)

            val expectedElement = expectedFlowElement(override, syncMethod)
            if (expectedElement != null) {
                val actualElement = function.returnType.arguments.firstOrNull()?.type?.let { canonical(normalize(it)) }
                softly.assertThat(actualElement).describedAs("Flow element type of %s", description)
                    .isEqualTo(expectedElement)
            }
            softly.assertThat(function.returnType.isMarkedNullable).describedAs("nullability of %s", description)
                .isFalse()
            return
        }

        val expectedReturnType = expectedReturnType(override, syncMethod)
        if (expectedReturnType != null) {
            softly.assertThat(canonical(normalize(function.returnType))).describedAs("return type of %s", description)
                .isEqualTo(expectedReturnType)
        }

        softly.assertThat(function.returnType.isMarkedNullable).describedAs("nullability of %s", description)
            .isEqualTo(expectNullableResult(syncMethod, syncGroup))
    }

    /**
     * The expected suspended result: from the override, or the sync return type translated to Kotlin. Returns null when no
     * expectation can be derived.
     */
    private fun expectedReturnType(override: String?, syncMethod: Method): String? {

        if (override != null) {
            return if (override.startsWith("Flow<")) null else canonical(override)
        }
        return canonical(TypeSignatures.normalizeBoxed(syncMethod.genericReturnType))
    }

    /**
     * Whether the suspended result is nullable. Collections are returned empty rather than null, `Unit` cannot be nullable,
     * and [KnownKotlinApiDeviations.NON_NULLABLE_RESULT] records the commands that always answer.
     */
    private fun expectNullableResult(syncMethod: Method, syncGroup: Class<*>): Boolean {

        val syncReturn = TypeSignatures.normalizeBoxed(syncMethod.genericReturnType)
        if (syncReturn == "Void" || syncReturn.startsWith("List<") || syncReturn.startsWith("Set<")) {
            return false
        }
        return !KnownApiDeviations.contains(KnownKotlinApiDeviations.NON_NULLABLE_RESULT, syncMethod, syncGroup) &&
            !KnownApiDeviations.contains(KnownKotlinApiDeviations.UNSOUND_NON_NULLABLE_RESULT, syncMethod, syncGroup)
    }

    /**
     * The expected Flow element: from the override, or the element type of a sync `List`/`Set` result, or the bare sync
     * return type (generic dispatch). Returns null when no expectation can be derived.
     */
    private fun expectedFlowElement(override: String?, syncMethod: Method): String? {

        if (override != null) {
            return canonical(override.removePrefix("Flow<").removeSuffix(">"))
        }

        val syncReturn = TypeSignatures.normalizeBoxed(syncMethod.genericReturnType)
        val element = when {
            syncReturn.startsWith("List<") -> syncReturn.removePrefix("List<").removeSuffix(">")
            syncReturn.startsWith("Set<") -> syncReturn.removePrefix("Set<").removeSuffix(">")
            syncReturn.contains('<') -> return null // no mechanical mapping for other generic containers
            else -> syncReturn
        }
        return canonical(element)
    }

    private fun isSkippedOnCoroutinesApi(syncMethod: Method, syncGroup: Class<*>): Boolean {

        if (KnownApiDeviations.contains(KnownKotlinApiDeviations.SKIP, syncMethod, syncGroup)) {
            return true
        }
        if (TypeSignatures.isStreamingChannelMethod(syncMethod)) {
            return true
        }
        return syncMethod.isAnnotationPresent(java.lang.Deprecated::class.java) &&
            !KnownApiDeviations.contains(KnownKotlinApiDeviations.KEEP_DEPRECATED, syncMethod, syncGroup)
    }

    /**
     * Index the command functions of a coroutine interface by name and erased parameter types, dropping the trailing
     * `Continuation` parameter of suspend functions so keys align with the sync methods.
     *
     * @param declaredOnly index only the functions declared on the interface itself; the aggregates inherit most of their
     *        surface from the per-group interfaces and need the inherited functions too.
     */
    private fun commandFunctions(coroutines: Class<*>, declaredOnly: Boolean = true): Map<String, KFunction<*>> {

        val functions = if (declaredOnly) coroutines.kotlin.declaredMemberFunctions else coroutines.kotlin.memberFunctions

        return functions.mapNotNull { function ->
            val javaMethod = function.javaMethod ?: return@mapNotNull null
            val parameters = erasedParameters(javaMethod).let { if (function.isSuspend) it.dropLast(1) else it }
            erasedKey(function.name, parameters) to function
        }.toMap()
    }

    /**
     * Erased parameter names, folding boxed types onto their primitives — the Kotlin flavor idiomatically declares
     * `Long`/`vararg Double` where Java uses the boxed `Long`/`Double[]`.
     */
    private fun erasedParameters(method: Method): List<String> =
        method.parameterTypes.map { PRIMITIVE_EQUIVALENTS[it.name] ?: it.name }

    private fun erasedKey(name: String, parameters: List<String>) = "$name(${parameters.joinToString(",")})"

    /** The canonical parameter list of a coroutine function, per parameter so that arrays and varargs fold correctly. */
    private fun parameterSignature(function: KFunction<*>): String =
        function.valueParameters.joinToString(", ") { canonical(normalize(it.type)) }

    /** The same rendering for the sync counterpart, so the two lists are directly comparable. */
    private fun parameterSignature(method: Method): String =
        method.genericParameterTypes.joinToString(", ") { canonical(TypeSignatures.normalize(it)) }

    /** Render a Kotlin type as a normalized, package-less string. */
    private fun normalize(type: KType): String =
        type.toString().replace('$', '.').replace(Regex("(?:[a-z][A-Za-z0-9_]*\\.)+"), "").replace("!", "").trim()

    /**
     * Reduce a normalized Java or Kotlin type rendering to a common form, so that the two flavors can be compared without
     * tripping over the ways the languages spell the same thing. Arrays are folded at the top level only, and the Kotlin
     * nullability marker is dropped — nullability is asserted separately, and only at the top level.
     */
    private fun canonical(type: String): String {

        // drop Kotlin's "?" nullability marker (it always follows a type) but keep a Java "?" wildcard (it never does),
        // and read Kotlin's star projection as that wildcard
        var result = type.trim().replace(Regex("(?<=[\\w>\\]])\\?"), "").replace("*", "?")

        // Kotlin's dedicated array types and the Array<out T> a vararg parameter reports
        result = ARRAY_TYPES[result] ?: result
        for (prefix in listOf("Array<out ", "Array<in ", "Array<")) {
            if (result.startsWith(prefix) && result.endsWith(">")) {
                result = canonical(result.substring(prefix.length, result.length - 1)) + "[]"
                break
            }
        }

        result = result.replace("out ", "? extends ").replace("in ", "? super ")

        for ((from, to) in SCALAR_EQUIVALENTS) {
            result = result.replace(Regex("\\b$from\\b"), to)
        }

        return result
    }

    companion object {

        private val PRIMITIVE_EQUIVALENTS = mapOf(
            "java.lang.Boolean" to "boolean", "java.lang.Byte" to "byte", "java.lang.Short" to "short",
            "java.lang.Integer" to "int", "java.lang.Long" to "long", "java.lang.Float" to "float",
            "java.lang.Double" to "double", "java.lang.Character" to "char",
            "[Ljava.lang.Boolean;" to "[Z", "[Ljava.lang.Byte;" to "[B", "[Ljava.lang.Short;" to "[S",
            "[Ljava.lang.Integer;" to "[I", "[Ljava.lang.Long;" to "[J", "[Ljava.lang.Float;" to "[F",
            "[Ljava.lang.Double;" to "[D", "[Ljava.lang.Character;" to "[C"
        )

        private val ARRAY_TYPES = mapOf(
            "ByteArray" to "byte[]", "ShortArray" to "short[]", "IntArray" to "int[]", "LongArray" to "long[]",
            "FloatArray" to "float[]", "DoubleArray" to "double[]", "BooleanArray" to "boolean[]",
            "CharArray" to "char[]"
        )

        /**
         * Boxing and naming differences, applied to both flavors so the comparison stays symmetric. Kotlin declares `Long`
         * where the Java API declares `long` (and vice versa), so both fold onto the primitive spelling.
         */
        private val SCALAR_EQUIVALENTS = listOf(
            "Any" to "Object", "Unit" to "Void",
            "MutableList" to "List", "MutableMap" to "Map", "MutableSet" to "Set", "MutableCollection" to "Collection",
            "Integer" to "int", "Int" to "int", "Long" to "long", "Double" to "double", "Boolean" to "boolean",
            "Byte" to "byte", "Short" to "short", "Float" to "float", "Character" to "char", "Char" to "char"
        )
    }

}
