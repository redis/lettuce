package com.lambdaworks.redis;

import java.util.List;

/**
 * Synchronous executed commands for Scripting.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 17.05.14 21:28
 */
public interface RedisScriptingConnection<K, V> {
    <T> T eval(String script, ScriptOutputType type, K... keys);

    <T> T eval(String script, ScriptOutputType type, K[] keys, V... values);

    <T> T evalsha(String digest, ScriptOutputType type, K... keys);

    <T> T evalsha(String digest, ScriptOutputType type, K[] keys, V... values);

    List<Boolean> scriptExists(String... digests);

    String scriptFlush();

    String scriptKill();

    String scriptLoad(V script);
}
