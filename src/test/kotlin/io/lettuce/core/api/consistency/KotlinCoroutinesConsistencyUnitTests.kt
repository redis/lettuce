/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.api.consistency

import io.lettuce.TestTags.UNIT_TEST
import java.lang.reflect.Method
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.jvm.javaMethod
import kotlinx.coroutines.flow.Flow
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

/**
 * Verify that the Kotlin coroutine command interfaces mirror the sync interfaces: every sync method (except deprecated,
 * streaming-channel and explicitly skipped methods) must exist as a `suspend fun`, or as a plain function when it is
 * non-suspendable or returns a [Flow]. Return-type details beyond the Flow/suspend shape (element nullability) are not
 * verified.
 */
@Tag(UNIT_TEST)
class KotlinCoroutinesConsistencyUnitTests {

    @ParameterizedTest
    @EnumSource(CommandInterfaces::class)
    fun syncMethodsExistOnCoroutinesApiWithMatchingShape(group: CommandInterfaces) {

        val softly = SoftAssertions()
        val coroutineFunctions = commandFunctions(group.coroutines())

        for (syncMethod in TypeSignatures.apiMethods(group.sync())) {

            if (isSkippedOnCoroutinesApi(syncMethod, group)) {
                continue
            }

            val function = coroutineFunctions[erasedKey(syncMethod.name, erasedParameters(syncMethod))]
            if (function == null) {
                softly.fail<Any>(
                    "%s is missing on %s", TypeSignatures.describe(group.sync(), syncMethod),
                    group.coroutines().simpleName
                )
                continue
            }

            verifyShape(softly, group, syncMethod, function)
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

    private fun verifyShape(softly: SoftAssertions, group: CommandInterfaces, syncMethod: Method, function: KFunction<*>) {

        val override = KnownApiDeviations.lookup(KnownApiDeviations.COROUTINES_RESULT_OVERRIDES, syncMethod, group.sync())
        val expectFlow = override?.startsWith("Flow<") ?: KnownApiDeviations.contains(
            KnownApiDeviations.COROUTINES_FLOW, syncMethod, group.sync()
        )
        val expectSuspend = !expectFlow &&
            !KnownApiDeviations.contains(KnownApiDeviations.COROUTINES_NON_SUSPENDABLE, syncMethod, group.sync())

        val description = "${group.coroutines().simpleName}.${KnownApiDeviations.signatureKey(syncMethod)}"

        softly.assertThat(function.isSuspend).describedAs("suspend modifier of %s", description).isEqualTo(expectSuspend)

        if (expectFlow) {
            softly.assertThat(function.returnType.classifier).describedAs("return type of %s", description)
                .isEqualTo(Flow::class)

            val expectedElement = expectedFlowElement(override, syncMethod)
            if (expectedElement != null) {
                val actualElement = function.returnType.arguments.firstOrNull()?.type?.let { normalize(it) }
                softly.assertThat(actualElement).describedAs("Flow element type of %s", description)
                    .isEqualTo(expectedElement)
            }
        }
    }

    /**
     * The expected Flow element: from the override, or the element type of a sync `List`/`Set` result, or the bare sync
     * return type (generic dispatch). Returns null when no expectation can be derived.
     */
    private fun expectedFlowElement(override: String?, syncMethod: Method): String? {

        if (override != null) {
            return override.removePrefix("Flow<").removeSuffix(">")
        }

        val syncReturn = TypeSignatures.normalizeBoxed(syncMethod.genericReturnType)
        val element = when {
            syncReturn.startsWith("List<") -> syncReturn.removePrefix("List<").removeSuffix(">")
            syncReturn.startsWith("Set<") -> syncReturn.removePrefix("Set<").removeSuffix(">")
            syncReturn.contains('<') -> return null // no mechanical mapping for other generic containers
            else -> syncReturn
        }
        return kotlinize(element)
    }

    private fun isSkippedOnCoroutinesApi(syncMethod: Method, group: CommandInterfaces): Boolean {

        if (KnownApiDeviations.contains(KnownApiDeviations.COROUTINES_SKIP, syncMethod, group.sync())) {
            return true
        }
        if (TypeSignatures.isStreamingChannelMethod(syncMethod)) {
            return true
        }
        return syncMethod.isAnnotationPresent(java.lang.Deprecated::class.java) &&
            !KnownApiDeviations.contains(KnownApiDeviations.COROUTINES_KEEP_DEPRECATED, syncMethod, group.sync())
    }

    /**
     * Index the command functions of a coroutine interface by name and erased parameter types, dropping the trailing
     * `Continuation` parameter of suspend functions so keys align with the sync methods.
     */
    private fun commandFunctions(coroutines: Class<*>): Map<String, KFunction<*>> {

        return coroutines.kotlin.declaredMemberFunctions.mapNotNull { function ->
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

    /** Render a Kotlin type as a normalized, package-less string, ignoring nullability. */
    private fun normalize(type: KType): String =
        type.toString().replace('$', '.').replace(Regex("(?:[a-z][A-Za-z0-9_]*\\.)+"), "").replace("?", "").trim()

    /** Translate a normalized Java type rendering to its Kotlin counterpart. */
    private fun kotlinize(javaType: String): String = javaType
        .replace("? extends ", "out ")
        .replace("? super ", "in ")
        .replace(Regex("\\bObject\\b"), "Any")
        .replace(Regex("\\bInteger\\b"), "Int")
        .replace("byte[]", "ByteArray")

    companion object {

        private val PRIMITIVE_EQUIVALENTS = mapOf(
            "java.lang.Boolean" to "boolean", "java.lang.Byte" to "byte", "java.lang.Short" to "short",
            "java.lang.Integer" to "int", "java.lang.Long" to "long", "java.lang.Float" to "float",
            "java.lang.Double" to "double", "java.lang.Character" to "char",
            "[Ljava.lang.Boolean;" to "[Z", "[Ljava.lang.Byte;" to "[B", "[Ljava.lang.Short;" to "[S",
            "[Ljava.lang.Integer;" to "[I", "[Ljava.lang.Long;" to "[J", "[Ljava.lang.Float;" to "[F",
            "[Ljava.lang.Double;" to "[D", "[Ljava.lang.Character;" to "[C"
        )
    }

}
