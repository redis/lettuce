Lettuce 4.4.6 RELEASE NOTES
===========================

This is the 6th bugfix release for Lettuce 4.4 shipping with 7 tickets fixed.
 
Upgrading is recommended for all users.  

You can find the full change log at the end of this document. 
Thanks to all contributors that made Lettuce 4.4.5.Final possible.

If you need any support, meet Lettuce at:

* Google Group: https://groups.google.com/d/forum/lettuce-redis-client-users
* Gitter: https://gitter.im/lettuce-io/Lobby
* GitHub Issues: https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/docs/

Fixes
-----
* MULTI is dispatched to slave nodes using SLAVE readFrom #779 (Thanks to @Yipei)
* ZSCAN match pattern encoding issue #792 (Thanks to @silvertype)
* Javadoc mentions Delay.exponential() is capped at 30 milliseconds #799 (Thanks to @phxql)
* GEORADIUS WITHCOORD returns wrong coordinate on multiple results #805 (Thanks to @dittos)
* FutureSyncInvocationHandler the statement "command.get ()" in the handlerInvocation method is unnecessary #809 (Thanks to @zhangweidavid)
* smembers returns elements in non-deterministic order #823 (Thanks to @alezandr)

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
