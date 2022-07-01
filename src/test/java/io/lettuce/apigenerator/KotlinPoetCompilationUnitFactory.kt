@file:Suppress("CanBeParameter")

package io.lettuce.apigenerator

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.AnnotationExpr
import com.github.javaparser.ast.type.ClassOrInterfaceType
import com.github.javaparser.ast.type.PrimitiveType.Primitive
import com.github.javaparser.ast.type.Type
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.KModifier.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import java.io.File
import java.lang.Deprecated
import java.util.regex.Pattern
import kotlin.Boolean
import kotlin.String
import kotlin.Suppress
import kotlin.let
import kotlin.requireNotNull
import kotlin.to

class KotlinPoetCompilationUnitFactory(
    private val templateFile: File,
    private val src: File,
    private val targetPackage: String,
    private val targetName: String,
    private val commentReplacer: (String) -> String
) {

    private val template = JavaParser.parse(templateFile)

    fun create() {
        val header = template.comment.get().toString().replace("Copyright (\\d{4})-(\\d{4})".toRegex(), "Copyright 2020-$2").plus("\n")

        val content = FileSpec.builder(targetPackage, targetName)
            .addKotlinDefaultImports(includeJvm = false, includeJs = false)
            .indent(FORMATTING_INDENT)
            .addType(
                TypeSpec.interfaceBuilder(targetName)
                    .addAnnotation(EXPERIMENTAL_API)
                    .addKdoc(commentReplacer(classDoc()))
                    .addTypeVariables(templateClass().typeParameters.map { typeParam ->
                        TypeVariableName(typeParam.nameAsString, ANY)
                    })
                    .addFunctions(methods()
                        .filter { method ->
                            (!method.isAnnotationPresent(Deprecated::class.java) || method.isIn(KEEP_DEPRECATED_METHODS.keys))
                                    && !method.isIn(SKIP_METHODS)
                                    && method.parameters.none { p -> "StreamingChannel" in p.typeAsString }
                        }
                        .map { method ->
                            FunSpec.builder(method.nameAsString)
                                .addModifiers(ABSTRACT)
                                .addKdoc(method.javadoc.orElse(null)?.toText()?.toKdoc().orEmpty())
                                .addModifiers(
                                    when {
                                        method.isIn(NON_SUSPENDABLE_METHODS) || method.isIn(FLOW_METHODS) -> emptyList()
                                        else -> listOf(SUSPEND)
                                    }
                                )
                                .addTypeVariables(method.typeParameters.map { typeParam ->
                                    TypeVariableName(typeParam.nameAsString, when {
                                        method.isIn(FLOW_METHODS) -> listOf(ANY)
                                        else -> typeParam.typeBound.map { it.toType() }
                                    })
                                })
                                .addAnnotations(method.annotations.map { ann ->
                                    val annotation = ann.nameAsString
                                    val identifier = method.name.identifier
                                    val replacement = KEEP_DEPRECATED_METHODS[identifier]
                                    if (annotation == "Deprecated") {
                                        AnnotationSpec.builder(DEPRECATED)
                                            .let {
                                                when {
                                                    replacement != null -> it.addMember("\"Use [$replacement] instead.\", ReplaceWith(\"$replacement\")")
                                                    else -> it.addMember("\"To be removed in next major release\"")
                                                }
                                            }
                                            .build()
                                    } else {
                                        AnnotationSpec.builder(ClassName.bestGuess(ann.fqn()))
                                            .build()
                                    }
                                })
                                .addParameters(
                                    method.parameters.map { param ->
                                        ParameterSpec.builder(param.nameAsString, param.type.toType(isForceNotNullable = true))
                                            .addModifiers(if (param.isVarArgs) listOf(VARARG) else emptyList())
                                            .build()
                                    }
                                )
                                .returns(CUSTOM_RESULT_SPEC[method.nameAsString] ?: method.type.toType(method = method))
                                .build()
                        })
                    .build()
            )
            .build()
            .toString()


        File(src, "${targetPackage.replace(".", "/")}/$targetName.kt").writeText(header + content)
    }

    private fun Type.toType(
        type: String = asString(),
        method: MethodDeclaration? = null,
        isFlowable: Boolean = method?.isIn(FLOW_METHODS) ?: false,
        isForceNotNullable: Boolean = method?.isIn(NON_NULLABLE_RESULT_METHODS) ?: false
    ): TypeName = when {
        isFlowable -> {
            FLOW
                .let {
                    when {
                        isCollection() || isMap() -> it.parameterizedBy(typeArguments().first().toType(isForceNotNullable = true))
                        else -> it.parameterizedBy(toType(isForceNotNullable = true))
                    }
                }
        }
        isPrimitiveType -> {
            val primitive = asPrimitiveType()
            when (requireNotNull(primitive.type)) {
                Primitive.BOOLEAN -> BOOLEAN
                Primitive.BYTE -> BYTE
                Primitive.CHAR -> CHAR
                Primitive.DOUBLE -> DOUBLE
                Primitive.FLOAT -> FLOAT
                Primitive.INT -> INT
                Primitive.LONG -> LONG
                Primitive.SHORT -> SHORT
            }
        }
        this is ClassOrInterfaceType && isBoxedType -> {
            toUnboxedType().toType()
        }
        isVoidType -> {
            UNIT
        }
        type == "Object" -> {
            ANY
        }
        type == "String" -> {
            STRING
        }
        type == "Number" -> {
            NUMBER
        }
        type == "T" -> {
            TypeVariableName(type)
        }
        isTypeParameter || type in templateClass().typeParameters.map { it.nameAsString } -> {
            TypeVariableName(type, ANY)
        }
        isWildcardType -> {
            val wildcardType = asWildcardType()
            when {
                wildcardType.extendedType.isPresent -> WildcardTypeName.producerOf(wildcardType.extendedType.get().toType(isForceNotNullable = true))
                wildcardType.superType.isPresent -> WildcardTypeName.consumerOf(wildcardType.extendedType.get().toType(isForceNotNullable = true))
                else -> STAR
            }
        }
        isArrayType -> {
            val componentType = asArrayType().componentType
            if (componentType.isPrimitiveType) {
                val primitive = componentType.asPrimitiveType()
                when (requireNotNull(primitive.type)) {
                    Primitive.BOOLEAN -> BOOLEAN_ARRAY
                    Primitive.BYTE -> BYTE_ARRAY
                    Primitive.CHAR -> CHAR_ARRAY
                    Primitive.DOUBLE -> DOUBLE_ARRAY
                    Primitive.FLOAT -> FLOAT_ARRAY
                    Primitive.INT -> INT_ARRAY
                    Primitive.LONG -> LONG_ARRAY
                    Primitive.SHORT -> SHORT_ARRAY
                }
            } else {
                ARRAY.parameterizedBy(componentType.toType(isForceNotNullable = true))
            }
        }
        typeArguments().isNotEmpty() -> {
            when {
                type.startsWith("Collection<") -> COLLECTION
                type.startsWith("List<") -> LIST
                type.startsWith("Set<") -> SET
                type.startsWith("Map<") -> MAP
                else -> ClassName.bestGuess(fqn())
            }.parameterizedBy(typeArguments().map { typeArg -> typeArg.toType(isForceNotNullable = true) })
        }
        else -> {
            ClassName.bestGuess(fqn())
        }
    }.copy(nullable = !isForceNotNullable && !isVoidType && !isFlowable && !isPrimitiveType && !isWildcardType && !isCollection())

    private fun templateClass() =
        template.types[0] as ClassOrInterfaceDeclaration
    private fun fileDoc(): String =
        template.comment.orElse(null)?.content.orEmpty().toKdoc()
    private fun classDoc(): String =
        templateClass().javadoc.orElse(null)?.toComment()?.content.orEmpty().toKdoc()
    private fun methods(): List<MethodDeclaration> =
        template.types[0].members.filterIsInstance<MethodDeclaration>()
    private fun MethodDeclaration.isIn(haystack: Collection<String>): Boolean =
        nameAsString in haystack || declarationAsString.substringAfter(" ") in haystack ||(parentNode.get() as ClassOrInterfaceDeclaration).nameAsString + "." + nameAsString in haystack
    private fun Type.isCollection(): Boolean =
        listOf("Collection<", "List<", "Set<").any { asString().startsWith(it) }
    private fun Type.isMap(): Boolean =
        listOf("Map<").any { asString().startsWith(it) }
    private fun Type.typeArguments(): List<Type> =
        (this as ClassOrInterfaceType).typeArguments.orElse(null).orEmpty()
    private fun Type.fqn(): String =
        asString().fqn()
    private fun AnnotationExpr.fqn(): String =
        nameAsString.fqn()
    private fun String.fqn(): String =
        "${template.imports.find { it.nameAsString.endsWith(substringBefore("<")) }?.nameAsString?.substringBeforeLast(".") ?: "io.lettuce.core"}.${substringBefore("<")}"

    private fun String.toKdoc(): String {
        return this
            .replaceSurrounding("{@code ", "}", "`", "`")
            .replaceSurrounding("{@link ", "}", "[", "]")
            .replace("java.lang.Object", "Any")
            .replace("Object", "Any")
            .replace("\\bdouble\\b".toRegex(), "Double")
            .replace("\\bint\\b".toRegex(), "Integer")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace(" * ", "")
    }

    private fun String.replaceSurrounding(
        prefix: String,
        suffix: String,
        replacePrefix: String,
        replaceSuffix: String
    ): String {
        val matcher = Pattern.compile(Pattern.quote(prefix) + "[a-zA-Z\\d.,#\\-~()*\\s]+" + Pattern.quote(suffix)).matcher(this)
        val result = StringBuffer()
        while (matcher.find()) {
            val substr = matcher.group().substringAfter(prefix).substringBefore(suffix)
            val replacement = (replacePrefix + substr + replaceSuffix).replace("()", "").replace("$replacePrefix#", replacePrefix)
            matcher.appendReplacement(result, replacement)
        }
        matcher.appendTail(result)
        return result.toString()
    }

    companion object {
        private const val FORMATTING_INDENT = "    "
        private val FLOW = ClassName("kotlinx.coroutines.flow", "Flow")
        private val DEPRECATED = ClassName("kotlin", "Deprecated")
        private val EXPERIMENTAL_API = ClassName("io.lettuce.core", "ExperimentalLettuceCoroutinesApi")
        private val NON_SUSPENDABLE_METHODS = setOf(
            "isOpen",
            "flushCommands",
            "setAutoFlushCommands"
        )
        private val SKIP_METHODS = setOf(
            "BaseRedisCommands.reset",
            "getStatefulConnection"
        )
        private val FLOW_METHODS = setOf(
            "aclList",
            "aclLog",
            "dispatch",
            "geohash",
            "georadius(K key, double longitude, double latitude, double distance, GeoArgs.Unit unit)",
            "georadius(K key, double longitude, double latitude, double distance, GeoArgs.Unit unit, GeoArgs geoArgs)",
            "georadiusbymember(K key, V member, double distance, GeoArgs.Unit unit)",
            "georadiusbymember(K key, V member, double distance, GeoArgs.Unit unit, GeoArgs geoArgs)",
            "geosearch",
            "hgetall",
            "hkeys",
            "hmget",
            "hvals",
            "keys",
            "mget",
            "sdiff",
            "sinter",
            "smembers",
            "smismember",
            "sort",
            "sortReadOnly",
            "srandmember(K key, long count)",
            "srandmember(ValueStreamingChannel<V> channel, K key, long count)",
            "sunion",
            "xclaim",
            "xpending(K key, K group, Range<String> range, Limit limit)",
            "xpending(K key, Consumer<K> consumer, Range<String> range, Limit limit)",
            "xpending(K key, XPendingArgs<K> args)",
            "xrange",
            "xread",
            "xreadgroup",
            "xrevrange",
            "zdiff",
            "zdiffWithScores",
            "zinter",
            "zinterWithScores",
            "zpopmax(K key, long count)",
            "zpopmin(K key, long count)",
            "zrange",
            "zrangeWithScores",
            "zrangebylex",
            "zrangebyscore",
            "zrangebyscoreWithScores",
            "zrevrange",
            "zrevrangeWithScores",
            "zrevrangebylex",
            "zrevrangebyscore",
            "zrevrangebyscore",
            "zrevrangebyscoreWithScores",
            "zunion",
            "zunionWithScores"
        )
        private val NON_NULLABLE_RESULT_METHODS = setOf(
            "discard",
            "multi",
            "exec",
            "watch",
            "unwatch",
            "getMasterAddrByName",
            "master",
            "reset",
            "failover",
            "monitor",
            "RedisSentinelCoroutinesCommands.set",
            "remove",
            "RedisSentinelCoroutinesCommands.clientSetname",
            "RedisSentinelCoroutinesCommands.clientKill",
            "RedisSentinelCoroutinesCommands.clientPause",
            "RedisSentinelCoroutinesCommands.clientList",
            "RedisSentinelCoroutinesCommands.info",
            "RedisSentinelCoroutinesCommands.ping",
            "pubsubNumsub",
            "pubsubNumpat",
            "echo",
            "ping",
            "readOnly",
            "readWrite"
        )
        private val CUSTOM_RESULT_SPEC = mapOf(
            "hgetall" to FLOW.parameterizedBy(ClassName("io.lettuce.core", "KeyValue").parameterizedBy(TypeVariableName("K", ANY), TypeVariableName("V", ANY))),
            "zmscore" to LIST.parameterizedBy(DOUBLE.copy(nullable = true))
        )
        private val KEEP_DEPRECATED_METHODS = mapOf(
            "flushallAsync" to "flushall(FlushMode.ASYNC)",
            "flushdbAsync" to "flushdb(FlushMode.ASYNC)",
            "slaveof" to "replicaof",
            "slaveofNoOne" to "replicaofNoOne",
            "slaves" to "replicas",
            "isOpen" to null,
            "setAutoFlushCommands" to null,
            "flushCommands" to null
        )
    }
}