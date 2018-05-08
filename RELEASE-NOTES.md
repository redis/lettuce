Lettuce 5.0.4 RELEASE NOTES
===========================

This is the fourth bugfix release for Lettuce 5 shipping with 12 tickets resolved.
 
Upgrading is recommended for all users.  

Reference documentation: https://lettuce.io/core/release/reference/.
JavaDoc documentation: https://lettuce.io/core/release/api/.

```xml
<dependency>
  <groupId>io.lettuce</groupId>
  <artifactId>lettuce-core</artifactId>
  <version>5.0.4.RELEASE</version>
</dependency>
```

You can find the full change log at the end of this document. Thanks to all contributors that made Lettuce 5.0.4.RELEASE possible.

Lettuce 5.0.4.RELEASE requires Java 8 and cannot be used with Java 6 or 7.

If you need any support, meet Lettuce at:

* Google Group: https://groups.google.com/d/forum/lettuce-redis-client-users
* Gitter: https://gitter.im/lettuce-io/Lobby
* GitHub Issues: https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/docs/

Commands
------------
* Add AUTH option to MIGRATE command #733
* Add MASTER type to KillArgs #760

Fixes
-----
* Warning when refreshing topology #756 (Thanks to @theliro)
* DefaultEndpoint.QUEUE_SIZE becomes out of sync, preventing command queueing #764 (Thanks to @nivekastoreth)
* DefaultEndpoint contains System.out.println(…) #765
* Do not retry completed commands through RetryListener #767

Other
-----
* Upgrade to netty 4.1.23.Final #755
* Upgrade to Reactor Bismuth SR8 #758
* Upgrade to RxJava 1.3.8 #759
* Extend documentation for argument objects #761
* Upgrade to RxJava 2.1.13 #771
* Upgrade to netty 4.1.24.Final #770

Lettuce requires a minimum of Java 8 to build and run. It is tested continuously
against the latest Redis source-build.

If you need any support, meet Lettuce at

* Google Group: https://groups.google.com/d/forum/lettuce-redis-client-users
or lettuce-redis-client-users@googlegroups.com
* Join the chat at https://gitter.im/lettuce-io/Lobby
* GitHub Issues: https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/docs/
