window.BENCHMARK_DATA = {
  "lastUpdate": 1737079132882,
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
        "date": 1733623227043,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSet",
            "value": 69214.80960679628,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatch",
            "value": 5503.631348500565,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatchFlush",
            "value": 5082.817796884994,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSet",
            "value": 70683.04883706599,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatch",
            "value": 6427.021113856163,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatchFlush",
            "value": 6463.536678098059,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncList",
            "value": 160984.9342261833,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncSet",
            "value": 69735.69588143296,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommands",
            "value": 89.38404973219322,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommandsAsBatch",
            "value": 370.02755395472815,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writeKeyedCommand",
            "value": 30.517385267038314,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writePlainCommand",
            "value": 7.7317984241944675,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSet",
            "value": 87315.1587303913,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatch",
            "value": 7580.5108013795925,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatchFlush",
            "value": 6454.170448887868,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSet",
            "value": 88754.9862232559,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatch",
            "value": 8403.32936797259,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatchFlush",
            "value": 8514.190694598958,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.syncSet",
            "value": 87778.64236231332,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashDirect",
            "value": 40.724456846230545,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashHeap",
            "value": 37.202869821121325,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedDirect",
            "value": 28.391164949213636,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedHeap",
            "value": 31.581145522205492,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeAllSlots",
            "value": 65969.38848384735,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeLowerSlots",
            "value": 32881.28672950389,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusAbsent",
            "value": 0.999322650135448,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusPresent",
            "value": 0.9874605456921641,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyEstimatedSize",
            "value": 208.57495827957865,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyExactSize",
            "value": 70.3692425858878,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueEstimatedSize",
            "value": 204.99090199818406,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueExactSize",
            "value": 73.03564692862412,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.decodeUtf8Unpooled",
            "value": 4.39677367328897,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeAsciiToBuf",
            "value": 14.89808159840731,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeIsoToBuf",
            "value": 27.694513683299995,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8PlainStringToBuf",
            "value": 17.968775323058306,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8ToBuf",
            "value": 29.589087577789876,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8Unpooled",
            "value": 85.96789212402443,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.decodeUnpooled",
            "value": 4.3949666866851995,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.encodeUnpooled",
            "value": 85.77539998692194,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createBatchCommands",
            "value": 865921.694944456,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createRegularCommands",
            "value": 844742.1462707432,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeAsyncCommand",
            "value": 27.066106432409384,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeCommandInterfaceCommand",
            "value": 168.31178599557592,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.asyncSet",
            "value": 6314.900616482354,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.batchSet",
            "value": 5558.973471192882,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.intercept.InvocationProxyFactoryBenchmark.run",
            "value": 55.4409181884447,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100Elements",
            "value": 384.34934336890325,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100ElementsWithResizeElement",
            "value": 1038.2398997696619,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16Elements",
            "value": 48.288805059853246,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16ElementsWithResizeElement",
            "value": 124.64497817849272,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureSingleElement",
            "value": 6.857801749443871,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureZeroElement",
            "value": 0.6296870705917315,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createAsyncCommandUsingByteArrayCodec",
            "value": 27.684705029902027,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingByteArrayCodec",
            "value": 23.045989289996506,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingStringCodec",
            "value": 13.246961187813048,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingByteArrayCodec",
            "value": 14.115243678937556,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingNewStringCodec",
            "value": 67.15750080731212,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingOldStringCodec",
            "value": 67.19494641767133,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndRead",
            "value": 230.7087223934237,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1",
            "value": 191.51212929119964,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch10",
            "value": 1513.8367078572378,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch100",
            "value": 15932.998545351165,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1000",
            "value": 154310.94650879165,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisEndpointBenchmark.measureUserWrite",
            "value": 36.88327733372801,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisStateMachineBenchmark.measureDecode",
            "value": 150.1765352540745,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.singleConnection",
            "value": 362.0578615905588,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.twentyConnections",
            "value": 7593.8624877126695,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.singleConnection",
            "value": 701.6931528787107,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.twentyConnections",
            "value": 13125.117386662532,
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
        "date": 1733709624277,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSet",
            "value": 69412.16179682373,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatch",
            "value": 5574.186407520826,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatchFlush",
            "value": 5023.48725795542,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSet",
            "value": 70477.35618731708,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatch",
            "value": 6284.839303340309,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatchFlush",
            "value": 6534.257003198344,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncList",
            "value": 161210.35809468554,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncSet",
            "value": 69698.27791191632,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommands",
            "value": 89.51412414530299,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommandsAsBatch",
            "value": 374.68573961812734,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writeKeyedCommand",
            "value": 31.08702390865313,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writePlainCommand",
            "value": 9.042778980134814,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSet",
            "value": 87193.43056029816,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatch",
            "value": 7451.567653454098,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatchFlush",
            "value": 6445.3381434505445,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSet",
            "value": 89240.5250592351,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatch",
            "value": 8179.598034876022,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatchFlush",
            "value": 8319.89111400871,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.syncSet",
            "value": 88677.49707022434,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashDirect",
            "value": 40.44808223116657,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashHeap",
            "value": 37.156217587413856,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedDirect",
            "value": 28.42605644767071,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedHeap",
            "value": 31.102379130073093,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeAllSlots",
            "value": 65466.87849434446,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeLowerSlots",
            "value": 32800.62763134329,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusAbsent",
            "value": 0.9950592894179497,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusPresent",
            "value": 0.9876924754629528,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyEstimatedSize",
            "value": 204.8721181140762,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyExactSize",
            "value": 70.60532973521886,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueEstimatedSize",
            "value": 218.56666905640174,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueExactSize",
            "value": 72.8878501689922,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.decodeUtf8Unpooled",
            "value": 4.427784611325898,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeAsciiToBuf",
            "value": 14.946492673548402,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeIsoToBuf",
            "value": 27.425513967657217,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8PlainStringToBuf",
            "value": 17.96359601208979,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8ToBuf",
            "value": 29.571478269632195,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8Unpooled",
            "value": 85.79451598542236,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.decodeUnpooled",
            "value": 4.40795473312861,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.encodeUnpooled",
            "value": 85.80434229162343,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createBatchCommands",
            "value": 840276.153169341,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createRegularCommands",
            "value": 862681.6085276136,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeAsyncCommand",
            "value": 27.356059085617375,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeCommandInterfaceCommand",
            "value": 171.68881107269334,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.asyncSet",
            "value": 6375.822885383478,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.batchSet",
            "value": 5656.359150006721,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.intercept.InvocationProxyFactoryBenchmark.run",
            "value": 63.499146111963924,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100Elements",
            "value": 386.7744283188663,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100ElementsWithResizeElement",
            "value": 572.8350333637453,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16Elements",
            "value": 48.38465756380348,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16ElementsWithResizeElement",
            "value": 122.69848995261921,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureSingleElement",
            "value": 6.871484480587118,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureZeroElement",
            "value": 0.630571097916689,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createAsyncCommandUsingByteArrayCodec",
            "value": 27.72857625296196,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingByteArrayCodec",
            "value": 22.98987901006978,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingStringCodec",
            "value": 13.263681862773206,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingByteArrayCodec",
            "value": 14.374538536273054,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingNewStringCodec",
            "value": 69.40211482834715,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingOldStringCodec",
            "value": 67.152780510533,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndRead",
            "value": 230.4126124323484,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1",
            "value": 168.3344602487662,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch10",
            "value": 1809.127749065216,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch100",
            "value": 13445.872940255407,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1000",
            "value": 135621.73150978077,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisEndpointBenchmark.measureUserWrite",
            "value": 36.91009963891799,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisStateMachineBenchmark.measureDecode",
            "value": 146.88087677105605,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.singleConnection",
            "value": 361.59756369963213,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.twentyConnections",
            "value": 7180.415264196274,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.singleConnection",
            "value": 659.4885037666106,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.twentyConnections",
            "value": 13033.578545644295,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "dependabot[bot]",
            "username": "dependabot[bot]",
            "email": "49699333+dependabot[bot]@users.noreply.github.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "0f383b9c56bb702209456a664bf0df0b39160958",
          "message": "Bump org.slf4j:jcl-over-slf4j from 1.7.25 to 2.0.16 (#2959)\n\nBumps org.slf4j:jcl-over-slf4j from 1.7.25 to 2.0.16.\r\n\r\n---\r\nupdated-dependencies:\r\n- dependency-name: org.slf4j:jcl-over-slf4j\r\n  dependency-type: direct:development\r\n  update-type: version-update:semver-major\r\n...\r\n\r\nSigned-off-by: dependabot[bot] <support@github.com>\r\nCo-authored-by: dependabot[bot] <49699333+dependabot[bot]@users.noreply.github.com>",
          "timestamp": "2024-12-09T16:13:25Z",
          "url": "https://github.com/redis/lettuce/commit/0f383b9c56bb702209456a664bf0df0b39160958"
        },
        "date": 1733842456822,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSet",
            "value": 71103.62543707433,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatch",
            "value": 5765.700333401734,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatchFlush",
            "value": 5243.15796358632,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSet",
            "value": 71328.10093095311,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatch",
            "value": 6615.272753729905,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatchFlush",
            "value": 6645.164501031369,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncList",
            "value": 164755.16050887102,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncSet",
            "value": 71911.3944491702,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommands",
            "value": 89.72602298138814,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommandsAsBatch",
            "value": 368.267626400314,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writeKeyedCommand",
            "value": 30.956660901865927,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writePlainCommand",
            "value": 7.704189064703007,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSet",
            "value": 88997.20949605094,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatch",
            "value": 7683.546466846597,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatchFlush",
            "value": 6596.6367506965325,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSet",
            "value": 90412.86192019787,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatch",
            "value": 8584.68275786684,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatchFlush",
            "value": 8659.278810034393,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.syncSet",
            "value": 89936.85612092793,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashDirect",
            "value": 40.489094580671875,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashHeap",
            "value": 37.09380952060659,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedDirect",
            "value": 28.390741717556693,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedHeap",
            "value": 31.259652986480212,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeAllSlots",
            "value": 60068.83764656015,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeLowerSlots",
            "value": 35667.10074646892,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusAbsent",
            "value": 1.0078246857562392,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusPresent",
            "value": 0.9924847968728889,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyEstimatedSize",
            "value": 214.41092450149813,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyExactSize",
            "value": 70.35254131822765,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueEstimatedSize",
            "value": 213.96439880003624,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueExactSize",
            "value": 73.18235088109834,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.decodeUtf8Unpooled",
            "value": 4.419585263125483,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeAsciiToBuf",
            "value": 14.858036891032484,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeIsoToBuf",
            "value": 27.841929061777495,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8PlainStringToBuf",
            "value": 17.984537063059776,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8ToBuf",
            "value": 29.566741031392944,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8Unpooled",
            "value": 87.22835426493472,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.decodeUnpooled",
            "value": 4.397220604922997,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.encodeUnpooled",
            "value": 86.89189314783445,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createBatchCommands",
            "value": 854926.4637632215,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createRegularCommands",
            "value": 850642.4527881735,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeAsyncCommand",
            "value": 27.38467821429287,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeCommandInterfaceCommand",
            "value": 159.76527832909224,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.asyncSet",
            "value": 6459.956801007315,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.batchSet",
            "value": 5655.364355273041,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.intercept.InvocationProxyFactoryBenchmark.run",
            "value": 54.82687664236376,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100Elements",
            "value": 384.45318488112446,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100ElementsWithResizeElement",
            "value": 574.0173477274595,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16Elements",
            "value": 48.42064019224647,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16ElementsWithResizeElement",
            "value": 122.78812989013156,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureSingleElement",
            "value": 6.86144322463881,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureZeroElement",
            "value": 0.6344438768910022,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createAsyncCommandUsingByteArrayCodec",
            "value": 27.70030400153584,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingByteArrayCodec",
            "value": 23.120779883194935,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingStringCodec",
            "value": 13.149220213598657,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingByteArrayCodec",
            "value": 14.15907850078364,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingNewStringCodec",
            "value": 67.23609643332593,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingOldStringCodec",
            "value": 69.3861608308025,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndRead",
            "value": 211.60153722305768,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1",
            "value": 170.69216616798622,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch10",
            "value": 1725.0243668895826,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch100",
            "value": 13752.790206534166,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1000",
            "value": 138242.54292677587,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisEndpointBenchmark.measureUserWrite",
            "value": 36.78130766492269,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisStateMachineBenchmark.measureDecode",
            "value": 148.32397658417952,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.singleConnection",
            "value": 354.67279721846387,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.twentyConnections",
            "value": 7300.457192439409,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.singleConnection",
            "value": 650.5785989394121,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.twentyConnections",
            "value": 12787.561746894524,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "dependabot[bot]",
            "username": "dependabot[bot]",
            "email": "49699333+dependabot[bot]@users.noreply.github.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "0f383b9c56bb702209456a664bf0df0b39160958",
          "message": "Bump org.slf4j:jcl-over-slf4j from 1.7.25 to 2.0.16 (#2959)\n\nBumps org.slf4j:jcl-over-slf4j from 1.7.25 to 2.0.16.\r\n\r\n---\r\nupdated-dependencies:\r\n- dependency-name: org.slf4j:jcl-over-slf4j\r\n  dependency-type: direct:development\r\n  update-type: version-update:semver-major\r\n...\r\n\r\nSigned-off-by: dependabot[bot] <support@github.com>\r\nCo-authored-by: dependabot[bot] <49699333+dependabot[bot]@users.noreply.github.com>",
          "timestamp": "2024-12-09T16:13:25Z",
          "url": "https://github.com/redis/lettuce/commit/0f383b9c56bb702209456a664bf0df0b39160958"
        },
        "date": 1733882393328,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSet",
            "value": 69428.14565177346,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatch",
            "value": 5616.739311846992,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatchFlush",
            "value": 5052.96812653137,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSet",
            "value": 70641.9088012791,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatch",
            "value": 6473.476869847117,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatchFlush",
            "value": 6368.588016833919,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncList",
            "value": 158479.25631211684,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncSet",
            "value": 71092.67804866095,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommands",
            "value": 88.94386155113635,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommandsAsBatch",
            "value": 370.03444986289725,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writeKeyedCommand",
            "value": 30.898736012076636,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writePlainCommand",
            "value": 7.815277342586302,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSet",
            "value": 87716.83833434622,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatch",
            "value": 7654.659819972306,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatchFlush",
            "value": 6525.178788089712,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSet",
            "value": 89843.94603981762,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatch",
            "value": 8215.586643332868,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatchFlush",
            "value": 8425.217027238516,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.syncSet",
            "value": 88987.60349474466,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashDirect",
            "value": 40.301763269576995,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashHeap",
            "value": 37.05907089062706,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedDirect",
            "value": 28.316234331955865,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedHeap",
            "value": 31.471135313538348,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeAllSlots",
            "value": 59910.53539743641,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeLowerSlots",
            "value": 32823.97320237874,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusAbsent",
            "value": 0.9969375680628417,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusPresent",
            "value": 0.984330047747185,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyEstimatedSize",
            "value": 195.63162239255064,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyExactSize",
            "value": 70.19130422569464,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueEstimatedSize",
            "value": 219.48368234636305,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueExactSize",
            "value": 73.06962764053586,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.decodeUtf8Unpooled",
            "value": 4.397244882268655,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeAsciiToBuf",
            "value": 14.908707975479478,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeIsoToBuf",
            "value": 27.657461445561115,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8PlainStringToBuf",
            "value": 17.91846008234092,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8ToBuf",
            "value": 29.53420019598741,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8Unpooled",
            "value": 85.84262790775276,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.decodeUnpooled",
            "value": 4.386690658298599,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.encodeUnpooled",
            "value": 85.6385411199815,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createBatchCommands",
            "value": 868750.9348701707,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createRegularCommands",
            "value": 847479.557200394,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeAsyncCommand",
            "value": 27.16891098162746,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeCommandInterfaceCommand",
            "value": 173.15503144202142,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.asyncSet",
            "value": 6389.173131222169,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.batchSet",
            "value": 5621.964788254665,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.intercept.InvocationProxyFactoryBenchmark.run",
            "value": 56.22430410973019,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100Elements",
            "value": 385.29760748168155,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100ElementsWithResizeElement",
            "value": 571.0778035375284,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16Elements",
            "value": 48.55516707791047,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16ElementsWithResizeElement",
            "value": 122.8090906193327,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureSingleElement",
            "value": 6.818805287325868,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureZeroElement",
            "value": 0.6308220318261051,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createAsyncCommandUsingByteArrayCodec",
            "value": 27.720664549356126,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingByteArrayCodec",
            "value": 23.00796445691934,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingStringCodec",
            "value": 13.102881458086017,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingByteArrayCodec",
            "value": 14.079222807523967,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingNewStringCodec",
            "value": 69.42156617926386,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingOldStringCodec",
            "value": 69.55504578777554,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndRead",
            "value": 227.4822356652278,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1",
            "value": 176.33590705913565,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch10",
            "value": 1485.5799883191214,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch100",
            "value": 15762.63697256337,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1000",
            "value": 137947.58262742136,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisEndpointBenchmark.measureUserWrite",
            "value": 36.83184872357093,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisStateMachineBenchmark.measureDecode",
            "value": 146.68182855103652,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.singleConnection",
            "value": 373.6910286841411,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.twentyConnections",
            "value": 7399.718133662714,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.singleConnection",
            "value": 693.1471541516868,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.twentyConnections",
            "value": 13953.563522307822,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "dependabot[bot]",
            "username": "dependabot[bot]",
            "email": "49699333+dependabot[bot]@users.noreply.github.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "0f383b9c56bb702209456a664bf0df0b39160958",
          "message": "Bump org.slf4j:jcl-over-slf4j from 1.7.25 to 2.0.16 (#2959)\n\nBumps org.slf4j:jcl-over-slf4j from 1.7.25 to 2.0.16.\r\n\r\n---\r\nupdated-dependencies:\r\n- dependency-name: org.slf4j:jcl-over-slf4j\r\n  dependency-type: direct:development\r\n  update-type: version-update:semver-major\r\n...\r\n\r\nSigned-off-by: dependabot[bot] <support@github.com>\r\nCo-authored-by: dependabot[bot] <49699333+dependabot[bot]@users.noreply.github.com>",
          "timestamp": "2024-12-09T16:13:25Z",
          "url": "https://github.com/redis/lettuce/commit/0f383b9c56bb702209456a664bf0df0b39160958"
        },
        "date": 1733968813690,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSet",
            "value": 68966.84440766078,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatch",
            "value": 5630.157301117311,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatchFlush",
            "value": 5023.297024989789,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSet",
            "value": 70749.6047896742,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatch",
            "value": 6431.310860966277,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatchFlush",
            "value": 6382.5428567185345,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncList",
            "value": 159836.88714503922,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncSet",
            "value": 69518.00573473459,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommands",
            "value": 89.53282851859517,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommandsAsBatch",
            "value": 406.4118825666233,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writeKeyedCommand",
            "value": 30.9777740164634,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writePlainCommand",
            "value": 7.707712898672772,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSet",
            "value": 88159.44245951882,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatch",
            "value": 7584.950389531115,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatchFlush",
            "value": 6530.937525504726,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSet",
            "value": 88905.47604496642,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatch",
            "value": 8301.488160820116,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatchFlush",
            "value": 8564.516269163101,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.syncSet",
            "value": 87472.1639809206,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashDirect",
            "value": 40.40731549726844,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashHeap",
            "value": 37.30153930299806,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedDirect",
            "value": 31.401796584176545,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedHeap",
            "value": 31.522040519493533,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeAllSlots",
            "value": 59597.22815475889,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeLowerSlots",
            "value": 13386.993672889837,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusAbsent",
            "value": 1.0020720694263894,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusPresent",
            "value": 0.9885327861233353,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyEstimatedSize",
            "value": 204.99624457017347,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyExactSize",
            "value": 70.4964296081462,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueEstimatedSize",
            "value": 210.30936776243053,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueExactSize",
            "value": 73.45661405372752,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.decodeUtf8Unpooled",
            "value": 4.40133262840438,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeAsciiToBuf",
            "value": 15.011478951653729,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeIsoToBuf",
            "value": 27.6763069599077,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8PlainStringToBuf",
            "value": 18.05117198877694,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8ToBuf",
            "value": 29.480646976441925,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8Unpooled",
            "value": 85.55322784221804,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.decodeUnpooled",
            "value": 4.418397518661459,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.encodeUnpooled",
            "value": 85.7190910977587,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createBatchCommands",
            "value": 838513.0513113274,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createRegularCommands",
            "value": 859225.8288211884,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeAsyncCommand",
            "value": 27.187596023629215,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeCommandInterfaceCommand",
            "value": 164.6807305246048,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.asyncSet",
            "value": 6465.006347362389,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.batchSet",
            "value": 5545.241196924593,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.intercept.InvocationProxyFactoryBenchmark.run",
            "value": 54.21861503986919,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100Elements",
            "value": 385.6517021085542,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100ElementsWithResizeElement",
            "value": 574.8474083552699,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16Elements",
            "value": 48.65440959928636,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16ElementsWithResizeElement",
            "value": 124.65380180735949,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureSingleElement",
            "value": 6.8677237515478335,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureZeroElement",
            "value": 0.6323498737539486,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createAsyncCommandUsingByteArrayCodec",
            "value": 27.735304074679238,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingByteArrayCodec",
            "value": 23.019701701837626,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingStringCodec",
            "value": 13.127736139240266,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingByteArrayCodec",
            "value": 14.061777321183413,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingNewStringCodec",
            "value": 67.21899578902043,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingOldStringCodec",
            "value": 67.07176126528336,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndRead",
            "value": 202.4810544389042,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1",
            "value": 159.97728898164004,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch10",
            "value": 1523.8781358527162,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch100",
            "value": 16341.204724962794,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1000",
            "value": 140985.97446893866,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisEndpointBenchmark.measureUserWrite",
            "value": 36.815645914004264,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisStateMachineBenchmark.measureDecode",
            "value": 148.3975283521399,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.singleConnection",
            "value": 357.5738865834682,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.twentyConnections",
            "value": 7653.43542258586,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.singleConnection",
            "value": 646.5046196185579,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.twentyConnections",
            "value": 13772.425156303821,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "dependabot[bot]",
            "username": "dependabot[bot]",
            "email": "49699333+dependabot[bot]@users.noreply.github.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "0f383b9c56bb702209456a664bf0df0b39160958",
          "message": "Bump org.slf4j:jcl-over-slf4j from 1.7.25 to 2.0.16 (#2959)\n\nBumps org.slf4j:jcl-over-slf4j from 1.7.25 to 2.0.16.\r\n\r\n---\r\nupdated-dependencies:\r\n- dependency-name: org.slf4j:jcl-over-slf4j\r\n  dependency-type: direct:development\r\n  update-type: version-update:semver-major\r\n...\r\n\r\nSigned-off-by: dependabot[bot] <support@github.com>\r\nCo-authored-by: dependabot[bot] <49699333+dependabot[bot]@users.noreply.github.com>",
          "timestamp": "2024-12-09T16:13:25Z",
          "url": "https://github.com/redis/lettuce/commit/0f383b9c56bb702209456a664bf0df0b39160958"
        },
        "date": 1734055221914,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSet",
            "value": 71820.46337865961,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatch",
            "value": 5631.575159723342,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatchFlush",
            "value": 5178.365121310681,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSet",
            "value": 71624.45787772132,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatch",
            "value": 6551.668319976971,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatchFlush",
            "value": 6427.0476906541935,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncList",
            "value": 160938.98495750688,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncSet",
            "value": 72403.65506725418,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommands",
            "value": 90.13934331901787,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommandsAsBatch",
            "value": 368.16129202285913,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writeKeyedCommand",
            "value": 30.77818416584077,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writePlainCommand",
            "value": 7.749516838532261,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSet",
            "value": 91137.99477604171,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatch",
            "value": 7597.48296013427,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatchFlush",
            "value": 6632.806016608114,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSet",
            "value": 90479.42643558022,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatch",
            "value": 8384.304500050335,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatchFlush",
            "value": 8444.89704579871,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.syncSet",
            "value": 90109.48864706769,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashDirect",
            "value": 40.81526232804287,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashHeap",
            "value": 37.1235051953649,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedDirect",
            "value": 28.518016812455595,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedHeap",
            "value": 31.631116493470877,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeAllSlots",
            "value": 59528.153076678864,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeLowerSlots",
            "value": 32809.98614447021,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusAbsent",
            "value": 1.002553930447776,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusPresent",
            "value": 0.9877080850345792,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyEstimatedSize",
            "value": 209.20843255384185,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyExactSize",
            "value": 69.96182880438155,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueEstimatedSize",
            "value": 208.18271542555004,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueExactSize",
            "value": 72.93123674446137,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.decodeUtf8Unpooled",
            "value": 4.408848277846211,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeAsciiToBuf",
            "value": 14.86032434653951,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeIsoToBuf",
            "value": 27.10717366304555,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8PlainStringToBuf",
            "value": 18.02154056920693,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8ToBuf",
            "value": 29.566324300477255,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8Unpooled",
            "value": 87.62276159066911,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.decodeUnpooled",
            "value": 4.387455156292165,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.encodeUnpooled",
            "value": 85.82299904343444,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createBatchCommands",
            "value": 838349.0319457732,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createRegularCommands",
            "value": 843583.1071557353,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeAsyncCommand",
            "value": 27.31361131127925,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeCommandInterfaceCommand",
            "value": 166.3175395407564,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.asyncSet",
            "value": 6447.235591597075,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.batchSet",
            "value": 5830.223780207412,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.intercept.InvocationProxyFactoryBenchmark.run",
            "value": 54.91894049478503,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100Elements",
            "value": 386.0838721359723,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100ElementsWithResizeElement",
            "value": 573.7003175329334,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16Elements",
            "value": 48.74336201006234,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16ElementsWithResizeElement",
            "value": 122.61573933953193,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureSingleElement",
            "value": 6.884721095728682,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureZeroElement",
            "value": 0.6325995468781854,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createAsyncCommandUsingByteArrayCodec",
            "value": 27.622791410145034,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingByteArrayCodec",
            "value": 23.03649458317315,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingStringCodec",
            "value": 13.221055488311283,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingByteArrayCodec",
            "value": 14.210070461813455,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingNewStringCodec",
            "value": 67.23372160074148,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingOldStringCodec",
            "value": 69.82139522656746,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndRead",
            "value": 219.35280014172204,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1",
            "value": 182.9248381736665,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch10",
            "value": 1723.6437214752132,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch100",
            "value": 13259.31299381972,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1000",
            "value": 142275.2484021714,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisEndpointBenchmark.measureUserWrite",
            "value": 36.98904207784202,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisStateMachineBenchmark.measureDecode",
            "value": 150.3849929045311,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.singleConnection",
            "value": 373.3059752502336,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.twentyConnections",
            "value": 7541.037655882648,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.singleConnection",
            "value": 676.8742507456424,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.twentyConnections",
            "value": 13151.13494960839,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "dependabot[bot]",
            "username": "dependabot[bot]",
            "email": "49699333+dependabot[bot]@users.noreply.github.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "0f383b9c56bb702209456a664bf0df0b39160958",
          "message": "Bump org.slf4j:jcl-over-slf4j from 1.7.25 to 2.0.16 (#2959)\n\nBumps org.slf4j:jcl-over-slf4j from 1.7.25 to 2.0.16.\r\n\r\n---\r\nupdated-dependencies:\r\n- dependency-name: org.slf4j:jcl-over-slf4j\r\n  dependency-type: direct:development\r\n  update-type: version-update:semver-major\r\n...\r\n\r\nSigned-off-by: dependabot[bot] <support@github.com>\r\nCo-authored-by: dependabot[bot] <49699333+dependabot[bot]@users.noreply.github.com>",
          "timestamp": "2024-12-09T16:13:25Z",
          "url": "https://github.com/redis/lettuce/commit/0f383b9c56bb702209456a664bf0df0b39160958"
        },
        "date": 1734141557886,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSet",
            "value": 70114.45999614322,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatch",
            "value": 5564.322975267981,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatchFlush",
            "value": 5010.816457292732,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSet",
            "value": 70104.01651563642,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatch",
            "value": 6403.010396175213,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatchFlush",
            "value": 6409.139764562911,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncList",
            "value": 157432.97999913676,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncSet",
            "value": 70038.31136929795,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommands",
            "value": 89.29704797914683,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommandsAsBatch",
            "value": 372.3829437056779,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writeKeyedCommand",
            "value": 30.878640996163416,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writePlainCommand",
            "value": 7.73267804454211,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSet",
            "value": 87400.58400886923,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatch",
            "value": 7559.6036095969275,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatchFlush",
            "value": 6537.366026067754,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSet",
            "value": 89633.66513335661,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatch",
            "value": 8333.497579497027,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatchFlush",
            "value": 8430.840304354238,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.syncSet",
            "value": 90198.3770880012,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashDirect",
            "value": 40.42221745448323,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashHeap",
            "value": 36.978100653931016,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedDirect",
            "value": 28.097922111223102,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedHeap",
            "value": 31.75228572723568,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeAllSlots",
            "value": 59724.769262633636,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeLowerSlots",
            "value": 35714.33237573369,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusAbsent",
            "value": 1.0001906638643625,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusPresent",
            "value": 0.9881665279184842,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyEstimatedSize",
            "value": 203.41728724435455,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyExactSize",
            "value": 70.60690851407608,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueEstimatedSize",
            "value": 215.6644398598731,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueExactSize",
            "value": 73.10868983837834,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.decodeUtf8Unpooled",
            "value": 4.416244047169025,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeAsciiToBuf",
            "value": 14.845850777240122,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeIsoToBuf",
            "value": 27.343128381436877,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8PlainStringToBuf",
            "value": 17.932749054187024,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8ToBuf",
            "value": 29.54035696991689,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8Unpooled",
            "value": 85.94974987662081,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.decodeUnpooled",
            "value": 4.3926007896588155,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.encodeUnpooled",
            "value": 85.59259203140968,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createBatchCommands",
            "value": 857451.7499622531,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createRegularCommands",
            "value": 864921.0836740462,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeAsyncCommand",
            "value": 27.042568341169567,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeCommandInterfaceCommand",
            "value": 171.76338228709992,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.asyncSet",
            "value": 6347.365232376002,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.batchSet",
            "value": 5638.0651378110515,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.intercept.InvocationProxyFactoryBenchmark.run",
            "value": 55.200089107873055,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100Elements",
            "value": 384.25528735554076,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100ElementsWithResizeElement",
            "value": 573.3297936690501,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16Elements",
            "value": 48.86356606827951,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16ElementsWithResizeElement",
            "value": 123.72435292337036,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureSingleElement",
            "value": 7.230399948252777,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureZeroElement",
            "value": 0.6321726683396282,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createAsyncCommandUsingByteArrayCodec",
            "value": 27.63580787427304,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingByteArrayCodec",
            "value": 23.02455648177644,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingStringCodec",
            "value": 13.144409706720456,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingByteArrayCodec",
            "value": 14.144867643135402,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingNewStringCodec",
            "value": 69.11656826965037,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingOldStringCodec",
            "value": 69.30137524252945,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndRead",
            "value": 232.10105445405793,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1",
            "value": 185.49194926166962,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch10",
            "value": 1554.3351444455811,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch100",
            "value": 13362.914253068,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1000",
            "value": 139858.8781231052,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisEndpointBenchmark.measureUserWrite",
            "value": 36.974711884734674,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisStateMachineBenchmark.measureDecode",
            "value": 147.56846689831156,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.singleConnection",
            "value": 355.12989230631194,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.twentyConnections",
            "value": 7374.696570952333,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.singleConnection",
            "value": 683.3138040675263,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.twentyConnections",
            "value": 13440.983478759823,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "dependabot[bot]",
            "username": "dependabot[bot]",
            "email": "49699333+dependabot[bot]@users.noreply.github.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "0f383b9c56bb702209456a664bf0df0b39160958",
          "message": "Bump org.slf4j:jcl-over-slf4j from 1.7.25 to 2.0.16 (#2959)\n\nBumps org.slf4j:jcl-over-slf4j from 1.7.25 to 2.0.16.\r\n\r\n---\r\nupdated-dependencies:\r\n- dependency-name: org.slf4j:jcl-over-slf4j\r\n  dependency-type: direct:development\r\n  update-type: version-update:semver-major\r\n...\r\n\r\nSigned-off-by: dependabot[bot] <support@github.com>\r\nCo-authored-by: dependabot[bot] <49699333+dependabot[bot]@users.noreply.github.com>",
          "timestamp": "2024-12-09T16:13:25Z",
          "url": "https://github.com/redis/lettuce/commit/0f383b9c56bb702209456a664bf0df0b39160958"
        },
        "date": 1734228016145,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSet",
            "value": 70163.87694924358,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatch",
            "value": 5690.355728288644,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatchFlush",
            "value": 5112.826997863368,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSet",
            "value": 71928.57095694199,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatch",
            "value": 6477.5136636950865,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatchFlush",
            "value": 6493.330501435767,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncList",
            "value": 161945.49021409734,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncSet",
            "value": 71569.22117764309,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommands",
            "value": 89.57018394495708,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommandsAsBatch",
            "value": 370.05604421260097,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writeKeyedCommand",
            "value": 31.11093768106262,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writePlainCommand",
            "value": 7.720012817799747,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSet",
            "value": 89974.31870647552,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatch",
            "value": 7670.815523305588,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatchFlush",
            "value": 6556.910765631546,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSet",
            "value": 90804.47063970575,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatch",
            "value": 8464.923193094572,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatchFlush",
            "value": 8378.43140145527,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.syncSet",
            "value": 89639.46559265329,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashDirect",
            "value": 40.530522287205976,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashHeap",
            "value": 37.178819668214715,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedDirect",
            "value": 28.501190730391265,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedHeap",
            "value": 31.29526343828703,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeAllSlots",
            "value": 65877.92415454381,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeLowerSlots",
            "value": 32866.05254752362,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusAbsent",
            "value": 0.99992185369179,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusPresent",
            "value": 0.9896621279941582,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyEstimatedSize",
            "value": 213.51251457021664,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyExactSize",
            "value": 70.44005013175882,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueEstimatedSize",
            "value": 219.0914785980452,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueExactSize",
            "value": 72.84698224426329,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.decodeUtf8Unpooled",
            "value": 4.391568034974314,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeAsciiToBuf",
            "value": 14.898094280070765,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeIsoToBuf",
            "value": 27.62069771210984,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8PlainStringToBuf",
            "value": 18.014099773166407,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8ToBuf",
            "value": 29.697453711357934,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8Unpooled",
            "value": 85.91194993206003,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.decodeUnpooled",
            "value": 4.408513505090982,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.encodeUnpooled",
            "value": 88.63562307775359,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createBatchCommands",
            "value": 855426.2053628329,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createRegularCommands",
            "value": 867171.5736481383,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeAsyncCommand",
            "value": 27.162695986800912,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeCommandInterfaceCommand",
            "value": 160.69883797674825,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.asyncSet",
            "value": 6459.354817058782,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.batchSet",
            "value": 5778.459501797679,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.intercept.InvocationProxyFactoryBenchmark.run",
            "value": 54.36340808009679,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100Elements",
            "value": 385.01625139271084,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100ElementsWithResizeElement",
            "value": 571.8076079106543,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16Elements",
            "value": 48.180952243081045,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16ElementsWithResizeElement",
            "value": 122.67469695142522,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureSingleElement",
            "value": 6.857471579138557,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureZeroElement",
            "value": 0.6326255963977939,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createAsyncCommandUsingByteArrayCodec",
            "value": 27.66493119401357,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingByteArrayCodec",
            "value": 23.00340501593277,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingStringCodec",
            "value": 13.226610682569222,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingByteArrayCodec",
            "value": 14.18559788230417,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingNewStringCodec",
            "value": 69.35676075157228,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingOldStringCodec",
            "value": 67.45203144360892,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndRead",
            "value": 201.0944300185898,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1",
            "value": 166.1492649390488,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch10",
            "value": 1491.269011733668,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch100",
            "value": 16335.426583025011,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1000",
            "value": 155071.2047513861,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisEndpointBenchmark.measureUserWrite",
            "value": 36.91296129960263,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisStateMachineBenchmark.measureDecode",
            "value": 158.28504362778827,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.singleConnection",
            "value": 362.2978180225726,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.twentyConnections",
            "value": 7449.933477288136,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.singleConnection",
            "value": 702.2493240640969,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.twentyConnections",
            "value": 13437.130897905308,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "dependabot[bot]",
            "username": "dependabot[bot]",
            "email": "49699333+dependabot[bot]@users.noreply.github.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "0f383b9c56bb702209456a664bf0df0b39160958",
          "message": "Bump org.slf4j:jcl-over-slf4j from 1.7.25 to 2.0.16 (#2959)\n\nBumps org.slf4j:jcl-over-slf4j from 1.7.25 to 2.0.16.\r\n\r\n---\r\nupdated-dependencies:\r\n- dependency-name: org.slf4j:jcl-over-slf4j\r\n  dependency-type: direct:development\r\n  update-type: version-update:semver-major\r\n...\r\n\r\nSigned-off-by: dependabot[bot] <support@github.com>\r\nCo-authored-by: dependabot[bot] <49699333+dependabot[bot]@users.noreply.github.com>",
          "timestamp": "2024-12-09T16:13:25Z",
          "url": "https://github.com/redis/lettuce/commit/0f383b9c56bb702209456a664bf0df0b39160958"
        },
        "date": 1734314418817,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSet",
            "value": 71769.47541383513,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatch",
            "value": 5685.200562015602,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatchFlush",
            "value": 5181.486437067879,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSet",
            "value": 72348.81568136663,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatch",
            "value": 6454.191737147163,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatchFlush",
            "value": 6558.941808019651,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncList",
            "value": 161146.63201345556,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncSet",
            "value": 71716.30795690339,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommands",
            "value": 89.5361647326567,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommandsAsBatch",
            "value": 375.9552671045723,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writeKeyedCommand",
            "value": 31.12554278869279,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writePlainCommand",
            "value": 7.745339520897026,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSet",
            "value": 89799.57641475888,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatch",
            "value": 7662.296661129304,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatchFlush",
            "value": 6548.48074012417,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSet",
            "value": 91113.97753886839,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatch",
            "value": 8531.113644264602,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatchFlush",
            "value": 8511.29446333046,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.syncSet",
            "value": 89943.24245368726,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashDirect",
            "value": 40.565171686403055,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashHeap",
            "value": 37.192848986324165,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedDirect",
            "value": 28.185478805690927,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedHeap",
            "value": 31.5967872700226,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeAllSlots",
            "value": 66280.62312102858,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeLowerSlots",
            "value": 32987.089625370245,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusAbsent",
            "value": 1.0036557016067862,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusPresent",
            "value": 0.9864265648812329,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyEstimatedSize",
            "value": 214.34261523653407,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyExactSize",
            "value": 70.51271287664434,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueEstimatedSize",
            "value": 217.9919879055748,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueExactSize",
            "value": 73.0832431045074,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.decodeUtf8Unpooled",
            "value": 4.4020618444918735,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeAsciiToBuf",
            "value": 14.88350968237907,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeIsoToBuf",
            "value": 27.469159151749444,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8PlainStringToBuf",
            "value": 18.027315791981227,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8ToBuf",
            "value": 29.625869784417482,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8Unpooled",
            "value": 86.86694944096698,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.decodeUnpooled",
            "value": 4.405247597763616,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.encodeUnpooled",
            "value": 86.85519590733024,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createBatchCommands",
            "value": 881843.3356532056,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createRegularCommands",
            "value": 869189.8115778627,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeAsyncCommand",
            "value": 27.385859586004933,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeCommandInterfaceCommand",
            "value": 186.9835273742504,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.asyncSet",
            "value": 6456.36884692981,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.batchSet",
            "value": 5692.461822292661,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.intercept.InvocationProxyFactoryBenchmark.run",
            "value": 53.41921037125063,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100Elements",
            "value": 385.30404197809827,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100ElementsWithResizeElement",
            "value": 575.8710858916972,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16Elements",
            "value": 48.80955949148155,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16ElementsWithResizeElement",
            "value": 124.23998512400321,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureSingleElement",
            "value": 6.897988761849828,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureZeroElement",
            "value": 0.632208976928308,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createAsyncCommandUsingByteArrayCodec",
            "value": 27.749064335257838,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingByteArrayCodec",
            "value": 23.13737583282538,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingStringCodec",
            "value": 13.30320395185414,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingByteArrayCodec",
            "value": 14.239318745909094,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingNewStringCodec",
            "value": 67.36683952942829,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingOldStringCodec",
            "value": 67.16372494783232,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndRead",
            "value": 222.55516829628215,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1",
            "value": 173.83863711038094,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch10",
            "value": 1733.248281725529,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch100",
            "value": 14096.730536049232,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1000",
            "value": 159814.30998687868,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisEndpointBenchmark.measureUserWrite",
            "value": 37.16555643734453,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisStateMachineBenchmark.measureDecode",
            "value": 148.44062689421412,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.singleConnection",
            "value": 364.2615570682779,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.twentyConnections",
            "value": 7264.998390389228,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.singleConnection",
            "value": 672.5425045947693,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.twentyConnections",
            "value": 13081.929710791997,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "dependabot[bot]",
            "username": "dependabot[bot]",
            "email": "49699333+dependabot[bot]@users.noreply.github.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "14de5b88f922581f996c3b3311cb253eb36b32db",
          "message": "Bump org.testcontainers:testcontainers from 1.20.1 to 1.20.4 (#3082)\n\nBumps [org.testcontainers:testcontainers](https://github.com/testcontainers/testcontainers-java) from 1.20.1 to 1.20.4.\r\n- [Release notes](https://github.com/testcontainers/testcontainers-java/releases)\r\n- [Changelog](https://github.com/testcontainers/testcontainers-java/blob/main/CHANGELOG.md)\r\n- [Commits](https://github.com/testcontainers/testcontainers-java/compare/1.20.1...1.20.4)\r\n\r\n---\r\nupdated-dependencies:\r\n- dependency-name: org.testcontainers:testcontainers\r\n  dependency-type: direct:development\r\n  update-type: version-update:semver-patch\r\n...\r\n\r\nSigned-off-by: dependabot[bot] <support@github.com>\r\nCo-authored-by: dependabot[bot] <49699333+dependabot[bot]@users.noreply.github.com>",
          "timestamp": "2024-12-16T14:59:31Z",
          "url": "https://github.com/redis/lettuce/commit/14de5b88f922581f996c3b3311cb253eb36b32db"
        },
        "date": 1734400809594,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSet",
            "value": 68594.46808714184,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatch",
            "value": 5514.851204807658,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatchFlush",
            "value": 5092.361852201662,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSet",
            "value": 69089.9651612865,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatch",
            "value": 6397.095884576929,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatchFlush",
            "value": 6241.404482019551,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncList",
            "value": 158788.0109798116,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncSet",
            "value": 68170.88568108625,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommands",
            "value": 89.73090355682402,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommandsAsBatch",
            "value": 369.92753985196697,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writeKeyedCommand",
            "value": 31.019231436086905,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writePlainCommand",
            "value": 7.725232010732874,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSet",
            "value": 86701.20536686796,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatch",
            "value": 7536.428414800701,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatchFlush",
            "value": 6420.77616331339,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSet",
            "value": 87321.62842187738,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatch",
            "value": 8204.993455386695,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatchFlush",
            "value": 8233.903151391736,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.syncSet",
            "value": 87264.6382969152,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashDirect",
            "value": 40.328881552166344,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashHeap",
            "value": 37.120233999390884,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedDirect",
            "value": 28.043632358740506,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedHeap",
            "value": 31.251058729812666,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeAllSlots",
            "value": 65714.38784395737,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeLowerSlots",
            "value": 35810.81809476595,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusAbsent",
            "value": 0.9995728433905064,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusPresent",
            "value": 0.9871800633377967,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyEstimatedSize",
            "value": 209.2471777876176,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyExactSize",
            "value": 70.46268240842781,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueEstimatedSize",
            "value": 207.71178022930226,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueExactSize",
            "value": 72.95644976035537,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.decodeUtf8Unpooled",
            "value": 4.391761383737264,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeAsciiToBuf",
            "value": 15.020524033089853,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeIsoToBuf",
            "value": 28.297173493210956,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8PlainStringToBuf",
            "value": 17.97804318808202,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8ToBuf",
            "value": 30.211718103541596,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8Unpooled",
            "value": 86.1725276303306,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.decodeUnpooled",
            "value": 4.391717972708936,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.encodeUnpooled",
            "value": 85.86491570068117,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createBatchCommands",
            "value": 831290.4085312041,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createRegularCommands",
            "value": 844151.3624495389,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeAsyncCommand",
            "value": 28.268450677610463,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeCommandInterfaceCommand",
            "value": 170.11972569125925,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.asyncSet",
            "value": 6302.915574285473,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.batchSet",
            "value": 5540.369082554966,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.intercept.InvocationProxyFactoryBenchmark.run",
            "value": 54.59678787610013,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100Elements",
            "value": 384.77134381409763,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100ElementsWithResizeElement",
            "value": 575.1172601905431,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16Elements",
            "value": 48.60018768816248,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16ElementsWithResizeElement",
            "value": 123.42474765562754,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureSingleElement",
            "value": 6.859387408790245,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureZeroElement",
            "value": 0.6292430454415043,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createAsyncCommandUsingByteArrayCodec",
            "value": 27.696821358652773,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingByteArrayCodec",
            "value": 23.038374907825077,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingStringCodec",
            "value": 13.117715475056164,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingByteArrayCodec",
            "value": 14.139963769815376,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingNewStringCodec",
            "value": 69.25784479700245,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingOldStringCodec",
            "value": 67.33278715805034,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndRead",
            "value": 209.68520241146658,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1",
            "value": 163.102347744658,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch10",
            "value": 1715.1781382014447,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch100",
            "value": 15733.3073657104,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1000",
            "value": 153611.1178897082,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisEndpointBenchmark.measureUserWrite",
            "value": 36.83806390943103,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisStateMachineBenchmark.measureDecode",
            "value": 148.61268823141702,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.singleConnection",
            "value": 358.9327666331449,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.twentyConnections",
            "value": 7445.95369304736,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.singleConnection",
            "value": 701.2567175275099,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.twentyConnections",
            "value": 13130.304072266274,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "dependabot[bot]",
            "username": "dependabot[bot]",
            "email": "49699333+dependabot[bot]@users.noreply.github.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "14de5b88f922581f996c3b3311cb253eb36b32db",
          "message": "Bump org.testcontainers:testcontainers from 1.20.1 to 1.20.4 (#3082)\n\nBumps [org.testcontainers:testcontainers](https://github.com/testcontainers/testcontainers-java) from 1.20.1 to 1.20.4.\r\n- [Release notes](https://github.com/testcontainers/testcontainers-java/releases)\r\n- [Changelog](https://github.com/testcontainers/testcontainers-java/blob/main/CHANGELOG.md)\r\n- [Commits](https://github.com/testcontainers/testcontainers-java/compare/1.20.1...1.20.4)\r\n\r\n---\r\nupdated-dependencies:\r\n- dependency-name: org.testcontainers:testcontainers\r\n  dependency-type: direct:development\r\n  update-type: version-update:semver-patch\r\n...\r\n\r\nSigned-off-by: dependabot[bot] <support@github.com>\r\nCo-authored-by: dependabot[bot] <49699333+dependabot[bot]@users.noreply.github.com>",
          "timestamp": "2024-12-16T14:59:31Z",
          "url": "https://github.com/redis/lettuce/commit/14de5b88f922581f996c3b3311cb253eb36b32db"
        },
        "date": 1734487219975,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSet",
            "value": 69461.69230342328,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatch",
            "value": 5591.707608924725,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatchFlush",
            "value": 5055.427790384104,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSet",
            "value": 69950.44994091918,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatch",
            "value": 6448.998162295428,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatchFlush",
            "value": 6514.204552827479,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncList",
            "value": 156288.5932497842,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncSet",
            "value": 69232.14596730749,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommands",
            "value": 90.01890071982955,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommandsAsBatch",
            "value": 370.7851308839164,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writeKeyedCommand",
            "value": 30.469188854794858,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writePlainCommand",
            "value": 7.71724014156302,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSet",
            "value": 85972.18746309227,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatch",
            "value": 7554.930541154536,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatchFlush",
            "value": 6456.3058690513935,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSet",
            "value": 88059.16121988413,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatch",
            "value": 8311.400641863465,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatchFlush",
            "value": 8399.769661643706,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.syncSet",
            "value": 87489.08668676045,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashDirect",
            "value": 40.641558960038296,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashHeap",
            "value": 37.03278524402121,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedDirect",
            "value": 28.34343220339366,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedHeap",
            "value": 31.48290778823292,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeAllSlots",
            "value": 64948.4698520068,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeLowerSlots",
            "value": 35695.50508599999,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusAbsent",
            "value": 0.9980787837563285,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusPresent",
            "value": 0.9849555192498898,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyEstimatedSize",
            "value": 211.8105304375702,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyExactSize",
            "value": 70.55444622087683,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueEstimatedSize",
            "value": 218.43844396423773,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueExactSize",
            "value": 73.3471156605689,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.decodeUtf8Unpooled",
            "value": 4.4082545316134505,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeAsciiToBuf",
            "value": 14.808831358621337,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeIsoToBuf",
            "value": 27.264227080169082,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8PlainStringToBuf",
            "value": 17.92130172662326,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8ToBuf",
            "value": 29.667769542659528,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8Unpooled",
            "value": 88.94004229027891,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.decodeUnpooled",
            "value": 4.418519387870059,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.encodeUnpooled",
            "value": 85.91647064095828,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createBatchCommands",
            "value": 849961.7878021741,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createRegularCommands",
            "value": 864893.5557724424,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeAsyncCommand",
            "value": 27.143530461012354,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeCommandInterfaceCommand",
            "value": 159.44833566749247,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.asyncSet",
            "value": 6409.968783519845,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.batchSet",
            "value": 5595.28130657855,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.intercept.InvocationProxyFactoryBenchmark.run",
            "value": 59.80501636746807,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100Elements",
            "value": 384.5145588114796,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100ElementsWithResizeElement",
            "value": 573.4611199821026,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16Elements",
            "value": 48.341807181974886,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16ElementsWithResizeElement",
            "value": 122.94128863137409,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureSingleElement",
            "value": 7.219300741024243,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureZeroElement",
            "value": 0.6337722902458274,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createAsyncCommandUsingByteArrayCodec",
            "value": 27.689459992759517,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingByteArrayCodec",
            "value": 23.02023833228146,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingStringCodec",
            "value": 13.088640803496123,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingByteArrayCodec",
            "value": 14.143926556525793,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingNewStringCodec",
            "value": 69.45938229288774,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingOldStringCodec",
            "value": 67.18105851706855,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndRead",
            "value": 210.5749487786514,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1",
            "value": 180.9606149198032,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch10",
            "value": 1646.8666485593476,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch100",
            "value": 13902.437489843465,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1000",
            "value": 153850.60706627174,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisEndpointBenchmark.measureUserWrite",
            "value": 36.87506077690379,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisStateMachineBenchmark.measureDecode",
            "value": 147.14049958124048,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.singleConnection",
            "value": 371.47007085226534,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.twentyConnections",
            "value": 7643.067145052744,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.singleConnection",
            "value": 670.69800336254,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.twentyConnections",
            "value": 13781.913981545582,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "dependabot[bot]",
            "username": "dependabot[bot]",
            "email": "49699333+dependabot[bot]@users.noreply.github.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "14de5b88f922581f996c3b3311cb253eb36b32db",
          "message": "Bump org.testcontainers:testcontainers from 1.20.1 to 1.20.4 (#3082)\n\nBumps [org.testcontainers:testcontainers](https://github.com/testcontainers/testcontainers-java) from 1.20.1 to 1.20.4.\r\n- [Release notes](https://github.com/testcontainers/testcontainers-java/releases)\r\n- [Changelog](https://github.com/testcontainers/testcontainers-java/blob/main/CHANGELOG.md)\r\n- [Commits](https://github.com/testcontainers/testcontainers-java/compare/1.20.1...1.20.4)\r\n\r\n---\r\nupdated-dependencies:\r\n- dependency-name: org.testcontainers:testcontainers\r\n  dependency-type: direct:development\r\n  update-type: version-update:semver-patch\r\n...\r\n\r\nSigned-off-by: dependabot[bot] <support@github.com>\r\nCo-authored-by: dependabot[bot] <49699333+dependabot[bot]@users.noreply.github.com>",
          "timestamp": "2024-12-16T14:59:31Z",
          "url": "https://github.com/redis/lettuce/commit/14de5b88f922581f996c3b3311cb253eb36b32db"
        },
        "date": 1734573581792,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSet",
            "value": 70108.53008198981,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatch",
            "value": 5643.564239138366,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatchFlush",
            "value": 5077.900842426459,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSet",
            "value": 70893.37854393864,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatch",
            "value": 6528.607283362146,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatchFlush",
            "value": 6506.166175652597,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncList",
            "value": 162229.84770518256,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncSet",
            "value": 70654.31499406837,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommands",
            "value": 89.32154040734207,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommandsAsBatch",
            "value": 368.1230140563769,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writeKeyedCommand",
            "value": 30.58643946655136,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writePlainCommand",
            "value": 7.862004681509532,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSet",
            "value": 87057.82871765597,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatch",
            "value": 7607.25793392346,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatchFlush",
            "value": 6506.560537149826,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSet",
            "value": 89333.37643422507,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatch",
            "value": 8511.713292989989,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatchFlush",
            "value": 8588.86660665811,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.syncSet",
            "value": 88564.10121583505,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashDirect",
            "value": 40.464670976029076,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashHeap",
            "value": 37.15007960325711,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedDirect",
            "value": 28.048075502037683,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedHeap",
            "value": 31.529852964053884,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeAllSlots",
            "value": 59498.283659194465,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeLowerSlots",
            "value": 35842.47168279137,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusAbsent",
            "value": 1.0010683345696267,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusPresent",
            "value": 0.9904895960161157,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyEstimatedSize",
            "value": 214.38237534971944,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyExactSize",
            "value": 70.2871455398109,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueEstimatedSize",
            "value": 213.44104300300123,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueExactSize",
            "value": 72.95404100892034,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.decodeUtf8Unpooled",
            "value": 4.4012512349767245,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeAsciiToBuf",
            "value": 14.910181249442138,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeIsoToBuf",
            "value": 27.79981363366803,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8PlainStringToBuf",
            "value": 18.111381730745972,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8ToBuf",
            "value": 29.70262280593704,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8Unpooled",
            "value": 86.19566347002907,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.decodeUnpooled",
            "value": 4.438375868719663,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.encodeUnpooled",
            "value": 85.92541459311502,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createBatchCommands",
            "value": 846554.7166564541,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createRegularCommands",
            "value": 848458.2778817083,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeAsyncCommand",
            "value": 27.298559345104756,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeCommandInterfaceCommand",
            "value": 161.81757034986614,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.asyncSet",
            "value": 6502.370925051237,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.batchSet",
            "value": 5563.934984842878,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.intercept.InvocationProxyFactoryBenchmark.run",
            "value": 59.110729097008345,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100Elements",
            "value": 385.1695998884648,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100ElementsWithResizeElement",
            "value": 572.9035511064765,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16Elements",
            "value": 48.348052489755446,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16ElementsWithResizeElement",
            "value": 122.72118340222796,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureSingleElement",
            "value": 6.846589500252817,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureZeroElement",
            "value": 0.6326122680930928,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createAsyncCommandUsingByteArrayCodec",
            "value": 27.667923789736157,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingByteArrayCodec",
            "value": 23.022220933414715,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingStringCodec",
            "value": 13.222875734135371,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingByteArrayCodec",
            "value": 14.152253830015031,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingNewStringCodec",
            "value": 67.13912468513851,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingOldStringCodec",
            "value": 69.10071427110758,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndRead",
            "value": 214.3156101228895,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1",
            "value": 168.27425312684898,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch10",
            "value": 1749.8899520694777,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch100",
            "value": 14101.554885315196,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1000",
            "value": 155424.02571086312,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisEndpointBenchmark.measureUserWrite",
            "value": 38.41756624398475,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisStateMachineBenchmark.measureDecode",
            "value": 148.31372997314915,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.singleConnection",
            "value": 365.42202457310776,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.twentyConnections",
            "value": 7618.071992048519,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.singleConnection",
            "value": 668.9663904609693,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.twentyConnections",
            "value": 13758.239333111012,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "dependabot[bot]",
            "username": "dependabot[bot]",
            "email": "49699333+dependabot[bot]@users.noreply.github.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "14de5b88f922581f996c3b3311cb253eb36b32db",
          "message": "Bump org.testcontainers:testcontainers from 1.20.1 to 1.20.4 (#3082)\n\nBumps [org.testcontainers:testcontainers](https://github.com/testcontainers/testcontainers-java) from 1.20.1 to 1.20.4.\r\n- [Release notes](https://github.com/testcontainers/testcontainers-java/releases)\r\n- [Changelog](https://github.com/testcontainers/testcontainers-java/blob/main/CHANGELOG.md)\r\n- [Commits](https://github.com/testcontainers/testcontainers-java/compare/1.20.1...1.20.4)\r\n\r\n---\r\nupdated-dependencies:\r\n- dependency-name: org.testcontainers:testcontainers\r\n  dependency-type: direct:development\r\n  update-type: version-update:semver-patch\r\n...\r\n\r\nSigned-off-by: dependabot[bot] <support@github.com>\r\nCo-authored-by: dependabot[bot] <49699333+dependabot[bot]@users.noreply.github.com>",
          "timestamp": "2024-12-16T14:59:31Z",
          "url": "https://github.com/redis/lettuce/commit/14de5b88f922581f996c3b3311cb253eb36b32db"
        },
        "date": 1734659968832,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSet",
            "value": 70349.72091919658,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatch",
            "value": 5620.325641947975,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatchFlush",
            "value": 5066.60775785063,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSet",
            "value": 71691.03279615355,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatch",
            "value": 6500.080330666479,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatchFlush",
            "value": 6539.889801331223,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncList",
            "value": 161891.73018533253,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncSet",
            "value": 71663.6229711408,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommands",
            "value": 89.80270443032259,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommandsAsBatch",
            "value": 368.2889715101718,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writeKeyedCommand",
            "value": 30.982114223968317,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writePlainCommand",
            "value": 7.71224110931765,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSet",
            "value": 89149.44212278795,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatch",
            "value": 7629.87552602269,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatchFlush",
            "value": 6556.473588505655,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSet",
            "value": 90028.76359455331,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatch",
            "value": 8310.38470518111,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatchFlush",
            "value": 8515.705693663323,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.syncSet",
            "value": 89369.9933165638,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashDirect",
            "value": 40.40088447242035,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashHeap",
            "value": 37.2516023297287,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedDirect",
            "value": 28.135083509749933,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedHeap",
            "value": 31.141820091420744,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeAllSlots",
            "value": 65038.19551438448,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeLowerSlots",
            "value": 35780.745636801956,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusAbsent",
            "value": 0.9994263032473375,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusPresent",
            "value": 0.9892446034454853,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyEstimatedSize",
            "value": 202.28821547226647,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyExactSize",
            "value": 70.14264601686853,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueEstimatedSize",
            "value": 202.5854929382311,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueExactSize",
            "value": 72.92253642038489,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.decodeUtf8Unpooled",
            "value": 4.387339018941372,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeAsciiToBuf",
            "value": 14.938088709627156,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeIsoToBuf",
            "value": 30.024719388608123,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8PlainStringToBuf",
            "value": 17.94363158324506,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8ToBuf",
            "value": 29.63531724629354,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8Unpooled",
            "value": 85.80277891465587,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.decodeUnpooled",
            "value": 4.403516162760682,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.encodeUnpooled",
            "value": 86.59391063961841,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createBatchCommands",
            "value": 840598.2778186143,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createRegularCommands",
            "value": 851146.2339352441,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeAsyncCommand",
            "value": 27.11663909900737,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeCommandInterfaceCommand",
            "value": 165.085142023402,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.asyncSet",
            "value": 6303.059591193492,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.batchSet",
            "value": 5744.764142174417,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.intercept.InvocationProxyFactoryBenchmark.run",
            "value": 54.972810693742986,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100Elements",
            "value": 386.22672844234796,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100ElementsWithResizeElement",
            "value": 573.8217393137514,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16Elements",
            "value": 48.620656628482905,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16ElementsWithResizeElement",
            "value": 123.51649695766794,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureSingleElement",
            "value": 6.8435426779969974,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureZeroElement",
            "value": 0.6303691739497869,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createAsyncCommandUsingByteArrayCodec",
            "value": 27.652617152342167,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingByteArrayCodec",
            "value": 22.96971275255708,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingStringCodec",
            "value": 13.094832096549439,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingByteArrayCodec",
            "value": 14.142972444436339,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingNewStringCodec",
            "value": 69.1992365680126,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingOldStringCodec",
            "value": 75.13701007745662,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndRead",
            "value": 226.38391809336295,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1",
            "value": 167.73554275377592,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch10",
            "value": 1499.4421752100727,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch100",
            "value": 16280.610885379618,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1000",
            "value": 144058.0334809169,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisEndpointBenchmark.measureUserWrite",
            "value": 40.05930432108158,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisStateMachineBenchmark.measureDecode",
            "value": 147.3189852223298,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.singleConnection",
            "value": 350.73003599744953,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.twentyConnections",
            "value": 7372.834956481539,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.singleConnection",
            "value": 625.8483900911391,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.twentyConnections",
            "value": 13529.992150934704,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "KIM MIN WOO",
            "username": "minwoo1999",
            "email": "79193811+minwoo1999@users.noreply.github.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "a751b7c9948435a0106fb81bdc93f41fc40d5a24",
          "message": "replace hardcoded GT and LT with CommandKeyword enum (#3079)",
          "timestamp": "2024-12-20T19:55:35Z",
          "url": "https://github.com/redis/lettuce/commit/a751b7c9948435a0106fb81bdc93f41fc40d5a24"
        },
        "date": 1734777600532,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSet",
            "value": 69562.45987605758,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatch",
            "value": 5603.9317649328095,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatchFlush",
            "value": 5063.748492700989,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSet",
            "value": 71319.2786525448,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatch",
            "value": 6406.105354469442,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatchFlush",
            "value": 6336.457773149703,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncList",
            "value": 158813.25415720599,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncSet",
            "value": 68327.65901528338,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommands",
            "value": 89.49262040824112,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommandsAsBatch",
            "value": 408.4693175844888,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writeKeyedCommand",
            "value": 31.102514643324405,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writePlainCommand",
            "value": 9.014026492314425,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSet",
            "value": 88352.67994060682,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatch",
            "value": 7658.265878737128,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatchFlush",
            "value": 6511.417756885121,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSet",
            "value": 90014.33296757432,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatch",
            "value": 8336.046843487948,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatchFlush",
            "value": 8512.538902603756,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.syncSet",
            "value": 90698.96053087673,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashDirect",
            "value": 40.369780828242064,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashHeap",
            "value": 37.16164159891866,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedDirect",
            "value": 28.483769114466092,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedHeap",
            "value": 31.210214516177274,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeAllSlots",
            "value": 26851.181130255245,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeLowerSlots",
            "value": 32830.96762877585,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusAbsent",
            "value": 1.0037981448558564,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusPresent",
            "value": 0.9914124381825772,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyEstimatedSize",
            "value": 198.58023400063558,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyExactSize",
            "value": 70.63157993732773,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueEstimatedSize",
            "value": 206.50923415218148,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueExactSize",
            "value": 72.65569114517041,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.decodeUtf8Unpooled",
            "value": 4.407925933286936,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeAsciiToBuf",
            "value": 14.867163386436058,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeIsoToBuf",
            "value": 27.196633847153983,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8PlainStringToBuf",
            "value": 17.985655100032858,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8ToBuf",
            "value": 29.570332923223283,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8Unpooled",
            "value": 85.74333650148057,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.decodeUnpooled",
            "value": 4.41280526400614,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.encodeUnpooled",
            "value": 87.26632197574433,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createBatchCommands",
            "value": 843177.9139879681,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createRegularCommands",
            "value": 854852.0754721748,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeAsyncCommand",
            "value": 27.24579895768894,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeCommandInterfaceCommand",
            "value": 171.53174632120442,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.asyncSet",
            "value": 6403.272861534962,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.batchSet",
            "value": 5657.916768988546,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.intercept.InvocationProxyFactoryBenchmark.run",
            "value": 54.43687628225486,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100Elements",
            "value": 387.55053076614684,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100ElementsWithResizeElement",
            "value": 568.5085740305769,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16Elements",
            "value": 48.408989170824654,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16ElementsWithResizeElement",
            "value": 122.57813141861172,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureSingleElement",
            "value": 6.864679982122068,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureZeroElement",
            "value": 0.6358888601309248,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createAsyncCommandUsingByteArrayCodec",
            "value": 27.63358015973646,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingByteArrayCodec",
            "value": 22.982376679829752,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingStringCodec",
            "value": 13.250308827460248,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingByteArrayCodec",
            "value": 14.14355209737495,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingNewStringCodec",
            "value": 69.26967005663293,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingOldStringCodec",
            "value": 69.91871312673761,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndRead",
            "value": 218.906918941707,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1",
            "value": 176.55554788089316,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch10",
            "value": 1561.3676526713452,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch100",
            "value": 16176.874923038238,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1000",
            "value": 156224.171001974,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisEndpointBenchmark.measureUserWrite",
            "value": 36.875923893683215,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisStateMachineBenchmark.measureDecode",
            "value": 148.90340917355053,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.singleConnection",
            "value": 358.55439389737387,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.twentyConnections",
            "value": 7251.898494935881,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.singleConnection",
            "value": 646.4587453435337,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.twentyConnections",
            "value": 14097.041203745644,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "KIM MIN WOO",
            "username": "minwoo1999",
            "email": "79193811+minwoo1999@users.noreply.github.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "a751b7c9948435a0106fb81bdc93f41fc40d5a24",
          "message": "replace hardcoded GT and LT with CommandKeyword enum (#3079)",
          "timestamp": "2024-12-20T19:55:35Z",
          "url": "https://github.com/redis/lettuce/commit/a751b7c9948435a0106fb81bdc93f41fc40d5a24"
        },
        "date": 1734832746273,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSet",
            "value": 69564.27051514112,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatch",
            "value": 5598.525768484195,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatchFlush",
            "value": 5183.138219123313,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSet",
            "value": 71192.69461993559,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatch",
            "value": 6429.200518418059,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatchFlush",
            "value": 6417.509957861574,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncList",
            "value": 158639.5124296114,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncSet",
            "value": 71213.92252465007,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommands",
            "value": 90.30730642514145,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommandsAsBatch",
            "value": 375.69572813788125,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writeKeyedCommand",
            "value": 31.491267797494753,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writePlainCommand",
            "value": 7.738817434704984,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSet",
            "value": 89658.00471810727,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatch",
            "value": 7569.456627130241,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatchFlush",
            "value": 6533.664015981642,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSet",
            "value": 90018.97340931316,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatch",
            "value": 8400.685305887217,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatchFlush",
            "value": 8493.13246171503,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.syncSet",
            "value": 89580.50395442554,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashDirect",
            "value": 40.45879673856485,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashHeap",
            "value": 37.176149659076245,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedDirect",
            "value": 28.544428893890107,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedHeap",
            "value": 31.497165241101676,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeAllSlots",
            "value": 66119.76342068503,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeLowerSlots",
            "value": 32815.51186511739,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusAbsent",
            "value": 0.9996953064097548,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusPresent",
            "value": 0.9879063952812505,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyEstimatedSize",
            "value": 203.62819661493234,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyExactSize",
            "value": 70.33804057430358,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueEstimatedSize",
            "value": 222.45943892691193,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueExactSize",
            "value": 73.59874016364022,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.decodeUtf8Unpooled",
            "value": 4.390216518672171,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeAsciiToBuf",
            "value": 14.833352144980958,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeIsoToBuf",
            "value": 26.889475566171217,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8PlainStringToBuf",
            "value": 17.975648772512745,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8ToBuf",
            "value": 29.777464730279757,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8Unpooled",
            "value": 85.82022648917516,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.decodeUnpooled",
            "value": 4.38763033663088,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.encodeUnpooled",
            "value": 89.29348947594059,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createBatchCommands",
            "value": 831664.5325176292,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createRegularCommands",
            "value": 857864.7313074789,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeAsyncCommand",
            "value": 27.009555982728028,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeCommandInterfaceCommand",
            "value": 167.01606629301554,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.asyncSet",
            "value": 6495.16715730194,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.batchSet",
            "value": 5662.196029200329,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.intercept.InvocationProxyFactoryBenchmark.run",
            "value": 53.60305164227479,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100Elements",
            "value": 395.8598996615659,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100ElementsWithResizeElement",
            "value": 574.4669969479094,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16Elements",
            "value": 49.017321592526194,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16ElementsWithResizeElement",
            "value": 122.62287689808693,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureSingleElement",
            "value": 6.878159286751561,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureZeroElement",
            "value": 0.6308552676541277,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createAsyncCommandUsingByteArrayCodec",
            "value": 27.700500975758246,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingByteArrayCodec",
            "value": 22.992160571297223,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingStringCodec",
            "value": 13.1621078474987,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingByteArrayCodec",
            "value": 14.16007896065607,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingNewStringCodec",
            "value": 69.29839776930271,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingOldStringCodec",
            "value": 69.50571890575854,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndRead",
            "value": 222.27723527706917,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1",
            "value": 172.9544972086571,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch10",
            "value": 1754.1041188698066,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch100",
            "value": 16093.286585344718,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1000",
            "value": 140415.58281603287,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisEndpointBenchmark.measureUserWrite",
            "value": 36.74858749423088,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisStateMachineBenchmark.measureDecode",
            "value": 150.34150610075423,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.singleConnection",
            "value": 364.2357467485517,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.twentyConnections",
            "value": 7396.6815232474455,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.singleConnection",
            "value": 693.5700177396245,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.twentyConnections",
            "value": 13416.285829078628,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "KIM MIN WOO",
            "username": "minwoo1999",
            "email": "79193811+minwoo1999@users.noreply.github.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "a751b7c9948435a0106fb81bdc93f41fc40d5a24",
          "message": "replace hardcoded GT and LT with CommandKeyword enum (#3079)",
          "timestamp": "2024-12-20T19:55:35Z",
          "url": "https://github.com/redis/lettuce/commit/a751b7c9948435a0106fb81bdc93f41fc40d5a24"
        },
        "date": 1734919181365,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSet",
            "value": 70919.78855448829,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatch",
            "value": 5661.716533682504,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatchFlush",
            "value": 5073.499621684996,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSet",
            "value": 70764.61194613915,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatch",
            "value": 6503.735660771441,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatchFlush",
            "value": 6497.286894792187,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncList",
            "value": 161436.19108411603,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncSet",
            "value": 69971.34674068665,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommands",
            "value": 89.54592909291765,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommandsAsBatch",
            "value": 371.25730355057914,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writeKeyedCommand",
            "value": 30.650036889460416,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writePlainCommand",
            "value": 7.732918111550822,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSet",
            "value": 89105.27822808418,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatch",
            "value": 7648.951965264253,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatchFlush",
            "value": 6532.438327381862,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSet",
            "value": 90687.78929251079,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatch",
            "value": 8482.303936688997,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatchFlush",
            "value": 8598.709720722401,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.syncSet",
            "value": 89684.48103306467,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashDirect",
            "value": 40.38181612246892,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashHeap",
            "value": 37.10010048721736,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedDirect",
            "value": 29.28653233620952,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedHeap",
            "value": 31.544796407397815,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeAllSlots",
            "value": 26883.19703045768,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeLowerSlots",
            "value": 13405.466300982353,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusAbsent",
            "value": 0.99710992913607,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusPresent",
            "value": 0.991763250509563,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyEstimatedSize",
            "value": 201.79744474088716,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyExactSize",
            "value": 74.46523474980071,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueEstimatedSize",
            "value": 227.17484935872994,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueExactSize",
            "value": 73.33533450421513,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.decodeUtf8Unpooled",
            "value": 4.412832574733573,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeAsciiToBuf",
            "value": 14.860425370995221,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeIsoToBuf",
            "value": 27.33412754362926,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8PlainStringToBuf",
            "value": 17.98279777132806,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8ToBuf",
            "value": 29.565871918181323,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8Unpooled",
            "value": 85.8606151274743,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.decodeUnpooled",
            "value": 4.393104611816285,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.encodeUnpooled",
            "value": 86.94502150523267,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createBatchCommands",
            "value": 861545.5637458215,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createRegularCommands",
            "value": 850171.4669586153,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeAsyncCommand",
            "value": 27.118086156338773,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeCommandInterfaceCommand",
            "value": 171.21083440666376,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.asyncSet",
            "value": 6473.828168518335,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.batchSet",
            "value": 5748.244530822691,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.intercept.InvocationProxyFactoryBenchmark.run",
            "value": 55.85000177792885,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100Elements",
            "value": 384.3593263367921,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100ElementsWithResizeElement",
            "value": 571.577644926866,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16Elements",
            "value": 49.22344235343851,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16ElementsWithResizeElement",
            "value": 122.83584337607631,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureSingleElement",
            "value": 6.874224809687346,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureZeroElement",
            "value": 0.6315537944876655,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createAsyncCommandUsingByteArrayCodec",
            "value": 27.774032697707106,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingByteArrayCodec",
            "value": 23.044184616939713,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingStringCodec",
            "value": 13.186295604408073,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingByteArrayCodec",
            "value": 14.1533299825719,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingNewStringCodec",
            "value": 69.05004854243751,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingOldStringCodec",
            "value": 69.50164475982473,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndRead",
            "value": 222.29284090006354,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1",
            "value": 186.85809718329287,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch10",
            "value": 1566.990450338275,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch100",
            "value": 13353.086348965484,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1000",
            "value": 156041.51531933594,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisEndpointBenchmark.measureUserWrite",
            "value": 36.779290646547416,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisStateMachineBenchmark.measureDecode",
            "value": 148.74355934322784,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.singleConnection",
            "value": 367.7318898460698,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.twentyConnections",
            "value": 7498.4006324005095,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.singleConnection",
            "value": 679.23436432407,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.twentyConnections",
            "value": 13469.590910450506,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "dependabot[bot]",
            "username": "dependabot[bot]",
            "email": "49699333+dependabot[bot]@users.noreply.github.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "f48b43d79cee0b132c6f14027492452276d8d7f3",
          "message": "Bump io.micrometer:micrometer-bom from 1.12.4 to 1.14.2 (#3096)\n\nBumps [io.micrometer:micrometer-bom](https://github.com/micrometer-metrics/micrometer) from 1.12.4 to 1.14.2.\r\n- [Release notes](https://github.com/micrometer-metrics/micrometer/releases)\r\n- [Commits](https://github.com/micrometer-metrics/micrometer/compare/v1.12.4...v1.14.2)\r\n\r\n---\r\nupdated-dependencies:\r\n- dependency-name: io.micrometer:micrometer-bom\r\n  dependency-type: direct:production\r\n  update-type: version-update:semver-minor\r\n...\r\n\r\nSigned-off-by: dependabot[bot] <support@github.com>\r\nCo-authored-by: dependabot[bot] <49699333+dependabot[bot]@users.noreply.github.com>",
          "timestamp": "2024-12-23T14:05:38Z",
          "url": "https://github.com/redis/lettuce/commit/f48b43d79cee0b132c6f14027492452276d8d7f3"
        },
        "date": 1735005549868,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSet",
            "value": 70754.93610157855,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatch",
            "value": 5671.989412465223,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatchFlush",
            "value": 5091.63061567919,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSet",
            "value": 70623.95052842557,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatch",
            "value": 6396.067122045957,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatchFlush",
            "value": 6529.370361086925,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncList",
            "value": 161982.39326564368,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncSet",
            "value": 70505.93656211211,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommands",
            "value": 89.38836047492254,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommandsAsBatch",
            "value": 371.20189211978175,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writeKeyedCommand",
            "value": 31.02374196387056,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writePlainCommand",
            "value": 9.039316776267635,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSet",
            "value": 89021.97327760904,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatch",
            "value": 7513.476127227838,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatchFlush",
            "value": 6479.092171379505,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSet",
            "value": 90567.90950007757,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatch",
            "value": 8341.966364176553,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatchFlush",
            "value": 8312.643702925603,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.syncSet",
            "value": 88400.3807476019,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashDirect",
            "value": 40.471766348967755,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashHeap",
            "value": 37.213900725357384,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedDirect",
            "value": 28.47426356695636,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedHeap",
            "value": 31.168164998506207,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeAllSlots",
            "value": 65964.43933764935,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeLowerSlots",
            "value": 35945.67407123481,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusAbsent",
            "value": 0.9983506129495321,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusPresent",
            "value": 0.9935644864714408,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyEstimatedSize",
            "value": 204.5384751684101,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyExactSize",
            "value": 70.53133928125051,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueEstimatedSize",
            "value": 214.38916046064523,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueExactSize",
            "value": 73.03786611750097,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.decodeUtf8Unpooled",
            "value": 4.404605647202646,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeAsciiToBuf",
            "value": 14.929907921621966,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeIsoToBuf",
            "value": 28.045935333797456,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8PlainStringToBuf",
            "value": 18.11618642331517,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8ToBuf",
            "value": 29.87209493328755,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8Unpooled",
            "value": 86.85727097626574,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.decodeUnpooled",
            "value": 4.397054772435913,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.encodeUnpooled",
            "value": 85.96323712596977,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createBatchCommands",
            "value": 849988.7459769467,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createRegularCommands",
            "value": 852727.4540184934,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeAsyncCommand",
            "value": 27.18193949726203,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeCommandInterfaceCommand",
            "value": 158.38193763007087,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.asyncSet",
            "value": 6306.953743116867,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.batchSet",
            "value": 5704.187739703697,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.intercept.InvocationProxyFactoryBenchmark.run",
            "value": 56.00593591677084,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100Elements",
            "value": 386.35771128939757,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100ElementsWithResizeElement",
            "value": 575.2216391723333,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16Elements",
            "value": 48.54938917878026,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16ElementsWithResizeElement",
            "value": 123.78326063106915,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureSingleElement",
            "value": 6.8620320749894645,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureZeroElement",
            "value": 0.6343073521262328,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createAsyncCommandUsingByteArrayCodec",
            "value": 27.714740662859857,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingByteArrayCodec",
            "value": 23.02058572133709,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingStringCodec",
            "value": 13.261067253904619,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingByteArrayCodec",
            "value": 14.227038497331574,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingNewStringCodec",
            "value": 80.20184550841194,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingOldStringCodec",
            "value": 67.37158878241834,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndRead",
            "value": 209.81635148154197,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1",
            "value": 176.39700117535637,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch10",
            "value": 1606.743394977222,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch100",
            "value": 16268.689369098274,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1000",
            "value": 147749.1440691216,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisEndpointBenchmark.measureUserWrite",
            "value": 36.76515889154477,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisStateMachineBenchmark.measureDecode",
            "value": 151.99671316723942,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.singleConnection",
            "value": 357.62341217643024,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.twentyConnections",
            "value": 7596.0621828202275,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.singleConnection",
            "value": 691.1718857758427,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.twentyConnections",
            "value": 13050.290851139533,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "dependabot[bot]",
            "username": "dependabot[bot]",
            "email": "49699333+dependabot[bot]@users.noreply.github.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "f48b43d79cee0b132c6f14027492452276d8d7f3",
          "message": "Bump io.micrometer:micrometer-bom from 1.12.4 to 1.14.2 (#3096)\n\nBumps [io.micrometer:micrometer-bom](https://github.com/micrometer-metrics/micrometer) from 1.12.4 to 1.14.2.\r\n- [Release notes](https://github.com/micrometer-metrics/micrometer/releases)\r\n- [Commits](https://github.com/micrometer-metrics/micrometer/compare/v1.12.4...v1.14.2)\r\n\r\n---\r\nupdated-dependencies:\r\n- dependency-name: io.micrometer:micrometer-bom\r\n  dependency-type: direct:production\r\n  update-type: version-update:semver-minor\r\n...\r\n\r\nSigned-off-by: dependabot[bot] <support@github.com>\r\nCo-authored-by: dependabot[bot] <49699333+dependabot[bot]@users.noreply.github.com>",
          "timestamp": "2024-12-23T14:05:38Z",
          "url": "https://github.com/redis/lettuce/commit/f48b43d79cee0b132c6f14027492452276d8d7f3"
        },
        "date": 1735091945677,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSet",
            "value": 69911.91087088952,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatch",
            "value": 5593.239899584397,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatchFlush",
            "value": 5110.88187236835,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSet",
            "value": 71689.52081076047,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatch",
            "value": 6433.931673417386,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatchFlush",
            "value": 6548.2928736458425,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncList",
            "value": 160526.37046245037,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncSet",
            "value": 70561.79156589537,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommands",
            "value": 89.2302071435825,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommandsAsBatch",
            "value": 372.26528439759966,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writeKeyedCommand",
            "value": 31.184374684202812,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writePlainCommand",
            "value": 9.021241742086945,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSet",
            "value": 88846.91314120106,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatch",
            "value": 7656.120319704724,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatchFlush",
            "value": 6524.832904580459,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSet",
            "value": 89904.97351026474,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatch",
            "value": 8482.254306275197,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatchFlush",
            "value": 8434.906819235413,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.syncSet",
            "value": 89587.7934670118,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashDirect",
            "value": 40.382830642848084,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashHeap",
            "value": 37.1141693684235,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedDirect",
            "value": 28.41351250904583,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedHeap",
            "value": 31.559105717736013,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeAllSlots",
            "value": 59644.346225092115,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeLowerSlots",
            "value": 13394.709228663782,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusAbsent",
            "value": 0.9905291656074983,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusPresent",
            "value": 0.9891312296526676,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyEstimatedSize",
            "value": 228.328888458751,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyExactSize",
            "value": 70.57770180627958,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueEstimatedSize",
            "value": 217.70215442413945,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueExactSize",
            "value": 72.1543971670718,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.decodeUtf8Unpooled",
            "value": 4.394094430213513,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeAsciiToBuf",
            "value": 14.86059255839226,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeIsoToBuf",
            "value": 27.627312142531373,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8PlainStringToBuf",
            "value": 17.947144727644606,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8ToBuf",
            "value": 35.846299549575996,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8Unpooled",
            "value": 86.47458446060907,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.decodeUnpooled",
            "value": 4.401189077634896,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.encodeUnpooled",
            "value": 86.4076368847429,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createBatchCommands",
            "value": 853085.0572409129,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createRegularCommands",
            "value": 844358.0666184897,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeAsyncCommand",
            "value": 27.193933875000777,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeCommandInterfaceCommand",
            "value": 166.58545152102334,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.asyncSet",
            "value": 6381.5277594704185,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.batchSet",
            "value": 5656.799146132511,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.intercept.InvocationProxyFactoryBenchmark.run",
            "value": 54.43083295166249,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100Elements",
            "value": 385.19732321090004,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100ElementsWithResizeElement",
            "value": 572.7999557930596,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16Elements",
            "value": 48.59198839724739,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16ElementsWithResizeElement",
            "value": 123.12902491038713,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureSingleElement",
            "value": 6.882573268847966,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureZeroElement",
            "value": 0.6303222666527534,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createAsyncCommandUsingByteArrayCodec",
            "value": 27.735061773736668,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingByteArrayCodec",
            "value": 23.08689166170272,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingStringCodec",
            "value": 13.172077518002496,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingByteArrayCodec",
            "value": 14.181941492956565,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingNewStringCodec",
            "value": 68.15912763377764,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingOldStringCodec",
            "value": 69.92410983305929,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndRead",
            "value": 207.41432884500074,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1",
            "value": 164.26825504132628,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch10",
            "value": 1768.6423074557183,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch100",
            "value": 14173.40335248537,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1000",
            "value": 154474.23742223842,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisEndpointBenchmark.measureUserWrite",
            "value": 36.80859338244565,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisStateMachineBenchmark.measureDecode",
            "value": 147.4366360774048,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.singleConnection",
            "value": 363.68710428925476,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.twentyConnections",
            "value": 7327.989205812066,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.singleConnection",
            "value": 681.0771831654587,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.twentyConnections",
            "value": 13795.525410297423,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "dependabot[bot]",
            "username": "dependabot[bot]",
            "email": "49699333+dependabot[bot]@users.noreply.github.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "f48b43d79cee0b132c6f14027492452276d8d7f3",
          "message": "Bump io.micrometer:micrometer-bom from 1.12.4 to 1.14.2 (#3096)\n\nBumps [io.micrometer:micrometer-bom](https://github.com/micrometer-metrics/micrometer) from 1.12.4 to 1.14.2.\r\n- [Release notes](https://github.com/micrometer-metrics/micrometer/releases)\r\n- [Commits](https://github.com/micrometer-metrics/micrometer/compare/v1.12.4...v1.14.2)\r\n\r\n---\r\nupdated-dependencies:\r\n- dependency-name: io.micrometer:micrometer-bom\r\n  dependency-type: direct:production\r\n  update-type: version-update:semver-minor\r\n...\r\n\r\nSigned-off-by: dependabot[bot] <support@github.com>\r\nCo-authored-by: dependabot[bot] <49699333+dependabot[bot]@users.noreply.github.com>",
          "timestamp": "2024-12-23T14:05:38Z",
          "url": "https://github.com/redis/lettuce/commit/f48b43d79cee0b132c6f14027492452276d8d7f3"
        },
        "date": 1735178371533,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSet",
            "value": 72494.70375022627,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatch",
            "value": 5725.797017523353,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatchFlush",
            "value": 5280.004515722537,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSet",
            "value": 73012.76629258893,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatch",
            "value": 6659.627792668258,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatchFlush",
            "value": 6681.091365730717,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncList",
            "value": 163829.47577250455,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncSet",
            "value": 73825.16607366216,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommands",
            "value": 89.50382787456608,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommandsAsBatch",
            "value": 373.22422198346396,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writeKeyedCommand",
            "value": 31.083390601789382,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writePlainCommand",
            "value": 7.733620046028115,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSet",
            "value": 90356.33016473224,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatch",
            "value": 7662.192133191423,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatchFlush",
            "value": 6599.936023935314,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSet",
            "value": 92534.84366472329,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatch",
            "value": 8612.494236746432,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatchFlush",
            "value": 8407.268802224442,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.syncSet",
            "value": 91499.66355454507,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashDirect",
            "value": 40.41015076720995,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashHeap",
            "value": 37.042685522279314,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedDirect",
            "value": 28.433814202568083,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedHeap",
            "value": 31.61685697998045,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeAllSlots",
            "value": 65210.962434691945,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeLowerSlots",
            "value": 13355.6432124063,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusAbsent",
            "value": 1.0011174517040975,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusPresent",
            "value": 0.9868563015607391,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyEstimatedSize",
            "value": 210.22418967821483,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyExactSize",
            "value": 71.11992107222457,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueEstimatedSize",
            "value": 213.09686825835965,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueExactSize",
            "value": 73.74423453553852,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.decodeUtf8Unpooled",
            "value": 4.400629079007054,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeAsciiToBuf",
            "value": 14.915171090489869,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeIsoToBuf",
            "value": 27.29362764524082,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8PlainStringToBuf",
            "value": 17.943995420498148,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8ToBuf",
            "value": 36.011460075891584,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8Unpooled",
            "value": 89.31499356751158,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.decodeUnpooled",
            "value": 4.392332830749268,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.encodeUnpooled",
            "value": 85.70145024918651,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createBatchCommands",
            "value": 847602.0518601045,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createRegularCommands",
            "value": 847146.7264626808,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeAsyncCommand",
            "value": 27.165768075525328,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeCommandInterfaceCommand",
            "value": 160.03475792368843,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.asyncSet",
            "value": 6578.052214060209,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.batchSet",
            "value": 5720.591741212713,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.intercept.InvocationProxyFactoryBenchmark.run",
            "value": 55.61313804024754,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100Elements",
            "value": 386.35223972266584,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100ElementsWithResizeElement",
            "value": 574.5513444599981,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16Elements",
            "value": 48.31611262112064,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16ElementsWithResizeElement",
            "value": 123.20440472653343,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureSingleElement",
            "value": 6.832870172726418,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureZeroElement",
            "value": 0.6311569281407472,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createAsyncCommandUsingByteArrayCodec",
            "value": 27.66909626658383,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingByteArrayCodec",
            "value": 23.061288073882622,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingStringCodec",
            "value": 13.144799951894706,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingByteArrayCodec",
            "value": 14.119993702287962,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingNewStringCodec",
            "value": 67.08531583933879,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingOldStringCodec",
            "value": 67.40850882483048,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndRead",
            "value": 227.12188238149034,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1",
            "value": 157.09640142076046,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch10",
            "value": 1768.667737117376,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch100",
            "value": 13990.831347054564,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1000",
            "value": 158710.732760144,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisEndpointBenchmark.measureUserWrite",
            "value": 36.792516304789046,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisStateMachineBenchmark.measureDecode",
            "value": 161.75212205243093,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.singleConnection",
            "value": 359.54668688522327,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.twentyConnections",
            "value": 7484.794627036351,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.singleConnection",
            "value": 695.0695323587553,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.twentyConnections",
            "value": 14297.447268825132,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "dependabot[bot]",
            "username": "dependabot[bot]",
            "email": "49699333+dependabot[bot]@users.noreply.github.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "f48b43d79cee0b132c6f14027492452276d8d7f3",
          "message": "Bump io.micrometer:micrometer-bom from 1.12.4 to 1.14.2 (#3096)\n\nBumps [io.micrometer:micrometer-bom](https://github.com/micrometer-metrics/micrometer) from 1.12.4 to 1.14.2.\r\n- [Release notes](https://github.com/micrometer-metrics/micrometer/releases)\r\n- [Commits](https://github.com/micrometer-metrics/micrometer/compare/v1.12.4...v1.14.2)\r\n\r\n---\r\nupdated-dependencies:\r\n- dependency-name: io.micrometer:micrometer-bom\r\n  dependency-type: direct:production\r\n  update-type: version-update:semver-minor\r\n...\r\n\r\nSigned-off-by: dependabot[bot] <support@github.com>\r\nCo-authored-by: dependabot[bot] <49699333+dependabot[bot]@users.noreply.github.com>",
          "timestamp": "2024-12-23T14:05:38Z",
          "url": "https://github.com/redis/lettuce/commit/f48b43d79cee0b132c6f14027492452276d8d7f3"
        },
        "date": 1735264771002,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSet",
            "value": 70177.39643142291,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatch",
            "value": 5593.706868835909,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatchFlush",
            "value": 5014.499942013661,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSet",
            "value": 71249.27698131267,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatch",
            "value": 6353.04810487163,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatchFlush",
            "value": 6337.61761307853,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncList",
            "value": 158071.3686677096,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncSet",
            "value": 70295.81271190968,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommands",
            "value": 89.38417245257713,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommandsAsBatch",
            "value": 371.80147338407335,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writeKeyedCommand",
            "value": 31.029322092783673,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writePlainCommand",
            "value": 7.753257783140022,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSet",
            "value": 87583.5838866787,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatch",
            "value": 7592.8435822098345,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatchFlush",
            "value": 6433.760064950348,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSet",
            "value": 88817.61542918361,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatch",
            "value": 8314.02605506265,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatchFlush",
            "value": 8352.56955329706,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.syncSet",
            "value": 88744.91277526955,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashDirect",
            "value": 40.34705370746157,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashHeap",
            "value": 37.101733660164385,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedDirect",
            "value": 28.036101603695037,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedHeap",
            "value": 31.08937910646471,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeAllSlots",
            "value": 59839.11088319759,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeLowerSlots",
            "value": 35717.79186793178,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusAbsent",
            "value": 1.0060132299229658,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusPresent",
            "value": 0.9922418271211011,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyEstimatedSize",
            "value": 216.60481600871327,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyExactSize",
            "value": 67.11619889155776,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueEstimatedSize",
            "value": 237.80444556702815,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueExactSize",
            "value": 67.27355471152384,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.decodeUtf8Unpooled",
            "value": 4.389123643836735,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeAsciiToBuf",
            "value": 14.961295772632983,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeIsoToBuf",
            "value": 27.049471170569735,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8PlainStringToBuf",
            "value": 17.94458596965807,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8ToBuf",
            "value": 29.895550958745524,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8Unpooled",
            "value": 85.71172636951408,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.decodeUnpooled",
            "value": 4.401519479388001,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.encodeUnpooled",
            "value": 86.42387357965828,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createBatchCommands",
            "value": 840474.2780338048,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createRegularCommands",
            "value": 833680.6058722397,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeAsyncCommand",
            "value": 27.234975085141677,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeCommandInterfaceCommand",
            "value": 161.27458591021758,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.asyncSet",
            "value": 6347.797665365584,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.batchSet",
            "value": 5642.515574688392,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.intercept.InvocationProxyFactoryBenchmark.run",
            "value": 54.18662887694918,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100Elements",
            "value": 385.07717745135005,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100ElementsWithResizeElement",
            "value": 575.25415897389,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16Elements",
            "value": 48.82755502146012,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16ElementsWithResizeElement",
            "value": 123.8427942459385,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureSingleElement",
            "value": 6.890183673265146,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureZeroElement",
            "value": 0.6315261870876491,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createAsyncCommandUsingByteArrayCodec",
            "value": 27.66253659530084,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingByteArrayCodec",
            "value": 23.069268329320565,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingStringCodec",
            "value": 13.084997741452955,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingByteArrayCodec",
            "value": 14.13199280110553,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingNewStringCodec",
            "value": 68.76341796487579,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingOldStringCodec",
            "value": 69.74998204451639,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndRead",
            "value": 220.8844639785047,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1",
            "value": 168.94568578101877,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch10",
            "value": 1504.3144516673358,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch100",
            "value": 13809.919449679273,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1000",
            "value": 135884.46662435285,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisEndpointBenchmark.measureUserWrite",
            "value": 36.86947623343176,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisStateMachineBenchmark.measureDecode",
            "value": 150.73208240068192,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.singleConnection",
            "value": 361.22917365701755,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.twentyConnections",
            "value": 7431.410977898493,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.singleConnection",
            "value": 699.7693958840507,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.twentyConnections",
            "value": 12784.40715000382,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "psw0946",
            "username": "psw0946",
            "email": "31843235+psw0946@users.noreply.github.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "b925816f773d2aba38614c9df7118765932ec54f",
          "message": "Add support up to max unsigned integer in Bitfield offset (#2964) (#3099)\n\n* Add support up to max unsigned integer in Bitfield offset (#2964)\r\n\r\n* Add test for reactive command",
          "timestamp": "2024-12-27T09:42:43Z",
          "url": "https://github.com/redis/lettuce/commit/b925816f773d2aba38614c9df7118765932ec54f"
        },
        "date": 1735351116006,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSet",
            "value": 70534.57297641455,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatch",
            "value": 5608.863504841824,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatchFlush",
            "value": 5080.437529867787,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSet",
            "value": 70918.40710807119,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatch",
            "value": 6408.371640996493,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatchFlush",
            "value": 6355.216296916307,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncList",
            "value": 159365.66283004015,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncSet",
            "value": 71782.38258721391,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommands",
            "value": 89.29477564411948,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommandsAsBatch",
            "value": 371.0502637827226,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writeKeyedCommand",
            "value": 30.996188906730822,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writePlainCommand",
            "value": 7.716393584060287,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSet",
            "value": 88265.0436574354,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatch",
            "value": 7536.073803744164,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatchFlush",
            "value": 6435.1996244949505,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSet",
            "value": 90528.1497640392,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatch",
            "value": 8287.014488313778,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatchFlush",
            "value": 8350.707976951384,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.syncSet",
            "value": 89109.10043297813,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashDirect",
            "value": 40.37199071478108,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashHeap",
            "value": 37.01173475585263,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedDirect",
            "value": 31.469904400687547,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedHeap",
            "value": 31.15544383498145,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeAllSlots",
            "value": 26865.74617350374,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeLowerSlots",
            "value": 13415.850667648116,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusAbsent",
            "value": 0.9997968219338557,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusPresent",
            "value": 0.9840597188950813,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyEstimatedSize",
            "value": 230.83091309926107,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyExactSize",
            "value": 70.41870306365652,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueEstimatedSize",
            "value": 214.0855353760026,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueExactSize",
            "value": 72.75071235524771,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.decodeUtf8Unpooled",
            "value": 4.38750549080738,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeAsciiToBuf",
            "value": 14.8644748713351,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeIsoToBuf",
            "value": 26.827771514934135,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8PlainStringToBuf",
            "value": 17.996285749099016,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8ToBuf",
            "value": 29.664239475062125,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8Unpooled",
            "value": 85.71855190489384,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.decodeUnpooled",
            "value": 4.3866499902664415,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.encodeUnpooled",
            "value": 86.8985097438842,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createBatchCommands",
            "value": 843161.1977875544,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createRegularCommands",
            "value": 845864.9718713328,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeAsyncCommand",
            "value": 27.202807035223437,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeCommandInterfaceCommand",
            "value": 176.58209459479713,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.asyncSet",
            "value": 6325.301170605718,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.batchSet",
            "value": 5648.284424123164,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.intercept.InvocationProxyFactoryBenchmark.run",
            "value": 55.4530429355229,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100Elements",
            "value": 384.62907541507366,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100ElementsWithResizeElement",
            "value": 572.971793919186,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16Elements",
            "value": 48.16088837429702,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16ElementsWithResizeElement",
            "value": 123.13242040279934,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureSingleElement",
            "value": 6.8721231154738645,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureZeroElement",
            "value": 0.6312181963601904,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createAsyncCommandUsingByteArrayCodec",
            "value": 27.730803409357076,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingByteArrayCodec",
            "value": 23.012740188961914,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingStringCodec",
            "value": 13.140484422468854,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingByteArrayCodec",
            "value": 14.164840826440344,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingNewStringCodec",
            "value": 67.5004063535489,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingOldStringCodec",
            "value": 69.2553907482239,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndRead",
            "value": 226.72949217703953,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1",
            "value": 176.67543644193648,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch10",
            "value": 1665.1273508937024,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch100",
            "value": 14250.839513474679,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1000",
            "value": 156689.42845714063,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisEndpointBenchmark.measureUserWrite",
            "value": 36.67526499125938,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisStateMachineBenchmark.measureDecode",
            "value": 146.8323811694309,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.singleConnection",
            "value": 364.0088569998935,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.twentyConnections",
            "value": 7613.392066209674,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.singleConnection",
            "value": 692.5750734782034,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.twentyConnections",
            "value": 13912.596960229212,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "psw0946",
            "username": "psw0946",
            "email": "31843235+psw0946@users.noreply.github.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "b925816f773d2aba38614c9df7118765932ec54f",
          "message": "Add support up to max unsigned integer in Bitfield offset (#2964) (#3099)\n\n* Add support up to max unsigned integer in Bitfield offset (#2964)\r\n\r\n* Add test for reactive command",
          "timestamp": "2024-12-27T09:42:43Z",
          "url": "https://github.com/redis/lettuce/commit/b925816f773d2aba38614c9df7118765932ec54f"
        },
        "date": 1735437539803,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSet",
            "value": 69934.90864364542,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatch",
            "value": 5560.141408959601,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatchFlush",
            "value": 4962.391437035564,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSet",
            "value": 70879.31122999002,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatch",
            "value": 6345.064686115643,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatchFlush",
            "value": 6407.439791806347,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncList",
            "value": 158191.90347955952,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncSet",
            "value": 70606.32902142596,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommands",
            "value": 89.20061748943162,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommandsAsBatch",
            "value": 373.70275244876126,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writeKeyedCommand",
            "value": 30.51490333806089,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writePlainCommand",
            "value": 7.752899069341765,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSet",
            "value": 87277.06854693189,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatch",
            "value": 7471.555055704545,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatchFlush",
            "value": 6418.87741707875,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSet",
            "value": 89574.01469566466,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatch",
            "value": 8289.048382028555,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatchFlush",
            "value": 8366.074240263322,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.syncSet",
            "value": 88039.55031356247,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashDirect",
            "value": 40.370510047000515,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashHeap",
            "value": 37.10437385158552,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedDirect",
            "value": 28.016353127569438,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedHeap",
            "value": 31.502869819994523,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeAllSlots",
            "value": 65855.30276216098,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeLowerSlots",
            "value": 32770.939531324126,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusAbsent",
            "value": 0.9976746578645823,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusPresent",
            "value": 0.9840005454393126,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyEstimatedSize",
            "value": 219.02902819360347,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyExactSize",
            "value": 70.53310943113264,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueEstimatedSize",
            "value": 213.64692860802847,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueExactSize",
            "value": 73.01367601000437,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.decodeUtf8Unpooled",
            "value": 4.394027215842998,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeAsciiToBuf",
            "value": 14.842764983096663,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeIsoToBuf",
            "value": 27.843093966555898,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8PlainStringToBuf",
            "value": 17.938047028320153,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8ToBuf",
            "value": 29.617760689200185,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8Unpooled",
            "value": 85.71755503911949,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.decodeUnpooled",
            "value": 4.408570417176961,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.encodeUnpooled",
            "value": 85.59364990143435,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createBatchCommands",
            "value": 848488.6789861455,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createRegularCommands",
            "value": 840334.7445562981,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeAsyncCommand",
            "value": 26.93263680523293,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeCommandInterfaceCommand",
            "value": 163.76495131868995,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.asyncSet",
            "value": 6293.350724728689,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.batchSet",
            "value": 5426.541560963533,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.intercept.InvocationProxyFactoryBenchmark.run",
            "value": 55.33468997578116,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100Elements",
            "value": 384.3415975808339,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100ElementsWithResizeElement",
            "value": 574.5673231225634,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16Elements",
            "value": 48.624675030731765,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16ElementsWithResizeElement",
            "value": 122.68195230637825,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureSingleElement",
            "value": 6.824829842131014,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureZeroElement",
            "value": 0.6314490685124599,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createAsyncCommandUsingByteArrayCodec",
            "value": 27.637746011227602,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingByteArrayCodec",
            "value": 22.91953726239908,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingStringCodec",
            "value": 13.115716690146806,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingByteArrayCodec",
            "value": 14.059788600574809,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingNewStringCodec",
            "value": 69.09053711715255,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingOldStringCodec",
            "value": 69.89067983328613,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndRead",
            "value": 227.7370840240056,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1",
            "value": 164.16133303517728,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch10",
            "value": 1539.4009268697182,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch100",
            "value": 16239.056312085206,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1000",
            "value": 136779.76598974827,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisEndpointBenchmark.measureUserWrite",
            "value": 38.452533111041035,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisStateMachineBenchmark.measureDecode",
            "value": 148.38141900868916,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.singleConnection",
            "value": 355.3144506424484,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.twentyConnections",
            "value": 7343.300067627277,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.singleConnection",
            "value": 652.7795661988745,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.twentyConnections",
            "value": 13294.321471166657,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "psw0946",
            "username": "psw0946",
            "email": "31843235+psw0946@users.noreply.github.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "b925816f773d2aba38614c9df7118765932ec54f",
          "message": "Add support up to max unsigned integer in Bitfield offset (#2964) (#3099)\n\n* Add support up to max unsigned integer in Bitfield offset (#2964)\r\n\r\n* Add test for reactive command",
          "timestamp": "2024-12-27T09:42:43Z",
          "url": "https://github.com/redis/lettuce/commit/b925816f773d2aba38614c9df7118765932ec54f"
        },
        "date": 1735523969730,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSet",
            "value": 70334.21616025755,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatch",
            "value": 5499.603029138337,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatchFlush",
            "value": 5095.2053782309395,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSet",
            "value": 70349.86325007834,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatch",
            "value": 6427.954134176989,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatchFlush",
            "value": 6559.69747855038,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncList",
            "value": 159521.8875457041,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncSet",
            "value": 70324.21498199491,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommands",
            "value": 89.00875652459213,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommandsAsBatch",
            "value": 371.65607574493174,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writeKeyedCommand",
            "value": 30.86615826493493,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writePlainCommand",
            "value": 7.7102852249588,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSet",
            "value": 87709.31544805711,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatch",
            "value": 7596.2033742417825,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatchFlush",
            "value": 6477.434314504577,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSet",
            "value": 91261.25095770694,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatch",
            "value": 8375.779991448055,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatchFlush",
            "value": 8391.509104186063,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.syncSet",
            "value": 90446.84513373501,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashDirect",
            "value": 40.39890385062435,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashHeap",
            "value": 37.09366152516771,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedDirect",
            "value": 28.04326336898648,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedHeap",
            "value": 31.457556458885005,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeAllSlots",
            "value": 59901.62374603166,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeLowerSlots",
            "value": 32776.147576888776,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusAbsent",
            "value": 0.9965502860579296,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusPresent",
            "value": 0.9853074112659256,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyEstimatedSize",
            "value": 202.36913053002118,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyExactSize",
            "value": 70.44768387330018,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueEstimatedSize",
            "value": 214.7696294088973,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueExactSize",
            "value": 73.25526485126221,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.decodeUtf8Unpooled",
            "value": 4.396501191988721,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeAsciiToBuf",
            "value": 14.864858544934469,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeIsoToBuf",
            "value": 27.710422990472257,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8PlainStringToBuf",
            "value": 17.950848475807057,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8ToBuf",
            "value": 29.66360953456003,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8Unpooled",
            "value": 85.99788673962107,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.decodeUnpooled",
            "value": 4.394954598363973,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.encodeUnpooled",
            "value": 85.76010934596457,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createBatchCommands",
            "value": 856881.3679253813,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createRegularCommands",
            "value": 829341.1844660384,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeAsyncCommand",
            "value": 26.915276381578916,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeCommandInterfaceCommand",
            "value": 159.83162824286154,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.asyncSet",
            "value": 6358.8996693311,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.batchSet",
            "value": 5589.845297649578,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.intercept.InvocationProxyFactoryBenchmark.run",
            "value": 55.46563631955195,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100Elements",
            "value": 384.702836622551,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100ElementsWithResizeElement",
            "value": 571.5182742359599,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16Elements",
            "value": 48.38004069641467,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16ElementsWithResizeElement",
            "value": 123.34008648713048,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureSingleElement",
            "value": 6.86155030561453,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureZeroElement",
            "value": 0.6297973045207933,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createAsyncCommandUsingByteArrayCodec",
            "value": 27.67272494735942,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingByteArrayCodec",
            "value": 23.03874898925971,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingStringCodec",
            "value": 13.136300625077439,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingByteArrayCodec",
            "value": 14.036865118378785,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingNewStringCodec",
            "value": 69.07669644738364,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingOldStringCodec",
            "value": 67.58356279569402,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndRead",
            "value": 219.5301281515172,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1",
            "value": 170.120716998432,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch10",
            "value": 1538.8958913798733,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch100",
            "value": 13958.6882502358,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1000",
            "value": 158996.3290873966,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisEndpointBenchmark.measureUserWrite",
            "value": 38.7741369545853,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisStateMachineBenchmark.measureDecode",
            "value": 147.8432765429462,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.singleConnection",
            "value": 370.72121615464187,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.twentyConnections",
            "value": 7453.723359181851,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.singleConnection",
            "value": 686.3140393664963,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.twentyConnections",
            "value": 13704.667908944471,
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
          "id": "ce92e6a84f0853a36e36e54c0df453594aaf7b2e",
          "message": "Handle UTF-8 characters in command arguments (#3075)",
          "timestamp": "2024-12-30T17:20:21Z",
          "url": "https://github.com/redis/lettuce/commit/ce92e6a84f0853a36e36e54c0df453594aaf7b2e"
        },
        "date": 1735610347581,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSet",
            "value": 69566.89529581947,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatch",
            "value": 5501.656386285414,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatchFlush",
            "value": 5023.5719106584465,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSet",
            "value": 69643.70401654583,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatch",
            "value": 6306.171978434833,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatchFlush",
            "value": 6311.552928009808,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncList",
            "value": 159085.05002245054,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncSet",
            "value": 70897.48129467177,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommands",
            "value": 89.05211527319474,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommandsAsBatch",
            "value": 371.5050835143719,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writeKeyedCommand",
            "value": 30.885601466983065,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writePlainCommand",
            "value": 7.722601294967809,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSet",
            "value": 88065.70836955037,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatch",
            "value": 7544.029362597333,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatchFlush",
            "value": 6402.319211572807,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSet",
            "value": 87956.60093731448,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatch",
            "value": 8310.890886267309,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatchFlush",
            "value": 8301.607214047586,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.syncSet",
            "value": 88340.80583554658,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashDirect",
            "value": 40.8634949095921,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashHeap",
            "value": 37.10075839777532,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedDirect",
            "value": 28.02823645974636,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedHeap",
            "value": 31.68270417772917,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeAllSlots",
            "value": 26775.188576775654,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeLowerSlots",
            "value": 32907.68411563331,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusAbsent",
            "value": 1.0077377464089623,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusPresent",
            "value": 0.9839345892773641,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyEstimatedSize",
            "value": 222.41217589255135,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyExactSize",
            "value": 70.31349015692759,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueEstimatedSize",
            "value": 212.7946660920019,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueExactSize",
            "value": 73.14177749569221,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.decodeUtf8Unpooled",
            "value": 4.409522786081087,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeAsciiToBuf",
            "value": 14.83669368582525,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeIsoToBuf",
            "value": 27.476290944047907,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8PlainStringToBuf",
            "value": 17.996653815847484,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8ToBuf",
            "value": 29.63722140858307,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8Unpooled",
            "value": 92.25739716077435,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.decodeUnpooled",
            "value": 4.402504636695491,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.encodeUnpooled",
            "value": 85.96825290904171,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createBatchCommands",
            "value": 813318.4462165895,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createRegularCommands",
            "value": 856755.5009351952,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeAsyncCommand",
            "value": 27.269620313300948,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeCommandInterfaceCommand",
            "value": 170.21403867441967,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.asyncSet",
            "value": 6281.907723294774,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.batchSet",
            "value": 5621.090578502777,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.intercept.InvocationProxyFactoryBenchmark.run",
            "value": 57.88080763428111,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100Elements",
            "value": 384.74976760780163,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100ElementsWithResizeElement",
            "value": 572.3124132170974,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16Elements",
            "value": 49.429612979785475,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16ElementsWithResizeElement",
            "value": 123.00009488465541,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureSingleElement",
            "value": 6.874281517962297,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureZeroElement",
            "value": 0.6316077731013985,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createAsyncCommandUsingByteArrayCodec",
            "value": 27.76555576617102,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingByteArrayCodec",
            "value": 23.183548079238694,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingStringCodec",
            "value": 13.127639614665004,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingByteArrayCodec",
            "value": 14.166928959157891,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingNewStringCodec",
            "value": 67.0863902849436,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingOldStringCodec",
            "value": 67.6622859912183,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndRead",
            "value": 227.96308999417266,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1",
            "value": 175.29911655974837,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch10",
            "value": 1547.972464540368,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch100",
            "value": 14714.536057355657,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1000",
            "value": 137121.796247987,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisEndpointBenchmark.measureUserWrite",
            "value": 36.9264654871428,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisStateMachineBenchmark.measureDecode",
            "value": 149.35592355991187,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.singleConnection",
            "value": 363.8756676248364,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.twentyConnections",
            "value": 7537.1275217881075,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.singleConnection",
            "value": 690.9833254576886,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.twentyConnections",
            "value": 13805.794029634908,
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
          "id": "ce92e6a84f0853a36e36e54c0df453594aaf7b2e",
          "message": "Handle UTF-8 characters in command arguments (#3075)",
          "timestamp": "2024-12-30T17:20:21Z",
          "url": "https://github.com/redis/lettuce/commit/ce92e6a84f0853a36e36e54c0df453594aaf7b2e"
        },
        "date": 1735696825590,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSet",
            "value": 70160.3563906836,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatch",
            "value": 5557.486180693375,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatchFlush",
            "value": 5097.69400024718,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSet",
            "value": 71802.99372118512,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatch",
            "value": 6444.419511940753,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatchFlush",
            "value": 6455.609819884418,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncList",
            "value": 162746.4639624462,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncSet",
            "value": 71067.99463768365,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommands",
            "value": 89.28500289885186,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommandsAsBatch",
            "value": 371.26893198300536,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writeKeyedCommand",
            "value": 30.985664296295745,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writePlainCommand",
            "value": 9.002082479459919,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSet",
            "value": 87823.36547177429,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatch",
            "value": 7486.905085257997,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatchFlush",
            "value": 6434.531184555554,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSet",
            "value": 89013.02947251647,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatch",
            "value": 8347.834517050032,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatchFlush",
            "value": 8422.826528824688,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.syncSet",
            "value": 88899.72106004367,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashDirect",
            "value": 40.43963215972536,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashHeap",
            "value": 37.05001298160976,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedDirect",
            "value": 28.06497088221459,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedHeap",
            "value": 31.19453891072836,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeAllSlots",
            "value": 64962.858299629996,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeLowerSlots",
            "value": 32744.897421216978,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusAbsent",
            "value": 0.9961574260382206,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusPresent",
            "value": 0.9864204811640335,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyEstimatedSize",
            "value": 220.24479281468675,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyExactSize",
            "value": 70.2642062616333,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueEstimatedSize",
            "value": 209.0484601449506,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueExactSize",
            "value": 73.01542537738092,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.decodeUtf8Unpooled",
            "value": 4.3853645373112595,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeAsciiToBuf",
            "value": 14.979288981589951,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeIsoToBuf",
            "value": 28.853539545767045,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8PlainStringToBuf",
            "value": 17.96696215276049,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8ToBuf",
            "value": 29.556845715855765,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8Unpooled",
            "value": 85.70094388475427,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.decodeUnpooled",
            "value": 4.393571725628588,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.encodeUnpooled",
            "value": 86.59079063594771,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createBatchCommands",
            "value": 837500.0991795256,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createRegularCommands",
            "value": 848801.8510177216,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeAsyncCommand",
            "value": 27.091780947034607,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeCommandInterfaceCommand",
            "value": 168.19471208723536,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.asyncSet",
            "value": 6291.9579112597,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.batchSet",
            "value": 5736.357651023069,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.intercept.InvocationProxyFactoryBenchmark.run",
            "value": 54.56835578217201,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100Elements",
            "value": 383.9761754756417,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100ElementsWithResizeElement",
            "value": 573.4191859940636,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16Elements",
            "value": 48.294907334974155,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16ElementsWithResizeElement",
            "value": 122.8225411144703,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureSingleElement",
            "value": 6.852895350835098,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureZeroElement",
            "value": 0.6315573201191762,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createAsyncCommandUsingByteArrayCodec",
            "value": 27.738384922766464,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingByteArrayCodec",
            "value": 23.003484025716453,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingStringCodec",
            "value": 13.115482347655899,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingByteArrayCodec",
            "value": 14.17003270507728,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingNewStringCodec",
            "value": 69.35675673884923,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingOldStringCodec",
            "value": 67.14692362862988,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndRead",
            "value": 216.77929000339788,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1",
            "value": 167.37715770421943,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch10",
            "value": 1722.7584984595446,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch100",
            "value": 15773.651724705769,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1000",
            "value": 141713.34432325402,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisEndpointBenchmark.measureUserWrite",
            "value": 38.52616167930479,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisStateMachineBenchmark.measureDecode",
            "value": 148.88163291799987,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.singleConnection",
            "value": 370.73106035085823,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.twentyConnections",
            "value": 7346.684499204591,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.singleConnection",
            "value": 657.4571762580398,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.twentyConnections",
            "value": 13746.02052203171,
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
          "id": "ce92e6a84f0853a36e36e54c0df453594aaf7b2e",
          "message": "Handle UTF-8 characters in command arguments (#3075)",
          "timestamp": "2024-12-30T17:20:21Z",
          "url": "https://github.com/redis/lettuce/commit/ce92e6a84f0853a36e36e54c0df453594aaf7b2e"
        },
        "date": 1735869550639,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSet",
            "value": 71914.21737380742,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatch",
            "value": 5670.448980039584,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatchFlush",
            "value": 5183.010537916755,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSet",
            "value": 71977.24061370168,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatch",
            "value": 6529.06124129289,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatchFlush",
            "value": 6486.588269347266,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncList",
            "value": 162108.593938476,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncSet",
            "value": 72296.97593976949,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommands",
            "value": 90.23476762889014,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommandsAsBatch",
            "value": 375.48132012461093,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writeKeyedCommand",
            "value": 31.02991633158855,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writePlainCommand",
            "value": 9.06593505821606,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSet",
            "value": 90072.09950804348,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatch",
            "value": 7670.026469671141,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatchFlush",
            "value": 6686.306728680387,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSet",
            "value": 90035.34637821295,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatch",
            "value": 8574.147420511345,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatchFlush",
            "value": 8493.944975349976,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.syncSet",
            "value": 90125.70921781463,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashDirect",
            "value": 40.39368775187823,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashHeap",
            "value": 37.2673937784278,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedDirect",
            "value": 28.111701118955658,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedHeap",
            "value": 31.48382072972243,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeAllSlots",
            "value": 59728.68531925187,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeLowerSlots",
            "value": 32828.16726246539,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusAbsent",
            "value": 1.0007825002960826,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusPresent",
            "value": 0.9915409588379926,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyEstimatedSize",
            "value": 208.08890088242697,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyExactSize",
            "value": 70.246746046597,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueEstimatedSize",
            "value": 201.0544833740874,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueExactSize",
            "value": 73.268295975948,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.decodeUtf8Unpooled",
            "value": 4.383705451882302,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeAsciiToBuf",
            "value": 14.902990899992963,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeIsoToBuf",
            "value": 27.98959251966473,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8PlainStringToBuf",
            "value": 18.041833689854336,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8ToBuf",
            "value": 29.798517700568176,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8Unpooled",
            "value": 85.90506813351183,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.decodeUnpooled",
            "value": 4.399684476191,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.encodeUnpooled",
            "value": 86.0602086411345,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createBatchCommands",
            "value": 853509.714573437,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createRegularCommands",
            "value": 852977.3247243433,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeAsyncCommand",
            "value": 27.31360942400692,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeCommandInterfaceCommand",
            "value": 161.80416419735047,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.asyncSet",
            "value": 6448.073472017111,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.batchSet",
            "value": 5671.605874108145,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.intercept.InvocationProxyFactoryBenchmark.run",
            "value": 55.88536514596829,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100Elements",
            "value": 385.79528131065086,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100ElementsWithResizeElement",
            "value": 573.5578192582309,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16Elements",
            "value": 48.816341782637465,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16ElementsWithResizeElement",
            "value": 122.89911357845772,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureSingleElement",
            "value": 6.843017515485043,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureZeroElement",
            "value": 0.6309568955857434,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createAsyncCommandUsingByteArrayCodec",
            "value": 27.75358262517029,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingByteArrayCodec",
            "value": 23.116282117252098,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingStringCodec",
            "value": 13.31829086262113,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingByteArrayCodec",
            "value": 14.32181842405293,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingNewStringCodec",
            "value": 69.42484095645816,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingOldStringCodec",
            "value": 70.5484494340758,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndRead",
            "value": 227.932408485407,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1",
            "value": 175.7959399756718,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch10",
            "value": 1822.1316830776886,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch100",
            "value": 14023.1392071577,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1000",
            "value": 139518.9892571003,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisEndpointBenchmark.measureUserWrite",
            "value": 38.457881811165386,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisStateMachineBenchmark.measureDecode",
            "value": 150.2775444287409,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.singleConnection",
            "value": 353.9637907466061,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.twentyConnections",
            "value": 7872.1819142707145,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.singleConnection",
            "value": 641.5058438553756,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.twentyConnections",
            "value": 13988.024742247182,
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
          "id": "ce92e6a84f0853a36e36e54c0df453594aaf7b2e",
          "message": "Handle UTF-8 characters in command arguments (#3075)",
          "timestamp": "2024-12-30T17:20:21Z",
          "url": "https://github.com/redis/lettuce/commit/ce92e6a84f0853a36e36e54c0df453594aaf7b2e"
        },
        "date": 1735955940129,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSet",
            "value": 70745.02706919731,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatch",
            "value": 5588.735319505349,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatchFlush",
            "value": 5134.325485547487,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSet",
            "value": 72248.24665075267,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatch",
            "value": 6356.287745465268,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatchFlush",
            "value": 6489.110351711899,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncList",
            "value": 160111.58644449656,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncSet",
            "value": 70271.72967146419,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommands",
            "value": 89.19793830726277,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommandsAsBatch",
            "value": 373.4663192735344,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writeKeyedCommand",
            "value": 31.02114540618797,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writePlainCommand",
            "value": 7.741952552536143,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSet",
            "value": 87052.57015247612,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatch",
            "value": 7531.968442887371,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatchFlush",
            "value": 6431.928739138466,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSet",
            "value": 89510.57984921402,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatch",
            "value": 8282.746036371409,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatchFlush",
            "value": 8347.944942966034,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.syncSet",
            "value": 88239.58062118264,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashDirect",
            "value": 40.515972077279194,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashHeap",
            "value": 37.215065175919804,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedDirect",
            "value": 28.150615780462363,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedHeap",
            "value": 31.199739393734898,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeAllSlots",
            "value": 65824.07808486321,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeLowerSlots",
            "value": 32713.718551587655,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusAbsent",
            "value": 1.0002716758946295,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusPresent",
            "value": 0.9890984130291478,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyEstimatedSize",
            "value": 196.97635368963788,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyExactSize",
            "value": 70.27859930050153,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueEstimatedSize",
            "value": 224.86036521405413,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueExactSize",
            "value": 73.03679947617832,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.decodeUtf8Unpooled",
            "value": 4.4195959802704845,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeAsciiToBuf",
            "value": 14.93205551980723,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeIsoToBuf",
            "value": 27.77004532579146,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8PlainStringToBuf",
            "value": 17.926746037469872,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8ToBuf",
            "value": 29.58014759954625,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8Unpooled",
            "value": 85.82090205288912,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.decodeUnpooled",
            "value": 4.465269655758808,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.encodeUnpooled",
            "value": 85.65782729865899,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createBatchCommands",
            "value": 846662.2093988679,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createRegularCommands",
            "value": 852761.344347802,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeAsyncCommand",
            "value": 27.155565990942762,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeCommandInterfaceCommand",
            "value": 161.5902073462653,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.asyncSet",
            "value": 6337.837371325693,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.batchSet",
            "value": 5647.345334725871,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.intercept.InvocationProxyFactoryBenchmark.run",
            "value": 55.33755135579221,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100Elements",
            "value": 384.1540856126302,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100ElementsWithResizeElement",
            "value": 575.149539897084,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16Elements",
            "value": 48.1710991689024,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16ElementsWithResizeElement",
            "value": 124.22497220138516,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureSingleElement",
            "value": 6.864223287820936,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureZeroElement",
            "value": 0.6300453952523366,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createAsyncCommandUsingByteArrayCodec",
            "value": 27.679211288509144,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingByteArrayCodec",
            "value": 23.046662790804266,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingStringCodec",
            "value": 13.171191106941013,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingByteArrayCodec",
            "value": 14.11796273317822,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingNewStringCodec",
            "value": 69.63025089119546,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingOldStringCodec",
            "value": 67.1981401464451,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndRead",
            "value": 227.756175634706,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1",
            "value": 183.62298846338976,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch10",
            "value": 1600.0134602059059,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch100",
            "value": 16402.36655468597,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1000",
            "value": 137969.11164607774,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisEndpointBenchmark.measureUserWrite",
            "value": 38.86386297136686,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisStateMachineBenchmark.measureDecode",
            "value": 147.47626799162202,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.singleConnection",
            "value": 366.31480366645366,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.twentyConnections",
            "value": 7670.059813987638,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.singleConnection",
            "value": 664.6799705308351,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.twentyConnections",
            "value": 13323.657848029316,
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
          "id": "ce92e6a84f0853a36e36e54c0df453594aaf7b2e",
          "message": "Handle UTF-8 characters in command arguments (#3075)",
          "timestamp": "2024-12-30T17:20:21Z",
          "url": "https://github.com/redis/lettuce/commit/ce92e6a84f0853a36e36e54c0df453594aaf7b2e"
        },
        "date": 1736042351460,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSet",
            "value": 69161.37794956964,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatch",
            "value": 5530.098360544449,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatchFlush",
            "value": 5086.237588035852,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSet",
            "value": 70895.00300689689,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatch",
            "value": 6341.252957423537,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatchFlush",
            "value": 6379.422604193185,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncList",
            "value": 158768.0268776281,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncSet",
            "value": 69474.26237972087,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommands",
            "value": 89.09779353392646,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommandsAsBatch",
            "value": 374.38032473312836,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writeKeyedCommand",
            "value": 31.754356956309937,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writePlainCommand",
            "value": 7.712434350411956,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSet",
            "value": 87295.52886123757,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatch",
            "value": 7481.116909202147,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatchFlush",
            "value": 6444.029222662131,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSet",
            "value": 89322.81104057762,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatch",
            "value": 8395.941052083053,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatchFlush",
            "value": 8360.728603384214,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.syncSet",
            "value": 88040.1053283929,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashDirect",
            "value": 40.440050223457085,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashHeap",
            "value": 37.116085801585356,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedDirect",
            "value": 28.390894198727086,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedHeap",
            "value": 31.61334746528208,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeAllSlots",
            "value": 26816.611311604152,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeLowerSlots",
            "value": 35621.7870839286,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusAbsent",
            "value": 1.0004677473494148,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusPresent",
            "value": 0.9888147242638803,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyEstimatedSize",
            "value": 171.48639284727392,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyExactSize",
            "value": 74.27350137023544,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueEstimatedSize",
            "value": 199.04293561035269,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueExactSize",
            "value": 73.53393351206698,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.decodeUtf8Unpooled",
            "value": 4.398988199864879,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeAsciiToBuf",
            "value": 14.837185428809573,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeIsoToBuf",
            "value": 28.325985333931015,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8PlainStringToBuf",
            "value": 17.93969221360191,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8ToBuf",
            "value": 29.71728604772477,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8Unpooled",
            "value": 85.76146791551353,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.decodeUnpooled",
            "value": 4.394190749089995,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.encodeUnpooled",
            "value": 86.02430470647387,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createBatchCommands",
            "value": 864850.0915962986,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createRegularCommands",
            "value": 843537.0400648525,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeAsyncCommand",
            "value": 27.052288855731682,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeCommandInterfaceCommand",
            "value": 166.71488244836593,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.asyncSet",
            "value": 6223.551009080275,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.batchSet",
            "value": 5605.440644320099,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.intercept.InvocationProxyFactoryBenchmark.run",
            "value": 55.82355302526944,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100Elements",
            "value": 385.11081669835613,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100ElementsWithResizeElement",
            "value": 573.8465392341144,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16Elements",
            "value": 48.18669487863592,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16ElementsWithResizeElement",
            "value": 124.13450514392466,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureSingleElement",
            "value": 6.847591075532553,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureZeroElement",
            "value": 0.6338407144187448,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createAsyncCommandUsingByteArrayCodec",
            "value": 27.716361435474187,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingByteArrayCodec",
            "value": 23.045803084862957,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingStringCodec",
            "value": 13.119117399976796,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingByteArrayCodec",
            "value": 14.115054521514555,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingNewStringCodec",
            "value": 67.55596828810869,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingOldStringCodec",
            "value": 67.32975498197695,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndRead",
            "value": 215.392154782033,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1",
            "value": 189.7845268518376,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch10",
            "value": 1505.0170749364893,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch100",
            "value": 14218.39463788461,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1000",
            "value": 159849.57613588334,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisEndpointBenchmark.measureUserWrite",
            "value": 36.833543272840885,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisStateMachineBenchmark.measureDecode",
            "value": 149.75846864581587,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.singleConnection",
            "value": 357.5077519633868,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.twentyConnections",
            "value": 7399.2449697163565,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.singleConnection",
            "value": 682.819839930354,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.twentyConnections",
            "value": 14417.002467036718,
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
          "id": "ce92e6a84f0853a36e36e54c0df453594aaf7b2e",
          "message": "Handle UTF-8 characters in command arguments (#3075)",
          "timestamp": "2024-12-30T17:20:21Z",
          "url": "https://github.com/redis/lettuce/commit/ce92e6a84f0853a36e36e54c0df453594aaf7b2e"
        },
        "date": 1736128833391,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSet",
            "value": 71881.32405618993,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatch",
            "value": 5781.692447090032,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatchFlush",
            "value": 5294.886388806925,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSet",
            "value": 72402.80345047821,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatch",
            "value": 6765.8279389205145,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatchFlush",
            "value": 6781.153566872019,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncList",
            "value": 162543.87433462642,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncSet",
            "value": 73542.47212443314,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommands",
            "value": 91.44098781126233,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommandsAsBatch",
            "value": 371.098367057232,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writeKeyedCommand",
            "value": 30.90205391791169,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writePlainCommand",
            "value": 7.757289940813196,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSet",
            "value": 89892.19951546035,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatch",
            "value": 7713.617217935976,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatchFlush",
            "value": 6642.075028563078,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSet",
            "value": 91598.97777463132,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatch",
            "value": 8657.168652156772,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatchFlush",
            "value": 8519.614488423771,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.syncSet",
            "value": 89507.24465676976,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashDirect",
            "value": 40.41586788729275,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashHeap",
            "value": 38.60592544307568,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedDirect",
            "value": 28.36714163354535,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedHeap",
            "value": 31.625140604990484,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeAllSlots",
            "value": 65983.04445431182,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeLowerSlots",
            "value": 32874.51530331718,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusAbsent",
            "value": 0.9998995164076391,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusPresent",
            "value": 0.9899542472207221,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyEstimatedSize",
            "value": 213.63671154138666,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyExactSize",
            "value": 70.26738477285797,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueEstimatedSize",
            "value": 215.9306115188396,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueExactSize",
            "value": 72.77662816496868,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.decodeUtf8Unpooled",
            "value": 4.394443799162225,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeAsciiToBuf",
            "value": 14.82336343884867,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeIsoToBuf",
            "value": 27.695635642480198,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8PlainStringToBuf",
            "value": 17.953753150054272,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8ToBuf",
            "value": 29.557123390129306,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8Unpooled",
            "value": 86.03552188387286,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.decodeUnpooled",
            "value": 4.395346377415046,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.encodeUnpooled",
            "value": 86.2912674097339,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createBatchCommands",
            "value": 855216.0907874971,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createRegularCommands",
            "value": 851420.2617137565,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeAsyncCommand",
            "value": 27.270369535004228,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeCommandInterfaceCommand",
            "value": 168.16655172416066,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.asyncSet",
            "value": 6408.87404693173,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.batchSet",
            "value": 5601.864385155519,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.intercept.InvocationProxyFactoryBenchmark.run",
            "value": 54.647280664098126,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100Elements",
            "value": 386.615529859262,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100ElementsWithResizeElement",
            "value": 575.203439919151,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16Elements",
            "value": 48.78426160504792,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16ElementsWithResizeElement",
            "value": 122.72022173853733,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureSingleElement",
            "value": 6.866291620446849,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureZeroElement",
            "value": 0.6320185848054801,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createAsyncCommandUsingByteArrayCodec",
            "value": 27.76904942415546,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingByteArrayCodec",
            "value": 23.00777448044765,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingStringCodec",
            "value": 13.232529902628993,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingByteArrayCodec",
            "value": 14.1597471843961,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingNewStringCodec",
            "value": 70.15302924666526,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingOldStringCodec",
            "value": 67.12523073607949,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndRead",
            "value": 228.0050228527597,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1",
            "value": 181.7800228980424,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch10",
            "value": 1764.069886803615,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch100",
            "value": 16416.74274147409,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1000",
            "value": 154069.09934614212,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisEndpointBenchmark.measureUserWrite",
            "value": 36.79750146839444,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisStateMachineBenchmark.measureDecode",
            "value": 148.4135566816108,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.singleConnection",
            "value": 361.8473345847203,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.twentyConnections",
            "value": 7277.428363398819,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.singleConnection",
            "value": 687.4709404665853,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.twentyConnections",
            "value": 13456.836326130306,
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
          "id": "a254a784c4c3bf7513b2fe55c06d3e0f46189dc5",
          "message": "Improve code by adding some null checks (#3115)",
          "timestamp": "2025-01-06T15:37:38Z",
          "url": "https://github.com/redis/lettuce/commit/a254a784c4c3bf7513b2fe55c06d3e0f46189dc5"
        },
        "date": 1736301558416,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSet",
            "value": 70549.90989347169,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatch",
            "value": 5572.112086149046,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatchFlush",
            "value": 5151.945311517926,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSet",
            "value": 71012.46122160317,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatch",
            "value": 6434.095806525216,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatchFlush",
            "value": 6346.966640810022,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncList",
            "value": 160331.87553527052,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncSet",
            "value": 70745.29611697975,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommands",
            "value": 89.70982199184019,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommandsAsBatch",
            "value": 409.296053068074,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writeKeyedCommand",
            "value": 30.9423820172728,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writePlainCommand",
            "value": 7.713933271449586,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSet",
            "value": 87050.59681749367,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatch",
            "value": 7490.047146534334,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatchFlush",
            "value": 6360.298964164865,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSet",
            "value": 88398.91682120282,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatch",
            "value": 8208.880265892123,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatchFlush",
            "value": 8266.006126045328,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.syncSet",
            "value": 88433.49037590386,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashDirect",
            "value": 40.358680288851126,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashHeap",
            "value": 37.143112541869456,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedDirect",
            "value": 28.0226143289076,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedHeap",
            "value": 31.477710297855076,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeAllSlots",
            "value": 59900.773108191366,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeLowerSlots",
            "value": 32840.769747361126,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusAbsent",
            "value": 1.0013642197474693,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusPresent",
            "value": 0.9875456561862521,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyEstimatedSize",
            "value": 208.90152892653282,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyExactSize",
            "value": 71.04373764693331,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueEstimatedSize",
            "value": 211.71982206865033,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueExactSize",
            "value": 73.39433010803727,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.decodeUtf8Unpooled",
            "value": 4.402690698099729,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeAsciiToBuf",
            "value": 14.943214348196378,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeIsoToBuf",
            "value": 27.124887151151675,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8PlainStringToBuf",
            "value": 17.955498585425058,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8ToBuf",
            "value": 29.752636629972745,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8Unpooled",
            "value": 86.91715904545553,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.decodeUnpooled",
            "value": 4.401107423650286,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.encodeUnpooled",
            "value": 85.7556467745879,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createBatchCommands",
            "value": 865167.757955653,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createRegularCommands",
            "value": 838920.7477193137,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeAsyncCommand",
            "value": 27.108592122856248,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeCommandInterfaceCommand",
            "value": 164.98827849832426,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.asyncSet",
            "value": 6316.786540204677,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.batchSet",
            "value": 5589.256943580928,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.intercept.InvocationProxyFactoryBenchmark.run",
            "value": 55.056118643820604,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100Elements",
            "value": 385.42572476028363,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100ElementsWithResizeElement",
            "value": 571.4558195398797,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16Elements",
            "value": 48.71743907589657,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16ElementsWithResizeElement",
            "value": 122.93662440299286,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureSingleElement",
            "value": 6.889150531216979,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureZeroElement",
            "value": 0.6298456483299237,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createAsyncCommandUsingByteArrayCodec",
            "value": 27.634517766380288,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingByteArrayCodec",
            "value": 22.975686246501446,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingStringCodec",
            "value": 13.14230028468844,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingByteArrayCodec",
            "value": 14.111434409013096,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingNewStringCodec",
            "value": 66.96868106064173,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingOldStringCodec",
            "value": 70.01994344210001,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndRead",
            "value": 218.95108890102443,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1",
            "value": 160.14462308368405,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch10",
            "value": 1752.504339396493,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch100",
            "value": 13475.411616955374,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1000",
            "value": 142732.52740599622,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisEndpointBenchmark.measureUserWrite",
            "value": 38.462506320776136,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisStateMachineBenchmark.measureDecode",
            "value": 147.97645442801806,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.singleConnection",
            "value": 376.9777559109636,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.twentyConnections",
            "value": 7710.076720228499,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.singleConnection",
            "value": 689.6952213160741,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.twentyConnections",
            "value": 13475.775785369915,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "Binbin",
            "username": "enjoy-binbin",
            "email": "binloveplay1314@qq.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "a8613e8c8540d49a67db1b8883ee6e1ac09036f2",
          "message": "Remove extra spaces in words in docs (#3120)\n\n* Remove extra spaces in words\r\n\r\n`reset LatenciesAfterEvent` should be `resetLatenciesAfterEvent`.\r\n`ev entEmitIntervalUnit` should be `eventEmitIntervalUnit`.\r\nOther than that, diff is all table formatting.\r\n\r\n* more cleanup\r\n\r\n* Add ClusterClientOptions to wordlist\r\n\r\n---------\r\n\r\nCo-authored-by: ggivo <ivo.gaydazhiev@redis.com>",
          "timestamp": "2025-01-08T13:14:46Z",
          "url": "https://github.com/redis/lettuce/commit/a8613e8c8540d49a67db1b8883ee6e1ac09036f2"
        },
        "date": 1736474373088,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSet",
            "value": 68381.5789683751,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatch",
            "value": 5500.184597504082,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatchFlush",
            "value": 5110.656415520756,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSet",
            "value": 69411.93147678782,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatch",
            "value": 6220.091979192137,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatchFlush",
            "value": 6299.673867968301,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncList",
            "value": 157511.4255345946,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncSet",
            "value": 70148.75996651557,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommands",
            "value": 89.21740758201531,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommandsAsBatch",
            "value": 408.8053492726459,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writeKeyedCommand",
            "value": 31.068349268906537,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writePlainCommand",
            "value": 7.726866856118559,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSet",
            "value": 87790.47391530605,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatch",
            "value": 7466.566077214123,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatchFlush",
            "value": 6401.100887791437,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSet",
            "value": 88900.63595701558,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatch",
            "value": 8274.378320842976,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatchFlush",
            "value": 8093.450133994111,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.syncSet",
            "value": 89359.0022856126,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashDirect",
            "value": 40.39060102197394,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashHeap",
            "value": 37.34165458176996,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedDirect",
            "value": 28.65444390894192,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedHeap",
            "value": 31.16489614242739,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeAllSlots",
            "value": 65302.859165982445,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeLowerSlots",
            "value": 32806.21228740509,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusAbsent",
            "value": 1.0003069070799318,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusPresent",
            "value": 0.9857835878631821,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyEstimatedSize",
            "value": 216.04360325513917,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyExactSize",
            "value": 70.10201555754931,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueEstimatedSize",
            "value": 210.0194720832772,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueExactSize",
            "value": 72.86440821881696,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.decodeUtf8Unpooled",
            "value": 4.388101408685552,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeAsciiToBuf",
            "value": 14.836571031625914,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeIsoToBuf",
            "value": 29.06339941920326,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8PlainStringToBuf",
            "value": 17.956212344409828,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8ToBuf",
            "value": 29.603145611986065,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8Unpooled",
            "value": 86.02200305597239,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.decodeUnpooled",
            "value": 4.399289521799343,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.encodeUnpooled",
            "value": 86.49270167564924,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createBatchCommands",
            "value": 848609.6488667788,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createRegularCommands",
            "value": 827914.9883530047,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeAsyncCommand",
            "value": 27.33350549381158,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeCommandInterfaceCommand",
            "value": 154.74865114740334,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.asyncSet",
            "value": 6286.207300655183,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.batchSet",
            "value": 5618.095325900236,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.intercept.InvocationProxyFactoryBenchmark.run",
            "value": 57.986765641702085,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100Elements",
            "value": 385.2302128657276,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100ElementsWithResizeElement",
            "value": 573.9050725626681,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16Elements",
            "value": 48.6221191656214,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16ElementsWithResizeElement",
            "value": 122.96338001907138,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureSingleElement",
            "value": 6.8632051142239785,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureZeroElement",
            "value": 0.6297623047666926,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createAsyncCommandUsingByteArrayCodec",
            "value": 27.702546736474897,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingByteArrayCodec",
            "value": 22.990879292658455,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingStringCodec",
            "value": 13.139392951609906,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingByteArrayCodec",
            "value": 14.100990521963421,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingNewStringCodec",
            "value": 69.51776198406526,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingOldStringCodec",
            "value": 70.03045260908853,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndRead",
            "value": 214.91082318027833,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1",
            "value": 163.8211317363081,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch10",
            "value": 1754.3336567868962,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch100",
            "value": 13959.062751178639,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1000",
            "value": 150600.85543023632,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisEndpointBenchmark.measureUserWrite",
            "value": 36.76726164177914,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisStateMachineBenchmark.measureDecode",
            "value": 149.99982499151784,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.singleConnection",
            "value": 356.9090622294364,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.twentyConnections",
            "value": 7295.191150098847,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.singleConnection",
            "value": 658.7052752119237,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.twentyConnections",
            "value": 13880.575414776138,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "Binbin",
            "username": "enjoy-binbin",
            "email": "binloveplay1314@qq.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "a8613e8c8540d49a67db1b8883ee6e1ac09036f2",
          "message": "Remove extra spaces in words in docs (#3120)\n\n* Remove extra spaces in words\r\n\r\n`reset LatenciesAfterEvent` should be `resetLatenciesAfterEvent`.\r\n`ev entEmitIntervalUnit` should be `eventEmitIntervalUnit`.\r\nOther than that, diff is all table formatting.\r\n\r\n* more cleanup\r\n\r\n* Add ClusterClientOptions to wordlist\r\n\r\n---------\r\n\r\nCo-authored-by: ggivo <ivo.gaydazhiev@redis.com>",
          "timestamp": "2025-01-08T13:14:46Z",
          "url": "https://github.com/redis/lettuce/commit/a8613e8c8540d49a67db1b8883ee6e1ac09036f2"
        },
        "date": 1736560766495,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSet",
            "value": 69840.51799906752,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatch",
            "value": 5604.520827280288,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatchFlush",
            "value": 5055.463088324599,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSet",
            "value": 72115.19549739249,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatch",
            "value": 6467.988091635007,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatchFlush",
            "value": 6454.454511514089,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncList",
            "value": 160970.68734272724,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncSet",
            "value": 71175.88257976898,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommands",
            "value": 89.51881503623638,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommandsAsBatch",
            "value": 371.51140302365076,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writeKeyedCommand",
            "value": 33.752261197080934,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writePlainCommand",
            "value": 7.736966606762643,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSet",
            "value": 87714.02817409515,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatch",
            "value": 7605.46826994695,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatchFlush",
            "value": 6404.945701207372,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSet",
            "value": 90477.428609742,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatch",
            "value": 8335.202587594376,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatchFlush",
            "value": 8360.531009826587,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.syncSet",
            "value": 88221.9762951636,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashDirect",
            "value": 40.41169513203261,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashHeap",
            "value": 37.23050385503456,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedDirect",
            "value": 28.472553489679406,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedHeap",
            "value": 31.590772941268632,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeAllSlots",
            "value": 59668.51946737092,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeLowerSlots",
            "value": 35826.335814211445,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusAbsent",
            "value": 1.0019347389677071,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusPresent",
            "value": 0.989105015983472,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyEstimatedSize",
            "value": 202.32872680724122,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyExactSize",
            "value": 72.10837261060115,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueEstimatedSize",
            "value": 206.93357021750597,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueExactSize",
            "value": 72.96032123121218,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.decodeUtf8Unpooled",
            "value": 4.394380876601304,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeAsciiToBuf",
            "value": 14.897873876580476,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeIsoToBuf",
            "value": 27.131622067964155,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8PlainStringToBuf",
            "value": 18.038825429601577,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8ToBuf",
            "value": 29.614596884765128,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8Unpooled",
            "value": 85.75052922747615,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.decodeUnpooled",
            "value": 4.3957945971545405,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.encodeUnpooled",
            "value": 86.02367854926663,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createBatchCommands",
            "value": 859605.8198273184,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createRegularCommands",
            "value": 850793.6073692696,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeAsyncCommand",
            "value": 26.980352274146032,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeCommandInterfaceCommand",
            "value": 165.1699472780138,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.asyncSet",
            "value": 6332.630293993733,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.batchSet",
            "value": 5744.829155147041,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.intercept.InvocationProxyFactoryBenchmark.run",
            "value": 56.93463076360574,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100Elements",
            "value": 384.88667847080893,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100ElementsWithResizeElement",
            "value": 574.1762585121609,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16Elements",
            "value": 48.33347390414537,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16ElementsWithResizeElement",
            "value": 122.69368881528654,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureSingleElement",
            "value": 7.219654449712827,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureZeroElement",
            "value": 0.6327363575952656,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createAsyncCommandUsingByteArrayCodec",
            "value": 27.659717295396735,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingByteArrayCodec",
            "value": 23.076000682154664,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingStringCodec",
            "value": 13.25512631305715,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingByteArrayCodec",
            "value": 14.226241713822603,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingNewStringCodec",
            "value": 69.41134834537829,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingOldStringCodec",
            "value": 67.80303909538426,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndRead",
            "value": 229.77260292055865,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1",
            "value": 166.4161080220033,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch10",
            "value": 1519.5966779461676,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch100",
            "value": 14945.50763153703,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1000",
            "value": 141489.64836961703,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisEndpointBenchmark.measureUserWrite",
            "value": 36.84151761079195,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisStateMachineBenchmark.measureDecode",
            "value": 150.66270777074394,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.singleConnection",
            "value": 358.43621571528513,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.twentyConnections",
            "value": 7485.898053161155,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.singleConnection",
            "value": 694.4423918071172,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.twentyConnections",
            "value": 12975.691535760745,
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
          "id": "29ddd1abb27dcdaef4087fb8910a95e19e319b15",
          "message": "Fix issue with array initialization (#3123)",
          "timestamp": "2025-01-11T18:25:35Z",
          "url": "https://github.com/redis/lettuce/commit/29ddd1abb27dcdaef4087fb8910a95e19e319b15"
        },
        "date": 1736647163691,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSet",
            "value": 76140.66923360819,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatch",
            "value": 5269.940045985327,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatchFlush",
            "value": 5413.304219741547,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSet",
            "value": 76511.8732019041,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatch",
            "value": 6472.854487229358,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatchFlush",
            "value": 6423.749774205617,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncList",
            "value": 176298.32158745287,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncSet",
            "value": 77467.97809490422,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommands",
            "value": 89.83849572157195,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommandsAsBatch",
            "value": 373.16921168128596,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writeKeyedCommand",
            "value": 30.99860051759984,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writePlainCommand",
            "value": 7.8004731019110904,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSet",
            "value": 95816.16568843892,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatch",
            "value": 7035.823149944151,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatchFlush",
            "value": 6866.198001549905,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSet",
            "value": 98155.06184399252,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatch",
            "value": 8215.892997940458,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatchFlush",
            "value": 8136.958459798834,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.syncSet",
            "value": 97953.60040228319,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashDirect",
            "value": 40.6887426722325,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashHeap",
            "value": 37.39432940972088,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedDirect",
            "value": 28.198631390146396,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedHeap",
            "value": 31.248318697817655,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeAllSlots",
            "value": 60243.9916635455,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeLowerSlots",
            "value": 32985.073888182626,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusAbsent",
            "value": 1.0031867512021673,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusPresent",
            "value": 0.9902386824944507,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyEstimatedSize",
            "value": 221.63539973707606,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyExactSize",
            "value": 72.14932807126135,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueEstimatedSize",
            "value": 209.00531381258338,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueExactSize",
            "value": 73.31912616473333,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.decodeUtf8Unpooled",
            "value": 4.409707096161811,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeAsciiToBuf",
            "value": 15.01154217984635,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeIsoToBuf",
            "value": 27.802175965435573,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8PlainStringToBuf",
            "value": 18.082070254432207,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8ToBuf",
            "value": 29.833067418782996,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8Unpooled",
            "value": 86.76328454377064,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.decodeUnpooled",
            "value": 4.4177377388130505,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.encodeUnpooled",
            "value": 86.53118819174472,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createBatchCommands",
            "value": 848621.3325558094,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createRegularCommands",
            "value": 865853.6604343908,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeAsyncCommand",
            "value": 27.452158227511358,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeCommandInterfaceCommand",
            "value": 170.83956150881048,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.asyncSet",
            "value": 6276.044443412602,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.batchSet",
            "value": 5876.403707558955,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.intercept.InvocationProxyFactoryBenchmark.run",
            "value": 55.294658136754336,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100Elements",
            "value": 386.9726101760276,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100ElementsWithResizeElement",
            "value": 580.2019140412938,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16Elements",
            "value": 49.018654875988204,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16ElementsWithResizeElement",
            "value": 123.23567120613492,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureSingleElement",
            "value": 6.914909973309979,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureZeroElement",
            "value": 0.6372738632152412,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createAsyncCommandUsingByteArrayCodec",
            "value": 27.808602567553407,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingByteArrayCodec",
            "value": 23.182630009535266,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingStringCodec",
            "value": 13.128293526207482,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingByteArrayCodec",
            "value": 14.20715603943476,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingNewStringCodec",
            "value": 67.6738519310704,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingOldStringCodec",
            "value": 67.58614296640044,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndRead",
            "value": 253.94506971752762,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1",
            "value": 167.63612427679527,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch10",
            "value": 1734.712079300285,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch100",
            "value": 14924.378715973802,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1000",
            "value": 153698.74600104024,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisEndpointBenchmark.measureUserWrite",
            "value": 36.81925278641245,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisStateMachineBenchmark.measureDecode",
            "value": 148.05285553342276,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.singleConnection",
            "value": 365.3778180722272,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.twentyConnections",
            "value": 7521.008873548887,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.singleConnection",
            "value": 652.1570780116557,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.twentyConnections",
            "value": 13624.223628195474,
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
          "id": "29ddd1abb27dcdaef4087fb8910a95e19e319b15",
          "message": "Fix issue with array initialization (#3123)",
          "timestamp": "2025-01-11T18:25:35Z",
          "url": "https://github.com/redis/lettuce/commit/29ddd1abb27dcdaef4087fb8910a95e19e319b15"
        },
        "date": 1736733590329,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSet",
            "value": 77823.14409909892,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatch",
            "value": 5385.403627962254,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatchFlush",
            "value": 5407.334739665293,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSet",
            "value": 77816.93464781663,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatch",
            "value": 6528.7483189696195,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatchFlush",
            "value": 6496.1390502191725,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncList",
            "value": 177870.27487550455,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncSet",
            "value": 77712.58119670773,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommands",
            "value": 89.79772234496485,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommandsAsBatch",
            "value": 376.7394537375038,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writeKeyedCommand",
            "value": 31.145197628499915,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writePlainCommand",
            "value": 7.769346108229162,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSet",
            "value": 100269.77873900079,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatch",
            "value": 7103.373845285723,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatchFlush",
            "value": 6970.056224062503,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSet",
            "value": 100066.8656304742,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatch",
            "value": 8364.199774368532,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatchFlush",
            "value": 8227.21297693954,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.syncSet",
            "value": 99877.62782576813,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashDirect",
            "value": 40.58837400080244,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashHeap",
            "value": 37.22441043588229,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedDirect",
            "value": 28.768857456378534,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedHeap",
            "value": 31.254513220293326,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeAllSlots",
            "value": 26905.316719162634,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeLowerSlots",
            "value": 32914.78849072849,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusAbsent",
            "value": 1.0037889282193062,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusPresent",
            "value": 0.9928229840704581,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyEstimatedSize",
            "value": 200.05455696606924,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyExactSize",
            "value": 71.07230672728807,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueEstimatedSize",
            "value": 228.68861443191471,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueExactSize",
            "value": 73.6874477009564,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.decodeUtf8Unpooled",
            "value": 4.438919935494137,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeAsciiToBuf",
            "value": 14.933393130226799,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeIsoToBuf",
            "value": 27.918692635447666,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8PlainStringToBuf",
            "value": 18.08176076777091,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8ToBuf",
            "value": 29.81900767791049,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8Unpooled",
            "value": 88.23791712548265,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.decodeUnpooled",
            "value": 4.42104891420882,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.encodeUnpooled",
            "value": 87.65294743893884,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createBatchCommands",
            "value": 847533.0666377482,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createRegularCommands",
            "value": 847482.4515001036,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeAsyncCommand",
            "value": 27.575504179268894,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeCommandInterfaceCommand",
            "value": 170.57070538291615,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.asyncSet",
            "value": 6360.741510031786,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.batchSet",
            "value": 5831.904669799406,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.intercept.InvocationProxyFactoryBenchmark.run",
            "value": 54.58460036945071,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100Elements",
            "value": 387.284045104108,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100ElementsWithResizeElement",
            "value": 575.4082928492967,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16Elements",
            "value": 49.320387225036576,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16ElementsWithResizeElement",
            "value": 123.74550741971743,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureSingleElement",
            "value": 6.8969754335773645,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureZeroElement",
            "value": 0.6365423276315253,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createAsyncCommandUsingByteArrayCodec",
            "value": 27.78632807353359,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingByteArrayCodec",
            "value": 23.123286852876124,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingStringCodec",
            "value": 13.09217574931721,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingByteArrayCodec",
            "value": 14.228272367549247,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingNewStringCodec",
            "value": 69.7182964550298,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingOldStringCodec",
            "value": 69.99803334448498,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndRead",
            "value": 259.2679011430373,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1",
            "value": 187.191928353371,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch10",
            "value": 1761.9737298013401,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch100",
            "value": 16176.616722168048,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1000",
            "value": 142071.02479002677,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisEndpointBenchmark.measureUserWrite",
            "value": 36.869041616177824,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisStateMachineBenchmark.measureDecode",
            "value": 149.20129499419997,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.singleConnection",
            "value": 351.55139168549294,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.twentyConnections",
            "value": 7185.067857097291,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.singleConnection",
            "value": 664.0398059322027,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.twentyConnections",
            "value": 13781.49263351336,
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
          "id": "29ddd1abb27dcdaef4087fb8910a95e19e319b15",
          "message": "Fix issue with array initialization (#3123)",
          "timestamp": "2025-01-11T18:25:35Z",
          "url": "https://github.com/redis/lettuce/commit/29ddd1abb27dcdaef4087fb8910a95e19e319b15"
        },
        "date": 1736819940344,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSet",
            "value": 77225.76751981158,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatch",
            "value": 5326.641293046551,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatchFlush",
            "value": 5497.387290043884,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSet",
            "value": 78338.57306261695,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatch",
            "value": 6370.61919686694,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatchFlush",
            "value": 6347.184416161826,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncList",
            "value": 178487.3974607814,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncSet",
            "value": 76423.64545017507,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommands",
            "value": 90.11623610678885,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommandsAsBatch",
            "value": 414.01100862767305,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writeKeyedCommand",
            "value": 31.113048422667525,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writePlainCommand",
            "value": 7.7937565401847575,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSet",
            "value": 98830.02897530778,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatch",
            "value": 6971.640345007721,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatchFlush",
            "value": 6886.352153010866,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSet",
            "value": 99524.07918989987,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatch",
            "value": 8059.788184172949,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatchFlush",
            "value": 8073.50177252888,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.syncSet",
            "value": 98507.1004212834,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashDirect",
            "value": 40.65423270388917,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashHeap",
            "value": 37.39001418006211,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedDirect",
            "value": 28.199039660053103,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedHeap",
            "value": 32.177563567889734,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeAllSlots",
            "value": 65738.21860877829,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeLowerSlots",
            "value": 35883.08163400211,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusAbsent",
            "value": 1.0046594694607496,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusPresent",
            "value": 1.0009668545670145,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyEstimatedSize",
            "value": 215.62780716487882,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyExactSize",
            "value": 78.908065356409,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueEstimatedSize",
            "value": 213.88999021121558,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueExactSize",
            "value": 74.20873572865911,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.decodeUtf8Unpooled",
            "value": 4.424647841356484,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeAsciiToBuf",
            "value": 15.036390198467592,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeIsoToBuf",
            "value": 27.94101684691723,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8PlainStringToBuf",
            "value": 18.045196955496877,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8ToBuf",
            "value": 29.716230430319797,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8Unpooled",
            "value": 86.612980230244,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.decodeUnpooled",
            "value": 4.479534599515903,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.encodeUnpooled",
            "value": 86.6417795327878,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createBatchCommands",
            "value": 872099.2238804313,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createRegularCommands",
            "value": 852024.5633076659,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeAsyncCommand",
            "value": 27.76961859957425,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeCommandInterfaceCommand",
            "value": 162.36350414325292,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.asyncSet",
            "value": 6416.128655632424,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.batchSet",
            "value": 5977.979514450678,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.intercept.InvocationProxyFactoryBenchmark.run",
            "value": 58.58895755155055,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100Elements",
            "value": 390.3823802892247,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100ElementsWithResizeElement",
            "value": 577.914879770058,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16Elements",
            "value": 49.06167917555464,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16ElementsWithResizeElement",
            "value": 123.2578446124058,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureSingleElement",
            "value": 6.90096365725473,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureZeroElement",
            "value": 0.6343770397687318,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createAsyncCommandUsingByteArrayCodec",
            "value": 27.864837211123593,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingByteArrayCodec",
            "value": 23.18635496209529,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingStringCodec",
            "value": 13.175295398693516,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingByteArrayCodec",
            "value": 14.308609013835667,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingNewStringCodec",
            "value": 68.35116981047852,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingOldStringCodec",
            "value": 68.11367225362233,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndRead",
            "value": 232.11748812982083,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1",
            "value": 197.38877673461835,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch10",
            "value": 1638.5726749587043,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch100",
            "value": 14258.0423707476,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1000",
            "value": 148711.9631393074,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisEndpointBenchmark.measureUserWrite",
            "value": 38.64551848225523,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisStateMachineBenchmark.measureDecode",
            "value": 147.98292186070788,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.singleConnection",
            "value": 375.25825118425126,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.twentyConnections",
            "value": 7907.025493014478,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.singleConnection",
            "value": 731.4975325471912,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.twentyConnections",
            "value": 14099.782575280266,
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
          "id": "29ddd1abb27dcdaef4087fb8910a95e19e319b15",
          "message": "Fix issue with array initialization (#3123)",
          "timestamp": "2025-01-11T18:25:35Z",
          "url": "https://github.com/redis/lettuce/commit/29ddd1abb27dcdaef4087fb8910a95e19e319b15"
        },
        "date": 1736992736982,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSet",
            "value": 75410.7133565475,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatch",
            "value": 5329.896162096826,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatchFlush",
            "value": 5387.597591210158,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSet",
            "value": 77312.68220037992,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatch",
            "value": 6381.799982772838,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatchFlush",
            "value": 6329.141120776728,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncList",
            "value": 172969.2179959192,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncSet",
            "value": 76566.42252506284,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommands",
            "value": 90.00191748702969,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommandsAsBatch",
            "value": 374.33134358392124,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writeKeyedCommand",
            "value": 31.284972069917906,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writePlainCommand",
            "value": 7.777144012801483,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSet",
            "value": 98278.8715299241,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatch",
            "value": 7030.4574962101615,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatchFlush",
            "value": 6979.569370415722,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSet",
            "value": 100152.408946099,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatch",
            "value": 8221.937066299832,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatchFlush",
            "value": 8292.78703505996,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.syncSet",
            "value": 98233.6833142617,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashDirect",
            "value": 40.66551637882677,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashHeap",
            "value": 37.35257615751516,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedDirect",
            "value": 30.90408222690788,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedHeap",
            "value": 31.338108711042263,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeAllSlots",
            "value": 27092.032784367148,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeLowerSlots",
            "value": 13463.109914579396,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusAbsent",
            "value": 1.0048075037593585,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusPresent",
            "value": 0.9971763501081583,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyEstimatedSize",
            "value": 209.03529065469553,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyExactSize",
            "value": 72.18516007221389,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueEstimatedSize",
            "value": 222.0412467545952,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueExactSize",
            "value": 73.44252991061069,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.decodeUtf8Unpooled",
            "value": 4.420660005667856,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeAsciiToBuf",
            "value": 14.96715559726313,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeIsoToBuf",
            "value": 27.580961345210994,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8PlainStringToBuf",
            "value": 18.063139443381758,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8ToBuf",
            "value": 30.657626940998796,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8Unpooled",
            "value": 88.30030631196485,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.decodeUnpooled",
            "value": 4.4162379741617945,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.encodeUnpooled",
            "value": 87.89746434171403,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createBatchCommands",
            "value": 852958.0809407744,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createRegularCommands",
            "value": 846830.2318932976,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeAsyncCommand",
            "value": 27.538281107404906,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeCommandInterfaceCommand",
            "value": 168.86925175352516,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.asyncSet",
            "value": 6280.786584016986,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.batchSet",
            "value": 5809.692587063709,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.intercept.InvocationProxyFactoryBenchmark.run",
            "value": 57.06586732501862,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100Elements",
            "value": 387.9866961599767,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100ElementsWithResizeElement",
            "value": 575.6448150452869,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16Elements",
            "value": 49.02348249129535,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16ElementsWithResizeElement",
            "value": 123.2538510329952,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureSingleElement",
            "value": 6.882336701933527,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureZeroElement",
            "value": 0.6318679436992921,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createAsyncCommandUsingByteArrayCodec",
            "value": 27.749976165140765,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingByteArrayCodec",
            "value": 23.136301969053516,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingStringCodec",
            "value": 13.108567646691665,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingByteArrayCodec",
            "value": 14.174014196018272,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingNewStringCodec",
            "value": 69.69766502546386,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingOldStringCodec",
            "value": 67.35785159446122,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndRead",
            "value": 222.59399272503657,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1",
            "value": 178.2033562768247,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch10",
            "value": 1761.7460127014983,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch100",
            "value": 14136.399835462298,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1000",
            "value": 157975.62944079368,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisEndpointBenchmark.measureUserWrite",
            "value": 36.8559592454182,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisStateMachineBenchmark.measureDecode",
            "value": 151.3341075253957,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.singleConnection",
            "value": 356.82844677621284,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.twentyConnections",
            "value": 7344.815544893594,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.singleConnection",
            "value": 690.3799347015572,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.twentyConnections",
            "value": 13687.22495074855,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "ggivo",
            "username": "ggivo",
            "email": "ivo.gaydazhiev@redis.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "68f2a1da1afb23e1935d8232aa76f12ae264a3ab",
          "message": "Migrate JSON tests infra to use client-lilb-test  (#3128)\n\n* Migrate JSON tests infra to use client-lilb-test image to support running tests against Redis CE 8.0\r\n\r\n* Remove legacy JSON test Docker image\r\n\r\n* formating\r\n\r\n* propagate REDIS_STACK_VERSION to failsafe",
          "timestamp": "2025-01-16T08:50:45Z",
          "url": "https://github.com/redis/lettuce/commit/68f2a1da1afb23e1935d8232aa76f12ae264a3ab"
        },
        "date": 1737079130869,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSet",
            "value": 78260.66379299252,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatch",
            "value": 5456.113027322973,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.asyncSetBatchFlush",
            "value": 5406.259307289822,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSet",
            "value": 79341.47080560605,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatch",
            "value": 6445.0381223013665,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.reactiveSetBatchFlush",
            "value": 6409.954106087433,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncList",
            "value": 176302.3381274228,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.RedisClientBenchmark.syncSet",
            "value": 78151.45630108632,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommands",
            "value": 89.90544617529568,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.write3KeyedCommandsAsBatch",
            "value": 372.8392608586616,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writeKeyedCommand",
            "value": 31.289443915294516,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.ClusterDistributionChannelWriterBenchmark.writePlainCommand",
            "value": 7.764310827120681,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSet",
            "value": 98412.53302805983,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatch",
            "value": 7057.287421403807,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.asyncSetBatchFlush",
            "value": 6946.674451848002,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSet",
            "value": 100886.96145616172,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatch",
            "value": 8255.05190041177,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.reactiveSetBatchFlush",
            "value": 8202.186284640737,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.RedisClusterClientBenchmark.syncSet",
            "value": 100601.75708932678,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashDirect",
            "value": 40.68584951543409,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashHeap",
            "value": 37.30147919202346,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedDirect",
            "value": 28.128358043116897,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.SlotHashBenchmark.measureSlotHashTaggedHeap",
            "value": 32.26832380914544,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeAllSlots",
            "value": 59741.87966294053,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.createClusterNodeLowerSlots",
            "value": 35862.40758078268,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusAbsent",
            "value": 1.0035666067685947,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.cluster.models.partitions.RedisClusterNodeBenchmark.querySlotStatusPresent",
            "value": 0.9942366220677951,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyEstimatedSize",
            "value": 204.02551466307375,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeKeyExactSize",
            "value": 70.67392169528476,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueEstimatedSize",
            "value": 216.03982977898562,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.ExactVsEstimatedSizeCodecBenchmark.encodeValueExactSize",
            "value": 73.50659943046303,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.decodeUtf8Unpooled",
            "value": 4.411292761565017,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeAsciiToBuf",
            "value": 14.96589441325204,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeIsoToBuf",
            "value": 28.185785204969555,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8PlainStringToBuf",
            "value": 18.057900722453816,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8ToBuf",
            "value": 29.72461615459156,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.StringCodecBenchmark.encodeUtf8Unpooled",
            "value": 93.95441368290368,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.decodeUnpooled",
            "value": 4.408593342926205,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.codec.Utf8StringCodecBenchmark.encodeUnpooled",
            "value": 86.16710548881407,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createBatchCommands",
            "value": 854730.5362890584,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.createRegularCommands",
            "value": 859561.3828716992,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeAsyncCommand",
            "value": 27.617981405014763,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandFactoryBenchmark.executeCommandInterfaceCommand",
            "value": 169.67065055565996,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.asyncSet",
            "value": 6396.969526265509,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.RedisCommandsBenchmark.batchSet",
            "value": 6120.07371529408,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.dynamic.intercept.InvocationProxyFactoryBenchmark.run",
            "value": 54.673813098419465,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100Elements",
            "value": 389.31331789121595,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure100ElementsWithResizeElement",
            "value": 573.9930283460819,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16Elements",
            "value": 48.93305669786936,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measure16ElementsWithResizeElement",
            "value": 123.6174624102152,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureSingleElement",
            "value": 6.907142393316397,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.output.ValueListOutputBenchmark.measureZeroElement",
            "value": 0.634966029535996,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createAsyncCommandUsingByteArrayCodec",
            "value": 27.782843544732174,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingByteArrayCodec",
            "value": 23.154226170258195,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.createCommandUsingStringCodec",
            "value": 13.094851927926564,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingByteArrayCodec",
            "value": 14.225518838542708,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingNewStringCodec",
            "value": 69.7165281811173,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandBenchmark.encodeCommandUsingOldStringCodec",
            "value": 69.61965105119756,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndRead",
            "value": 239.24708895812984,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1",
            "value": 162.02328201691915,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch10",
            "value": 1795.1393452110194,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch100",
            "value": 14045.174213553279,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.CommandHandlerBenchmark.measureNettyWriteAndReadBatch1000",
            "value": 144859.3248604624,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisEndpointBenchmark.measureUserWrite",
            "value": 36.909698585657566,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.protocol.RedisStateMachineBenchmark.measureDecode",
            "value": 160.10956259345585,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.singleConnection",
            "value": 368.3966968252002,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.AsyncConnectionPoolBenchmark.twentyConnections",
            "value": 7538.762922619273,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.singleConnection",
            "value": 696.4990995613626,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.lettuce.core.support.GenericConnectionPoolBenchmark.twentyConnections",
            "value": 13320.31599510366,
            "unit": "ns/op",
            "extra": "iterations: 10\nforks: 1\nthreads: 1"
          }
        ]
      }
    ]
  }
}