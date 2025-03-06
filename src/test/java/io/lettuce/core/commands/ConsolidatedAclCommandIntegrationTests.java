/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
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
package io.lettuce.core.commands;

import io.lettuce.core.*;
import io.lettuce.core.api.sync.RedisCommands;

import io.lettuce.test.condition.RedisConditions;
import org.junit.jupiter.api.*;

import java.util.Arrays;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration tests for ACL commands with Redis modules since Redis 8.0.
 *
 * @author M Sazzadul Hoque
 */
@Tag(INTEGRATION_TEST)
public class ConsolidatedAclCommandIntegrationTests {

    private static RedisClient client;

    private static RedisCommands<String, String> redis;

    @BeforeAll
    public static void setup() {
        RedisURI redisURI = RedisURI.Builder.redis("127.0.0.1").withPort(16379).build();

        client = RedisClient.create(redisURI);
        redis = client.connect().sync();
        assumeTrue(RedisConditions.of(redis).hasVersionGreaterOrEqualsTo("7.9"));
    }

    @AfterAll
    static void teardown() {
        if (client != null) {
            client.shutdown();
        }
    }

    @BeforeEach
    void setUp() {
        redis.flushall();
        redis.aclUsers().stream().filter(o -> !"default".equals(o)).forEach(redis::aclDeluser);
        redis.aclLogReset();
    }

    @Test
    public void listACLCategoriesTest() {
        assertThat(redis.aclCat()).containsAll(Arrays.asList(AclCategory.BLOOM, AclCategory.CUCKOO, AclCategory.CMS,
                AclCategory.TOPK, AclCategory.TDIGEST, AclCategory.SEARCH, AclCategory.TIMESERIES, AclCategory.JSON));
    }

    @Test
    void grantBloomCommandCatTest() {
        grantModuleCommandCatTest(AclCategory.BLOOM, "bloom");
    }

    @Test
    void grantCuckooCommandCatTest() {
        grantModuleCommandCatTest(AclCategory.CUCKOO, "cuckoo");
    }

    @Test
    void grantCmsCommandCatTest() {
        grantModuleCommandCatTest(AclCategory.CMS, "cms");
    }

    @Test
    void grantTopkCommandCatTest() {
        grantModuleCommandCatTest(AclCategory.TOPK, "topk");
    }

    @Test
    void grantTdigestCommandCatTest() {
        grantModuleCommandCatTest(AclCategory.TDIGEST, "tdigest");
    }

    @Test
    void grantSearchCommandCatTest() {
        grantModuleCommandCatTest(AclCategory.SEARCH, "search");
    }

    @Test
    void grantTimeseriesCommandCatTest() {
        grantModuleCommandCatTest(AclCategory.TIMESERIES, "timeseries");
    }

    @Test
    void grantJsonCommandCatTest() {
        grantModuleCommandCatTest(AclCategory.JSON, "json");
    }

    private void grantModuleCommandCatTest(AclCategory category, String categoryStr) {
        assertThat(redis.aclDeluser("foo")).isNotNull();
        AclSetuserArgs args = AclSetuserArgs.Builder.on().addCategory(category);
        assertThat(redis.aclSetuser("foo", args)).isEqualTo("OK");
        assertThat(redis.aclGetuser("foo")).contains("-@all +@" + categoryStr);
        assertThat(redis.aclDeluser("foo")).isNotNull();
    }

}
