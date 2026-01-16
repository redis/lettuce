Lettuce 7.4.0.BETA1 RELEASE NOTES
==============================

The Lettuce team is pleased to announce the **first beta release of Lettuce 7.4.0**!

This release introduces **Automatic Client-Side Endpoint Failover** through the newly added **MultiDBClient**.

### Key Features

- **Automatic Detection**: The client monitors the health of all configured Redis endpoints.
- **Seamless Failover**: If the active endpoint fails, traffic is automatically rerouted to the next healthiest endpoint based on configurable priorities.
- **Customizable**: Developers can configure endpoints, set priorities, adjust failure sensitivity, and plug in custom health checks or failure detection logic.


**Lettuce 7.4.0.BETA1** supports Redis **2.6+** up to Redis **8.x** and requires **Java 8** or newer. The driver is tested against Redis **8.4**, **8.2**, **7.4**, and **7.2**.



We encourage you to try out this beta and provide feedback ahead of the general availability release.

**Full Changelog**: https://github.com/redis/lettuce/compare/7.2.0.RELEASE...7.4.0.BETA1

## Contributors
We'd like to thank all the contributors who worked on this release!
@atakavci, @ggivo, @uglide