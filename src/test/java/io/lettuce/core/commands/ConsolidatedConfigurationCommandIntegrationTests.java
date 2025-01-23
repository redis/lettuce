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

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.TestSupport;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.condition.RedisConditions;

import java.util.Collections;
import javax.inject.Inject;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Integration tests for {@link io.lettuce.core.api.sync.RedisServerCommands} with Redis modules since Redis 8.0.
 *
 * @author M Sazzadul Hoque
 */
@Tag(INTEGRATION_TEST)
@ExtendWith(LettuceExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ConsolidatedConfigurationCommandIntegrationTests extends TestSupport {

    private final RedisClient client;

    private final RedisCommands<String, String> redis;

    @Inject
    protected ConsolidatedConfigurationCommandIntegrationTests(RedisClient client, RedisCommands<String, String> redis) {
        this.client = client;
        this.redis = redis;
    }

    @BeforeEach
    void setUp() {
        assumeTrue(RedisConditions.of(redis).hasVersionGreaterOrEqualsTo("7.9"));
        this.redis.flushall();
    }

    @Disabled(value = "failing")
    @Test
    public void setSearchConfigGloballyTest() {
        final String configParam = "search-default-dialect";
        // confirm default - Redis 8.0-M03 has no default dialect
        // assertThat(redis.configGet(configParam)).isEqualTo(Collections.singletonMap(configParam, "1"));
        assertThat(redis.configGet(configParam)).isEmpty();

        try {
            assertThat(redis.configSet(configParam, "2")).isEqualTo("OK");
            assertThat(redis.configGet(configParam)).isEqualTo(Collections.singletonMap(configParam, "2"));
        } finally {
            // restore to default
            assertThat(redis.configSet(configParam, "1")).isEqualTo("OK");
        }
    }

    @Test
    public void setReadOnlySearchConfigTest() {
        assertThatThrownBy(() -> redis.configSet("search-max-doctablesize", "10"))
                .isInstanceOf(RedisCommandExecutionException.class);
    }

    @Test
    public void getSearchConfigSettingTest() {
        assertThat(redis.configGet("search-timeout")).hasSize(0); // Redis 8.0-M03 has no default value
    }

    @Test
    public void getTSConfigSettingTest() {
        assertThat(redis.configGet("ts-retention-policy")).hasSize(1);
    }

    @Test
    public void getBFConfigSettingTest() {
        assertThat(redis.configGet("bf-error-rate")).hasSize(1);
    }

    @Test
    public void getCFConfigSettingTest() {
        assertThat(redis.configGet("cf-initial-size")).hasSize(1);
    }

    @Test
    public void getAllConfigSettings() {
        assertThat(redis.configGet("*")).isNotEmpty();
    }

}
