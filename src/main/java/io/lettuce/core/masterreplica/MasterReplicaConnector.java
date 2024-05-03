package io.lettuce.core.masterreplica;

import java.util.concurrent.CompletableFuture;

import io.lettuce.core.codec.RedisCodec;

/**
 * Interface declaring an asynchronous connect method to connect a Master/Replica setup.
 *
 * @author Mark Paluch
 * @since 5.1
 */
interface MasterReplicaConnector<K, V> {

    /**
     * Asynchronously connect to a Master/Replica setup given {@link RedisCodec}.
     *
     * @return Future that is notified about the connection progress.
     */
    CompletableFuture<StatefulRedisMasterReplicaConnection<K, V>> connectAsync();

}
