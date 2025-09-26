Lettuce 7.0.0.BETA2 RELEASE NOTES
==============================

The Lettuce team is pleased to announce the second beta release of Lettuce 7.0!

The release focuses on introducing hitless upgrades functionality, API improvements, and cleanup of deprecated features.

Key changes include:
- Introduction of hitless upgrades support for graceful maintenance event handling
- Enhanced JSON API with string-based access to avoid unnecessary conversions
- Removal of multiple deprecated APIs and options as part of the major version upgrade
As part of the 7.0 line, this beta also removes several deprecated APIs and options.

Lettuce 7.0.0.BETA2 supports Redis 2.6+ up to Redis 8.x and requires Java 8 or newer. The driver is tested against Redis 8.2, 8.0, 7.4, and 7.2.

Thanks to all contributors who made Lettuce 7.0.0.BETA2 possible!

If you need any support, meet Lettuce at

* GitHub Discussions: https://github.com/lettuce-io/lettuce-core/discussions
* Stack Overflow (Questions): https://stackoverflow.com/questions/tagged/lettuce
* Join the chat at https://discord.gg/redis and look for the "Help:Tools Lettuce" channel 
* GitHub Issues (Bug reports, feature requests): https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/core/7.0.0.BETA2/reference/
* Javadoc: https://lettuce.io/core/7.0.0.BETA2/api/

# Changes

## 🔥 Breaking Changes

- chore: remove usage of deprecated connection methods in command APIs in integration tests ([#3328](https://github.com/redis/lettuce/issues/3328), [#3343](https://github.com/redis/lettuce/pull/3343))
- Remove deprecated `dnsResolver` option ([#3328](https://github.com/redis/lettuce/issues/3328), [#3333](https://github.com/redis/lettuce/pull/3333))
- Remove deprecated `reset()` method from Lettuce API and internals ([#3395](https://github.com/redis/lettuce/pull/3395))
- Make Utility Class constructor private to enforce noninstantiability ([#3266](https://github.com/redis/lettuce/pull/3266))
- Enable adaptive refresh by default ([#3249](https://github.com/redis/lettuce/issues/3249), [#3316](https://github.com/redis/lettuce/pull/3316))
- Remove deprecated code from ISSUE [#1314](https://github.com/redis/lettuce/issues/1314) ([#3328](https://github.com/redis/lettuce/issues/3328), [#3351](https://github.com/redis/lettuce/pull/3351))
- chore: deprecated `withPassword(String)` method ([#3328](https://github.com/redis/lettuce/issues/3328), [#3350](https://github.com/redis/lettuce/pull/3350))
- Remove deprecated `Utf8StringCodec` class ([#3328](https://github.com/redis/lettuce/issues/3328), [#3389](https://github.com/redis/lettuce/pull/3389))
- chore: remove deprecated default timeout from `AbstractRedisClient` ([#3328](https://github.com/redis/lettuce/issues/3328), [#3344](https://github.com/redis/lettuce/pull/3344))
- chore: remove deprecated `ClientOptions#cancelCommandsOnReconnectFailure` ([#3328](https://github.com/redis/lettuce/issues/3328), [#3346](https://github.com/redis/lettuce/pull/3346))

---

## 🚀 New Features

- Add String-based JSON API to avoid unnecessary conversions ([#3369](https://github.com/redis/lettuce/pull/3369), [#3394](https://github.com/redis/lettuce/pull/3394))
- React to maintenance events ([#3345](https://github.com/redis/lettuce/issues/3345), [#3354](https://github.com/redis/lettuce/pull/3354))

---

## 🐛 Bug Fixes

- Timeouts seen during endpoint re-bind and migrate ([#3426](https://github.com/redis/lettuce/pull/3426))
- Fix a `NullPointerException` in `DelegateJsonObject` ([#3417](https://github.com/redis/lettuce/issues/3417), [#3418](https://github.com/redis/lettuce/pull/3418))

---

## 💡 Other

- Return `name` method to `ProtocolKeyword` public interface ([#3424](https://github.com/redis/lettuce/pull/3424))
- Refactor JsonValue to Object mapping ([#3412](https://github.com/redis/lettuce/issues/3412), [#3413](https://github.com/redis/lettuce/pull/3413))
- Using non-native transports with `SocketOptions` should cause an error ([#3279](https://github.com/redis/lettuce/pull/3279))

---

**Full Changelog**: [6.8.0.RELEASE...7.0.0.BETA2](https://github.com/redis/lettuce/compare/6.8.0.RELEASE...7.0.0.BETA2)
