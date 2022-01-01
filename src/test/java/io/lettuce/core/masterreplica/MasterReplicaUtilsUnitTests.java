/*
 * Copyright 2020-2022 the original author or authors.
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
package io.lettuce.core.masterreplica;

import static org.assertj.core.api.AssertionsForInterfaceTypes.*;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import io.lettuce.core.RedisURI;
import io.lettuce.core.models.role.RedisInstance;

/**
 * Unit tests for {@link RedisMasterReplicaNode}.
 *
 * @author Mark Paluch
 */
class MasterReplicaUtilsUnitTests {

    @Test
    void isChangedShouldReturnFalse() {

        RedisMasterReplicaNode upstream = new RedisMasterReplicaNode("host", 1234, RedisURI.create("host", 111),
                RedisInstance.Role.UPSTREAM);
        RedisMasterReplicaNode replica = new RedisMasterReplicaNode("host", 234, RedisURI.create("host", 234),
                RedisInstance.Role.REPLICA);

        RedisMasterReplicaNode newupstream = new RedisMasterReplicaNode("host", 1234, RedisURI.create("host", 555),
                RedisInstance.Role.UPSTREAM);
        RedisMasterReplicaNode newslave = new RedisMasterReplicaNode("host", 234, RedisURI.create("host", 666),
                RedisInstance.Role.REPLICA);

        assertThat(ReplicaUtils.isChanged(Arrays.asList(upstream, replica), Arrays.asList(newupstream, newslave))).isFalse();
        assertThat(ReplicaUtils.isChanged(Arrays.asList(replica, upstream), Arrays.asList(newupstream, newslave))).isFalse();

        assertThat(ReplicaUtils.isChanged(Arrays.asList(newupstream, newslave), Arrays.asList(upstream, replica))).isFalse();
        assertThat(ReplicaUtils.isChanged(Arrays.asList(newupstream, newslave), Arrays.asList(replica, upstream))).isFalse();
    }

    @Test
    void isChangedShouldReturnTrueBecauseSlaveIsGone() {

        RedisMasterReplicaNode upstream = new RedisMasterReplicaNode("host", 1234, RedisURI.create("host", 111),
                RedisInstance.Role.UPSTREAM);
        RedisMasterReplicaNode replica = new RedisMasterReplicaNode("host", 234, RedisURI.create("host", 234),
                RedisInstance.Role.UPSTREAM);

        RedisMasterReplicaNode newupstream = new RedisMasterReplicaNode("host", 1234, RedisURI.create("host", 111),
                RedisInstance.Role.UPSTREAM);

        assertThat(ReplicaUtils.isChanged(Arrays.asList(upstream, replica), Arrays.asList(newupstream))).isTrue();
    }

    @Test
    void isChangedShouldReturnTrueBecauseHostWasMigrated() {

        RedisMasterReplicaNode upstream = new RedisMasterReplicaNode("host", 1234, RedisURI.create("host", 111),
                RedisInstance.Role.UPSTREAM);
        RedisMasterReplicaNode replica = new RedisMasterReplicaNode("host", 234, RedisURI.create("host", 234),
                RedisInstance.Role.REPLICA);

        RedisMasterReplicaNode newupstream = new RedisMasterReplicaNode("host", 1234, RedisURI.create("host", 555),
                RedisInstance.Role.UPSTREAM);
        RedisMasterReplicaNode newslave = new RedisMasterReplicaNode("newhost", 234, RedisURI.create("newhost", 666),
                RedisInstance.Role.REPLICA);

        assertThat(ReplicaUtils.isChanged(Arrays.asList(upstream, replica), Arrays.asList(newupstream, newslave))).isTrue();
        assertThat(ReplicaUtils.isChanged(Arrays.asList(replica, upstream), Arrays.asList(newupstream, newslave))).isTrue();
        assertThat(ReplicaUtils.isChanged(Arrays.asList(newupstream, newslave), Arrays.asList(upstream, replica))).isTrue();
        assertThat(ReplicaUtils.isChanged(Arrays.asList(newslave, newupstream), Arrays.asList(upstream, replica))).isTrue();
    }

    @Test
    void isChangedShouldReturnTrueBecauseRolesSwitched() {

        RedisMasterReplicaNode upstream = new RedisMasterReplicaNode("host", 1234, RedisURI.create("host", 111),
                RedisInstance.Role.UPSTREAM);
        RedisMasterReplicaNode replica = new RedisMasterReplicaNode("host", 234, RedisURI.create("host", 234),
                RedisInstance.Role.UPSTREAM);

        RedisMasterReplicaNode newslave = new RedisMasterReplicaNode("host", 1234, RedisURI.create("host", 111),
                RedisInstance.Role.REPLICA);
        RedisMasterReplicaNode newupstream = new RedisMasterReplicaNode("host", 234, RedisURI.create("host", 234),
                RedisInstance.Role.UPSTREAM);

        assertThat(ReplicaUtils.isChanged(Arrays.asList(upstream, replica), Arrays.asList(newupstream, newslave))).isTrue();
        assertThat(ReplicaUtils.isChanged(Arrays.asList(upstream, replica), Arrays.asList(newslave, newupstream))).isTrue();
    }
}
