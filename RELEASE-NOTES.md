Lettuce 7.0.0 RELEASE NOTES
==============================

The Lettuce team is pleased to announce the release of Lettuce 7.0.0!

The release focuses on introducing **Maintenance events support** functionality, API improvements, and cleanup of deprecated features.

### Key changes
- **Maintenance events support** for graceful maintenance handling
- **Enhanced JSON API** with `String`-based access to avoid unnecessary conversions
- **Removal of deprecated APIs** and options as part of the major version upgrade
- **Upgrading to Netty 4.2**

**Lettuce 7.0.0.RELEASE** supports Redis **2.6+** up to Redis **8.x** and requires **Java 8** or newer. The driver is tested against Redis **8.2**, **8.0**, **7.4**, and **7.2**.

Thanks to all contributors who made Lettuce 7.0.0.RELEASE possible!

If you need any support, meet Lettuce at

* GitHub Discussions: https://github.com/lettuce-io/lettuce-core/discussions
* Stack Overflow (Questions): https://stackoverflow.com/questions/tagged/lettuce
* Join the chat at https://discord.gg/redis and look for the "Help:Tools Lettuce" channel 
* GitHub Issues (Bug reports, feature requests): https://github.com/lettuce-io/lettuce-core/issues
* Documentation: https://lettuce.io/core/7.0.0.RELEASE/reference/
* Javadoc: https://lettuce.io/core/7.0.0.RELEASE/api/

# Changes

## 🔥 Breaking Changes

- The KEYS command needs to be keyless (#3341)
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

- Search - request/response policies implementation with API overrides (#3465)
- Implement JSON raw return types (#3478)
- Add support for EPSILON and WITHATTRIBS arguments in VSIM command (#3449)
- Add String-based JSON API to avoid unnecessary conversions (#3369) (#3394)
- React to maintenance events #3345 (#3354, #3450, #3426)

## 🐛 Bug Fixes
- Fix cluster scan deadlock (#3448)
- Timeouts seen during endpoint re-bind and migrate (#3426)
- Fix a NullPointerException in DelegateJsonObject #3417 (#3418)

## 💡 Other

- feat: add JSON read-only commands to ReadOnlyCommands (#3462)
- Upgrading to Netty 4.2 (#3405)
- Make search commands truly keyless (#3456)
- Add getCodec method to StatefulConnection (#3444)
- Return name method to ProtocolKeyword public interface. (#3424)
- Refactor JsonValue to Object mapping #3412 (#3413)
- Using non-native transports with SocketOptions should cause an error (#3279)
---

**Full Changelog**: [6.8.0.RELEASE...7.0.0.RELEASE](https://github.com/redis/lettuce/compare/6.8.0.RELEASE...7.0.0.BETA2)
