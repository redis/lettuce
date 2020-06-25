/*
 * Copyright 2018-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.test.resource;

import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.test.settings.TestSettings;

/**
 * @author Mark Paluch
 */
public class DefaultRedisClusterClient {

    private static final DefaultRedisClusterClient instance = new DefaultRedisClusterClient();

    private RedisClusterClient redisClient;

    private DefaultRedisClusterClient() {
        redisClient = RedisClusterClient.create(
                RedisURI.Builder.redis(TestSettings.host(), TestSettings.port(900)).withClientName("my-client").build());
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                FastShutdown.shutdown(redisClient);
            }

        });
    }

    /**
     * Do not close the client.
     *
     * @return the default redis client for the tests.
     */
    public static RedisClusterClient get() {
        instance.redisClient.setOptions(ClusterClientOptions.create());
        return instance.redisClient;
    }

}
