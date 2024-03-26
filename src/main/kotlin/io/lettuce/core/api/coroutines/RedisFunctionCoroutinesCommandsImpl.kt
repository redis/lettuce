
package io.lettuce.core.api.coroutines

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.FlushMode
import io.lettuce.core.FunctionRestoreMode
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.api.reactive.RedisFunctionReactiveCommands
import kotlinx.coroutines.reactive.awaitFirstOrNull


/**
 * Coroutine executed commands (based on reactive commands) for the Function API.
 *
 * @author Mark Paluch
 * @since 6.3
 */
@ExperimentalLettuceCoroutinesApi
internal class RedisFunctionCoroutinesCommandsImpl<K : Any, V : Any>(internal val ops: RedisFunctionReactiveCommands<K, V>) :
    RedisFunctionCoroutinesCommands<K, V> {

    override suspend fun <T : Any> fcall(
        function: String,
        type: ScriptOutputType,
        vararg keys: K
    ): T? = ops.fcall<T>(function, type, *keys).awaitFirstOrNull()

    override suspend fun <T : Any> fcall(
        function: String,
        type: ScriptOutputType,
        keys: Array<K>,
        vararg values: V
    ): T? = ops.fcall<T>(function, type, keys, *values).awaitFirstOrNull()

    override suspend fun <T : Any> fcallReadOnly(
        function: String,
        type: ScriptOutputType,
        vararg keys: K
    ): T? = ops.fcallReadOnly<T>(function, type, *keys).awaitFirstOrNull()

    override suspend fun <T : Any> fcallReadOnly(
        function: String,
        type: ScriptOutputType,
        keys: Array<K>,
        vararg values: V
    ): T? = ops.fcallReadOnly<T>(function, type, keys, *values).awaitFirstOrNull()

    override suspend fun functionLoad(functionCode: String): String? =
        ops.functionLoad(functionCode).awaitFirstOrNull()

    override suspend fun functionLoad(functionCode: String, replace: Boolean): String? =
        ops.functionLoad(functionCode, replace).awaitFirstOrNull()

    override suspend fun functionDump(): ByteArray? =
        ops.functionDump().awaitFirstOrNull()

    override suspend fun functionRestore(dump: ByteArray): String? =
        ops.functionRestore(dump).awaitFirstOrNull()

    override suspend fun functionRestore(
        dump: ByteArray,
        mode: FunctionRestoreMode
    ): String? = ops.functionRestore(dump, mode).awaitFirstOrNull()

    override suspend fun functionFlush(flushMode: FlushMode): String? =
        ops.functionFlush(flushMode).awaitFirstOrNull()

    override suspend fun functionKill(): String? = ops.functionKill().awaitFirstOrNull()

    override suspend fun functionList(): List<Map<String, Any>> =
        ops.functionList().collectList().awaitFirstOrNull()!!

    override suspend fun functionList(libraryName: String): List<Map<String, Any>> =
        ops.functionList(libraryName).collectList().awaitFirstOrNull()!!

}

