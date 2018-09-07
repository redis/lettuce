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

connection.reactive().get(…) // Tracing tries to obtain the current tracer from Reactor's Context
```

A note on reactive Tracing: Reactive Tracing with Brave requires either a Span or a 
TraceContext object to be available in Reactor's Context.

Commands
--------
* Add support for ZPOPMIN, ZPOPMAX, BZPOPMIN, BZPOPMAX commands #778
* Add REPLACE option to RESTORE. #783 (Thanks to @christophstrobl)
* Add XGROUP DESTROY #789
* Add support for CLIENT UNBLOCK #812

Enhancements
------------
* Cancel commands after disconnect in at-most-once mode. #547
* Consider adding host/port mapper #689
* Add tracing support using Brave (OpenZipkin) #782
* Improve builders and resources with Java 8 default/static interface methods #791
* ZSCAN match pattern encoding issue #792 (Thanks to @silvertype)
* Accept brave.Tracing instead of brave.Tracer #798
* Bind Master/Slave transactional commands to Master during an ongoing transaction #800
* FutureSyncInvocationHandler the statement "command.get ()" in the handlerInvocation method is unnecessary #809 (Thanks to @zhangweidavid)
* Fall back to initial seed nodes on topology refresh when dynamicRefreshSources is enabled #822 (Thanks to @stuartharper)
* Assert Java 11 build compatibility #841

Fixes
-----
* MULTI is dispatched to slave nodes using SLAVE readFrom #779 (Thanks to @Yipei)
* Reduce service name in BraveTracing to just Redis #797
* Javadoc mentions Delay.exponential() is capped at 30 milliseconds #799 (Thanks to @phxql)
* Read From Slaves is not working #804 (Thanks to @EXPEbdodla)
* GEORADIUS WITHCOORD returns wrong coordinate on multiple results #805 (Thanks to @dittos)
* RedisClusterCommands does not extend RedisStreamCommands #821
* smembers returns elements in non-deterministic order #823 (Thanks to @alezandr)
* StackOverflowError on ScanStream.scan(…).subscribe() #824
* ZINCRBY member should be value-typed #826 (Thanks to @fuyuanpai)

Other
-----
* Update What's new for Lettuce 5.1 in reference docs #777
* Improve Javadoc of QUIT method #781
* Upgrade to AssertJ 3.10.0 #794
* Upgrade to netty 4.1.29.Final #836
* Upgrade to Spring Framework 4.3.18 #837
* Upgrade to Reactor Bismuth-SR11 #838
* Upgrade to Brave 5.2.0 #839
* Upgrade to RxJava 2.2.2 #840
* Release Lettuce 5.1.0.RC1 #842

Lettuce requires a minimum of Java 8 to build and run. It is tested continuously
against the latest Redis source-build.

If you need any support, meet Lettuce at

* Google Group: https://groups.google.com/d/forum/lettuce-redis-client-users
or lettuce-redis-client-users@googlegroups.com
* Join the chat at https://gitter.im/lettuce-io/Lobby
* GitHub Issues: https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/docs/
