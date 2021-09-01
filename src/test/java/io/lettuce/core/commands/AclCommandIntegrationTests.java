/*
 * Copyright 2011-2021 the original author or authors.
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
package io.lettuce.core.commands;

import static org.assertj.core.api.Assertions.*;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.AclCategory;
import io.lettuce.core.AclSetuserArgs;
import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.TestSupport;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.condition.EnabledOnCommand;

/**
 * Integration tests for ACL commands.
 *
 * @author Mikhael Sokolov
 * @author Mark Paluch
 */
@ExtendWith(LettuceExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledOnCommand("ACL")
public class AclCommandIntegrationTests extends TestSupport {

    private final RedisCommands<String, String> redis;

    @Inject
    protected AclCommandIntegrationTests(RedisCommands<String, String> redis) {
        this.redis = redis;
    }

    @BeforeEach
    void setUp() {
        redis.flushall();
        redis.aclUsers().stream().filter(o -> !"default".equals(o)).forEach(redis::aclDeluser);
        redis.aclLogReset();

    }

    @Test
    public void aclCat() {
        assertThat(redis.aclCat()).isNotEmpty();
        assertThat(redis.aclCat(AclCategory.SLOW)).isNotEmpty();
    }

    @Test
    void aclDeluser() {
        assertThat(redis.aclDeluser("non-existing")).isZero();
    }

    @Test
    void aclGenpass() {
        assertThat(redis.aclGenpass()).hasSize(64);
        assertThat(redis.aclGenpass(128)).hasSize(32);
    }

    @Test
    void aclGetuser() {
        assertThat(redis.aclGetuser("default")).contains("flags");
    }

    @Test
    void aclLoad() {
        assertThatThrownBy(redis::aclLoad).isInstanceOf(RedisCommandExecutionException.class).hasMessageContaining("ERR This Redis instance is not configured to use an ACL file.");
    }

    @Test
    void aclLog() {
        assertThat(redis.aclLogReset()).isEqualTo("OK");
        assertThatThrownBy(() -> redis.auth("non-existing1", "foobar"));
        assertThatThrownBy(() -> redis.auth("non-existing2", "foobar"));
        assertThat(redis.aclLog()).hasSize(2).first().hasFieldOrProperty("reason");
        assertThat(redis.aclLog(1)).hasSize(1);
        assertThat(redis.aclLogReset()).isEqualTo("OK");
        assertThat(redis.aclLog()).isEmpty();
    }

    @Test
    void aclList() {
        assertThat(redis.aclList()).hasSize(1).first().asString().contains("user default");
    }

    @Test
    void aclSave() {
        assertThatThrownBy(redis::aclSave).isInstanceOf(RedisCommandExecutionException.class).hasMessageContaining("ERR This Redis instance is not configured to use an ACL file.");
    }

    @Test
    void aclSetuser() {
        assertThat(redis.aclDeluser("foo")).isNotNull();
        AclSetuserArgs args = AclSetuserArgs.Builder.on().addCommand(CommandType.GET).keyPattern("objects:*").addPassword("foobared");
        assertThat(redis.aclSetuser("foo", args)).isEqualTo("OK");
        assertThat(redis.aclGetuser("foo")).contains("commands").contains("passwords").contains("keys");
        assertThat(redis.aclDeluser("foo")).isNotNull();
    }

    @Test
    void aclSetuserWithCategories() {
        assertThat(redis.aclDeluser("foo")).isNotNull();
        AclSetuserArgs args = AclSetuserArgs.Builder.on().addCategory(AclCategory.CONNECTION);
        assertThat(redis.aclSetuser("foo", args)).isEqualTo("OK");
        assertThat(redis.aclGetuser("foo")).contains("-@all +@connection");
        assertThat(redis.aclDeluser("foo")).isNotNull();
    }

    @Test
    void aclUsers() {
        assertThat(redis.aclUsers()).hasSize(1).first().isEqualTo("default");
    }

    @Test
    void aclWhoami() {
        assertThat(redis.aclWhoami()).isEqualTo("default");
    }
}
