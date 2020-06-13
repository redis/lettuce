/*
 * Copyright 2020 the original author or authors.
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
package io.lettuce.core.masterreplica;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import io.lettuce.core.RedisURI;
import io.lettuce.core.models.role.RedisInstance;

/**
 * @author Mark Paluch
 */
class UpstreamReplicaUtilsUnitTests {

    @Test
    void isChangedShouldReturnFalse() {

        RedisUpstreamReplicaNode upstream = new RedisUpstreamReplicaNode("host", 1234, RedisURI.create("host", 111),
                RedisInstance.Role.UPSTREAM);
        RedisUpstreamReplicaNode replica = new RedisUpstreamReplicaNode("host", 234, RedisURI.create("host", 234),
                RedisInstance.Role.REPLICA);

        RedisUpstreamReplicaNode newupstream = new RedisUpstreamReplicaNode("host", 1234, RedisURI.create("host", 555),
                RedisInstance.Role.UPSTREAM);
        RedisUpstreamReplicaNode newslave = new RedisUpstreamReplicaNode("host", 234, RedisURI.create("host", 666),
                RedisInstance.Role.REPLICA);

        assertThat(ReplicaUtils.isChanged(Arrays.asList(upstream, replica), Arrays.asList(newupstream, newslave))).isFalse();
        assertThat(ReplicaUtils.isChanged(Arrays.asList(replica, upstream), Arrays.asList(newupstream, newslave))).isFalse();

        assertThat(ReplicaUtils.isChanged(Arrays.asList(newupstream, newslave), Arrays.asList(upstream, replica))).isFalse();
        assertThat(ReplicaUtils.isChanged(Arrays.asList(newupstream, newslave), Arrays.asList(replica, upstream))).isFalse();
    }

    @Test
    void isChangedShouldReturnTrueBecauseSlaveIsGone() {

        RedisUpstreamReplicaNode upstream = new RedisUpstreamReplicaNode("host", 1234, RedisURI.create("host", 111),
                RedisInstance.Role.UPSTREAM);
        RedisUpstreamReplicaNode replica = new RedisUpstreamReplicaNode("host", 234, RedisURI.create("host", 234),
                RedisInstance.Role.UPSTREAM);

        RedisUpstreamReplicaNode newupstream = new RedisUpstreamReplicaNode("host", 1234, RedisURI.create("host", 111),
                RedisInstance.Role.UPSTREAM);

        assertThat(ReplicaUtils.isChanged(Arrays.asList(upstream, replica), Arrays.asList(newupstream))).isTrue();
    }

    @Test
    void isChangedShouldReturnTrueBecauseHostWasMigrated() {

        RedisUpstreamReplicaNode upstream = new RedisUpstreamReplicaNode("host", 1234, RedisURI.create("host", 111),
                RedisInstance.Role.UPSTREAM);
        RedisUpstreamReplicaNode replica = new RedisUpstreamReplicaNode("host", 234, RedisURI.create("host", 234),
                RedisInstance.Role.REPLICA);

        RedisUpstreamReplicaNode newupstream = new RedisUpstreamReplicaNode("host", 1234, RedisURI.create("host", 555),
                RedisInstance.Role.UPSTREAM);
        RedisUpstreamReplicaNode newslave = new RedisUpstreamReplicaNode("newhost", 234, RedisURI.create("newhost", 666),
                RedisInstance.Role.REPLICA);

        assertThat(ReplicaUtils.isChanged(Arrays.asList(upstream, replica), Arrays.asList(newupstream, newslave))).isTrue();
        assertThat(ReplicaUtils.isChanged(Arrays.asList(replica, upstream), Arrays.asList(newupstream, newslave))).isTrue();
        assertThat(ReplicaUtils.isChanged(Arrays.asList(newupstream, newslave), Arrays.asList(upstream, replica))).isTrue();
        assertThat(ReplicaUtils.isChanged(Arrays.asList(newslave, newupstream), Arrays.asList(upstream, replica))).isTrue();
    }

    @Test
    void isChangedShouldReturnTrueBecauseRolesSwitched() {

        RedisUpstreamReplicaNode upstream = new RedisUpstreamReplicaNode("host", 1234, RedisURI.create("host", 111),
                RedisInstance.Role.UPSTREAM);
        RedisUpstreamReplicaNode replica = new RedisUpstreamReplicaNode("host", 234, RedisURI.create("host", 234),
                RedisInstance.Role.UPSTREAM);

        RedisUpstreamReplicaNode newslave = new RedisUpstreamReplicaNode("host", 1234, RedisURI.create("host", 111),
                RedisInstance.Role.REPLICA);
        RedisUpstreamReplicaNode newupstream = new RedisUpstreamReplicaNode("host", 234, RedisURI.create("host", 234),
                RedisInstance.Role.UPSTREAM);

        assertThat(ReplicaUtils.isChanged(Arrays.asList(upstream, replica), Arrays.asList(newupstream, newslave))).isTrue();
        assertThat(ReplicaUtils.isChanged(Arrays.asList(upstream, replica), Arrays.asList(newslave, newupstream))).isTrue();
    }
}
