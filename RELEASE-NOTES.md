Lettuce 7.0.0.BETA2 RELEASE NOTES
==============================

The Lettuce team is pleased to announce the second beta release of Lettuce 7.0!

The release focuses on introducing **Maintenance events support** functionality, API improvements, and cleanup of deprecated features.

### Key changes
- **Maintenance events support** for graceful maintenance handling
- **Enhanced JSON API** with `String`-based access to avoid unnecessary conversions
- **Removal of deprecated APIs** and options as part of the major version upgrade
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

- chore: remove usage of deprecated connection methods in command APIs in integration tests (#3328) (#3343)
- Remove deprecated dnsResolver option (#3328) (#3333)
- Remove deprecated `reset()` method from Lettuce API and internals (#3395)
- Make Utility Class constructor private to enforce noninstantiability (#3266)
- Enable adaptive refresh by default #3249 (#3316)
- ISSUE#3328 - Remove deprecated code from ISSUE#1314 (#3351)
- chore: deprecated withPassword(String) method (#3328) (#3350)
- Remove deprecated Utf8StringCodec class (#3328) (#3389)
- chore: remove deprecated default timeout from AbstractRedisClient (#3328) (#3344)
- chore: remove deprecated ClientOptions#cancelCommandsOnReconnectFailure (#3328) (#3346)

## 🚀 New Features

- Add support for EPSILON and WITHATTRIBS arguments in VSIM command (#3449)
- Add String-based JSON API to avoid unnecessary conversions (#3369) (#3394)
- React to maintenance events #3345 (#3354)

## 🐛 Bug Fixes
- Rename maintenance notification configuration properties (#3450)
- Timeouts seen during endpoint re-bind and migrate (#3426)
- Fix a NullPointerException in DelegateJsonObject #3417 (#3418)

## 💡 Other

- Timeouts seen during endpoint re-bind and migrate (#3426)
- Return name method to ProtocolKeyword public interface. (#3424)
- Refactor JsonValue to Object mapping #3412 (#3413)
- Using non-native transports with SocketOptions should cause an error (#3279)

## 🧰 Maintenance

- Fixing compilation error in benchmark code (#3442)
- docs: Fix various typos in documentation (#3423)
- bump test inra to 8.2.1-pre (#3399)
- Fixing the benchmarks action (#3402)
- Disable flaky test to stabilize the pipeline (#3403)
- Update redis-search.md (#3401)
- Bump kotlin.version from 1.7.21 to 2.0.0 (#2979)
- 
---

**Full Changelog**: [6.8.0.RELEASE...7.0.0.BETA2](https://github.com/redis/lettuce/compare/6.8.0.RELEASE...7.0.0.BETA2)
