/**
 * Client support for Redis Master/Slave setups. {@link io.lettuce.core.masterslave.MasterSlave} supports self-managed,
 * Redis Sentinel-managed, AWS ElastiCache and Azure Redis managed Master/Slave setups.
 *
 * Connections can be obtained by providing the {@link io.lettuce.core.RedisClient}, a {@link io.lettuce.core.RedisURI} and a {@link io.lettuce.core.codec.RedisCodec}.
 *
 * <pre class="code">
 *
 *   RedisClient client = RedisClient.create();
 *   StatefulRedisMasterSlaveConnection<String, String> connection = MasterSlave.connect(client,
 *                                                                      RedisURI.create("redis://localhost"),
 *                                                                      StringCodec.UTF8);
 *   // ...
 *
 *   connection.close();
 *   client.shutdown();
 * </pre>
 */
package io.lettuce.core.masterslave;

