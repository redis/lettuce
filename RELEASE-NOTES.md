Lettuce 5.2.0 RELEASE NOTES
===========================

The Lettuce team is pleased to announce the Lettuce 5.2.0 release! 
This release contains new features, bug fixes, and enhancements.
 
Most notable changes are:

* Sentinel refinements
* Node randomization when reading from Redis Cluster nodes
* Codec enhancements
* Migrated tests to JUnit 5

Find the full changelog at the end of this document that lists all 110 tickets.
Work for Lettuce 6, the next major release has already started. Lettuce 6 will support RESP3 and should ship in time for Redis 6.

Thanks to all contributors who made Lettuce 5.2.0.RELEASE possible.
Lettuce requires a minimum of Java 8 to build and run and is compatible with Java 13. It is tested continuously against the latest Redis source-build.

If you need any support, meet Lettuce at

* Google Group (General discussion, announcements, and releases): https://groups.google.com/d/forum/lettuce-redis-client-users
or lettuce-redis-client-users@googlegroups.com
* Stack Overflow (Questions): https://stackoverflow.com/questions/tagged/lettuce
* Join the chat at https://gitter.im/lettuce-io/Lobby for general discussion
* GitHub Issues (Bug reports, feature requests): https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/core/5.2.0.RELEASE/reference/
* Javadoc: https://lettuce.io/core/5.2.0.RELEASE/api/

Sentinel refinements
----------------------------------

Since this release, Lettuce can connect to Redis Sentinel using secure sockets (SSL) and can authenticate against Sentinel using passwords.
To use SSL, enable SSL usage on `RedisURI` the same way how it's done for Redis Standalone and Redis Cluster.
Lettuce also introduced a new protocol scheme with `rediss-sentinel://`. Please note that enabling SSL enables encryption for both, Sentinel and data node communication.

Password authentication follows the same pattern. Setting a password on a Sentinel `RedisURI` enables password authentication for Sentinel and data nodes.

Read from random Redis Cluster node
----------------------------------

Redis Cluster supports now node randomization to avoid excessive usage of individual nodes when reading from these. 
`ReadFrom` now exposes a `isOrderSensitive()` method to indicate whether a `ReadFrom` setting is order-sensitive or whether respecting order isn't required.
Custom `ReadFrom` implementations should consider this change. 
`ReadFrom.ANY` is a newly introduced setting that allows reading from any node (master or a qualified replica) without an ordering preference if multiple replicas qualify for reading.

Codec enhancements
-----------------

As of this release, we ship two notable enhancements for codecs:

* `CipherCodec` for transparent encryption and decryption of values.
* Codec composition through `RedisCodec.of(…)` to set up a composed codec from a key and a value codec.

`CipherCodec` is created by using a delegate codec and `CipherSupplier` for encryption and decryption. Values are encoded first and then encrypted before values are written to Redis.

```java
SecretKeySpec key = …;
CipherCodec.CipherSupplier encrypt = new CipherCodec.CipherSupplier() {
    @Override
    public Cipher get(CipherCodec.KeyDescriptor keyDescriptor) throws GeneralSecurityException {

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key);
        return cipher;
    }
};

CipherCodec.CipherSupplier decrypt = (CipherCodec.KeyDescriptor keyDescriptor) -> {

    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
    cipher.init(Cipher.DECRYPT_MODE, key);
    return cipher;
};

RedisCodec<String, String> crypto = CipherCodec.forValues(StringCodec.UTF8, encrypt, decrypt);
```

`CipherCodec` supports multiple keys to increment key versions and to decrypt values with older key versions. The code above is to illustrate how to construct `CipherCodec` and does not guarantee to outline a secure `Cipher` setup that meets your requirements.

With `RedisCodec.of(…)`, applications can quickly compose a `RedisCodec` of already existing codecs instead of writing an entire codec from scratch. Codec composition is useful to use different codecs for key and value encoding:

```java
RedisCodec<String, byte[]> composed = RedisCodec.of(StringCodec.ASCII, ByteArrayCodec.INSTANCE);
```

Read more on Codecs at: https://github.com/lettuce-io/lettuce-core/wiki/Codecs

Commands
------------
* Add support for "MEMORY USAGE key [SAMPLES count]" #853 (Thanks to @darkaquarius)
* Add support for XINFO #996 (Thanks to @Erzhoumu)
* Add support for XGROUP CREATE … MKSTREAM #898 (Thanks to @wenerme)

Enhancements
------------
* Make adaptive topology refresh better usable for failover/master-slave promotion changes #672 (Thanks to @mudasirale)
* Allow randomization of read candidates using Redis Cluster #834 (Thanks to @nborole)
* AsyncExecutions should implement CompletionStage #862
* BraveTracing should support custom names for the service name #865 (Thanks to @worldtiki)
* Emit Connection Error events #885 (Thanks to @squishypenguin)
* Allow usage of publishOn scheduler for reactive signal emission #905
* Optimize aggregation buffer cleanup in CommandHandler #906 (Thanks to @gavincook)
* Add shutdown logging to client, ClientResources, and EventLoopGroupProvider #918
* Add flag to disable reporting of span tags #920 (Thanks to @worldtiki)
* Optimization: Use Cluster write connections for read commands when using ReadFrom.MASTER #923
* ByteBuf.release() was not called before it's garbage-collected #930 (Thanks to @zhouzq)
* Add cipher codec #934
* Introduce adaptive trigger event #952
* Deprecate PING on connect option #965
* Do not require CLIENT LIST in cluster topology refresh #973 (Thanks to @bentharage)
* Add support for Redis Sentinel authentication #1002 (Thanks to @ssouris)
* Improve mutators for ClientOptions, ClusterClientOptions, and ClientResources #1003
* TopologyComparators performance issue #1011 (Thanks to @alessandrosimi-sa)
* Attach topology retrieval exceptions when Lettuce cannot retrieve a topology update #1024 (Thanks to @StillerRen)
* Support TLS connections in Sentinel mode #1048 (Thanks to @ae6rt)
* Reduce object allocations for assertions #1068
* Reduce object allocations for Cluster topology parsing #1069
* Support ByteArrayCodec only for values #1122 (Thanks to @dmandalidis)
* lettuce#ClusterTopologyRefresh is too slow，because of “client list” #1126 (Thanks to @xjs1919)

Fixes
-----
* Unable to reconnect Pub/Sub connection with authorization #868 (Thanks to @maestroua)
* Reduce allocations in topology comparator #870
* Fix recordCommandLatency to work properly #874 (Thanks to @jongyeol)
* Bug: Include hostPortString in the error message #876 (Thanks to @LarryBattle)
* Reference docs CSS prevents HTTPS usage #878
* ReactiveCommandSegmentCommandFactory resolves StreamingOutput for all reactive types #879 (Thanks to @yozhag)
* ClassCastException occurs when executing RedisClusterClient::connectPubSub with global timeout feature #895 (Thanks to @be-hase)
* Flux that reads from a hash, processes elements and writes to a set, completes prematurely #897 (Thanks to @vkurland)
* Fixed stackoverflow exception inside CommandLatencyCollectorOptions #899 (Thanks to @LarryBattle)
* PubSubEndpoint.channels and patterns contain duplicate binary channel/pattern names #911 (Thanks to @lwiddershoven)
* DefaultCommandMethodVerifier reports invalid parameter count #925 (Thanks to @GhaziTriki)
* Chunked Pub/Sub message receive with interleaved command responses leaves commands uncompleted #936 (Thanks to @GavinTianYang)
* Fix typo in log message #970 (Thanks to @twz123)
* Result is lost when published on another executor #986 (Thanks to @trueinsider)
* Cancel ClusterTopologyRefreshTask in RedisClusterClient.shutdownAsync() #989 (Thanks to @johnsiu)
* ClassCastException occurs when using RedisCluster with custom-command-interface and Async API #994 (Thanks to @tamanugi)
* Application-level exceptions in Pub/Sub notifications mess up pub sub decoding state and cause timeouts #997 (Thanks to @giridharkannan)
* RedisClient.shutdown hangs because event loops terminate before connections are closed #998 (Thanks to @Poorva17)
* "Knowing Redis" section in documentation has the wrong link for meanings.  #1050 (Thanks to @Raghaava)
* ClassCastException occurs when using RedisCommandFactory with custom commands #1075 (Thanks to @mdebellefeuille)
* EventLoop thread blocked by EmitterProcessor.onNext(…) causes timeouts #1086 (Thanks to @trishaarao79)
* RedisClusterNode without slots is never considered having same slots as an equal object #1089 (Thanks to @y2klyf)
* RedisClusterClient doesn't respect the SocketOptions connectTimeout #1119 (Thanks to @magnusandy)
* Remove duplicated ConnectionWatchdog #1132 (Thanks to @mors741)

Other
-----
* Migrate tests to JUnit 5 #430
* Could not generate CGLIB subclass of class io.lettuce.core.support.ConnectionPoolSupport$1 #843 (Thanks to @peerit12)
* Adapt to changed terminology for master/replica #845
* Ability to provide a custom service name for spans #866 (Thanks to @worldtiki)
* Remove tempusfugit dependency #871
* Makefile refactor download redis #877 (Thanks to @LarryBattle)
* Upgrade to Reactor Californium SR1 #883
* Upgrade to Redis 5 GA #893
* Document MasterSlave connection behavior on partial node failures #894 (Thanks to @jocull)
* Upgrade to RxJava 2.2.3 #901
* Upgrade to Spring Framework 4.3.20.RELEASE #902
* Upgrade to netty 4.1.30.Final #903
* Upgrade to Reactor Core 3.2.2.RELEASE #904
* Deprecate StatefulConnection.reset() #907 (Thanks to @semberal)
* Upgrade to Reactor Core 3.2.3.RELEASE #931
* Upgrade to netty 4.1.31.Final #932
* Upgrade to RxJava 2.2.4 #933
* Javadoc is missing Javadoc links to Project Reactor types (Flux, Mono) #942
* Extend year range for 2019 in license headers #950
* Use ConcurrentHashMap.newKeySet() as replacement of Netty's ConcurrentSet #961
* Streamline communication sections in readme, issue templates and contribution guide #967
* Upgrade to stunnel 5.50 #968
* Replace old reactive API docs #974 (Thanks to @pine)
* Upgrade to Reactor 3.2.6.RELEASE #975
* Upgrade to netty 4.1.33.Final #976
* Upgrade to HdrHistogram 2.1.11 #978
* Upgrade to RxJava 2.2.6 #979
* Use JUnit BOM for dependency management and upgrade to JUnit 5.4.0 #980
* Use logj42 BOM for dependency management and upgrade to 2.11.2 #981
* Upgrade to AssertJ 3.12.0 #983
* Upgrade to AssertJ 3.12.1 #991
* Upgrade to Reactor Core 3.2.8.RELEASE #1006
* Upgrade to netty 4.1.35.Final #1017
* Upgrade to Reactor Core 3.3 #1030
* Upgrade to Brave 5.6 #1031
* Migrate off TopicProcessor to EmitterProcessor #1032
* Upgrade to Netty 4.1.36.Final #1033
* Upgrade to JUnit 5.4.2 #1034
* Upgrade to Mockito 2.27.0 #1035
* Upgrade to RxJava 2.2.8 #1036
* Upgrade build plugin dependencies #1037
* RedisURI: fix missing "the" in Javadoc #1049 (Thanks to @perlun)
* Upgrade to RxJava 2.2.9 #1054
* Upgrade to jsr305 3.0.2 #1055
* Upgrade TravisCI build to Xenial/Java 11 #1056
* Upgrade to OpenWebBeans 2.0.11 #1062
* Simplify Redis…CommandsImpl to accept connection interfaces #1077
* Upgrade to netty 4.1.38.Final #1093
* Upgrade to Reactor Core 3.2.11.RELEASE #1094
* Upgrade to Mockito 3.0 #1095
* Upgrade to JUnit 5.5.1 #1096
* Upgrade to Brave 5.6.9 #1097
* Update plugin versions #1098
* Upgrade to Commons Pool 2.7.0 #1099
* Make ListSubscriber public #1103 (Thanks to @jruaux)
* Upgrade to netty 4.1.42.Final #1131
* Add default manifest entries and automatic module name #1135
* Upgrade to Brave 5.7.0 #1136
* Deprecate Utf8StringCodec in favor or StringCodec.UTF8 #1137
