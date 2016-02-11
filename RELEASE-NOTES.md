lettuce 3.4.1 RELEASE NOTES
=========================

lettuce 3.4.1 is a bugfix release to fix issues with Redis Cluster and thread interrupts.

Fixes
-----
* NPE when issuing multi-exec against cluster #187 (thanks to @rovarghe)
* ClusterTopologyRefresh fails with NPE on password-secured Cluster nodes if password is not set #189
* Redis Cluster Node connections are not authenticated in a password protected Cluster (lettuce 3.4) #190
* Set interrupted bit after catching InterruptedException #192


lettuce requires a minimum of Java 8 to build and Java 6 run. It is tested
continuously against Redis 3.0 and the unstable branch.

If you need any support, meet lettuce at

* Google Group: https://groups.google.com/d/forum/lettuce-redis-client-users
                or lettuce-redis-client-users@googlegroups.com
* Join the chat at https://gitter.im/mp911de/lettuce
* Github Issues: https://github.com/mp911de/lettuce/issues
* Wiki: https://github.com/mp911de/lettuce/wiki
