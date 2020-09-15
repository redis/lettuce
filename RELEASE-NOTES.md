Lettuce 6.0.0 RC2 RELEASE NOTES
==============================

The Lettuce team is delighted to announce the availability of the second and final Lettuce 6 release candidate.

Most notable changes that ship with this release are

* Kotlin Coroutine API
* Redesign command latency metrics publishing

We're working now towards the final release of Lettuce 6.0 by incorporating feedback from this release candidate.

Kotlin Coroutine API
--------------------

Kotlin users can now use a Coroutine API that exposes suspended methods and uses `Flow` where appropriate.
The API can be obtained through an extension function for Standalone, Cluster, and Sentinel connections exposing the appropriate API.  

```kotlin
val suspendableCommands: RedisSuspendableCommands<String, String> = connection.suspendable()

val foo1 = suspendableCommands.set("foo", "bar")
val foo2 = suspendableCommands.keys("fo*")
```

Additionally, we ship two extensions that simplify transactional usage by providing a `multi` closure:

```kotlin
val result: TransactionResult = connection.async().multi {
    set("foo", "bar")
    get("foo")
}
```

The API is marked experimental and requires opt-in through `@ExperimentalLettuceCoroutinesApi` to avoid Compiler warnings. 
We expect further evolution of the API towards a more `Flow`-oriented API where now `List` is returned to enable streaming of large responses.

Redesign command latency metrics publishing
-------------------------------------------

This is a mostly internal change that switches from `CommandLatencyCollector` to the newly introduced `CommandLatencyRecorder` interface. 
Unless you're implementing or configuring `CommandLatencyCollector` yourself, you should not see any changes.

This change is motivated by support for libraries that provide latency observability without publishing metrics to the `EventBus`.

Thanks to all contributors who made Lettuce 6.0.0.RC2 possible.
Lettuce requires a minimum of Java 8 to build and run and is compatible with Java 15. It is tested continuously against the latest Redis source-build.

If you need any support, meet Lettuce at

* Google Group (General discussion, announcements, and releases): https://groups.google.com/g/lettuce-redis-client-users
or lettuce-redis-client-users@googlegroups.com
* Stack Overflow (Questions): https://stackoverflow.com/questions/tagged/lettuce
* Join the chat at https://gitter.im/lettuce-io/Lobby for general discussion
* GitHub Issues (Bug reports, feature requests): https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/core/6.0.0.RC2/reference/
* Javadoc: https://lettuce.io/core/6.0.0.RC2/api/

API cleanups/Breaking Changes
-----------------------------

With this release, we took the opportunity to introduce a series of changes that put the API into a cleaner shape.

* Redesign command latency metrics publishing #1409

Enhancements
------------
* Kotlin Coroutine API #1387 (Thanks to @SokoMishaLov)
* Add support for aarch64 #1396 (Thanks to @odidev)

Fixes
-----
* Wrong cast in StringCodec may lead to IndexOutOfBoundsException #1367 (Thanks to @dmandalidis)
* Sentinel authentication failed when using the pingBeforeActivateConnection parameter #1401 (Thanks to @viniciusxyz)
* RedisURI.toString() should not reveal password #1405
* MasterReplica.connect(…) doesn't consider username with Redis 6 #1406
* LPOS command sends FIRST instead of RANK #1410 (Thanks to @christophstrobl)

Other
-----
* Upgrade to Reactor 3.3.9.RELEASE #1384
* Upgrade to Project Reactor 3.3.10.RELEASE #1411
* Upgrade to netty 4.1.52.Final #1412
* Upgrade test/optional dependencies #1413
