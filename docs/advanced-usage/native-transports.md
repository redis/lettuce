# Native Transports

Netty provides three platform-specific JNI transports:

- epoll on Linux

- io_uring on Linux (Incubator)

- kqueue on macOS/BSD

Lettuce defaults to native transports if the appropriate library is
available within its runtime. Using a native transport adds features
specific to a particular platform, generate less garbage and generally
improve performance when compared to the NIO based transport. Native
transports are required to connect to Redis via [Unix Domain
Sockets](#unix-domain-sockets) and are suitable for TCP connections as
well.

Native transports are available with:

- Linux **epoll** x86_64 systems with a minimum netty version of
  `4.0.26.Final`, requiring `netty-transport-native-epoll`, classifier
  `linux-x86_64`

  ``` xml
  <dependency>
      <groupId>io.netty</groupId>
      <artifactId>netty-transport-native-epoll</artifactId>
      <version>${netty-version}</version>
      <classifier>linux-x86_64</classifier>
  </dependency>
  ```

- Linux **io_uring** x86_64 systems with a minimum netty version of
  `4.1.54.Final`, requiring `netty-incubator-transport-native-io_uring`,
  classifier `linux-x86_64`. Note that this transport is still
  experimental.

  ``` xml
  <dependency>
      <groupId>io.netty.incubator</groupId>
      <artifactId>netty-incubator-transport-native-io_uring</artifactId>
      <version>0.0.1.Final</version>
      <classifier>linux-x86_64</classifier>
  </dependency>
  ```

- macOS **kqueue** x86_64 systems with a minimum netty version of
  `4.1.11.Final`, requiring `netty-transport-native-kqueue`, classifier
  `osx-x86_64`

  ``` xml
  <dependency>
      <groupId>io.netty</groupId>
      <artifactId>netty-transport-native-kqueue</artifactId>
      <version>${netty-version}</version>
      <classifier>osx-x86_64</classifier>
  </dependency>
  ```

You can disable native transport use through system properties. Set
`io.lettuce.core.epoll`, `io.lettuce.core.iouring` respective
`io.lettuce.core.kqueue` to `false` (default is `true`, if unset).

## Limitations

Native transport support does not work with the shaded version of
Lettuce because of two reasons:

1.  `netty-transport-native-epoll` and `netty-transport-native-kqueue`
    are not packaged into the shaded jar. So adding the jar to the
    classpath will resolve in different netty base classes (such as
    `io.netty.channel.EventLoopGroup` instead of
    `com.lambdaworks.io.netty.channel.EventLoopGroup`)

2.  Support for using epoll/kqueue with shaded netty requires netty 4.1
    and all parts of netty to be shaded.

!!! NOTE
    The limitations regarding shaded JAR files are applicable only to 
    Lettuce versions prior to 5.0.1. Since version 5.0.1, Lettuce no
    longer releases shaded JAR files, and these limitations are no
    longer relevant for newer versions.

!!! NOTE
    Having both the `io_uring` and the `epoll` native transports available
    in the classpath would - by default - load the `io_uring` driver with
    precedence. Use the `io.lettuce.core.iouring` system property to
    disable `io_uring` in this case and instead load the `epoll` transport.

See also Netty [documentation on native
transports](http://netty.io/wiki/native-transports.html).

## Unix Domain Sockets

Lettuce supports since version 3.2 Unix Domain Sockets for local Redis
connections.

``` java
RedisURI redisUri = RedisURI.Builder
                             .socket("/tmp/redis")
                             .withPassword("authentication")
                             .withDatabase(2)
                             .build();

RedisClient client = RedisClient.create(redisUri);
```

``` java
RedisURI redisUri = RedisURI.create("redis-socket:///tmp/redis");
RedisClient client = RedisClient.create(redisUri);
```

Unix Domain Sockets are inter-process communication channels on POSIX
compliant systems. They allow exchanging data between processes on the
same host operating system. When using Redis, which is usually a network
service, Unix Domain Sockets are usable only if connecting locally to a
single instance. Redis Sentinel and Redis Cluster, maintain tables of
remote or local nodes and act therefore as a registry. Unix Domain
Sockets are not beneficial with Redis Sentinel and Redis Cluster.

Using `RedisClusterClient` with Unix Domain Sockets would connect to the
local node using a socket and open TCP connections to all the other
hosts. A good example is connecting locally to a standalone or a single
cluster node to gain performance.

See [Native Transports](#native-transports) for more details and
limitations.
