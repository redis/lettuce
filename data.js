window.BENCHMARK_DATA = {
  "lastUpdate": 1733536805739,
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
      },
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
        "date": 1732154373058,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSet",
            "value": 68697.55310620884,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatch",
            "value": 5388.635998485495,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatchFlush",
            "value": 4877.429294975858,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSet",
            "value": 69442.42062231412,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatch",
            "value": 6167.179216651601,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatchFlush",
            "value": 6170.479879624613,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncList",
            "value": 156158.0543845002,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncSet",
            "value": 68821.04524085548,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommands",
            "value": 89.39049829441325,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommandsAsBatch",
            "value": 374.64970800488265,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writeKeyedCommand",
            "value": 30.915559104537277,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writePlainCommand",
            "value": 7.71865220790165,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSet",
            "value": 85736.85837919121,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatch",
            "value": 7293.469902028026,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatchFlush",
            "value": 6219.877543548662,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSet",
            "value": 88000.13081829883,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatch",
            "value": 8166.875658255973,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatchFlush",
            "value": 8111.403990617114,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.syncSet",
            "value": 87254.76366372511,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashDirect",
            "value": 40.511812125530945,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashHeap",
            "value": 37.092581749619846,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedDirect",
            "value": 31.311240246024845,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedHeap",
            "value": 31.532561497569805,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeAllSlots",
            "value": 65257.21183849538,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeLowerSlots",
            "value": 32744.85773284877,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusAbsent",
            "value": 1.001764123152583,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusPresent",
            "value": 0.9857791981356554,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyEstimatedSize",
            "value": 203.11244624243832,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyExactSize",
            "value": 70.18094633503117,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueEstimatedSize",
            "value": 220.74427009699625,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueExactSize",
            "value": 73.06042657030427,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.decodeUtf8Unpooled",
            "value": 4.390985983686839,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeAsciiToBuf",
            "value": 14.835259333891836,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeIsoToBuf",
            "value": 28.668077409168554,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8PlainStringToBuf",
            "value": 17.96960093222202,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8ToBuf",
            "value": 29.629558138802103,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8Unpooled",
            "value": 86.6449442757354,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.decodeUnpooled",
            "value": 4.398832685969684,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.encodeUnpooled",
            "value": 85.56707119700755,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createBatchCommands",
            "value": 840085.2029024682,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createRegularCommands",
            "value": 830772.6823063855,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeAsyncCommand",
            "value": 27.169652063389503,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeCommandInterfaceCommand",
            "value": 161.58931844794142,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.asyncSet",
            "value": 6065.282325002846,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.batchSet",
            "value": 5395.685582327535,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.intercept.InvocationProxyFactoryBenchmark.run",
            "value": 55.36219486401577,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100Elements",
            "value": 395.68757452528325,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100ElementsWithResizeElement",
            "value": 575.5068770812949,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16Elements",
            "value": 48.58341482908128,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16ElementsWithResizeElement",
            "value": 122.69862663262236,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureSingleElement",
            "value": 7.184449695274213,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureZeroElement",
            "value": 0.6299777936373052,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createAsyncCommandUsingByteArrayCodec",
            "value": 27.632674248803255,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingByteArrayCodec",
            "value": 23.098745857350437,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingStringCodec",
            "value": 13.161261358653181,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingByteArrayCodec",
            "value": 14.072515082018631,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingNewStringCodec",
            "value": 67.58512217566617,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingOldStringCodec",
            "value": 67.28219261249309,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndRead",
            "value": 149.476204806064,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1",
            "value": 147.29265416100566,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch10",
            "value": 1317.5113765070225,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch100",
            "value": 11497.769849194843,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1000",
            "value": 116115.11662352776,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisEndpointBenchmark.measureUserWrite",
            "value": 37.25999300702779,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisStateMachineBenchmark.measureDecode",
            "value": 148.979596036613,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.singleConnection",
            "value": 358.84749794259994,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.twentyConnections",
            "value": 7538.128878518427,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.singleConnection",
            "value": 695.3609965740292,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.twentyConnections",
            "value": 13279.651862470597,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          }
        ]
      },
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
        "date": 1732240773117,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSet",
            "value": 71304.86825814398,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatch",
            "value": 5392.889150536067,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatchFlush",
            "value": 4847.580426452156,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSet",
            "value": 70211.39343482009,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatch",
            "value": 6335.706282101489,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatchFlush",
            "value": 6287.087063717923,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncList",
            "value": 161572.72436387118,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncSet",
            "value": 70455.26208749512,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommands",
            "value": 89.4045023661491,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommandsAsBatch",
            "value": 370.70794141896215,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writeKeyedCommand",
            "value": 30.524252904450112,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writePlainCommand",
            "value": 7.727246374899876,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSet",
            "value": 87783.21651603366,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatch",
            "value": 7378.527148838881,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatchFlush",
            "value": 6347.5151848298065,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSet",
            "value": 89684.13689610761,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatch",
            "value": 8212.917460756094,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatchFlush",
            "value": 8265.299692385368,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.syncSet",
            "value": 88336.63960666578,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashDirect",
            "value": 40.43511566179127,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashHeap",
            "value": 39.243629189500325,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedDirect",
            "value": 31.250675087217566,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedHeap",
            "value": 31.558653938208426,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeAllSlots",
            "value": 59947.40825980694,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeLowerSlots",
            "value": 32793.46746667936,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusAbsent",
            "value": 1.0006077519299503,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusPresent",
            "value": 0.9894419550895381,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyEstimatedSize",
            "value": 198.8800821394437,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyExactSize",
            "value": 70.28421068520537,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueEstimatedSize",
            "value": 209.2574069406516,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueExactSize",
            "value": 67.81704358034126,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.decodeUtf8Unpooled",
            "value": 4.47609304275787,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeAsciiToBuf",
            "value": 14.968238589407417,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeIsoToBuf",
            "value": 27.142436430555147,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8PlainStringToBuf",
            "value": 18.143646039825605,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8ToBuf",
            "value": 29.680819962835056,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8Unpooled",
            "value": 85.69897546465734,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.decodeUnpooled",
            "value": 4.3987096281974445,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.encodeUnpooled",
            "value": 86.06957719573815,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createBatchCommands",
            "value": 867865.7918085868,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createRegularCommands",
            "value": 842320.8748799103,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeAsyncCommand",
            "value": 27.075435447977746,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeCommandInterfaceCommand",
            "value": 168.82278867615267,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.asyncSet",
            "value": 6185.761570834085,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.batchSet",
            "value": 5499.27654204777,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.intercept.InvocationProxyFactoryBenchmark.run",
            "value": 54.174646968219655,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100Elements",
            "value": 388.4792715235684,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100ElementsWithResizeElement",
            "value": 574.7119238335979,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16Elements",
            "value": 48.66795297092997,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16ElementsWithResizeElement",
            "value": 125.73813520348627,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureSingleElement",
            "value": 7.191468568503128,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureZeroElement",
            "value": 0.6294085598060343,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createAsyncCommandUsingByteArrayCodec",
            "value": 27.66213957268871,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingByteArrayCodec",
            "value": 22.98465792091771,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingStringCodec",
            "value": 13.160381129484339,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingByteArrayCodec",
            "value": 14.108820527027271,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingNewStringCodec",
            "value": 67.11304309744496,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingOldStringCodec",
            "value": 68.47504498798739,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndRead",
            "value": 157.0025597965084,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1",
            "value": 142.76855870387357,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch10",
            "value": 1390.9905741767448,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch100",
            "value": 11892.333396988457,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1000",
            "value": 127562.49478641052,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisEndpointBenchmark.measureUserWrite",
            "value": 36.551659250284914,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisStateMachineBenchmark.measureDecode",
            "value": 145.96646605313555,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.singleConnection",
            "value": 351.5364407849369,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.twentyConnections",
            "value": 7259.135299318048,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.singleConnection",
            "value": 675.404858643925,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.twentyConnections",
            "value": 13293.274668824713,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          }
        ]
      },
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
        "date": 1732327188594,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSet",
            "value": 68626.48701564767,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatch",
            "value": 5413.771041436953,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatchFlush",
            "value": 4853.931826401208,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSet",
            "value": 68889.78229246996,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatch",
            "value": 6015.117840730616,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatchFlush",
            "value": 6175.316921730339,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncList",
            "value": 157159.8492591998,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncSet",
            "value": 68845.81804604616,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommands",
            "value": 89.3509562386085,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommandsAsBatch",
            "value": 370.4150421619648,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writeKeyedCommand",
            "value": 31.693270377481063,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writePlainCommand",
            "value": 7.733796437764886,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSet",
            "value": 86589.5807481293,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatch",
            "value": 7227.174667931715,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatchFlush",
            "value": 6184.888819854157,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSet",
            "value": 86771.3285958536,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatch",
            "value": 7932.144168045166,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatchFlush",
            "value": 8001.009844889119,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.syncSet",
            "value": 85953.74472672163,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashDirect",
            "value": 40.40756020206008,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashHeap",
            "value": 37.13787635491514,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedDirect",
            "value": 28.212609872845043,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedHeap",
            "value": 31.243278002496584,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeAllSlots",
            "value": 65675.40558766728,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeLowerSlots",
            "value": 32851.276645607744,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusAbsent",
            "value": 1.0013713410038139,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusPresent",
            "value": 0.9868581333001083,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyEstimatedSize",
            "value": 203.6736053090464,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyExactSize",
            "value": 66.67532000299215,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueEstimatedSize",
            "value": 198.92118569837345,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueExactSize",
            "value": 67.85134176844954,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.decodeUtf8Unpooled",
            "value": 4.416868367121936,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeAsciiToBuf",
            "value": 14.861080255849382,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeIsoToBuf",
            "value": 27.77539335996401,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8PlainStringToBuf",
            "value": 17.95010599867602,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8ToBuf",
            "value": 29.541021921956307,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8Unpooled",
            "value": 85.57122088389413,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.decodeUnpooled",
            "value": 4.395877234802677,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.encodeUnpooled",
            "value": 85.62205323262648,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createBatchCommands",
            "value": 865205.2452913157,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createRegularCommands",
            "value": 849631.9443014419,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeAsyncCommand",
            "value": 27.22588022263592,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeCommandInterfaceCommand",
            "value": 168.53052225326442,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.asyncSet",
            "value": 6064.580458925173,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.batchSet",
            "value": 5395.192501334369,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.intercept.InvocationProxyFactoryBenchmark.run",
            "value": 58.07432743111501,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100Elements",
            "value": 383.62049881679496,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100ElementsWithResizeElement",
            "value": 574.1376861397484,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16Elements",
            "value": 48.638141745936046,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16ElementsWithResizeElement",
            "value": 122.66248658614617,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureSingleElement",
            "value": 6.8582810035745805,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureZeroElement",
            "value": 0.6336490760400416,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createAsyncCommandUsingByteArrayCodec",
            "value": 27.744128251004526,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingByteArrayCodec",
            "value": 23.004920823770863,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingStringCodec",
            "value": 13.13083481929485,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingByteArrayCodec",
            "value": 14.105010654270796,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingNewStringCodec",
            "value": 69.25292553751292,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingOldStringCodec",
            "value": 67.0758814212896,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndRead",
            "value": 139.2768902584331,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1",
            "value": 132.24463938509905,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch10",
            "value": 1389.9322876501271,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch100",
            "value": 12156.175552151351,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1000",
            "value": 128580.57769026703,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisEndpointBenchmark.measureUserWrite",
            "value": 37.67929118541735,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisStateMachineBenchmark.measureDecode",
            "value": 149.20450226829888,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.singleConnection",
            "value": 354.9001788836636,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.twentyConnections",
            "value": 7402.256791249303,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.singleConnection",
            "value": 653.0215807091396,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.twentyConnections",
            "value": 13031.719592535068,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          }
        ]
      },
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
        "date": 1732499993859,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSet",
            "value": 70234.90028989183,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatch",
            "value": 5441.936195978115,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatchFlush",
            "value": 4918.991701079244,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSet",
            "value": 69937.91828778615,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatch",
            "value": 6212.055460992889,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatchFlush",
            "value": 6124.717387664993,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncList",
            "value": 157731.3993206884,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncSet",
            "value": 69851.92663491241,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommands",
            "value": 89.88848667237315,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommandsAsBatch",
            "value": 406.8624830325081,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writeKeyedCommand",
            "value": 31.075891236358405,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writePlainCommand",
            "value": 7.7280116847241205,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSet",
            "value": 86735.63178199585,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatch",
            "value": 7439.928871545247,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatchFlush",
            "value": 6356.405499177579,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSet",
            "value": 89538.41427264453,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatch",
            "value": 7967.932338470188,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatchFlush",
            "value": 8172.586463595893,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.syncSet",
            "value": 87900.93771317256,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashDirect",
            "value": 40.38102556204928,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashHeap",
            "value": 37.089072436597135,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedDirect",
            "value": 31.20137115555459,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedHeap",
            "value": 31.197891091041395,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeAllSlots",
            "value": 65924.41056939984,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeLowerSlots",
            "value": 13301.978509966279,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusAbsent",
            "value": 1.0023755985438785,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusPresent",
            "value": 0.9893377931157181,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyEstimatedSize",
            "value": 227.49878919216673,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyExactSize",
            "value": 70.68562250734576,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueEstimatedSize",
            "value": 217.37783567497587,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueExactSize",
            "value": 73.00536992552034,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.decodeUtf8Unpooled",
            "value": 4.394457261825409,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeAsciiToBuf",
            "value": 14.942419964196622,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeIsoToBuf",
            "value": 27.80227369941668,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8PlainStringToBuf",
            "value": 17.986491146080475,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8ToBuf",
            "value": 32.44375143465563,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8Unpooled",
            "value": 85.92651191653013,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.decodeUnpooled",
            "value": 4.397748937789393,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.encodeUnpooled",
            "value": 85.73668988209161,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createBatchCommands",
            "value": 836289.7848901388,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createRegularCommands",
            "value": 831869.509144298,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeAsyncCommand",
            "value": 27.219960792719284,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeCommandInterfaceCommand",
            "value": 166.9322021592579,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.asyncSet",
            "value": 6133.280012324104,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.batchSet",
            "value": 5391.906612071524,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.intercept.InvocationProxyFactoryBenchmark.run",
            "value": 55.35488145906413,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100Elements",
            "value": 384.8590343160557,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100ElementsWithResizeElement",
            "value": 575.0938040786682,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16Elements",
            "value": 48.59047088147097,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16ElementsWithResizeElement",
            "value": 124.40545042618972,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureSingleElement",
            "value": 6.909169489199428,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureZeroElement",
            "value": 0.6289817109350511,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createAsyncCommandUsingByteArrayCodec",
            "value": 27.7448919860553,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingByteArrayCodec",
            "value": 23.070865481481793,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingStringCodec",
            "value": 13.17082768313187,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingByteArrayCodec",
            "value": 14.126808614738763,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingNewStringCodec",
            "value": 69.23925707773348,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingOldStringCodec",
            "value": 67.15026328329368,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndRead",
            "value": 143.46879916993834,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1",
            "value": 152.43349565267098,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch10",
            "value": 1335.6252925452766,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch100",
            "value": 12628.114723111183,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1000",
            "value": 126132.63488972785,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisEndpointBenchmark.measureUserWrite",
            "value": 37.39789718149323,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisStateMachineBenchmark.measureDecode",
            "value": 150.68148844214107,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.singleConnection",
            "value": 366.9648500654388,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.twentyConnections",
            "value": 7285.234503674075,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.singleConnection",
            "value": 648.4373707546945,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.twentyConnections",
            "value": 13247.064849238042,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          }
        ]
      },
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
        "date": 1732672803527,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSet",
            "value": 70577.35956667324,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatch",
            "value": 5445.958562426635,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatchFlush",
            "value": 4962.019027685406,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSet",
            "value": 70919.45748908425,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatch",
            "value": 6289.4723593136205,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatchFlush",
            "value": 6268.364574514965,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncList",
            "value": 157639.66913785686,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncSet",
            "value": 69091.73056289785,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommands",
            "value": 89.41654318043871,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommandsAsBatch",
            "value": 370.71686159992544,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writeKeyedCommand",
            "value": 30.987659675514628,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writePlainCommand",
            "value": 7.744605951770339,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSet",
            "value": 88080.7219264808,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatch",
            "value": 7434.3469760875505,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatchFlush",
            "value": 6245.912767425121,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSet",
            "value": 88593.11875282155,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatch",
            "value": 8192.610387432434,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatchFlush",
            "value": 8275.169219422567,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.syncSet",
            "value": 87329.03098336092,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashDirect",
            "value": 40.5290702923081,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashHeap",
            "value": 37.390352060787635,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedDirect",
            "value": 31.314020459331715,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedHeap",
            "value": 31.238507811946466,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeAllSlots",
            "value": 66090.8775771226,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeLowerSlots",
            "value": 32885.96352353807,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusAbsent",
            "value": 1.0039988031385525,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusPresent",
            "value": 0.9907371176898222,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyEstimatedSize",
            "value": 187.02744718158485,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyExactSize",
            "value": 70.17593512944188,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueEstimatedSize",
            "value": 187.69875104204596,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueExactSize",
            "value": 73.4993884008038,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.decodeUtf8Unpooled",
            "value": 4.39477211922169,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeAsciiToBuf",
            "value": 15.002616316521813,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeIsoToBuf",
            "value": 29.05203450459959,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8PlainStringToBuf",
            "value": 17.96905426950288,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8ToBuf",
            "value": 29.78676295405993,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8Unpooled",
            "value": 85.75320442939798,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.decodeUnpooled",
            "value": 4.399930188369733,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.encodeUnpooled",
            "value": 86.52359460806372,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createBatchCommands",
            "value": 856508.37984391,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createRegularCommands",
            "value": 856562.2369288851,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeAsyncCommand",
            "value": 27.104355057319687,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeCommandInterfaceCommand",
            "value": 183.46147956096598,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.asyncSet",
            "value": 6136.605711996612,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.batchSet",
            "value": 5443.453478646339,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.intercept.InvocationProxyFactoryBenchmark.run",
            "value": 55.84063575828353,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100Elements",
            "value": 386.2905716512197,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100ElementsWithResizeElement",
            "value": 576.2348063437737,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16Elements",
            "value": 48.51584947836387,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16ElementsWithResizeElement",
            "value": 122.70834596940472,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureSingleElement",
            "value": 7.221438542341977,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureZeroElement",
            "value": 0.6320426116237667,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createAsyncCommandUsingByteArrayCodec",
            "value": 27.759486091843655,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingByteArrayCodec",
            "value": 23.09545672538349,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingStringCodec",
            "value": 13.268958113609765,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingByteArrayCodec",
            "value": 14.186071041329921,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingNewStringCodec",
            "value": 70.10700963515914,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingOldStringCodec",
            "value": 67.11252498551882,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndRead",
            "value": 165.25762902812497,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1",
            "value": 143.93070370316912,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch10",
            "value": 1306.8459496313396,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch100",
            "value": 12946.809054784178,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1000",
            "value": 122654.37978609004,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisEndpointBenchmark.measureUserWrite",
            "value": 36.79599143410441,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisStateMachineBenchmark.measureDecode",
            "value": 148.97550970530784,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.singleConnection",
            "value": 365.7144885017363,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.twentyConnections",
            "value": 7540.535725268887,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.singleConnection",
            "value": 670.8358696737548,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.twentyConnections",
            "value": 13689.62965998213,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          }
        ]
      },
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
        "date": 1732759200040,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSet",
            "value": 71373.70356970168,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatch",
            "value": 5603.819230581666,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatchFlush",
            "value": 4964.755712015161,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSet",
            "value": 70894.81978311336,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatch",
            "value": 6343.499256316284,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatchFlush",
            "value": 6365.364399963801,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncList",
            "value": 161001.5527958393,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncSet",
            "value": 71924.33569164053,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommands",
            "value": 89.39240965802831,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommandsAsBatch",
            "value": 372.4020675321035,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writeKeyedCommand",
            "value": 31.089935118637563,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writePlainCommand",
            "value": 7.742614483360667,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSet",
            "value": 88078.88660165729,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatch",
            "value": 7465.045909404046,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatchFlush",
            "value": 6381.841731359143,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSet",
            "value": 89682.98220051354,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatch",
            "value": 8147.4829781216395,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatchFlush",
            "value": 8376.126768935872,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.syncSet",
            "value": 88769.39103847253,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashDirect",
            "value": 40.40278719658421,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashHeap",
            "value": 37.36043123198928,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedDirect",
            "value": 28.607178878500054,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedHeap",
            "value": 31.56217986260445,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeAllSlots",
            "value": 26877.179155027,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeLowerSlots",
            "value": 35831.98580207582,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusAbsent",
            "value": 0.9995904756638904,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusPresent",
            "value": 0.9915183472583802,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyEstimatedSize",
            "value": 203.51853891344402,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyExactSize",
            "value": 71.06232303293208,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueEstimatedSize",
            "value": 216.38673354871008,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueExactSize",
            "value": 73.08412757892319,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.decodeUtf8Unpooled",
            "value": 4.403344745581078,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeAsciiToBuf",
            "value": 14.934425395112806,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeIsoToBuf",
            "value": 27.694713839362112,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8PlainStringToBuf",
            "value": 18.053013434039777,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8ToBuf",
            "value": 29.72264840184115,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8Unpooled",
            "value": 86.00334545253668,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.decodeUnpooled",
            "value": 4.403609106043491,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.encodeUnpooled",
            "value": 86.96379004632722,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createBatchCommands",
            "value": 855800.6636548709,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createRegularCommands",
            "value": 871578.6988497984,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeAsyncCommand",
            "value": 27.350738063655335,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeCommandInterfaceCommand",
            "value": 167.1413966392128,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.asyncSet",
            "value": 6226.35168287713,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.batchSet",
            "value": 5565.296934304686,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.intercept.InvocationProxyFactoryBenchmark.run",
            "value": 55.24421742434207,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100Elements",
            "value": 387.71076997989445,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100ElementsWithResizeElement",
            "value": 573.7269525139559,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16Elements",
            "value": 48.37121526133098,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16ElementsWithResizeElement",
            "value": 122.88148758185498,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureSingleElement",
            "value": 6.894482631848851,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureZeroElement",
            "value": 0.6327088414666439,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createAsyncCommandUsingByteArrayCodec",
            "value": 27.914234495115313,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingByteArrayCodec",
            "value": 23.210507386417838,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingStringCodec",
            "value": 14.08934985839619,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingByteArrayCodec",
            "value": 15.137697888633781,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingNewStringCodec",
            "value": 69.45131109344396,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingOldStringCodec",
            "value": 69.36244316156731,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndRead",
            "value": 148.5252173996452,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1",
            "value": 139.20498533732723,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch10",
            "value": 1480.752245582807,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch100",
            "value": 12174.025506666701,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1000",
            "value": 131549.54954860802,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisEndpointBenchmark.measureUserWrite",
            "value": 49.73049190617147,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisStateMachineBenchmark.measureDecode",
            "value": 148.5869176522792,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.singleConnection",
            "value": 360.28819172744534,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.twentyConnections",
            "value": 7296.100889509633,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.singleConnection",
            "value": 651.2895606235769,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.twentyConnections",
            "value": 13761.398782897955,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          }
        ]
      },
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
        "date": 1732845579365,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSet",
            "value": 70237.36703374016,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatch",
            "value": 5459.763349782164,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatchFlush",
            "value": 4990.220320700831,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSet",
            "value": 71607.9098759157,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatch",
            "value": 6271.795255474709,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatchFlush",
            "value": 6278.346153897779,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncList",
            "value": 163186.23192790712,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncSet",
            "value": 70054.5616126376,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommands",
            "value": 92.02553882992659,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommandsAsBatch",
            "value": 370.62052173530384,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writeKeyedCommand",
            "value": 31.7061298502461,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writePlainCommand",
            "value": 7.72932571424774,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSet",
            "value": 88944.28738542311,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatch",
            "value": 7403.64745185818,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatchFlush",
            "value": 6378.357219715914,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSet",
            "value": 90811.25733231778,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatch",
            "value": 8082.133147295343,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatchFlush",
            "value": 8027.145266263772,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.syncSet",
            "value": 88890.56389636575,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashDirect",
            "value": 40.32252205215729,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashHeap",
            "value": 37.069775827669304,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedDirect",
            "value": 28.397603195791827,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedHeap",
            "value": 31.60956160872209,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeAllSlots",
            "value": 26985.67764691145,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeLowerSlots",
            "value": 35808.29187980936,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusAbsent",
            "value": 1.0003733635331835,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusPresent",
            "value": 0.9830966045870321,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyEstimatedSize",
            "value": 211.45874372281872,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyExactSize",
            "value": 73.09795412793895,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueEstimatedSize",
            "value": 211.27006681504503,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueExactSize",
            "value": 72.95535863906835,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.decodeUtf8Unpooled",
            "value": 4.388533520336246,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeAsciiToBuf",
            "value": 14.83871618065252,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeIsoToBuf",
            "value": 27.714010961116987,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8PlainStringToBuf",
            "value": 17.939171908756464,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8ToBuf",
            "value": 29.72421274500029,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8Unpooled",
            "value": 86.18920748797025,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.decodeUnpooled",
            "value": 4.389689093696388,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.encodeUnpooled",
            "value": 86.51730038667493,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createBatchCommands",
            "value": 846655.1832708521,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createRegularCommands",
            "value": 843773.5313372922,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeAsyncCommand",
            "value": 27.294350501110607,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeCommandInterfaceCommand",
            "value": 165.5969492396386,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.asyncSet",
            "value": 6271.434313474679,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.batchSet",
            "value": 5480.795257325569,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.intercept.InvocationProxyFactoryBenchmark.run",
            "value": 55.99861951787589,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100Elements",
            "value": 385.4511089751545,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100ElementsWithResizeElement",
            "value": 573.5423562958275,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16Elements",
            "value": 48.26174650178389,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16ElementsWithResizeElement",
            "value": 123.17471689888198,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureSingleElement",
            "value": 6.865555092436895,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureZeroElement",
            "value": 0.6325567627616525,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createAsyncCommandUsingByteArrayCodec",
            "value": 27.75973958548646,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingByteArrayCodec",
            "value": 23.04114336068961,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingStringCodec",
            "value": 13.180062290996997,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingByteArrayCodec",
            "value": 14.186002962799966,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingNewStringCodec",
            "value": 67.46649223784968,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingOldStringCodec",
            "value": 69.42762483404053,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndRead",
            "value": 154.352691755168,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1",
            "value": 154.83060058044515,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch10",
            "value": 1266.912293114949,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch100",
            "value": 12515.023179856362,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1000",
            "value": 115089.04888214893,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisEndpointBenchmark.measureUserWrite",
            "value": 36.69808556197686,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisStateMachineBenchmark.measureDecode",
            "value": 151.80597345236933,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.singleConnection",
            "value": 359.51599805638665,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.twentyConnections",
            "value": 8020.606132903117,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.singleConnection",
            "value": 671.4159210992681,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.twentyConnections",
            "value": 13634.807512393594,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          }
        ]
      },
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
        "date": 1732931960273,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSet",
            "value": 67918.98755262533,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatch",
            "value": 5333.118701665857,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatchFlush",
            "value": 4927.259597958191,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSet",
            "value": 70129.6764182231,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatch",
            "value": 6089.186529779946,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatchFlush",
            "value": 6054.370279894001,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncList",
            "value": 156450.9243468245,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncSet",
            "value": 68599.65872098246,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommands",
            "value": 88.17872145561915,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommandsAsBatch",
            "value": 368.8694050654564,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writeKeyedCommand",
            "value": 30.51453479234105,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writePlainCommand",
            "value": 7.683622105252589,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSet",
            "value": 86027.14975693777,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatch",
            "value": 7154.179025838045,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatchFlush",
            "value": 6274.742386316412,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSet",
            "value": 89320.21978524662,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatch",
            "value": 7997.975466107635,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatchFlush",
            "value": 8016.498085147966,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.syncSet",
            "value": 87124.10159395469,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashDirect",
            "value": 39.93595663950086,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashHeap",
            "value": 36.91562166641417,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedDirect",
            "value": 28.021967791183,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedHeap",
            "value": 31.324887440088133,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeAllSlots",
            "value": 70486.2416205303,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeLowerSlots",
            "value": 35259.633038669446,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusAbsent",
            "value": 0.9867631876447319,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusPresent",
            "value": 0.9808914047715689,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyEstimatedSize",
            "value": 215.32304597402927,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyExactSize",
            "value": 65.51422587202006,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueEstimatedSize",
            "value": 227.0369987790901,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueExactSize",
            "value": 72.19692476842238,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.decodeUtf8Unpooled",
            "value": 4.350041114779643,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeAsciiToBuf",
            "value": 14.777093687247325,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeIsoToBuf",
            "value": 27.706889824673066,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8PlainStringToBuf",
            "value": 17.71094977506469,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8ToBuf",
            "value": 29.774382142620958,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8Unpooled",
            "value": 85.49139690966612,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.decodeUnpooled",
            "value": 4.3624348094605745,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.encodeUnpooled",
            "value": 84.69133661416342,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createBatchCommands",
            "value": 855608.884669473,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createRegularCommands",
            "value": 812246.3955782634,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeAsyncCommand",
            "value": 26.99787275370877,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeCommandInterfaceCommand",
            "value": 157.64705756996335,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.asyncSet",
            "value": 5997.672559976445,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.batchSet",
            "value": 5269.463585419162,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.intercept.InvocationProxyFactoryBenchmark.run",
            "value": 54.18736517015676,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100Elements",
            "value": 382.69471532720445,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100ElementsWithResizeElement",
            "value": 566.7861757187126,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16Elements",
            "value": 48.08607017475614,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16ElementsWithResizeElement",
            "value": 122.04717878785235,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureSingleElement",
            "value": 6.744513457994866,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureZeroElement",
            "value": 0.6214202596619545,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createAsyncCommandUsingByteArrayCodec",
            "value": 27.838727200843216,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingByteArrayCodec",
            "value": 22.659962977611322,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingStringCodec",
            "value": 12.99119099155104,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingByteArrayCodec",
            "value": 13.918103286319283,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingNewStringCodec",
            "value": 66.1337964243123,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingOldStringCodec",
            "value": 68.34553919606353,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndRead",
            "value": 153.0824036001188,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1",
            "value": 141.3590539987981,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch10",
            "value": 1359.4920053825413,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch100",
            "value": 12317.604539965536,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1000",
            "value": 129856.42802422389,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisEndpointBenchmark.measureUserWrite",
            "value": 37.55984266305002,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisStateMachineBenchmark.measureDecode",
            "value": 148.0472695101762,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.singleConnection",
            "value": 350.5149351261844,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.twentyConnections",
            "value": 7172.644494251089,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.singleConnection",
            "value": 671.119322833395,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.twentyConnections",
            "value": 13718.829281157878,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "Mark Paluch",
            "username": "mp911de",
            "email": "mpaluch@paluch.biz"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "26fbdbb5863d8f5e9652b29089495656eb3e3d85",
          "message": "Propagate handshake failures to Handshake future (#3058)\n\nWe now complete the handshake future exceptionally if handshake settings do not match our expectations.\r\nThis can happen if e.g. the connection id is being sent as String instead of an integer.",
          "timestamp": "2024-11-30T12:33:37Z",
          "url": "https://github.com/redis/lettuce/commit/26fbdbb5863d8f5e9652b29089495656eb3e3d85"
        },
        "date": 1733018440591,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSet",
            "value": 70215.85535643877,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatch",
            "value": 5430.9491767096615,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatchFlush",
            "value": 4838.726903243263,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSet",
            "value": 69982.22812537944,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatch",
            "value": 6200.804302733468,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatchFlush",
            "value": 6219.151497947903,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncList",
            "value": 161176.11584821943,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncSet",
            "value": 69860.07361152684,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommands",
            "value": 90.0316613947779,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommandsAsBatch",
            "value": 370.21115824746994,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writeKeyedCommand",
            "value": 30.803075719316276,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writePlainCommand",
            "value": 7.726559434767337,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSet",
            "value": 88067.67426356599,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatch",
            "value": 7371.691914214216,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatchFlush",
            "value": 6368.610361511717,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSet",
            "value": 89914.69595211463,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatch",
            "value": 8205.20264226987,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatchFlush",
            "value": 8252.508859569265,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.syncSet",
            "value": 88785.4769829454,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashDirect",
            "value": 40.359039430218346,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashHeap",
            "value": 37.01608225259596,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedDirect",
            "value": 28.490927919367284,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedHeap",
            "value": 31.498543473928475,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeAllSlots",
            "value": 65153.05231682376,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeLowerSlots",
            "value": 35776.43564610487,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusAbsent",
            "value": 0.9997879170743655,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusPresent",
            "value": 0.986461841006323,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyEstimatedSize",
            "value": 200.76150229735225,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyExactSize",
            "value": 70.32733788766231,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueEstimatedSize",
            "value": 211.84910605793158,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueExactSize",
            "value": 73.16352582275512,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.decodeUtf8Unpooled",
            "value": 4.400450439636463,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeAsciiToBuf",
            "value": 14.947299390348258,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeIsoToBuf",
            "value": 27.20037992036884,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8PlainStringToBuf",
            "value": 18.137943484677674,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8ToBuf",
            "value": 29.69691213040648,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8Unpooled",
            "value": 85.84185587519104,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.decodeUnpooled",
            "value": 4.4016792422121505,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.encodeUnpooled",
            "value": 85.72912988211462,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createBatchCommands",
            "value": 851888.4954698374,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createRegularCommands",
            "value": 853731.2803942641,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeAsyncCommand",
            "value": 27.279806022516244,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeCommandInterfaceCommand",
            "value": 165.04797473016754,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.asyncSet",
            "value": 6124.73157734602,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.batchSet",
            "value": 5473.2378829556055,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.intercept.InvocationProxyFactoryBenchmark.run",
            "value": 53.88311743991126,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100Elements",
            "value": 385.4512189495346,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100ElementsWithResizeElement",
            "value": 571.6698813038204,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16Elements",
            "value": 48.33181475506622,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16ElementsWithResizeElement",
            "value": 122.92572287318217,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureSingleElement",
            "value": 6.897311633435278,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureZeroElement",
            "value": 0.6310035850933214,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createAsyncCommandUsingByteArrayCodec",
            "value": 27.650270746386788,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingByteArrayCodec",
            "value": 23.027567599884257,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingStringCodec",
            "value": 13.138311769823094,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingByteArrayCodec",
            "value": 14.16182154617475,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingNewStringCodec",
            "value": 67.3449136053727,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingOldStringCodec",
            "value": 70.09803138264392,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndRead",
            "value": 156.37689662334816,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1",
            "value": 143.60717766527037,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch10",
            "value": 1298.1465897365333,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch100",
            "value": 12716.064050047473,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1000",
            "value": 115833.37903212846,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisEndpointBenchmark.measureUserWrite",
            "value": 37.40367090684303,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisStateMachineBenchmark.measureDecode",
            "value": 147.34604285765536,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.singleConnection",
            "value": 384.87696906872856,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.twentyConnections",
            "value": 7678.602873016523,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.singleConnection",
            "value": 685.2157985426978,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.twentyConnections",
            "value": 14143.453599090066,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "dltmd202",
            "username": "Dltmd202",
            "email": "75921696+Dltmd202@users.noreply.github.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "5c211aeb808277c2ccedf17e8684f5140101b62f",
          "message": "Deprecate the STRALGO command and implement the LCS in its place (#3037)\n\n* Deprecate the STRALGO command and implement the LCS in its place\r\n\r\n* Polishing\r\n\r\n---------\r\n\r\nCo-authored-by: Tihomir Mateev <tihomir.mateev@gmail.com>",
          "timestamp": "2024-12-01T19:11:19Z",
          "url": "https://github.com/redis/lettuce/commit/5c211aeb808277c2ccedf17e8684f5140101b62f"
        },
        "date": 1733104821908,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSet",
            "value": 69930.21113422785,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatch",
            "value": 5512.640273139412,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatchFlush",
            "value": 4921.917010185533,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSet",
            "value": 71927.89220584198,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatch",
            "value": 6240.091137594951,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatchFlush",
            "value": 6330.638981847235,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncList",
            "value": 161251.53760767885,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncSet",
            "value": 71185.66059352775,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommands",
            "value": 89.73574650153886,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommandsAsBatch",
            "value": 370.22716375900296,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writeKeyedCommand",
            "value": 31.159318095662645,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writePlainCommand",
            "value": 7.721332955542915,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSet",
            "value": 88328.901635215,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatch",
            "value": 7512.009760935312,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatchFlush",
            "value": 6419.9200846994045,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSet",
            "value": 90981.18639658115,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatch",
            "value": 8314.785938916239,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatchFlush",
            "value": 8319.992818609138,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.syncSet",
            "value": 89894.22918435396,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashDirect",
            "value": 40.41399154066286,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashHeap",
            "value": 38.2549623513979,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedDirect",
            "value": 28.472698708323882,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedHeap",
            "value": 31.14612094451985,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeAllSlots",
            "value": 59806.86189673436,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeLowerSlots",
            "value": 35964.436398106416,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusAbsent",
            "value": 1.0014623575745047,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusPresent",
            "value": 0.9915950735071885,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyEstimatedSize",
            "value": 190.21502084775332,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyExactSize",
            "value": 70.25445346316974,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueEstimatedSize",
            "value": 199.94262304066575,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueExactSize",
            "value": 73.1081444529963,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.decodeUtf8Unpooled",
            "value": 4.409872904408829,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeAsciiToBuf",
            "value": 14.978909742829845,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeIsoToBuf",
            "value": 27.674791846859232,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8PlainStringToBuf",
            "value": 18.004650357259433,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8ToBuf",
            "value": 29.591462034569474,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8Unpooled",
            "value": 86.91573227873968,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.decodeUnpooled",
            "value": 4.396438190920816,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.encodeUnpooled",
            "value": 86.09759658328623,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createBatchCommands",
            "value": 824849.9507935026,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createRegularCommands",
            "value": 854181.1744457104,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeAsyncCommand",
            "value": 27.29961293821508,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeCommandInterfaceCommand",
            "value": 165.83343112870463,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.asyncSet",
            "value": 6211.799705332154,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.batchSet",
            "value": 5393.534855118827,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.intercept.InvocationProxyFactoryBenchmark.run",
            "value": 56.03934367357603,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100Elements",
            "value": 385.33276647058983,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100ElementsWithResizeElement",
            "value": 578.6238166943795,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16Elements",
            "value": 48.36177053584237,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16ElementsWithResizeElement",
            "value": 123.03650923284833,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureSingleElement",
            "value": 6.889523440187742,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureZeroElement",
            "value": 0.6312660081042056,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createAsyncCommandUsingByteArrayCodec",
            "value": 27.948890017434955,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingByteArrayCodec",
            "value": 23.053417779455806,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingStringCodec",
            "value": 13.348848757997468,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingByteArrayCodec",
            "value": 14.264373627966048,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingNewStringCodec",
            "value": 67.33758067063201,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingOldStringCodec",
            "value": 69.95919297029909,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndRead",
            "value": 158.4363429038156,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1",
            "value": 141.84094514615782,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch10",
            "value": 1420.0675457835584,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch100",
            "value": 11741.081840802579,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1000",
            "value": 127025.52092894292,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisEndpointBenchmark.measureUserWrite",
            "value": 37.90021277845159,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisStateMachineBenchmark.measureDecode",
            "value": 149.06397772982967,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.singleConnection",
            "value": 364.73546298727194,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.twentyConnections",
            "value": 7545.169077484454,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.singleConnection",
            "value": 681.8043076187354,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.twentyConnections",
            "value": 13767.195638264582,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          }
        ]
      },
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
          "id": "28a4154d6ad7f8309ca5affae9e69f5da4c38e16",
          "message": "Test failures not reported because step is skipped (#3067)",
          "timestamp": "2024-12-02T13:26:29Z",
          "url": "https://github.com/redis/lettuce/commit/28a4154d6ad7f8309ca5affae9e69f5da4c38e16"
        },
        "date": 1733191208219,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSet",
            "value": 65638.97781844118,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatch",
            "value": 5245.197120918067,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatchFlush",
            "value": 4635.638975330611,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSet",
            "value": 70337.04143417014,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatch",
            "value": 6180.999374052905,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatchFlush",
            "value": 6235.61442775055,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncList",
            "value": 157153.32285385331,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncSet",
            "value": 67225.45948410899,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommands",
            "value": 87.99443742810367,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommandsAsBatch",
            "value": 358.486011484351,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writeKeyedCommand",
            "value": 30.03594466092975,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writePlainCommand",
            "value": 7.50322210783065,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSet",
            "value": 85291.87389078623,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatch",
            "value": 7391.553339406889,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatchFlush",
            "value": 6244.198647814018,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSet",
            "value": 88188.51931911775,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatch",
            "value": 8233.394275370894,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatchFlush",
            "value": 8301.792303724342,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.syncSet",
            "value": 87702.59229412799,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashDirect",
            "value": 40.37595501408482,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashHeap",
            "value": 37.04109180005381,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedDirect",
            "value": 27.993803018592853,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedHeap",
            "value": 31.188502260404398,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeAllSlots",
            "value": 65225.11343895469,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeLowerSlots",
            "value": 32977.027189235494,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusAbsent",
            "value": 0.9989767177907737,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusPresent",
            "value": 0.9846492808134585,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyEstimatedSize",
            "value": 215.8877335844552,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyExactSize",
            "value": 70.31912493534799,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueEstimatedSize",
            "value": 214.94092742452617,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueExactSize",
            "value": 73.15138823482243,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.decodeUtf8Unpooled",
            "value": 4.414239995877244,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeAsciiToBuf",
            "value": 15.026760160766752,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeIsoToBuf",
            "value": 27.730100783592395,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8PlainStringToBuf",
            "value": 18.166124726935834,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8ToBuf",
            "value": 32.53779091834433,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8Unpooled",
            "value": 85.7260850197047,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.decodeUnpooled",
            "value": 4.427988814894021,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.encodeUnpooled",
            "value": 86.33300833692954,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createBatchCommands",
            "value": 849829.0822518732,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createRegularCommands",
            "value": 847556.4643121166,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeAsyncCommand",
            "value": 27.159317026373525,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeCommandInterfaceCommand",
            "value": 169.71577592479076,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.asyncSet",
            "value": 6132.373810811421,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.batchSet",
            "value": 5531.987863807873,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.intercept.InvocationProxyFactoryBenchmark.run",
            "value": 56.165407273834454,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100Elements",
            "value": 384.7468065315697,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100ElementsWithResizeElement",
            "value": 574.9042944881796,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16Elements",
            "value": 48.23554728847874,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16ElementsWithResizeElement",
            "value": 122.30953510909293,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureSingleElement",
            "value": 6.850392997310598,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureZeroElement",
            "value": 0.6320990126835082,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createAsyncCommandUsingByteArrayCodec",
            "value": 27.988194234901165,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingByteArrayCodec",
            "value": 23.21522870395365,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingStringCodec",
            "value": 13.132127407667436,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingByteArrayCodec",
            "value": 14.115118123174891,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingNewStringCodec",
            "value": 67.19567334321958,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingOldStringCodec",
            "value": 66.34041847469213,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndRead",
            "value": 160.33432508416803,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1",
            "value": 145.32713562907247,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch10",
            "value": 1500.3456610152543,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch100",
            "value": 12582.911396604302,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1000",
            "value": 123180.63868245292,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisEndpointBenchmark.measureUserWrite",
            "value": 36.956045784814876,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisStateMachineBenchmark.measureDecode",
            "value": 145.85553699449105,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.singleConnection",
            "value": 369.8918537108332,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.twentyConnections",
            "value": 7243.115428739086,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.singleConnection",
            "value": 686.9047964181684,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.twentyConnections",
            "value": 13067.200514157672,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          }
        ]
      },
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
          "id": "28a4154d6ad7f8309ca5affae9e69f5da4c38e16",
          "message": "Test failures not reported because step is skipped (#3067)",
          "timestamp": "2024-12-02T13:26:29Z",
          "url": "https://github.com/redis/lettuce/commit/28a4154d6ad7f8309ca5affae9e69f5da4c38e16"
        },
        "date": 1733277608710,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSet",
            "value": 71137.70978353737,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatch",
            "value": 5571.496196178458,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatchFlush",
            "value": 4995.872378973669,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSet",
            "value": 71270.94889651201,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatch",
            "value": 6254.876158254632,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatchFlush",
            "value": 6224.949637491298,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncList",
            "value": 163304.185917917,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncSet",
            "value": 71354.69853994306,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommands",
            "value": 89.92858083987268,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommandsAsBatch",
            "value": 407.1005199617131,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writeKeyedCommand",
            "value": 30.99941145332075,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writePlainCommand",
            "value": 7.7258838488117645,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSet",
            "value": 88471.97659740722,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatch",
            "value": 7464.381962013207,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatchFlush",
            "value": 6319.440336098458,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSet",
            "value": 89238.64548070199,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatch",
            "value": 8075.109871229436,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatchFlush",
            "value": 8223.574835502794,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.syncSet",
            "value": 88528.92959018098,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashDirect",
            "value": 40.45396664356259,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashHeap",
            "value": 37.45984201737998,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedDirect",
            "value": 28.402278887641774,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedHeap",
            "value": 31.244867632678343,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeAllSlots",
            "value": 26740.301256868017,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeLowerSlots",
            "value": 32875.70707650696,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusAbsent",
            "value": 1.0023671162374845,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusPresent",
            "value": 0.994551641457566,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyEstimatedSize",
            "value": 211.52101206308416,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyExactSize",
            "value": 70.56596386025961,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueEstimatedSize",
            "value": 209.435697554722,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueExactSize",
            "value": 72.63927623464852,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.decodeUtf8Unpooled",
            "value": 4.404941483901109,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeAsciiToBuf",
            "value": 14.962753892911618,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeIsoToBuf",
            "value": 27.692638161586622,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8PlainStringToBuf",
            "value": 18.004775914216513,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8ToBuf",
            "value": 29.67079585446506,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8Unpooled",
            "value": 85.83245512094193,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.decodeUnpooled",
            "value": 4.4075637636093195,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.encodeUnpooled",
            "value": 85.75184574761326,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createBatchCommands",
            "value": 848017.0817924462,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createRegularCommands",
            "value": 864230.996687774,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeAsyncCommand",
            "value": 31.14157413185574,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeCommandInterfaceCommand",
            "value": 174.74374248452958,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.asyncSet",
            "value": 6212.608392559695,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.batchSet",
            "value": 5536.639131845832,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.intercept.InvocationProxyFactoryBenchmark.run",
            "value": 57.139319830105464,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100Elements",
            "value": 384.8319692299855,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100ElementsWithResizeElement",
            "value": 572.6908636792443,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16Elements",
            "value": 48.32210692822137,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16ElementsWithResizeElement",
            "value": 122.88510624905636,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureSingleElement",
            "value": 6.854254715814538,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureZeroElement",
            "value": 0.6285715728241401,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createAsyncCommandUsingByteArrayCodec",
            "value": 27.739095091848867,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingByteArrayCodec",
            "value": 23.019521995504338,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingStringCodec",
            "value": 13.221020124433673,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingByteArrayCodec",
            "value": 14.169715657883902,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingNewStringCodec",
            "value": 67.12743972220872,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingOldStringCodec",
            "value": 69.2451674749065,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndRead",
            "value": 160.73504927036308,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1",
            "value": 143.49452531046717,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch10",
            "value": 1465.8918784556963,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch100",
            "value": 12077.900073786006,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1000",
            "value": 130807.72073866361,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisEndpointBenchmark.measureUserWrite",
            "value": 38.62513607423818,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisStateMachineBenchmark.measureDecode",
            "value": 160.77359778572207,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.singleConnection",
            "value": 394.1595346778205,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.twentyConnections",
            "value": 7828.87709876741,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.singleConnection",
            "value": 694.1652856570848,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.twentyConnections",
            "value": 13934.47064922667,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          }
        ]
      },
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
          "id": "28a4154d6ad7f8309ca5affae9e69f5da4c38e16",
          "message": "Test failures not reported because step is skipped (#3067)",
          "timestamp": "2024-12-02T13:26:29Z",
          "url": "https://github.com/redis/lettuce/commit/28a4154d6ad7f8309ca5affae9e69f5da4c38e16"
        },
        "date": 1733363999368,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSet",
            "value": 71095.62068228339,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatch",
            "value": 5658.609699798347,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatchFlush",
            "value": 4966.737520589472,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSet",
            "value": 72331.7562120478,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatch",
            "value": 6388.818087602554,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatchFlush",
            "value": 6226.568230372521,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncList",
            "value": 162063.8526703282,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncSet",
            "value": 71841.44255653159,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommands",
            "value": 89.38406745869436,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommandsAsBatch",
            "value": 369.63672960779405,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writeKeyedCommand",
            "value": 31.01470104138744,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writePlainCommand",
            "value": 7.765927003384258,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSet",
            "value": 90069.74012064526,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatch",
            "value": 7623.828385502497,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatchFlush",
            "value": 6423.442778258168,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSet",
            "value": 90533.49783850067,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatch",
            "value": 8261.820169621871,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatchFlush",
            "value": 8407.03974020589,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.syncSet",
            "value": 90039.70248101874,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashDirect",
            "value": 40.46907593271315,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashHeap",
            "value": 37.08225576331149,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedDirect",
            "value": 28.07859811154936,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedHeap",
            "value": 31.47238306708549,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeAllSlots",
            "value": 27447.87172150155,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeLowerSlots",
            "value": 13354.125425667156,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusAbsent",
            "value": 1.006539105444392,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusPresent",
            "value": 0.9879154206185025,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyEstimatedSize",
            "value": 219.12999657202505,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyExactSize",
            "value": 70.34733048163959,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueEstimatedSize",
            "value": 213.618842377534,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueExactSize",
            "value": 72.9635791401234,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.decodeUtf8Unpooled",
            "value": 4.395481632930635,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeAsciiToBuf",
            "value": 14.919330611480978,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeIsoToBuf",
            "value": 27.8027285429201,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8PlainStringToBuf",
            "value": 17.95753625059984,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8ToBuf",
            "value": 29.77092207913076,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8Unpooled",
            "value": 86.29271431304859,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.decodeUnpooled",
            "value": 4.414987746416637,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.encodeUnpooled",
            "value": 86.29061247831389,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createBatchCommands",
            "value": 857376.4597887186,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createRegularCommands",
            "value": 814191.300597535,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeAsyncCommand",
            "value": 27.4379282474004,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeCommandInterfaceCommand",
            "value": 168.39781209258646,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.asyncSet",
            "value": 6246.310237374022,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.batchSet",
            "value": 5510.109490326775,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.intercept.InvocationProxyFactoryBenchmark.run",
            "value": 55.363577281466426,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100Elements",
            "value": 396.9128831314184,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100ElementsWithResizeElement",
            "value": 574.8889541367431,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16Elements",
            "value": 48.65237893557635,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16ElementsWithResizeElement",
            "value": 124.19150226324632,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureSingleElement",
            "value": 6.877975226424804,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureZeroElement",
            "value": 0.6308528812188774,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createAsyncCommandUsingByteArrayCodec",
            "value": 27.810047256376784,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingByteArrayCodec",
            "value": 23.112097585646133,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingStringCodec",
            "value": 13.286202195239998,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingByteArrayCodec",
            "value": 14.317368599132811,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingNewStringCodec",
            "value": 67.31720181159803,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingOldStringCodec",
            "value": 69.73721771569767,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndRead",
            "value": 146.52918223560286,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1",
            "value": 143.39860189100904,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch10",
            "value": 1520.5210964748296,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch100",
            "value": 11806.671972992215,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1000",
            "value": 130731.50828118059,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisEndpointBenchmark.measureUserWrite",
            "value": 38.531891492130164,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisStateMachineBenchmark.measureDecode",
            "value": 148.03996332047763,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.singleConnection",
            "value": 357.74064801506563,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.twentyConnections",
            "value": 7466.210724412662,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.singleConnection",
            "value": 681.9172465196987,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.twentyConnections",
            "value": 13355.415387904335,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          }
        ]
      },
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
          "id": "28a4154d6ad7f8309ca5affae9e69f5da4c38e16",
          "message": "Test failures not reported because step is skipped (#3067)",
          "timestamp": "2024-12-02T13:26:29Z",
          "url": "https://github.com/redis/lettuce/commit/28a4154d6ad7f8309ca5affae9e69f5da4c38e16"
        },
        "date": 1733450380552,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSet",
            "value": 68396.56388271766,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatch",
            "value": 5424.570333506939,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatchFlush",
            "value": 4840.104741121229,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSet",
            "value": 69563.73243052178,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatch",
            "value": 6156.600646080393,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatchFlush",
            "value": 6041.761888212981,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncList",
            "value": 155231.11772897094,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncSet",
            "value": 68522.94607590632,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommands",
            "value": 88.96154539138152,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommandsAsBatch",
            "value": 406.57907282825187,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writeKeyedCommand",
            "value": 30.87120149440576,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writePlainCommand",
            "value": 7.700576444533326,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSet",
            "value": 86765.17224581147,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatch",
            "value": 7358.27084343269,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatchFlush",
            "value": 6184.528602410077,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSet",
            "value": 87157.73276223887,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatch",
            "value": 8078.465438549785,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatchFlush",
            "value": 8078.604170536205,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.syncSet",
            "value": 87631.72687644878,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashDirect",
            "value": 40.38877301450937,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashHeap",
            "value": 37.84668936566457,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedDirect",
            "value": 28.116234463906533,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedHeap",
            "value": 31.112086395668392,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeAllSlots",
            "value": 65218.651280240156,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeLowerSlots",
            "value": 35827.06322765919,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusAbsent",
            "value": 1.001158048806871,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusPresent",
            "value": 0.9895992166938516,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyEstimatedSize",
            "value": 216.0376254693515,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyExactSize",
            "value": 70.70014467824862,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueEstimatedSize",
            "value": 222.01380300232955,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueExactSize",
            "value": 72.73765498626781,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.decodeUtf8Unpooled",
            "value": 4.382955709721522,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeAsciiToBuf",
            "value": 15.267644675036184,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeIsoToBuf",
            "value": 27.112204050383674,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8PlainStringToBuf",
            "value": 17.977575554887572,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8ToBuf",
            "value": 29.691478250283616,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8Unpooled",
            "value": 85.91917657994748,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.decodeUnpooled",
            "value": 4.397789052992971,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.encodeUnpooled",
            "value": 85.93179938689023,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createBatchCommands",
            "value": 860121.208516215,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createRegularCommands",
            "value": 840931.2761759933,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeAsyncCommand",
            "value": 27.21308825673292,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeCommandInterfaceCommand",
            "value": 160.75580340258463,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.asyncSet",
            "value": 6111.378979908566,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.batchSet",
            "value": 5410.560133108839,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.intercept.InvocationProxyFactoryBenchmark.run",
            "value": 56.6214250251292,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100Elements",
            "value": 385.2609274069457,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100ElementsWithResizeElement",
            "value": 571.5270847378026,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16Elements",
            "value": 48.71781841362123,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16ElementsWithResizeElement",
            "value": 127.62356632728222,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureSingleElement",
            "value": 6.8368034923431065,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureZeroElement",
            "value": 0.630202090742944,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createAsyncCommandUsingByteArrayCodec",
            "value": 27.62287472517604,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingByteArrayCodec",
            "value": 22.96969399212558,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingStringCodec",
            "value": 13.218689817031684,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingByteArrayCodec",
            "value": 14.12301524531982,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingNewStringCodec",
            "value": 67.09773838053854,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingOldStringCodec",
            "value": 66.5715526518193,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndRead",
            "value": 152.51158291488431,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1",
            "value": 141.09739664528826,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch10",
            "value": 1372.4080926799165,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch100",
            "value": 11713.343859414714,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1000",
            "value": 126781.41068180681,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisEndpointBenchmark.measureUserWrite",
            "value": 37.81187489778619,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisStateMachineBenchmark.measureDecode",
            "value": 147.8958801504735,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.singleConnection",
            "value": 360.62038787422273,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.twentyConnections",
            "value": 7524.849342213945,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.singleConnection",
            "value": 709.8658845286596,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.twentyConnections",
            "value": 13906.934205869542,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "leisurelyrcxf",
            "username": "okg-cxf",
            "email": "145091193+okg-cxf@users.noreply.github.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "b76f985e75327b714c14454ca715f1c0648811d9",
          "message": "Fix: make sure FIFO order between write and notify channel active (#2597)\n\n* fix: make sure FIFO order for write() when notifyChannelActive(), also make sure channel access thread-safe and avoid potential NPE\r\n\r\n* Formatting issues\r\n\r\n---------\r\n\r\nCo-authored-by: Tihomir Mateev <tihomir.mateev@gmail.com>",
          "timestamp": "2024-12-06T16:02:53Z",
          "url": "https://github.com/redis/lettuce/commit/b76f985e75327b714c14454ca715f1c0648811d9"
        },
        "date": 1733536803219,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSet",
            "value": 70667.55902000905,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatch",
            "value": 5672.621890863277,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatchFlush",
            "value": 5035.755510557627,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSet",
            "value": 72122.82805093663,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatch",
            "value": 6397.344641971079,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatchFlush",
            "value": 6513.237984494178,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncList",
            "value": 161739.74712999153,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncSet",
            "value": 70369.58914382829,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommands",
            "value": 89.20989271332925,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommandsAsBatch",
            "value": 373.44098938133885,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writeKeyedCommand",
            "value": 30.927305609258855,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writePlainCommand",
            "value": 7.7254341933648005,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSet",
            "value": 89096.7284231217,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatch",
            "value": 7631.049687001815,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatchFlush",
            "value": 6575.466477030396,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSet",
            "value": 91407.5058911893,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatch",
            "value": 8495.849486028692,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatchFlush",
            "value": 8561.06528784447,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.syncSet",
            "value": 89957.60623894424,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashDirect",
            "value": 40.35618245409299,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashHeap",
            "value": 37.04428205555139,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedDirect",
            "value": 28.325805624796818,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedHeap",
            "value": 31.567639648098936,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeAllSlots",
            "value": 65082.22791302955,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeLowerSlots",
            "value": 13353.771836323382,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusAbsent",
            "value": 1.0013946755187992,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusPresent",
            "value": 0.9933148493168529,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyEstimatedSize",
            "value": 207.2663289899625,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyExactSize",
            "value": 71.08734351779343,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueEstimatedSize",
            "value": 215.4022061350779,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueExactSize",
            "value": 72.9993413392059,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.decodeUtf8Unpooled",
            "value": 4.386819917635052,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeAsciiToBuf",
            "value": 15.135288445967984,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeIsoToBuf",
            "value": 27.713420333300313,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8PlainStringToBuf",
            "value": 18.032088254849732,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8ToBuf",
            "value": 29.69099713994462,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8Unpooled",
            "value": 85.99914750323187,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.decodeUnpooled",
            "value": 4.407337074228683,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.encodeUnpooled",
            "value": 85.76449995243775,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createBatchCommands",
            "value": 854446.2545819102,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createRegularCommands",
            "value": 867159.9532526057,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeAsyncCommand",
            "value": 27.25096700052206,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeCommandInterfaceCommand",
            "value": 169.66531160149248,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.asyncSet",
            "value": 6379.325791091975,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.batchSet",
            "value": 5691.358279596893,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.intercept.InvocationProxyFactoryBenchmark.run",
            "value": 53.5216281109126,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100Elements",
            "value": 386.50454444382854,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100ElementsWithResizeElement",
            "value": 572.4987945567699,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16Elements",
            "value": 48.39049895719386,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16ElementsWithResizeElement",
            "value": 123.5469274730877,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureSingleElement",
            "value": 6.874954302874582,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureZeroElement",
            "value": 0.631389392613446,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createAsyncCommandUsingByteArrayCodec",
            "value": 27.772532797831758,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingByteArrayCodec",
            "value": 23.005748362550186,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingStringCodec",
            "value": 13.14639072598089,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingByteArrayCodec",
            "value": 14.132591520819489,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingNewStringCodec",
            "value": 64.05457363858527,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingOldStringCodec",
            "value": 69.63193295933465,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndRead",
            "value": 223.89759289299772,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1",
            "value": 169.3830447048046,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch10",
            "value": 1731.2636199825072,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch100",
            "value": 13840.287701792648,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1000",
            "value": 143225.3466031635,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisEndpointBenchmark.measureUserWrite",
            "value": 36.96907664868468,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisStateMachineBenchmark.measureDecode",
            "value": 146.7040796528502,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.singleConnection",
            "value": 362.63089170460387,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.twentyConnections",
            "value": 7493.522106643308,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.singleConnection",
            "value": 688.4486351457174,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.twentyConnections",
            "value": 13098.18450732014,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          }
        ]
      }
    ]
  }
}