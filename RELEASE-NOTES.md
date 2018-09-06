Lettuce 5.1.0 RC1 RELEASE NOTES
===========================

This is the release candidate of Lettuce 5.1 shipping with improvements and 
bugfixes.
 
Most notable changes are:

* Brave (OpenZipkin) tracing support
* Lettuce build is now Java 11 compatible

Find the full change log at the end of this document that lists all 93 tickets.

Thanks to all contributors who made Lettuce 5.1.0.RC1 possible.
Lettuce 5.1.0.RC1 requires Java 8 to 11.

Reference documentation: https://lettuce.io/core/5.1.0.RC1/reference/.
JavaDoc documentation: https://lettuce.io/core/5.1.0.RC1/api/.

Add tracing support using Brave (OpenZipkin)
--------------------------------------------

Lettuce now supports command tracing using Brave. 
Tracing can be configured through `ClientResources`. The parent span is propagated either 
through `Tracing.currentTracer()` (synchronous/asynchronous usage) or 
by registering a Brave Span in the Reactor context when using Reactive execution.

Lettuce wraps Brave data models to support tracing in a vendor-agnostic way if 
Brave is on the class path.

Usage example:

```java
Tracing tracing = …;

ClientResources clientResources = ClientResources.builder().tracing(BraveTracing.create(tracing)).build();

RedisClient client = RedisClient.create(clientResources, redisUri);

StatefulRedisConnection<String, String> connection = client.connect();

connection.sync().get(…) // Tracing tries to obtain the current tracer from Tracing.currentTracer()

connection.reactive().get(…) // Trace tries to obtain the current tracer from Reactor's Context
```

A note on reactive Tracing: Reactive Tracing with Brave requires either a Span or a 
TraceContext object to be available in Reactor's Context.

Lettuce requires a minimum of Java 8 to build and run. It is tested continuously
against the latest Redis source-build.

If you need any support, meet Lettuce at

* Google Group: https://groups.google.com/d/forum/lettuce-redis-client-users
or lettuce-redis-client-users@googlegroups.com
* Join the chat at https://gitter.im/lettuce-io/Lobby
* GitHub Issues: https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/docs/
