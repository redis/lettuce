/**
 * Client support for Redis Primary/Replica setups. {@link io.lettuce.core.primaryreplica.PrimaryReplica} supports self-managed,
 * Redis Sentinel-managed, AWS ElastiCache and Azure Redis managed Primary/Replica setups.
 *
 * Connections can be obtained by providing the {@link io.lettuce.core.RedisClient}, a {@link io.lettuce.core.RedisURI} and a
 * {@link io.lettuce.core.codec.RedisCodec}.
 *
 * <pre class="code">
 *
 * RedisClient client = RedisClient.create();
 * StatefulRedisPrimaryReplicaConnection<String, String> connection = PrimaryReplica.connect(client,
 *         RedisURI.create("redis://localhost"), StringCodec.UTF8);
 * // ...
 *
 * connection.close();
 * client.shutdown();
 * </pre>
 */
package io.lettuce.core.masterreplica;
