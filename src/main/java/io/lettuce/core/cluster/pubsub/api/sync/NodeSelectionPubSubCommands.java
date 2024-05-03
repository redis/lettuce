package io.lettuce.core.cluster.pubsub.api.sync;

import io.lettuce.core.cluster.api.sync.Executions;

/**
 * Synchronous executed commands on a node selection for Pub/Sub.
 *
 * @author Mark Paluch
 * @since 4.4
 */
public interface NodeSelectionPubSubCommands<K, V> {

    /**
     * Listen for messages published to channels matching the given patterns.
     *
     * @param patterns the patterns
     * @return Executions to synchronize {@code psubscribe} completion
     */
    Executions<Void> psubscribe(K... patterns);

    /**
     * Stop listening for messages posted to channels matching the given patterns.
     *
     * @param patterns the patterns
     * @return Executions Future to synchronize {@code punsubscribe} completion
     */
    Executions<Void> punsubscribe(K... patterns);

    /**
     * Listen for messages published to the given channels.
     *
     * @param channels the channels
     * @return Executions Future to synchronize {@code subscribe} completion
     */
    Executions<Void> subscribe(K... channels);

    /**
     * Stop listening for messages posted to the given channels.
     *
     * @param channels the channels
     * @return Executions Future to synchronize {@code unsubscribe} completion.
     */
    Executions<Void> unsubscribe(K... channels);

    /**
     * Listen for messages published to the given shard channels.
     *
     * @param shardChannels the channels
     * @return Executions Future to synchronize {@code subscribe} completion
     * @since 7.0
     */
    Executions<Void> ssubscribe(K... shardChannels);

}
