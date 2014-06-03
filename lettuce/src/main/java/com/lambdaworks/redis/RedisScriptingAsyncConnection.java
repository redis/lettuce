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
    <T> RedisFuture<T> eval(String script, ScriptOutputType type, K... keys);

    <T> RedisFuture<T> eval(String script, ScriptOutputType type, K[] keys, V... values);

    <T> RedisFuture<T> evalsha(String digest, ScriptOutputType type, K... keys);

    <T> RedisFuture<T> evalsha(String digest, ScriptOutputType type, K[] keys, V... values);

    RedisFuture<List<Boolean>> scriptExists(String... digests);

    RedisFuture<String> scriptFlush();

    RedisFuture<String> scriptKill();

    RedisFuture<String> scriptLoad(V script);
}
