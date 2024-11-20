window.BENCHMARK_DATA = {
  "lastUpdate": 1732100550476,
  "repoUrl": "https://github.com/redis/lettuce",
  "entries": {
    "Benchmark": [
      {
        "commit": {
          "author": {
            "name": "Tihomir Krasimirov Mateev",
            "username": "tishun",
            "email": "tihomir.mateev@redis.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "d616b16661fb134884d32d6f487869318b3b1a03",
          "message": "Using the right name for the file this time (#3057)",
          "timestamp": "2024-11-20T10:34:45Z",
          "url": "https://github.com/redis/lettuce/commit/d616b16661fb134884d32d6f487869318b3b1a03"
        },
        "date": 1732100548625,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSet",
            "value": 72776.66949122012,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatch",
            "value": 5588.771117767321,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatchFlush",
            "value": 5023.131364638531,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSet",
            "value": 73867.09651887265,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatch",
            "value": 6353.744346075217,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatchFlush",
            "value": 6365.824865216304,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncList",
            "value": 164429.56937957832,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncSet",
            "value": 72230.42854039048,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommands",
            "value": 90.07124499600394,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommandsAsBatch",
            "value": 370.67016389170675,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writeKeyedCommand",
            "value": 30.55523006025594,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writePlainCommand",
            "value": 7.7753206913667725,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSet",
            "value": 89807.76086343479,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatch",
            "value": 7563.722096509868,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatchFlush",
            "value": 6423.548894132968,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSet",
            "value": 92610.94143318952,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatch",
            "value": 8361.059322365621,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatchFlush",
            "value": 8371.79583108381,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.syncSet",
            "value": 90582.1907814508,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashDirect",
            "value": 40.378597522316504,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashHeap",
            "value": 37.16826452749936,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedDirect",
            "value": 31.149816938993347,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedHeap",
            "value": 31.562638993390248,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeAllSlots",
            "value": 59857.2410984827,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeLowerSlots",
            "value": 35728.977942984086,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusAbsent",
            "value": 0.9995094639990997,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusPresent",
            "value": 0.98828598987113,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyEstimatedSize",
            "value": 203.47899979358115,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyExactSize",
            "value": 70.11963266957837,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueEstimatedSize",
            "value": 201.64808538875837,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueExactSize",
            "value": 73.0555688140987,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.decodeUtf8Unpooled",
            "value": 4.39237556469673,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeAsciiToBuf",
            "value": 14.85005648880625,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeIsoToBuf",
            "value": 30.256886513799294,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8PlainStringToBuf",
            "value": 18.020910986882612,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8ToBuf",
            "value": 29.723033603201294,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8Unpooled",
            "value": 85.82622035728929,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.decodeUnpooled",
            "value": 4.4154293778047045,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.encodeUnpooled",
            "value": 85.89441318347028,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createBatchCommands",
            "value": 839457.8223612426,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createRegularCommands",
            "value": 885784.2987423846,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeAsyncCommand",
            "value": 27.834523903282054,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeCommandInterfaceCommand",
            "value": 169.68371027792568,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.asyncSet",
            "value": 6441.969738179989,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.batchSet",
            "value": 5566.897686543712,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.intercept.InvocationProxyFactoryBenchmark.run",
            "value": 58.417628760691045,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100Elements",
            "value": 385.881622702734,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100ElementsWithResizeElement",
            "value": 574.9177300917157,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16Elements",
            "value": 48.64370771739655,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16ElementsWithResizeElement",
            "value": 122.92405568580084,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureSingleElement",
            "value": 7.218927149423065,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureZeroElement",
            "value": 0.6314614488021686,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createAsyncCommandUsingByteArrayCodec",
            "value": 28.02078539706971,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingByteArrayCodec",
            "value": 23.172116029107393,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingStringCodec",
            "value": 13.989077100234166,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingByteArrayCodec",
            "value": 14.8404543894223,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingNewStringCodec",
            "value": 67.81953375807562,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingOldStringCodec",
            "value": 69.92813786192805,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndRead",
            "value": 158.9508188036388,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1",
            "value": 139.48455042816605,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch10",
            "value": 1380.4342238747925,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch100",
            "value": 11655.031107205263,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1000",
            "value": 127856.08710863985,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisEndpointBenchmark.measureUserWrite",
            "value": 38.11815075800394,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisStateMachineBenchmark.measureDecode",
            "value": 148.96590234149016,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.singleConnection",
            "value": 376.6133595009051,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.twentyConnections",
            "value": 7526.9569956911455,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.singleConnection",
            "value": 697.3736722442236,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.twentyConnections",
            "value": 13536.216522502127,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          }
        ]
      }
    ]
  }
}