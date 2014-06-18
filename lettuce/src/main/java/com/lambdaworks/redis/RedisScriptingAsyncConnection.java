package com.lambdaworks.redis;

import java.util.List;

/**
 * Asynchronous executed commands for Scripting.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 17.05.14 21:28
 */
public interface RedisScriptingAsyncConnection<K, V> {
    /**
     * Execute a Lua script server side.
     */
    <T> RedisFuture<T> eval(String script, ScriptOutputType type, K... keys);

    /**
     * Execute a Lua script server side.
     * 
     * @param script the script type: string
     * @param type the numkeys type: long
     * @param keys the key
     * @param values the arg type: value
     */
    <T> RedisFuture<T> eval(String script, ScriptOutputType type, K[] keys, V... values);

    /**
     * Execute a Lua script server side.
     */
    <T> RedisFuture<T> evalsha(String digest, ScriptOutputType type, K... keys);

    /**
     * Execute a Lua script server side.
     * 
     * @param digest the sha1 type: string
     * @param type the numkeys type: long
     * @param keys the key
     * @param values the arg type: value
     */
    <T> RedisFuture<T> evalsha(String digest, ScriptOutputType type, K[] keys, V... values);

    RedisFuture<List<Boolean>> scriptExists(String... digests);

    RedisFuture<String> scriptFlush();

    RedisFuture<String> scriptKill();

    RedisFuture<String> scriptLoad(V script);
}
