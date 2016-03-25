package com.lambdaworks.redis;

import java.util.List;

import com.lambdaworks.redis.api.async.RedisScriptingAsyncCommands;

/**
 * Asynchronous executed commands for Scripting.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 3.0
 * @deprecated Use {@literal RedisScriptingAsyncCommands}
 */
@Deprecated
public interface RedisScriptingAsyncConnection<K, V> {
    /**
     * Execute a Lua script server side.
     * 
     * @param script Lua 5.1 script.
     * @param type output type
     * @param keys key names
     * @param <T> expected return type
     * @return script result
     */
    <T> RedisFuture<T> eval(String script, ScriptOutputType type, K... keys);

    /**
     * Execute a Lua script server side.
     * 
     * @param script Lua 5.1 script.
     * @param type the type
     * @param keys the keys
     * @param values the values
     * @param <T> expected return type
     * @return script result
     */
    <T> RedisFuture<T> eval(String script, ScriptOutputType type, K[] keys, V... values);

    /**
     * Evaluates a script cached on the server side by its SHA1 digest
     * 
     * @param digest SHA1 of the script
     * @param type the type
     * @param keys the keys
     * @param <T> expected return type
     * @return script result
     */
    <T> RedisFuture<T> evalsha(String digest, ScriptOutputType type, K... keys);

    /**
     * Execute a Lua script server side.
     * 
     * @param digest SHA1 of the script
     * @param type the type
     * @param keys the keys
     * @param values the values
     * @param <T> expected return type
     * @return script result
     */
    <T> RedisFuture<T> evalsha(String digest, ScriptOutputType type, K[] keys, V... values);

    /**
     * Check existence of scripts in the script cache.
     * 
     * @param digests script digests
     * @return RedisFuture&lt;List&lt;Boolean&gt;&gt; array-reply The command returns an array of integers that correspond to
     *         the specified SHA1 digest arguments. For every corresponding SHA1 digest of a script that actually exists in the
     *         script cache, an 1 is returned, otherwise 0 is returned.
     */
    RedisFuture<List<Boolean>> scriptExists(String... digests);

    /**
     * Remove all the scripts from the script cache.
     * 
     * @return RedisFuture&lt;String&gt; simple-string-reply
     */
    RedisFuture<String> scriptFlush();

    /**
     * Kill the script currently in execution.
     * 
     * @return RedisFuture&lt;String&gt; simple-string-reply
     */
    RedisFuture<String> scriptKill();

    /**
     * Load the specified Lua script into the script cache.
     * 
     * @param script script content
     * @return RedisFuture&lt;String&gt; bulk-string-reply This command returns the SHA1 digest of the script added into the
     *         script cache.
     */
    RedisFuture<String> scriptLoad(V script);
}
