# Compatibility and Roadmap

This page provides an overview of Lettuce compatibility with Java, Netty, and Redis versions, along with a timeline of major features introduced in each release.

## Redis Modules and Features Support

Lettuce provides native support for Redis core data types and select Redis modules. The tables below show the current support status.

### Supported Core Data Types

| Data Type | Support Status | Since Version | Notes |
|-----------|---------------|---------------|-------|
| [Strings](https://redis.io/docs/latest/develop/data-types/strings/) | Supported | 1.0 | Full support including `SET`, `GET`, `MSET`, `MGET`, etc. |
| [Hashes](https://redis.io/docs/latest/develop/data-types/hashes/) | Supported | 1.0 | Full support including Hash Field Expiration (6.4+) |
| [Lists](https://redis.io/docs/latest/develop/data-types/lists/) | Supported | 1.0 | Full support including blocking operations |
| [Sets](https://redis.io/docs/latest/develop/data-types/sets/) | Supported | 1.0 | Full support |
| [Sorted Sets](https://redis.io/docs/latest/develop/data-types/sorted-sets/) | Supported | 1.0 | Full support |
| [Streams](https://redis.io/docs/latest/develop/data-types/streams/) | Supported | 5.1 | Full support for `XADD`, `XREAD`, `XREADGROUP`, etc. |
| [HyperLogLog](https://redis.io/docs/latest/develop/data-types/hyperloglogs/) | Supported | 1.0 | Full support |
| [Geospatial](https://redis.io/docs/latest/develop/data-types/geospatial/) | Supported | 4.0 | Full support for `GEO*` commands |
| [Bitmaps](https://redis.io/docs/latest/develop/data-types/bitmaps/) | Supported | 1.0 | Full support |
| [Bitfields](https://redis.io/docs/latest/develop/data-types/bitfields/) | Supported | 4.2 | Full support |
| [Vector Sets](https://redis.io/docs/latest/develop/data-types/vector-sets/) | Supported | 6.7 | Full support for `VADD`, `VSIM`, `VCARD`, etc. |

### Supported Redis Modules / Features

| Module / Feature | Support Status | Since Version | Documentation |
|------------------|---------------|---------------|---------------|
| [RedisJSON](https://redis.io/docs/latest/develop/data-types/json/) | Supported | 6.5 | [Redis JSON Guide](user-guide/redis-json.md) |
| [RediSearch (Query Engine)](https://redis.io/docs/latest/develop/interact/search-and-query/) | Supported | 6.8 | [Redis Search Guide](user-guide/redis-search.md) |
| [Vector Search](https://redis.io/docs/latest/develop/interact/search-and-query/query/vector-search/) | Supported | 6.8 | Part of RediSearch support |
| [Pub/Sub](https://redis.io/docs/latest/develop/interact/pubsub/) | Supported | 1.0 | Full support including Sharded Pub/Sub (6.4+) |
| [Transactions](https://redis.io/docs/latest/develop/interact/transactions/) | Supported | 1.0 | `MULTI`/`EXEC`/`WATCH` |
| [Scripting (Lua)](https://redis.io/docs/latest/develop/interact/programmability/eval-intro/) | Supported | 1.0 | `EVAL`, `EVALSHA`, `SCRIPT` commands |
| [Functions](https://redis.io/docs/latest/develop/interact/programmability/functions-intro/) | Supported | 6.3 | `FCALL`, `FUNCTION LOAD`, etc. |
| [ACL](https://redis.io/docs/latest/operate/oss_and_stack/management/security/acl/) | Supported | 6.0 | Full ACL command support |
| [Client Tracking](https://redis.io/docs/latest/develop/use/client-side-caching/) | Supported | 6.5 | `CLIENT TRACKING` command |

### Unsupported Redis Modules

| Module | Support Status | Notes |
|--------|---------------|-------|
| [RedisTimeSeries](https://redis.io/docs/latest/develop/data-types/timeseries/) | Not Supported | Use [Redis Command Interfaces](redis-command-interfaces.md) for custom commands |
| [RedisBloom](https://redis.io/docs/latest/develop/data-types/probabilistic/) | Not Supported | Use [Redis Command Interfaces](redis-command-interfaces.md) for custom commands |
| [RedisGraph](https://redis.io/docs/latest/operate/oss_and_stack/stack-with-enterprise/deprecated-features/graph/) | Not Supported | Deprecated by Redis; use [Redis Command Interfaces](redis-command-interfaces.md) |
| [Triggers and Functions](https://redis.io/docs/latest/operate/oss_and_stack/stack-with-enterprise/deprecated-features/triggers-and-functions/) | Not Supported | Use [Redis Command Interfaces](redis-command-interfaces.md) for custom commands |

> **Note:** For unsupported modules, Lettuce provides the [Redis Command Interfaces](redis-command-interfaces.md) feature which allows you to define custom command interfaces for any Redis module. This enables type-safe access to module commands without waiting for native driver support.

### Example: Using Custom Commands for Unsupported Modules

```java
// Define a custom interface for RedisTimeSeries commands
interface TimeSeriesCommands extends Commands {

    @Command("TS.CREATE")
    String tsCreate(String key);

    @Command("TS.ADD")
    Long tsAdd(String key, long timestamp, double value);

    @Command("TS.GET")
    List<Object> tsGet(String key);
}

// Use the custom commands
RedisCommandFactory factory = new RedisCommandFactory(connection);
TimeSeriesCommands ts = factory.getCommands(TimeSeriesCommands.class);
ts.tsCreate("sensor:temperature");
ts.tsAdd("sensor:temperature", System.currentTimeMillis(), 23.5);
```


## Java Compatibility

Lettuce requires **Java 8** as the minimum version since Lettuce 5.0. The table below shows Java compatibility by Lettuce version:

| Lettuce Version                                                                                                                         | Minimum Java | Tested Up To |
|-----------------------------------------------------------------------------------------------------------------------------------------|--------------|--------------|
| [5.0](https://github.com/redis/lettuce/releases/tag/5.0.0.RELEASE) - [5.3](https://github.com/redis/lettuce/releases/tag/5.3.0.RELEASE) | Java 8       | Java 11      |
| [6.0](https://github.com/redis/lettuce/releases/tag/6.0.0.RELEASE) - [6.8](https://github.com/redis/lettuce/releases/tag/6.8.0.RELEASE) | Java 8       | Java 21      |
| [7.0](https://github.com/redis/lettuce/releases/tag/7.0.0.RELEASE) and later                                                            | Java 8       | Java 24      | 

> **Note:** Lettuce 6.x and 7.x binaries are compiled with Java 8 as the target but are tested against newer Java versions including the latest LTS releases.

## Netty Driver Versions

Lettuce is built on top of [Netty](https://netty.io/) for networking. The tables below show the specific Netty versions used by each Lettuce release.

### Version Mapping Table

| Lettuce Version                                                                | Netty Version                                                                    |
|--------------------------------------------------------------------------------|----------------------------------------------------------------------------------|
| [6.0.8.RELEASE](https://github.com/redis/lettuce/releases/tag/6.0.8.RELEASE)   | [4.1.60.Final](https://github.com/netty/netty/releases/tag/netty-4.1.60.Final)   | 
| [6.1.10.RELEASE](https://github.com/redis/lettuce/releases/tag/6.1.10.RELEASE) | [4.1.89.Final](https://github.com/netty/netty/releases/tag/netty-4.1.89.Final)   |
| [6.2.7.RELEASE](https://github.com/redis/lettuce/releases/tag/6.2.7.RELEASE)   | [4.1.94.Final](https://github.com/netty/netty/releases/tag/netty-4.1.94.Final)   |
| [6.3.2.RELEASE](https://github.com/redis/lettuce/releases/tag/6.3.2.RELEASE)   | [4.1.107.Final](https://github.com/netty/netty/releases/tag/netty-4.1.107.Final) | 
| [6.4.2.RELEASE](https://github.com/redis/lettuce/releases/tag/6.4.2.RELEASE)   | [4.1.113.Final](https://github.com/netty/netty/releases/tag/netty-4.1.113.Final) |
| [6.5.5.RELEASE](https://github.com/redis/lettuce/releases/tag/6.5.5.RELEASE)   | [4.1.118.Final](https://github.com/netty/netty/releases/tag/netty-4.1.118.Final) |
| [6.6.0.RELEASE](https://github.com/redis/lettuce/releases/tag/6.6.0.RELEASE)   | [4.1.118.Final](https://github.com/netty/netty/releases/tag/netty-4.1.118.Final) | 
| [6.7.1.RELEASE](https://github.com/redis/lettuce/releases/tag/6.7.1.RELEASE)   | [4.1.125.Final](https://github.com/netty/netty/releases/tag/netty-4.1.125.Final) | 
| [6.8.2.RELEASE](https://github.com/redis/lettuce/releases/tag/6.8.2.RELEASE)   | [4.1.125.Final](https://github.com/netty/netty/releases/tag/netty-4.1.125.Final) | 
| [7.0.1.RELEASE](https://github.com/redis/lettuce/releases/tag/7.0.1.RELEASE)   | [4.2.5.Final](https://github.com/netty/netty/releases/tag/netty-4.2.5.Final)     | 
| [7.1.1.RELEASE](https://github.com/redis/lettuce/releases/tag/7.1.1.RELEASE)   | [4.2.5.Final](https://github.com/netty/netty/releases/tag/netty-4.2.5.Final)     | 
| [7.2.1.RELEASE](https://github.com/redis/lettuce/releases/tag/7.2.1.RELEASE)   | [4.2.5.Final](https://github.com/netty/netty/releases/tag/netty-4.2.5.Final)     | 

### Netty 4.1 to 4.2 Migration

Lettuce 7.0 marks the migration from Netty 4.1.x to Netty 4.2.x, which brings:

- **Adaptive memory allocator** as the default allocator
- **Updated threading model** for NIO event loops
- **Performance improvements** in buffer management

> **Note:** Service releases (patch versions) typically inherit the Netty version from the previous patch unless explicitly upgraded for security or stability reasons.

## Redis Compatibility

Lettuce supports a wide range of Redis versions. See [Redis releases](https://github.com/redis/redis/releases) for details.

| Lettuce Version                                                     | Tested Redis Versions |
|---------------------------------------------------------------------|-----------------------|
| [5.x](https://github.com/redis/lettuce/releases?q=5.&expanded=true) | Redis 2.6 - 6.0       | 
| [6.x](https://github.com/redis/lettuce/releases?q=6.&expanded=true) | Redis 2.6 - 8.2       | 
| [7.x](https://github.com/redis/lettuce/releases?q=7.&expanded=true) | Redis 2.6 - 8.4       | 

> **Note:** Lettuce maintains backward compatibility with older Redis versions while adding support for new features in the latest releases.

## See Also

- [New Features](new-features.md) - Detailed feature documentation
- [Getting Started](getting-started.md) - Quick start guide
- [Overview](overview.md) - Lettuce overview
- [Redis Command Interfaces](redis-command-interfaces.md) - Custom command support

