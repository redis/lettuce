# Overview

This document is the reference guide for Lettuce. It explains how to use
Lettuce, its concepts, semantics, and the syntax.

You can read this reference guide in a linear fashion, or you can skip
sections if something does not interest you.

This section provides some basic introduction to Redis. The rest of the
document refers only to Lettuce features and assumes the user is
familiar with Redis concepts.

## Knowing Redis

If you are new to Redis, you can find a good introduction to Redis on [redis.io](https://redis.io/docs/latest/develop/)

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

Lettuce 7.x binaries require JDK level 8.0 and above.

In terms of [Redis](https://redis.io/), at least 2.6.

## Where to go from here

- Head to [Getting Started](getting-started.md) if you feel like jumping
  straight into the code.

- Go to [High-Availability and Sharding](ha-sharding.md) for Master/Replica
  ("Master/Slave"), Redis Sentinel and Redis Cluster topics.

- In order to dig deeper into the core features of Reactor:

  - If you’re looking for client configuration options, performance
    related behavior and how to use various transports, go to Advanced usage (see [Client Resources](advanced-usage/client-resources.md), [Client Options](advanced-usage/client-options.md), [Native Transports](advanced-usage/native-transports.md)).

  - See [Integration and Extension](integration-extension.md) for
    extending Lettuce with codecs or integrate it in your CDI/Spring
    application.

  - You want to know more about **at-least-once** and **at-most-once**?
    Take a look into [Command execution reliability](advanced-usage/command-execution-reliability.md).

