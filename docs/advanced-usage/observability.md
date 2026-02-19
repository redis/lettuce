# Observability

The following section explains Lettuces metrics and tracing
capabilities.

## Metrics

Command latency metrics give insight into command execution and
latencies. Metrics are collected for every completed command. Lettuce
has two mechanisms to collect latency metrics:

- [Built-in](#built-in-latency-tracking) (since version 3.4 using
  HdrHistogram and LatencyUtils. Enabled by default if both libraries
  are available on the classpath.)

- [Micrometer](#micrometer) (since version 6.1)

## Built-in latency tracking

Each command is tracked with:

- Execution count

- Latency to first response (min, max, percentiles)

- Latency to complete (min, max, percentiles)

Command latencies are tracked on remote endpoint (distinction by host
and port or socket path) and command type level (`GET`, `SET`, …​). It is
possible to track command latencies on a per-connection level (see
`DefaultCommandLatencyCollectorOptions`).

Command latencies are transported using Events on the `EventBus`. The
`EventBus` can be obtained from the [client
resources](client-resources.md) of the client instance. Please
keep in mind that the `EventBus` is used for various event types. Filter
on the event type if you’re interested only in particular event types.

``` java
RedisClient client = RedisClient.create();
EventBus eventBus = client.getResources().eventBus();

Subscription subscription = eventBus.get()
                .filter(redisEvent -> redisEvent instanceof CommandLatencyEvent)
                .cast(CommandLatencyEvent.class)
                .subscribe(e -> System.out.println(e.getLatencies()));
```

The `EventBus` uses Reactor Processors to publish events. This example
prints the received latencies to `stdout`. The interval and the
collection of command latency metrics can be configured in the
`ClientResources`.

### Prerequisites

Lettuce requires the LatencyUtils dependency (at least 2.0) to provide
latency metrics. Make sure to include that dependency on your classpath.
Otherwise, you won’t be able using latency metrics.

If using Maven, add the following dependency to your pom.xml:

``` xml
<dependency>
    <groupId>org.latencyutils</groupId>
    <artifactId>LatencyUtils</artifactId>
    <version>2.0.3</version>
</dependency>
```

### Disabling command latency metrics

To disable metrics collection, use own `ClientResources` with a disabled
`DefaultCommandLatencyCollectorOptions`:

``` java
ClientResources res = DefaultClientResources
        .builder()
        .commandLatencyCollectorOptions( DefaultCommandLatencyCollectorOptions.disabled())
        .build();

RedisClient client = RedisClient.create(res);
```

### CommandLatencyCollector Options

The following settings are available to configure from
`DefaultCommandLatencyCollectorOptions`:

| Name                                                                                                                                                                                                                                                                                                                                                                                  | Method                     | Default                         |
|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------|---------------------------------|
| **Disable metrics tracking**                                                                                                                                                                                                                                                                                                                                                          | `disable`                  | `false`                         |
| Disables tracking of command latency metrics.                                                                                                                                                                                                                                                                                                                                         |                            |                                 |
| **Latency time unit**                                                                                                                                                                                                                                                                                                                                                                 | `targetUnit`               | `MICROSECONDS`                  |
| The target unit for command latency values. All values in the `CommandLatencyEvent` and a `CommandMetrics` instance are `long` values scaled to the `targetUnit`.                                                                                                                                                                                                                     |                            |                                 |
| **Latency percentiles**                                                                                                                                                                                                                                                                                                                                                               | `targetPercentiles`        | `50.0, 90 .0, 95.0, 99.0, 99.9` |
| A `double`-array of percentiles for latency metrics. The `CommandMetrics` contains a map that holds the percentile value and the latency value according to the percentile. Note that percentiles here must be specified in the range between 0 and 100.                                                                                                                              |                            |                                 |
| **Reset latencies after publish**                                                                                                                                                                                                                                                                                                                                                     | `resetLatenciesAfterEvent` | `true`                          |
| Allows controlling whether the latency metrics are reset to zero once they were published. Setting `resetLatenciesAfterEvent` allows accumulating metrics over a long period for long-term analytics.                                                                                                                                                                                  |                            |                                 |
| **Local socket distinction**                                                                                                                                                                                                                                                                                                                                                          | `localDistinction`         | `false`                         |
| Enables per connection metrics tracking instead of per host/port. If `true`, multiple connections to the same host/connection point will be recorded separately which allows to inspection of every connection individually. If `false`, multiple connections to the same host/connection point will be recorded together. This allows a consolidated view on one particular service. |                            |                                 |

### EventPublisher Options

The following settings are available to configure from
`DefaultEventPublisherOptions`:

| Name                                              | Method                  | Default   |
|---------------------------------------------------|-------------------------|-----------|
| **Disable event publisher**                       | `disable`               | `false`   |
| Disables event publishing.                        |                         |           |
| **Event publishing time unit**                    | `eventEmitIntervalUnit` | `MINUTES` |
| The `TimeUnit` for the event publishing interval. |                         |           |
| **Event publishing interval**                     | `eventEmitInterval`     | `10`      |
| The interval for the event publishing.            |                         |           |

## Micrometer

Commands are tracked by using two Micrometer `Timer`s:
`lettuce.command.firstresponse` and `lettuce.command.completion`. The
following tags are attached to each timer:

- `command`: Name of the command (`GET`, `SET`, …​)

- `local`: Local socket (`localhost/127.0.0.1:45243` or `ANY` when local
  distinction is disabled, which is the default behavior)

- `remote`: Remote socket (`localhost/127.0.0.1:6379`)

Command latencies are reported using the provided `MeterRegistry`.

``` java
MeterRegistry meterRegistry = …;
MicrometerOptions options = MicrometerOptions.create();
ClientResources resources = ClientResources.builder().commandLatencyRecorder(new MicrometerCommandLatencyRecorder(meterRegistry, options)).build();

RedisClient client = RedisClient.create(resources);
```

### Prerequisites

Lettuce requires Micrometer (`micrometer-core`) to integrate with
Micrometer.

If using Maven, add the following dependency to your pom.xml:

``` xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-core</artifactId>
    <version>${micrometer.version}</version> <!-- e.g. micrometer.version==1.6.0 -->
</dependency>
```

### Micrometer Options

The following settings are available to configure from
`MicrometerOptions`:

| Name                                                                                                                                                                                                                                                                                                                                                                               | Method              | Default                                                                            |
|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------|------------------------------------------------------------------------------------|
| **Disable metrics tracking**                                                                                                                                                                                                                                                                                                                                                       | `disable`           | `false`                                                                            |
| Disables tracking of command latency metrics.                                                                                                                                                                                                                                                                                                                                      |                     |                                                                                    |
| **Histogram**                                                                                                                                                                                                                                                                                                                                                                      | `histogram`         | `false`                                                                            |
| Enable histogram buckets used to generate aggregable percentile approximations in monitoring systems that have query facilities to do so.                                                                                                                                                                                                                                          |                     |                                                                                    |
| **Local socket distinction**                                                                                                                                                                                                                                                                                                                                                       | `localDistinction`  | `false`                                                                            |
| Enables per connection metrics tracking instead of per host/port. If `true`, multiple connections to the same host/connection point will be recorded separately which allows inspection of every connection individually. If `false`, multiple connections to the same host/connection point will be recorded together. This allows a consolidated view on one particular service. |                     |                                                                                    |
| **Maximum Latency**                                                                                                                                                                                                                                                                                                                                                                | `maxLatency`        | `5 Minutes`                                                                        |
| Sets the maximum value that this timer is expected to observe. Applies only if Histogram publishing is enabled.                                                                                                                                                                                                                                                                    |                     |                                                                                    |
| **Minimum Latency**                                                                                                                                                                                                                                                                                                                                                                | `minLatency`        | `1ms`                                                                              |
| Sets the minimum value that this timer is expected to observe. Applies only if Histogram publishing is enabled.                                                                                                                                                                                                                                                                    |                     |                                                                                    |
| **Additional Tags**                                                                                                                                                                                                                                                                                                                                                                | `tags`              | `Tags.empty()`                                                                     |
| Extra tags to add to the generated metrics.                                                                                                                                                                                                                                                                                                                                        |                     |                                                                                    |
| **Latency percentiles**                                                                                                                                                                                                                                                                                                                                                            | `targetPercentiles` | `0.5, 0.9, 0.95,  0.99, 0.999 (corresponding with 50.0, 90.0, 95.0, 99.0, 99.9)` |
| A `double`-array of percentiles for latency metrics. Values must be supplied in the range of `0.0` (0th percentile) up to `1.0` (100th percentile). The `CommandMetrics` contains a map that holds the percentile value and the latency value according to the percentile. This applies only if Histogram publishing is enabled.                                                   |                     |                                                                                    |

## Tracing

Tracing gives insights about individual Redis commands sent to Redis to
trace their frequency, duration and to trace of which commands a
particular activity consists. Lettuce provides a tracing SPI to avoid
mandatory tracing library dependencies. Lettuce ships integrations with
[Micrometer Tracing](https://github.com/micrometer-metrics/tracing) and
[Brave](https://github.com/openzipkin/brave) which can be configured
through [client resources](client-resources.md).

### Micrometer Tracing

With Micrometer tracing enabled, Lettuce creates an observation for each
Redis command resulting in spans per Command and corresponding Meters if
configured in Micrometer’s `ObservationContext`.

##### Prerequisites

Lettuce requires the Micrometer Tracing dependency to provide Tracing
functionality. Make sure to include that dependency on your classpath.

If using Maven, add the following dependency to your pom.xml:

``` xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing</artifactId>
</dependency>
```

The following example shows how to configure tracing through
`ClientResources`:

``` java
ObservationRegistry observationRegistry = …;

MicrometerTracing tracing = new MicrometerTracing(observationRegistry, "Redis");

ClientResources resources = ClientResources.builder().tracing(tracing).build();
```

### Brave

With Brave tracing enabled, Lettuce creates a span for each Redis
command. The following options can be configured:

- `serviceName` (defaults to `redis`).

- `Endpoint` customizer. This option can be used together with a custom
  `SocketAddressResolver` to attach custom endpoint details.

- `Span` customizer. Allows for customization of spans based on the
  actual Redis `Command` object.

- Inclusion/Exclusion of all command arguments in a span. By default,
  all arguments are included.

##### Prerequisites

Lettuce requires the Brave dependency (at least 5.1) to provide Tracing
functionality. Make sure to include that dependency on your classpath.

If using Maven, add the following dependency to your pom.xml:

``` xml
<dependency>
    <groupId>io.zipkin.brave</groupId>
    <artifactId>brave</artifactId>
</dependency>
```

The following example shows how to configure tracing through
`ClientResources`:

``` java
brave.Tracing clientTracing = …;

BraveTracing tracing = BraveTracing.builder().tracing(clientTracing)
    .excludeCommandArgsFromSpanTags()
    .serviceName("custom-service-name-goes-here")
    .spanCustomizer((command, span) -> span.tag("cmd", command.getType().name()))
    .build();

ClientResources resources = ClientResources.builder().tracing(tracing).build();
```

Lettuce ships with a Tracing SPI in `io.lettuce.core.tracing` that
allows custom tracer implementations.
