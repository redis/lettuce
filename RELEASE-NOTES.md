Lettuce 5.2.2 RELEASE NOTES
===========================

The Lettuce team is pleased to announce the Lettuce 5.2.2 release! 
This release ships with mostly bug fixes and dependency upgrades addressing 13 tickets in total.
 
Find the full changelog at the end of this document.

Thanks to all contributors who made Lettuce 5.2.2.RELEASE possible.
Lettuce requires a minimum of Java 8 to build and run and is compatible with Java 14. It is tested continuously against the latest Redis source-build.

If you need any support, meet Lettuce at

* Google Group (General discussion, announcements, and releases): https://groups.google.com/d/forum/lettuce-redis-client-users
or lettuce-redis-client-users@googlegroups.com
* Stack Overflow (Questions): https://stackoverflow.com/questions/tagged/lettuce
* Join the chat at https://gitter.im/lettuce-io/Lobby for general discussion
* GitHub Issues (Bug reports, feature requests): https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/core/5.2.2.RELEASE/reference/
* Javadoc: https://lettuce.io/core/5.2.2.RELEASE/api/

Enhancements
------------
* Optimization of BITFIELD args generation #1175 (Thanks to @ianpojman)

Fixes
-----
* BoundedAsyncPool doesn't work with a negative maxTotal #1181 (Thanks to @sguillope)
* Issuing `GEORADIUS_RO` on a replica fails when no masters are available. #1198 (Thanks to @leif-erikson)
* TLS setup fails to a master reported by sentinel #1209 (Thanks to @ae6rt)
* Lettuce metrics creates lots of long arrays, and gives out of memory error.  #1210 (Thanks to @omjego)
* CommandSegments.StringCommandType does not implement hashCode()/equals() #1211
* Unclear documentation about quiet time for RedisClient#shutdown  #1212 (Thanks to @LychakGalina)
* Write race condition while migrating/importing a slot #1218 (Thanks to @phyok)
* Relax field count check in CommandDetailParser #1200
* Lettuce not able to reconnect automatically to SSL+authenticated ElastiCache node #1201 (Thanks to @chadlwilson)

Other
-----
* Disable RedisURIBuilderUnitTests failing on Windows OS #1204 (Thanks to @kshchepanovskyi)
* Un-deprecate ClientOptions.pingBeforeActivateConnection #1208
* Upgrade dependencies #1225
