Lettuce 6.9.0 RELEASE NOTES
==============================

The Lettuce team is pleased to announce the Lettuce 6.9.0 release!
This release changes how commands are rejected while a connection is not available. Read the behavior change
below before you upgrade.

Lettuce 6 supports Redis 2.6+ up to Redis 8.2. In terms of Java runtime, Lettuce requires
at least Java 8 and works with Java 21.

Thanks to all contributors who made Lettuce 6.9.0 possible.

* GitHub Discussions: https://github.com/lettuce-io/lettuce-core/discussions
* Stack Overflow (Questions): https://stackoverflow.com/questions/tagged/lettuce
* Join the chat at https://discord.gg/redis and look for the "Help:Tools Lettuce" channel 
* GitHub Issues (Bug reports, feature requests): https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/core/6.9.0.RELEASE/reference/
* Javadoc: https://lettuce.io/core/6.9.0.RELEASE/api/

Behavior change
---------------
This applies when commands are rejected while disconnected: with `DisconnectedBehavior.REJECT_COMMANDS`, or with
`DisconnectedBehavior.DEFAULT` and auto-reconnect disabled.

* Redis Cluster: a command now fails right away when the connection to its target node is still being
  established, for example the first commands to a node after startup, or after a new slot mapping appears. This
  also applies to commands redirected with `MOVED`/`ASK`. Before, these commands waited for the connection
  attempt, up to the connect timeout.
* All connection types: when writing a command to the channel fails, the command is now rejected instead of
  being re-queued.

To avoid rejections right after startup, connect to the cluster nodes up front.

This change is also in 7.7.0 and later. The 7.0.x – 7.6.x lines do not include it.

Fixes
-----
* DisconnectedBehavior.REJECT_COMMANDS does not fail fast for Redis Cluster commands waiting on lazy node connection establishment in https://github.com/redis/lettuce/issues/3747 (backport of https://github.com/redis/lettuce/pull/3782 by @Vaibhav026)

**Full Changelog**: https://github.com/redis/lettuce/compare/6.8.2.RELEASE...6.9.0.RELEASE
