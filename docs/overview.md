# Overview

This document is the reference guide for Lettuce. It explains how to use
Lettuce, its concepts, semantics, and the syntax.

You can read this reference guide in a linear fashion, or you can skip
sections if something does not interest you.

This section provides some basic introduction to Redis. The rest of the
document refers only to Lettuce features and assumes the user is
familiar with Redis concepts.

## Knowing Redis

NoSQL stores have taken the storage world by storm. It is a vast domain
with a plethora of solutions, terms and patterns (to make things worse
even the term itself has multiple
[meanings](https://www.google.com/search?q=nosql+acronym)). While some
of the principles are common, it is crucial that the user is familiar to
some degree with Redis. The best way to get acquainted to these
solutions is to read and follow their documentation - it usually doesn't
take more than 5-10 minutes to go through them and if you are coming
from an RDMBS-only background many times these exercises can be an
eye-opener.

The jumping off ground for learning about Redis is
[redis.io](https://www.redis.io/). Here is a list of other useful
resources:

- The [interactive tutorial](https://try.redis.io/) introduces Redis.

- The [command references](https://redis.io/commands) explains Redis
  commands and contains links to getting started guides, reference
  documentation and tutorials.

## Project Reactor

[Reactor](https://projectreactor.io) is a highly optimized reactive
library for building efficient, non-blocking applications on the JVM
based on the [Reactive Streams
Specification](https://github.com/reactive-streams/reactive-streams-jvm).
Reactor based applications can sustain very high throughput message
rates and operate with a very low memory footprint, making it suitable
for building efficient event-driven applications using the microservices
architecture.

Reactor implements two publishers
[Flux\<T\>](https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Flux.html)
and
[Mono\<T\>](https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Mono.html),
both of which support non-blocking back-pressure. This enables exchange
of data between threads with well-defined memory usage, avoiding
unnecessary intermediate buffering or blocking.

## Non-blocking API for Redis

Lettuce is a scalable thread-safe Redis client based on
[netty](https://netty.io) and Reactor. Lettuce provides
[synchronous](user-guide/connecting-redis.md#basic-usage), [asynchronous](user-guide/async-api.md) and
[reactive](user-guide/reactive-api.md) APIs to interact with Redis.

## Requirements

Lettuce 6.x binaries require JDK level 8.0 and above.

In terms of [Redis](https://redis.io/), at least 2.6.

## Additional Help Resources

Learning a new framework is not always straight forward.In this section,
we try to provide what we think is an easy-to-follow guide for starting
with Lettuce. However, if you encounter issues or you are just looking
for an advice, feel free to use one of the links below:

### Support

There are a few support options available:

- Lettuce on Stackoverflow
  [Stackoverflow](https://stackoverflow.com/questions/tagged/lettuce) is
  a tag for all Lettuce users to share information and help each
  other.Note that registration is needed **only** for posting.

- Get in touch with the community on
  [Gitter](https://gitter.im/lettuce-io/Lobby).

- GitHub Discussions:
  <https://github.com/redis/lettuce/discussions>

- Report bugs (or ask questions) in GitHub issues
  <https://github.com/redis/lettuce/issues>.

### Following Development

For information on the Lettuce source code repository, nightly builds
and snapshot artifacts please see the [Lettuce
homepage](https://lettuce.io). You can help make lettuce best serve the
needs of the lettuce community by interacting with developers through
the Community on
[Stackoverflow](https://stackoverflow.com/questions/tagged/lettuce). If
you encounter a bug or want to suggest an improvement, please create a
ticket on the lettuce issue
[tracker](https://github.com/redis/lettuce/issues).

### Project Metadata

- Version Control – <https://github.com/redis/lettuce>

- Releases and Binary Packages –
  <https://github.com/redis/lettuce/releases>

- Issue tracker – <https://github.com/redis/lettuce/issues>

- Release repository – <https://repo1.maven.org/maven2/> (Maven Central)

- Snapshot repository –
  <https://oss.sonatype.org/content/repositories/snapshots/> (OSS
  Sonatype Snapshots)

## Where to go from here

- Head to [Getting Started](getting-started.md) if you feel like jumping
  straight into the code.

- Go to [High-Availability and
  Sharding](ha-sharding.md) for Master/Replica
  ("Master/Slave"), Redis Sentinel and Redis Cluster topics.

- In order to dig deeper into the core features of Reactor:

  - If you’re looking for client configuration options, performance
    related behavior and how to use various transports, go to [Advanced
    usage](advanced-usage.md).

  - See [Integration and Extension](integration-extension.md) for
    extending Lettuce with codecs or integrate it in your CDI/Spring
    application.

  - You want to know more about **at-least-once** and **at-most-once**?
    Take a look into [Command execution
    reliability](advanced-usage.md#command-execution-reliability).

