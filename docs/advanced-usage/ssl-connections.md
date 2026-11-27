# SSL Connections

Lettuce supports SSL connections since version 3.1 on Redis Standalone
connections and since version 4.2 on Redis Cluster. [Redis supports SSL since version 6.0](https://redis.io/docs/latest/operate/oss_and_stack/management/security/encryption/).

First, you need to [enable SSL on your Redis server](https://redis.io/docs/latest/operate/oss_and_stack/management/security/encryption/).

Next step is connecting lettuce over SSL to Redis.

``` java
RedisURI redisUri = RedisURI.Builder.redis("localhost")
                                 .withSsl(true)
                                 .withPassword("authentication")
                                 .withDatabase(2)
                                 .build();

RedisClient client = RedisClient.create(redisUri);
```

``` java
RedisURI redisUri = RedisURI.create("rediss://authentication@localhost/2");
RedisClient client = RedisClient.create(redisUri);
```

``` java
RedisURI redisUri = RedisURI.Builder.redis("localhost")
                                 .withSsl(true)
                                 .withPassword("authentication")
                                 .build();

RedisClusterClient client = RedisClusterClient.create(redisUri);
```

## Limitations

Lettuce supports SSL only on Redis Standalone and Redis Cluster
connections and since 5.2, also for Master resolution using Redis
Sentinel or Redis Master/Replicas.

## Connection Procedure and Reconnect

When connecting using SSL, Lettuce performs an SSL handshake before you
can use the connection. Plain text connections do not perform a
handshake. Errors during the handshake throw
`RedisConnectionException`s.

Reconnection behavior is also different to plain text connections. If an
SSL handshake fails on reconnect (because of peer/certification
verification or peer does not talk SSL) reconnection will be disabled
for the connection. You will also find an error log entry within your
logs.

## Certificate Chains/Root Certificate/Self-Signed Certificates

Lettuce uses Java defaults for the trust store that is usually `cacerts`
in your `jre/lib/security` directory and comes with customizable SSL
options via [ClientOptions](client-options.md). If you need to add you
own root certificate, so you can configure `SslOptions`, import it
either to `cacerts` or you provide an own trust store and set the
necessary system properties:

``` java
SslOptions sslOptions = SslOptions.builder()
        .jdkSslProvider()
        .truststore(new File("yourtruststore.jks"), "changeit")
        .build();

ClientOptions clientOptions = ClientOptions.builder().sslOptions(sslOptions).build();
```

``` java
System.setProperty("javax.net.ssl.trustStore", "yourtruststore.jks");
System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
```

## Host/Peer Verification

By default, Lettuce verifies the certificate against the validity and
the common name (Name validation not supported on Java 1.6, only
available on Java 1.7 and higher) of the Redis host you are connecting
to. This behavior can be turned off:

``` java
RedisURI redisUri = ...
redisUri.setVerifyPeer(false);
```

or

``` java
RedisURI redisUri = RedisURI.Builder.redis(host(), sslPort())
                                 .withSsl(true)
                                 .withVerifyPeer(false)
                                 .build();
```

## StartTLS

If you need to issue a StartTLS before you can use SSL, set the
`startTLS` property of `RedisURI` to `true`. StartTLS is disabled by
default.

``` java
RedisURI redisUri = ...
redisUri.setStartTls(true);
```

or

``` java
RedisURI redisUri = RedisURI.Builder.redis(host(), sslPort())
                                 .withSsl(true)
                                 .withStartTls(true)
                                 .build();
```
