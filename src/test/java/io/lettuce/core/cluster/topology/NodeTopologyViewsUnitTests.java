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
package io.lettuce.core.cluster.topology;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.lettuce.core.RedisURI;

/**
 * @author Mark Paluch
 */
class NodeTopologyViewsUnitTests {

    @Test
    void shouldReuseKnownUris() {

        RedisURI localhost = RedisURI.create("127.0.0.1", 6479);
        RedisURI otherhost = RedisURI.create("127.0.0.2", 7000);

        RedisURI host3 = RedisURI.create("127.0.0.3", 7000);

        String viewByLocalhost = "1 127.0.0.1:6479 master,myself - 0 1401258245007 2 connected 8000-11999\n"
                + "2 127.0.0.2:7000 master - 111 1401258245007 222 connected 7000 12000 12002-16383\n"
                + "3 127.0.0.3:7000 master - 111 1401258245007 222 connected 7000 12000 12002-16383\n";

        String viewByOtherhost = "1 127.0.0.2:6479 master - 0 1401258245007 2 connected 8000-11999\n"
                + "2 127.0.0.2:7000 master,myself - 111 1401258245007 222 connected 7000 12000 12002-16383\n"
                + "3 127.0.0.3:7000 master - 111 1401258245007 222 connected 7000 12000 12002-16383\n";

        NodeTopologyView localhostView = new NodeTopologyView(localhost, viewByLocalhost, "", 0);
        NodeTopologyView otherhostView = new NodeTopologyView(otherhost, viewByOtherhost, "", 0);

        NodeTopologyViews nodeTopologyViews = new NodeTopologyViews(Arrays.asList(localhostView, otherhostView));

        Set<RedisURI> clusterNodes = nodeTopologyViews.getClusterNodes();
        assertThat(clusterNodes).contains(localhost, otherhost, host3);
    }

    @Test
    void shouldFailWithoutOwnPartition() {

        RedisURI localhost = RedisURI.create("127.0.0.1", 6479);

        String viewByLocalhost = "1 127.0.0.1:6479 master - 0 1401258245007 2 connected 8000-11999\n";

        assertThatThrownBy(() -> new NodeTopologyView(localhost, viewByLocalhost, "", 0).getOwnPartition())
                .isInstanceOf(IllegalStateException.class);
    }

}
