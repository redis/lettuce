window.BENCHMARK_DATA = {
  "lastUpdate": 1732499995663,
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
      }
    ]
  }
}