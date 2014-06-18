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
    /**
     * Execute a Lua script server side.
     */
    <T> T eval(String script, ScriptOutputType type, K... keys);

    /**
     * Execute a Lua script server side.
     * 
     * @param script the script type: string
     * @param type the numkeys type: long
     * @param keys the key
     * @param values the arg type: value
     */
    <T> T eval(String script, ScriptOutputType type, K[] keys, V... values);

    /**
     * Execute a Lua script server side.
     */
    <T> T evalsha(String digest, ScriptOutputType type, K... keys);

    /**
     * Execute a Lua script server side.
     * 
     * @param digest the sha1 type: string
     * @param type the numkeys type: long
     * @param keys the key
     * @param values the arg type: value
     */
    <T> T evalsha(String digest, ScriptOutputType type, K[] keys, V... values);

    List<Boolean> scriptExists(String... digests);

    String scriptFlush();

    String scriptKill();

    String scriptLoad(V script);
}
