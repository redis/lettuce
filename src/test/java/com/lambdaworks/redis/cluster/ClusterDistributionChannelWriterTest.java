/*
 * Copyright 2011-2016 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import com.lambdaworks.redis.internal.HostAndPort;

/**
 * @author Mark Paluch
 */
public class ClusterDistributionChannelWriterTest {

    @Test
    public void shouldParseAskTargetCorrectly() throws Exception {

        HostAndPort askTarget = ClusterDistributionChannelWriter.getAskTarget("ASK 1234 127.0.0.1:6381");

        assertThat(askTarget.getHostText()).isEqualTo("127.0.0.1");
        assertThat(askTarget.getPort()).isEqualTo(6381);
    }

    @Test
    public void shouldParseIPv6AskTargetCorrectly() throws Exception {

        HostAndPort askTarget = ClusterDistributionChannelWriter.getAskTarget("ASK 1234 1:2:3:4::6:6381");

        assertThat(askTarget.getHostText()).isEqualTo("1:2:3:4::6");
        assertThat(askTarget.getPort()).isEqualTo(6381);
    }

    @Test
    public void shouldParseMovedTargetCorrectly() throws Exception {

        HostAndPort moveTarget = ClusterDistributionChannelWriter.getMoveTarget("MOVED 1234 127.0.0.1:6381");

        assertThat(moveTarget.getHostText()).isEqualTo("127.0.0.1");
        assertThat(moveTarget.getPort()).isEqualTo(6381);
    }

    @Test
    public void shouldParseIPv6MovedTargetCorrectly() throws Exception {

        HostAndPort moveTarget = ClusterDistributionChannelWriter.getMoveTarget("MOVED 1234 1:2:3:4::6:6381");

        assertThat(moveTarget.getHostText()).isEqualTo("1:2:3:4::6");
        assertThat(moveTarget.getPort()).isEqualTo(6381);
    }
}
