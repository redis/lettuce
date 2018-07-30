Lettuce 5.0.5 RELEASE NOTES
===========================

This is the fifth bugfix release for Lettuce 5 shipping with 10 tickets resolved.
 
Upgrading is recommended for all users.  

Reference documentation: https://lettuce.io/core/release/reference/.
JavaDoc documentation: https://lettuce.io/core/release/api/.

```xml
<dependency>
  <groupId>io.lettuce</groupId>
  <artifactId>lettuce-core</artifactId>
  <version>5.0.5.RELEASE</version>
</dependency>
```

You can find the full change log at the end of this document. Thanks to all contributors that made Lettuce 5.0.5.RELEASE possible.

Lettuce 5.0.5.RELEASE requires Java 8 and cannot be used with Java 6 or 7.

If you need any support, meet Lettuce at:

* Google Group: https://groups.google.com/d/forum/lettuce-redis-client-users
* Gitter: https://gitter.im/lettuce-io/Lobby
* GitHub Issues: https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/docs/

Enhancements
------------
* ZSCAN match pattern encoding issue #792 (Thanks to @silvertype)
* FutureSyncInvocationHandler the statement "command.get ()" in the handlerInvocation method is unnecessary #809 (Thanks to @zhangweidavid)

Fixes
-----
* MULTI is dispatched to slave nodes using SLAVE readFrom #779 (Thanks to @Yipei)
* Javadoc mentions Delay.exponential() is capped at 30 milliseconds #799 (Thanks to @phxql)
* Read From Slaves is not working #804 (Thanks to @EXPEbdodla)
* GEORADIUS WITHCOORD returns wrong coordinate on multiple results #805 (Thanks to @dittos)
* smembers returns elements in non-deterministic order #823 (Thanks to @alezandr)
* StackOverflowError on ScanStream.scan(…).subscribe() #824
* Improve Javadoc of QUIT method #781

Other
-----
* Upgrade to AssertJ 3.10.0 #794

Lettuce requires a minimum of Java 8 to build and run. It is tested continuously
against the latest Redis source-build.

If you need any support, meet Lettuce at

* Google Group: https://groups.google.com/d/forum/lettuce-redis-client-users
or lettuce-redis-client-users@googlegroups.com
* Join the chat at https://gitter.im/lettuce-io/Lobby
* GitHub Issues: https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/docs/
