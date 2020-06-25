/*
 * Copyright 2011-2020 the original author or authors.
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
package io.lettuce.core.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import io.lettuce.core.RedisClient;
import io.lettuce.core.cluster.RedisClusterClient;

/**
 * @author Mark Paluch
 * @since 3.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
public class SpringIntegrationTests {

    @Autowired
    @Qualifier("RedisClient1")
    private RedisClient redisClient1;

    @Autowired
    @Qualifier("RedisClient2")
    private RedisClient redisClient2;

    @Autowired
    @Qualifier("RedisClient3")
    private RedisClient redisClient3;

    @Autowired
    @Qualifier("RedisClusterClient1")
    private RedisClusterClient redisClusterClient1;

    @Autowired
    @Qualifier("RedisClusterClient2")
    private RedisClusterClient redisClusterClient2;

    @Test
    public void testSpring() {

        assertThat(redisClient1).isNotNull();
        assertThat(redisClient2).isNotNull();
        assertThat(redisClient3).isNotNull();
        assertThat(redisClusterClient1).isNotNull();
        assertThat(redisClusterClient2).isNotNull();
    }

}
