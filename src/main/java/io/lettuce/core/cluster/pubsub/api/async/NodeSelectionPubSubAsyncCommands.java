package io.lettuce.core.cluster.pubsub.api.async;

import io.lettuce.core.cluster.api.async.AsyncExecutions;

/**
 * Asynchronous executed commands on a node selection for Pub/Sub.
 *
 * @author Mark Paluch
 * @since 4.4
 */
public interface NodeSelectionPubSubAsyncCommands<K, V> {

    /**
     * Listen for messages published to channels matching the given patterns.
     *
     * @param patterns the patterns
     * @return RedisFuture&lt;Void&gt; Future to synchronize {@code psubscribe} completion
     */
    AsyncExecutions<Void> psubscribe(K... patterns);

    /**
     * Stop listening for messages posted to channels matching the given patterns.
     *
     * @param patterns the patterns
     * @return RedisFuture&lt;Void&gt; Future to synchronize {@code punsubscribe} completion
     */
    AsyncExecutions<Void> punsubscribe(K... patterns);

    /**
     * Listen for messages published to the given channels.
     *
     * @param channels the channels
     * @return RedisFuture&lt;Void&gt; Future to synchronize {@code subscribe} completion
     */
    AsyncExecutions<Void> subscribe(K... channels);

    /**
     * Stop listening for messages posted to the given channels.
     *
     * @param channels the channels
     * @return RedisFuture&lt;Void&gt; Future to synchronize {@code unsubscribe} completion.
     */
    AsyncExecutions<Void> unsubscribe(K... channels);

    /**
     * Listen for messages published to the given shard channels.
     *
     * @param shardChannels the channels
     * @return RedisFuture&lt;Void&gt; Future to synchronize {@code subscribe} completion
     */
    AsyncExecutions<Void> ssubscribe(K... shardChannels);
}
