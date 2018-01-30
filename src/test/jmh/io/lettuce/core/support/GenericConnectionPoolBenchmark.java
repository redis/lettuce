/*
 * Copyright 2017-2018 the original author or authors.
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
package io.lettuce.core.support;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.openjdk.jmh.annotations.*;

import io.lettuce.core.EmptyRedisChannelWriter;
import io.lettuce.core.EmptyStatefulRedisConnection;
import io.lettuce.core.api.StatefulRedisConnection;

/**
 * @author Mark Paluch
 */
@State(Scope.Benchmark)
public class GenericConnectionPoolBenchmark {

    private GenericObjectPool<StatefulRedisConnection<String, String>> pool;
    private StatefulRedisConnection[] holder = new StatefulRedisConnection[20];

    @Setup
    public void setup() {

        GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setMinIdle(0);
        config.setMaxIdle(20);
        config.setMaxTotal(20);

        pool = ConnectionPoolSupport.createGenericObjectPool(() -> new EmptyStatefulRedisConnection(
                EmptyRedisChannelWriter.INSTANCE), config);
    }

    @TearDown(Level.Iteration)
    public void tearDown() {
        pool.clear();
    }

    @Benchmark
    public void singleConnection() throws Exception {
        pool.returnObject(pool.borrowObject());
    }

    @Benchmark
    public void twentyConnections() throws Exception {

        for (int i = 0; i < holder.length; i++) {
            holder[i] = pool.borrowObject();
        }

        for (int i = 0; i < holder.length; i++) {
            pool.returnObject(holder[i]);
        }
    }
}
