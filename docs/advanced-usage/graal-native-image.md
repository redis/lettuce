# Graal Native Image

This section explains how to use Lettuce with Graal Native Image
compilation.

## Why Create a Native Image?

The GraalVM
[`native-image`](http://www.graalvm.org/docs/reference-manual/aot-compilation/)
tool enables ahead-of-time (AOT) compilation of Java applications into
native executables or shared libraries. While traditional Java code is
just-in-time (JIT) compiled at run time, AOT compilation has two main
advantages:

1.  First, it improves the start-up time since the code is already
    pre-compiled into efficient machine code.

2.  Second, it reduces the memory footprint of Java applications since
    it eliminates the need to include infrastructure to load and
    optimize code at run time.

There are additional advantages such as more predictable performance and
less total CPU usage.

## Building Native Images

Native images assume a closed world principle in which all code needs to
be known at the time the native image is built. Graal's SubstrateVM
analyzes class files during native image build-time to determine what
bytecode needs to be translated into a native image. While this task can
be achieved to a good extent by analyzing static bytecode, it’s harder
for dynamic parts of the code such as reflection. When using reflective
access or Java proxies, the native image build process requires a little
bit of help so it can include parts that are required during runtime.

Lettuce ships with configuration files that specifically describe which
classes are used by Lettuce during runtime and which Java proxies get
created.

Starting as of Lettuce 5.3.2, the following configuration files are
available:

- `META-INF/native-image/io.lettuce/lettuce-core/native-image.properties`

- `META-INF/native-image/io.lettuce/lettuce-core/proxy-config.json`

- `META-INF/native-image/io.lettuce/lettuce-core/reflect-config.json`

Those cover Lettuce operations for `RedisClient` and
`RedisClusterClient`.

Depending on your configuration you might need additional configuration
for Netty, HdrHistogram (metrics collection), Reactive Libraries, and
dynamic Redis Command interfaces.

## HdrHistogram/Command Latency Metrics

Lettuce uses HdrHistogram and LatencyUtils to accumulate metrics. You
can use your application without these. If you want to use Command
Latency Metrics, please add the following lines to your own
`reflect-config.json` file:

``` json
  {
    "name": "org.HdrHistogram.Histogram"
  },
  {
    "name": "org.LatencyUtils.PauseDetector"
  }
```

## Dynamic Command Interfaces

You can use Dynamic Command Interfaces when compiling your code to a
GraalVM Native Image. GraalVM requires two information as Lettuce
inspects command interfaces using reflection and it creates a Java
proxy:

1.  Add the command interface class name to your `reflect-config.json`
    using ideally `allDeclaredMethods:true`.

2.  Add the command interface class name to your `proxy-config.json`

<div class="formalpara-title">

**`reflect-config.json`**

</div>

``` json
[
  {
    "name": "com.example.MyCommands",
    "allDeclaredMethods": true
  },
]
```

<div class="formalpara-title">

**`proxy-config.json`**

</div>

``` json
[
  ["com.example.MyCommands"]
]
```

### Reactive Libraries

If you decide to use a specific reactive library with dynamic command
interfaces, please add the following lines to your `reflect-config.json`
file, depending on the presence of Rx Java 1-3:

``` json
  {
    "name": "rx.Completable"
  },
  {
    "name": "io.reactivex.Flowable"
  },
  {
    "name": "io.reactivex.rxjava3.core.Flowable"
  }
```

## Limitations

For now, native images must be compiled with
`--report-unsupported-elements-at-runtime` to ignore missing Method
Handles and annotation synthetization failures.

### Netty Config

To properly start up the netty stack, the following reflection
configuration is required for netty and the JDK in
`reflect-config.json`:

``` json
  {
    "name":"io.netty.util.internal.shaded.org.jctools.queues.BaseMpscLinkedArrayQueueColdProducerFields",
    "fields":[{"name":"producerLimit","allowUnsafeAccess" :  true}]
  },
  {
    "name":"io.netty.util.internal.shaded.org.jctools.queues.BaseMpscLinkedArrayQueueConsumerFields",
    "fields":[{"name":"consumerIndex","allowUnsafeAccess" :  true}]
  },
  {
    "name":"io.netty.util.internal.shaded.org.jctools.queues.BaseMpscLinkedArrayQueueProducerFields",
    "fields":[{"name":"producerIndex", "allowUnsafeAccess" :  true}]
  },
  {
    "name":"io.netty.util.internal.shaded.org.jctools.queues.MpscArrayQueueConsumerIndexField",
    "fields":[{"name":"consumerIndex", "allowUnsafeAccess" :  true}]
  },
  {
    "name":"io.netty.util.internal.shaded.org.jctools.queues.MpscArrayQueueProducerIndexField",
    "fields":[{"name":"producerIndex", "allowUnsafeAccess" :  true}]
  },
  {
    "name":"io.netty.util.internal.shaded.org.jctools.queues.MpscArrayQueueProducerLimitField",
    "fields":[{"name":"producerLimit","allowUnsafeAccess" :  true}]
  },
  {
    "name":"java.nio.Buffer",
    "fields":[{"name":"address", "allowUnsafeAccess":true}]
  },
  {
    "name":"java.nio.DirectByteBuffer",
    "fields":[{"name":"cleaner", "allowUnsafeAccess":true}],
    "methods":[{"name":"<init>","parameterTypes":["long","int"] }]
  },
  {
    "name":"io.netty.buffer.AbstractReferenceCountedByteBuf",
    "fields":[{"name":"refCnt", "allowUnsafeAccess":true}]
  },
  {
    "name":"io.netty.buffer.AbstractByteBufAllocator",
    "allPublicMethods": true,
    "allDeclaredFields":true,
    "allDeclaredMethods":true,
    "allDeclaredConstructors":true
  },
  {
    "name":"io.netty.buffer.PooledByteBufAllocator",
    "allPublicMethods": true,
    "allDeclaredFields":true,
    "allDeclaredMethods":true,
    "allDeclaredConstructors":true
  },
  {
    "name":"io.netty.channel.ChannelDuplexHandler",
    "allPublicMethods": true,
    "allDeclaredConstructors":true
  },
  {
    "name":"io.netty.channel.ChannelHandlerAdapter",
    "allPublicMethods": true,
    "allDeclaredConstructors":true
  },
  {
    "name": "io.netty.channel.ChannelInboundHandlerAdapter",
    "allPublicMethods": true,
    "allDeclaredConstructors":true
  },
  {
    "name": "io.netty.channel.ChannelInitializer",
    "allPublicMethods": true,
    "allDeclaredConstructors":true
  },
  {
    "name": "io.netty.channel.ChannelOutboundHandlerAdapter",
    "allPublicMethods": true,
    "allDeclaredConstructors":true
  },
  {
    "name": "io.netty.channel.DefaultChannelPipeline$HeadContext",
    "allPublicMethods": true,
    "allDeclaredConstructors":true
  },
  {
    "name": "io.netty.channel.DefaultChannelPipeline$TailContext",
    "allPublicMethods": true,
    "allDeclaredConstructors":true
  },
  {
    "name": "io.netty.channel.socket.nio.NioSocketChannel",
    "allPublicMethods": true,
    "allDeclaredConstructors":true
  },
  {
    "name": "io.netty.handler.codec.MessageToByteEncoder",
    "allPublicMethods": true,
    "allDeclaredConstructors":true
  },
  {
    "name":"io.netty.util.ReferenceCountUtil",
    "allPublicMethods": true,
    "allDeclaredConstructors":true
  }
```

### Functionality

We don’t have found a way yet to invoke default interface methods on
proxies without `MethodHandle`. Hence the `NodeSelection` API
(`masters()`, `all()` and others on `RedisAdvancedClusterCommands` and
`RedisAdvancedClusterAsyncCommands`) do not work.
