Lettuce 8.0.0.BETA1
==============================
The Lettuce team is pleased to announce the **first beta release of Lettuce 8.0**!

This release makes **Project Reactor not-required**: applications that use only the synchronous and asynchronous APIs can exclude `reactor-core`:

```xml
<dependency>
    <groupId>io.lettuce</groupId>
    <artifactId>lettuce-core</artifactId>
    <version>8.0.0.BETA1</version>
    <exclusions>
        <exclusion>
            <groupId>io.projectreactor</groupId>
            <artifactId>reactor-core</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

Lettuce 8.0 also removes Reactor-based APIs, most of them deprecated in 7.8 and 7.9, and changes Reactor-typed extension points. See [What's new in Lettuce 8.0](https://github.com/redis/lettuce/blob/main/docs/new-features.md#whats-new-in-lettuce-80) for details and the breaking changes.

PR: [reactor-optional]  Make 'Project Reactor' optional dependency by @atakavci in https://github.com/redis/lettuce/pull/3731

**Lettuce 8.0.0.BETA1** supports Redis **2.6+** up to Redis **8.x** and requires **Java 8** or newer. The driver is tested against Redis **8.12**, **8.10**, **8.8**, **8.6**, **8.4**, **8.2**, **7.4** and **7.2**.

We encourage you to try out this beta, in particular with Reactor excluded, and to provide feedback ahead of the general availability release.

**Full Changelog**: https://github.com/redis/lettuce/compare/7.9.0.RELEASE...8.0.0.BETA1