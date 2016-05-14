package com.lambdaworks.redis.cluster.topology;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Set;

import org.junit.Test;

import com.lambdaworks.redis.RedisURI;

/**
 * @author Mark Paluch
 */
public class NodeTopologyViewsTest {

    @Test
    public void shouldReuseKnownUris() throws Exception {

        RedisURI localhost = RedisURI.create("localhost", 6479);
        RedisURI otherhost = RedisURI.create("127.0.0.2", 7000);

        RedisURI host3 = RedisURI.create("127.0.0.3", 7000);

        String viewByLocalhost = "1 127.0.0.1:6479 master,myself - 0 1401258245007 2 connected 8000-11999\n"
                + "2 127.0.0.2:7000 master - 111 1401258245007 222 connected 7000 12000 12002-16383\n"
                + "3 127.0.0.3:7000 master - 111 1401258245007 222 connected 7000 12000 12002-16383\n";

        String viewByOtherhost = "1 127.0.0.1:6479 master - 0 1401258245007 2 connected 8000-11999\n"
                + "2 127.0.0.2:7000 master,myself - 111 1401258245007 222 connected 7000 12000 12002-16383\n"
                + "3 127.0.0.3:7000 master - 111 1401258245007 222 connected 7000 12000 12002-16383\n";

        NodeTopologyView localhostView = new NodeTopologyView(localhost, viewByLocalhost, "", 0);
        NodeTopologyView otherhostView = new NodeTopologyView(otherhost, viewByOtherhost, "", 0);

        NodeTopologyViews nodeTopologyViews = new NodeTopologyViews(Arrays.asList(localhostView, otherhostView));

        Set<RedisURI> clusterNodes = nodeTopologyViews.getClusterNodes();
        assertThat(clusterNodes).contains(localhost, otherhost, host3);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldFailWithoutOwnPartition() throws Exception {

        RedisURI localhost = RedisURI.create("localhost", 6479);

        String viewByLocalhost = "1 127.0.0.1:6479 master - 0 1401258245007 2 connected 8000-11999\n";

        new NodeTopologyView(localhost, viewByLocalhost, "", 0);
    }
}