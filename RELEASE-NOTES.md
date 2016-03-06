lettuce 4.2 RELEASE NOTES
=========================

lettuce 4.2 requires Java 8 and cannot be used with Java 6 or 7.

Redis Cluster and SSL
---------------------
Redis 3.2 introduces an option to announce a specific IP address/port using `cluster-announce-ip` and
`cluster-announce-port`. This is useful for Docker and NAT'ed use cases. Furthermore, you can "hide" your Redis
Cluster nodes behind anything, for example, `stunnel`. A Redis Cluster node will announce the specified port/IP
which maps then to `stunnel`, and you get an SSL-protected Redis Cluster.

Redis Cluster SSL works pretty much the same as Redis Standalone with SSL. You can configure SSL and
other SSL/TLS options using `RedisURI`.

```java
RedisURI redisURI = RedisURI.Builder.redis(host(), 7443)
                                .withSsl(true)
                                .withVerifyPeer(false)
                                .build();

RedisClusterClient redisClusterClient = RedisClusterClient.create(redisURI);
StatefulRedisClusterConnection<String, String> connection = redisClusterClient.connect();
```

You should disable the `verifyPeer` option. The reason is the address resolution mechanism is decoupled from the SSL
verification part. With enabled verification all connections would be verified against the first RedisURI node.

When creating a `RedisClusterClient` using `RedisClusterClientFactoryBean` the `verifyPeer` option is disabled by default.


If you need any support, meet lettuce at:

* Google Group: https://groups.google.com/d/forum/lettuce-redis-client-users
* Gitter: https://gitter.im/mp911de/lettuce
* Github Issues: https://github.com/mp911de/lettuce/issues


Enhancements
------------

Fixes
-----

Other
------

lettuce requires a minimum of Java 8 to build and run. It is tested continuously against the latest Redis source-build.

If you need any support, meet lettuce at

* Google Group: https://groups.google.com/d/forum/lettuce-redis-client-users
                or lettuce-redis-client-users@googlegroups.com
* Join the chat at https://gitter.im/mp911de/lettuce
* Github Issues: https://github.com/mp911de/lettuce/issues
* Wiki: https://github.com/mp911de/lettuce/wiki