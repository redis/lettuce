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
package io.lettuce.core.masterslave;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import io.lettuce.core.RedisURI;
import io.lettuce.core.models.role.RedisInstance;

/**
 * @author Mark Paluch
 */
class MasterSlaveUtilsUnitTests {

    @Test
    void isChangedShouldReturnFalse() {

        RedisMasterSlaveNode master = new RedisMasterSlaveNode("host", 1234, RedisURI.create("host", 111),
                RedisInstance.Role.MASTER);
        RedisMasterSlaveNode replica = new RedisMasterSlaveNode("host", 234, RedisURI.create("host", 234),
                RedisInstance.Role.SLAVE);

        RedisMasterSlaveNode newmaster = new RedisMasterSlaveNode("host", 1234, RedisURI.create("host", 555),
                RedisInstance.Role.MASTER);
        RedisMasterSlaveNode newslave = new RedisMasterSlaveNode("host", 234, RedisURI.create("host", 666),
                RedisInstance.Role.SLAVE);

        assertThat(MasterSlaveUtils.isChanged(Arrays.asList(master, replica), Arrays.asList(newmaster, newslave))).isFalse();
        assertThat(MasterSlaveUtils.isChanged(Arrays.asList(replica, master), Arrays.asList(newmaster, newslave))).isFalse();

        assertThat(MasterSlaveUtils.isChanged(Arrays.asList(newmaster, newslave), Arrays.asList(master, replica))).isFalse();
        assertThat(MasterSlaveUtils.isChanged(Arrays.asList(newmaster, newslave), Arrays.asList(replica, master))).isFalse();
    }

    @Test
    void isChangedShouldReturnTrueBecauseSlaveIsGone() {

        RedisMasterSlaveNode master = new RedisMasterSlaveNode("host", 1234, RedisURI.create("host", 111),
                RedisInstance.Role.MASTER);
        RedisMasterSlaveNode replica = new RedisMasterSlaveNode("host", 234, RedisURI.create("host", 234),
                RedisInstance.Role.MASTER);

        RedisMasterSlaveNode newmaster = new RedisMasterSlaveNode("host", 1234, RedisURI.create("host", 111),
                RedisInstance.Role.MASTER);

        assertThat(MasterSlaveUtils.isChanged(Arrays.asList(master, replica), Arrays.asList(newmaster))).isTrue();
    }

    @Test
    void isChangedShouldReturnTrueBecauseHostWasMigrated() {

        RedisMasterSlaveNode master = new RedisMasterSlaveNode("host", 1234, RedisURI.create("host", 111),
                RedisInstance.Role.MASTER);
        RedisMasterSlaveNode replica = new RedisMasterSlaveNode("host", 234, RedisURI.create("host", 234),
                RedisInstance.Role.SLAVE);

        RedisMasterSlaveNode newmaster = new RedisMasterSlaveNode("host", 1234, RedisURI.create("host", 555),
                RedisInstance.Role.MASTER);
        RedisMasterSlaveNode newslave = new RedisMasterSlaveNode("newhost", 234, RedisURI.create("newhost", 666),
                RedisInstance.Role.SLAVE);

        assertThat(MasterSlaveUtils.isChanged(Arrays.asList(master, replica), Arrays.asList(newmaster, newslave))).isTrue();
        assertThat(MasterSlaveUtils.isChanged(Arrays.asList(replica, master), Arrays.asList(newmaster, newslave))).isTrue();
        assertThat(MasterSlaveUtils.isChanged(Arrays.asList(newmaster, newslave), Arrays.asList(master, replica))).isTrue();
        assertThat(MasterSlaveUtils.isChanged(Arrays.asList(newslave, newmaster), Arrays.asList(master, replica))).isTrue();
    }

    @Test
    void isChangedShouldReturnTrueBecauseRolesSwitched() {

        RedisMasterSlaveNode master = new RedisMasterSlaveNode("host", 1234, RedisURI.create("host", 111),
                RedisInstance.Role.MASTER);
        RedisMasterSlaveNode replica = new RedisMasterSlaveNode("host", 234, RedisURI.create("host", 234),
                RedisInstance.Role.MASTER);

        RedisMasterSlaveNode newslave = new RedisMasterSlaveNode("host", 1234, RedisURI.create("host", 111),
                RedisInstance.Role.SLAVE);
        RedisMasterSlaveNode newmaster = new RedisMasterSlaveNode("host", 234, RedisURI.create("host", 234),
                RedisInstance.Role.MASTER);

        assertThat(MasterSlaveUtils.isChanged(Arrays.asList(master, replica), Arrays.asList(newmaster, newslave))).isTrue();
        assertThat(MasterSlaveUtils.isChanged(Arrays.asList(master, replica), Arrays.asList(newslave, newmaster))).isTrue();
    }

}
