package com.lambdaworks.redis.cluster;

import java.util.List;

import com.lambdaworks.redis.ScriptOutputType;
import com.lambdaworks.redis.cluster.api.async.AsyncExecutions;
import com.lambdaworks.redis.output.KeyStreamingChannel;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public interface ExampleForMultipleAsyncExecutions<K, V> {
    /**
     * Find all keys matching the given pattern.
     *
     * @param pattern the pattern type: patternkey (pattern)
     * @return RedisFuture&lt;List&lt;K&gt;&gt; array-reply list of keys matching {@code pattern}.
     */
    AsyncExecutions<List<K>> keys(K pattern);

    /**
     * Find all keys matching the given pattern.
     *
     * @param channel the channel
     * @param pattern the pattern
     *
     * @return RedisFuture&lt;Long&gt; array-reply list of keys matching {@code pattern}.
     */
    AsyncExecutions<Long> keys(KeyStreamingChannel<K> channel, K pattern);

    /**
     * Set a configuration parameter to the given value.
     *
     * @param parameter the parameter name
     * @param value the parameter value
     * @return RedisFuture&lt;String&gt; simple-string-reply: {@code OK} when the configuration was set properly. Otherwise an
     *         error is returned.
     */
    AsyncExecutions<String> configSet(String parameter, String value);

    /**
     * Execute a Lua script server side.
     *
     * @param script Lua 5.1 script.
     * @param type output type
     * @param keys key names
     * @param <T> expected return type
     * @return script result
     */
    <T> AsyncExecutions<T> eval(String script, ScriptOutputType type, K... keys);

    /**
     * Kill the script currently in execution.
     *
     * @return RedisFuture&lt;String&gt; simple-string-reply
     */
    AsyncExecutions<String> scriptKill();
}
