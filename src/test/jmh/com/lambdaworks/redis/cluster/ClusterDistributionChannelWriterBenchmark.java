/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lambdaworks.redis.cluster;

import java.net.SocketAddress;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import com.lambdaworks.redis.*;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.cluster.models.partitions.Partitions;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;
import com.lambdaworks.redis.codec.ByteArrayCodec;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.output.ValueOutput;
import com.lambdaworks.redis.protocol.Command;
import com.lambdaworks.redis.protocol.CommandArgs;
import com.lambdaworks.redis.protocol.CommandType;

/**
 * @author Mark Paluch
 */
@State(Scope.Benchmark)
public class ClusterDistributionChannelWriterBenchmark {

    private final static ClientOptions CLIENT_OPTIONS = ClientOptions.create();
    private final static RedisChannelWriter EMPTY_WRITER = EmptyRedisChannelWriter.INSTANCE;
    private final static EmptyStatefulRedisConnection CONNECTION = EmptyStatefulRedisConnection.INSTANCE;
    private static final ValueOutput<byte[], byte[]> VALUE_OUTPUT = new ValueOutput<>(ByteArrayCodec.INSTANCE);

    private final static Command<byte[], byte[], byte[]> KEYED_COMMAND1 = new Command<>(CommandType.GET, VALUE_OUTPUT,
            new CommandArgs<>(ByteArrayCodec.INSTANCE).addKey("benchmark1".getBytes()));

    private final static Command<byte[], byte[], byte[]> KEYED_COMMAND2 = new Command<>(CommandType.GET, VALUE_OUTPUT,
            new CommandArgs<>(ByteArrayCodec.INSTANCE).addKey("benchmark2".getBytes()));

    private final static Command<byte[], byte[], byte[]> KEYED_COMMAND3 = new Command<>(CommandType.GET, VALUE_OUTPUT,
            new CommandArgs<>(ByteArrayCodec.INSTANCE).addKey("benchmark3".getBytes()));

    private final static Command<byte[], byte[], byte[]> PLAIN_COMMAND = new Command<>(CommandType.GET, VALUE_OUTPUT,
            new CommandArgs<>(ByteArrayCodec.INSTANCE));

    private final static List<Command<byte[], byte[], byte[]>> COMMANDS = Arrays.asList(KEYED_COMMAND1, KEYED_COMMAND2,
            KEYED_COMMAND3);

    private ClusterDistributionChannelWriter writer;

    @Setup
    public void setup() {

        writer = new ClusterDistributionChannelWriter(CLIENT_OPTIONS, EMPTY_WRITER, ClusterEventListener.NO_OP, null);

        Partitions partitions = new Partitions();

        partitions.add(new RedisClusterNode(RedisURI.create("localhost", 1), "1", true, null, 0, 0, 0, IntStream.range(0, 8191)
                .boxed().collect(Collectors.toList()), new HashSet<>()));

        partitions.add(new RedisClusterNode(RedisURI.create("localhost", 2), "2", true, null, 0, 0, 0, IntStream
                .range(8192, SlotHash.SLOT_COUNT).boxed().collect(Collectors.toList()), new HashSet<>()));

        partitions.updateCache();

        writer.setPartitions(partitions);
        writer.setClusterConnectionProvider(new PooledClusterConnectionProvider<>(new RedisClusterClient() {

            @Override
            <K, V> StatefulRedisConnection<K, V> connectToNode(RedisCodec<K, V> codec, String nodeId,
                    RedisChannelWriter clusterWriter, Supplier<SocketAddress> socketAddressSupplier) {
                return CONNECTION;
            }
        }, EMPTY_WRITER, ByteArrayCodec.INSTANCE));

        writer.setPartitions(partitions);
    }

    // @Benchmark
    public void writeKeyedCommand() {
        writer.write(KEYED_COMMAND1);
    }

    @Benchmark
    public void write3KeyedCommands() {
        writer.write(KEYED_COMMAND1);
        writer.write(KEYED_COMMAND2);
        writer.write(KEYED_COMMAND3);
    }

    @Benchmark
    public void write3KeyedCommandsAsBatch() {
        writer.write(COMMANDS);
    }

    // @Benchmark
    public void writePlainCommand() {
        writer.write(PLAIN_COMMAND);
    }
}
