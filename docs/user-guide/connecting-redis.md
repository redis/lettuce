# Connecting Redis

Connections to a Redis Standalone, Sentinel, or Cluster require a
specification of the connection details. The unified form is `RedisURI`.
You can provide the database, password and timeouts within the
`RedisURI`. You have following possibilities to create a `RedisURI`:

1.  Use an URI:

    ``` java
        RedisURI.create("redis://localhost/");
    ```

2.  Use the Builder

    ``` java
        RedisURI.Builder.redis("localhost", 6379).auth("password").database(1).build();
    ```

3.  Set directly the values in `RedisURI`

    ``` java
        new RedisURI("localhost", 6379, 60, TimeUnit.SECONDS);
    ```

## URI syntax

**Redis Standalone**

    redis :// [[username :] password@] host [:port][/database]
              [?[timeout=timeout[d|h|m|s|ms|us|ns]] [&clientName=clientName]
              [&libraryName=libraryName] [&libraryVersion=libraryVersion] ]

**Redis Standalone (SSL)**

    rediss :// [[username :] password@] host [: port][/database]
               [?[timeout=timeout[d|h|m|s|ms|us|ns]] [&clientName=clientName]
               [&libraryName=libraryName] [&libraryVersion=libraryVersion] ]

**Redis Standalone (Unix Domain Sockets)**

    redis-socket :// [[username :] password@]path
                     [?[timeout=timeout[d|h|m|s|ms|us|ns]] [&database=database]
                     [&clientName=clientName] [&libraryName=libraryName]
                     [&libraryVersion=libraryVersion] ]

**Redis Sentinel**

    redis-sentinel :// [[username :] password@] host1[:port1] [, host2[:port2]] [, hostN[:portN]] [/database]
                       [?[timeout=timeout[d|h|m|s|ms|us|ns]] [&sentinelMasterId=sentinelMasterId]
                       [&clientName=clientName] [&libraryName=libraryName]
                       [&libraryVersion=libraryVersion] ]

**Schemes**

- `redis` Redis Standalone

- `rediss` Redis Standalone SSL

- `redis-socket` Redis Standalone Unix Domain Socket

- `redis-sentinel` Redis Sentinel

**Timeout units**

- `d` Days

- `h` Hours

- `m` Minutes

- `s` Seconds

- `ms` Milliseconds

- `us` Microseconds

- `ns` Nanoseconds

Hint: The database parameter within the query part has higher precedence
than the database in the path.

RedisURI supports Redis Standalone, Redis Sentinel and Redis Cluster
with plain, SSL, TLS and unix domain socket connections.

Hint: The database parameter within the query part has higher precedence
than the database in the path. RedisURI supports Redis Standalone, Redis
Sentinel and Redis Cluster with plain, SSL, TLS and unix domain socket
connections.

## Authentication

Redis URIs may contain authentication details that effectively lead to
usernames with passwords, password-only, or no authentication.
Connections are authenticated by using the information provided through
`RedisCredentials`. Credentials are obtained at connection time from
`RedisCredentialsProvider`. When configuring username/password on the
URI statically, then a `StaticCredentialsProvider` holds the configured
information.

**Notes**

- When using Redis Sentinel, the password from the URI applies to the
  data nodes only. Sentinel authentication must be configured for each
  sentinel node.

- Usernames are supported as of Redis 6.

- Library name and library version are automatically set on Redis 7.2 or
  greater.

## Basic Usage

``` java
RedisClient client = RedisClient.create("redis://localhost");          

StatefulRedisConnection<String, String> connection = client.connect(); 

RedisCommands<String, String> commands = connection.sync();            

String value = commands.get("foo");                                    

...

connection.close();                                                    

client.shutdown();                                                     
```

- Create the `RedisClient` instance and provide a Redis URI pointing to
  localhost, Port 6379 (default port).

- Open a Redis Standalone connection. The endpoint is used from the
  initialized `RedisClient`

- Obtain the command API for synchronous execution. Lettuce supports
  asynchronous and reactive execution models, too.

- Issue a `GET` command to get the key `foo`.

- Close the connection when you’re done. This happens usually at the
  very end of your application. Connections are designed to be
  long-lived.

- Shut down the client instance to free threads and resources. This
  happens usually at the very end of your application.

Each Redis command is implemented by one or more methods with names
identical to the lowercase Redis command name. Complex commands with
multiple modifiers that change the result type include the CamelCased
modifier as part of the command name, e.g. `zrangebyscore` and
`zrangebyscoreWithScores`.

Redis connections are designed to be long-lived and thread-safe, and if
the connection is lost will reconnect until `close()` is called. Pending
commands that have not timed out will be (re)sent after successful
reconnection.

All connections inherit a default timeout from their RedisClient and  
and will throw a `RedisException` when non-blocking commands fail to
return a result before the timeout expires. The timeout defaults to 60
seconds and may be changed in the RedisClient or for each connection.
Synchronous methods will throw a `RedisCommandExecutionException` in
case Redis responds with an error. Asynchronous connections do not throw
exceptions when Redis responds with an error.

### RedisURI

The RedisURI contains the host/port and can carry
authentication/database details. On a successful connect you get
authenticated, and the database is selected afterward. This applies  
also after re-establishing a connection after a connection loss.

A Redis URI can also be created from an URI string. Supported formats
are:

- `redis://[password@]host[:port][/databaseNumber]` Plaintext Redis
  connection

- `rediss://[password@]host[:port][/databaseNumber]` [SSL
  Connections](../advanced-usage.md#ssl-connections) Redis connection

- `redis-sentinel://[password@]host[:port][,host2[:port2]][/databaseNumber]#sentinelMasterId`
  for using Redis Sentinel

- `redis-socket:///path/to/socket` [Unix Domain
  Sockets](../advanced-usage.md#unix-domain-sockets) connection to Redis

### Exceptions

In the case of an exception/error response from Redis, you’ll receive a
`RedisException` containing  
the error message. `RedisException` is a `RuntimeException`.

### Examples

``` java
RedisClient client = RedisClient.create(RedisURI.create("localhost", 6379));
client.setDefaultTimeout(20, TimeUnit.SECONDS);

// …

client.shutdown();
```

``` java
RedisURI redisUri = RedisURI.Builder.redis("localhost")
                                .withPassword("authentication")
                                .withDatabase(2)
                                .build();
RedisClient client = RedisClient.create(redisUri);

// …

client.shutdown();
```

``` java
RedisURI redisUri = RedisURI.Builder.redis("localhost")
                                .withSsl(true)
                                .withPassword("authentication")
                                .withDatabase(2)
                                .build();
RedisClient client = RedisClient.create(redisUri);

// …

client.shutdown();
```

``` java
RedisURI redisUri = RedisURI.create("redis://authentication@localhost/2");
RedisClient client = RedisClient.create(redisUri);

// …

client.shutdown();
```

