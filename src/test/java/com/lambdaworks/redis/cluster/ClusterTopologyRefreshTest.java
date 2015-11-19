package com.lambdaworks.redis.cluster;

import static com.lambdaworks.redis.cluster.ClusterTopologyRefresh.isChanged;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.cluster.models.partitions.ClusterPartitionParser;
import com.lambdaworks.redis.cluster.models.partitions.Partitions;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
@RunWith(MockitoJUnitRunner.class)
public class ClusterTopologyRefreshTest {

    public static final String NODES = "c37ab8396be428403d4e55c0d317348be27ed973 127.0.0.1:7381 master - 111 1401258245007 222 connected 7000 12000 12002-16383\n"
            + "3d005a179da7d8dc1adae6409d47b39c369e992b 127.0.0.1:7380 master - 0 1401258245007 2 disconnected 8000-11999\n";

    private ClusterTopologyRefresh sut;
    @Mock
    private RedisClusterClient client;

    @Before
    public void before() throws Exception {
        sut = new ClusterTopologyRefresh(client);
        when(client.getFirstUri()).thenReturn(RedisURI.create("redis://localhost:999"));

    }

    @Test
    public void isChangedSamePartitions() throws Exception {
        Partitions partitions1 = ClusterPartitionParser.parse(NODES);
        Partitions partitions2 = ClusterPartitionParser.parse(NODES);
        assertThat(isChanged(partitions1, partitions2)).isFalse();
    }

    @Test
    public void isChangedDifferentOrder() throws Exception {
        String nodes1 = "3d005a179da7d8dc1adae6409d47b39c369e992b 127.0.0.1:7380 master,myself - 0 1401258245007 2 disconnected 8000-11999\n"
                + "c37ab8396be428403d4e55c0d317348be27ed973 127.0.0.1:7381 master - 111 1401258245007 222 connected 7000 12000 12002-16383\n";

        String nodes2 = "c37ab8396be428403d4e55c0d317348be27ed973 127.0.0.1:7381 master,myself - 111 1401258245007 222 connected 7000 12000 12002-16383\n"
                + "3d005a179da7d8dc1adae6409d47b39c369e992b 127.0.0.1:7380 master - 0 1401258245007 2 disconnected 8000-11999\n";

        assertThat(nodes1).isNotEqualTo(nodes2);
        Partitions partitions1 = ClusterPartitionParser.parse(nodes1);
        Partitions partitions2 = ClusterPartitionParser.parse(nodes2);
        assertThat(isChanged(partitions1, partitions2)).isFalse();
    }

    @Test
    public void isChangedPortChanged() throws Exception {
        String nodes1 = "3d005a179da7d8dc1adae6409d47b39c369e992b 127.0.0.1:7382 master - 0 1401258245007 2 disconnected 8000-11999\n"
                + "c37ab8396be428403d4e55c0d317348be27ed973 127.0.0.1:7381 master - 111 1401258245007 222 connected 7000 12000 12002-16383\n";

        String nodes2 = "c37ab8396be428403d4e55c0d317348be27ed973 127.0.0.1:7381 master - 111 1401258245007 222 connected 7000 12000 12002-16383\n"
                + "3d005a179da7d8dc1adae6409d47b39c369e992b 127.0.0.1:7380 master - 0 1401258245007 2 disconnected 8000-11999\n";

        Partitions partitions1 = ClusterPartitionParser.parse(nodes1);
        Partitions partitions2 = ClusterPartitionParser.parse(nodes2);
        assertThat(isChanged(partitions1, partitions2)).isFalse();
    }

    @Test
    public void isChangedSlotsChanged() throws Exception {
        String nodes1 = "3d005a179da7d8dc1adae6409d47b39c369e992b 127.0.0.1:7380 master - 0 1401258245007 2 disconnected 8000-11999\n"
                + "c37ab8396be428403d4e55c0d317348be27ed973 127.0.0.1:7381 master - 111 1401258245007 222 connected 7000 12000 12002-16383\n";

        String nodes2 = "3d005a179da7d8dc1adae6409d47b39c369e992b 127.0.0.1:7380 master - 0 1401258245007 2 disconnected 8000-11999\n"
                + "c37ab8396be428403d4e55c0d317348be27ed973 127.0.0.1:7381 master - 111 1401258245007 222 connected 7000 12000 12001-16383\n";

        Partitions partitions1 = ClusterPartitionParser.parse(nodes1);
        Partitions partitions2 = ClusterPartitionParser.parse(nodes2);
        assertThat(isChanged(partitions1, partitions2)).isTrue();
    }

    @Test
    public void isChangedNodeIdChanged() throws Exception {
        String nodes1 = "3d005a179da7d8dc1adae6409d47b39c369e992b 127.0.0.1:7380 master - 0 1401258245007 2 disconnected 8000-11999\n"
                + "c37ab8396be428403d4e55c0d317348be27ed973 127.0.0.1:7381 master - 111 1401258245007 222 connected 7000 12000 12002-16383\n";

        String nodes2 = "3d005a179da7d8dc1adae6409d47b39c369e992aa 127.0.0.1:7380 master - 0 1401258245007 2 disconnected 8000-11999\n"
                + "c37ab8396be428403d4e55c0d317348be27ed973 127.0.0.1:7381 master - 111 1401258245007 222 connected 7000 12000 12002-16383\n";

        Partitions partitions1 = ClusterPartitionParser.parse(nodes1);
        Partitions partitions2 = ClusterPartitionParser.parse(nodes2);
        assertThat(isChanged(partitions1, partitions2)).isTrue();
    }

    @Test
    public void isChangedFlagsChanged() throws Exception {
        String nodes1 = "3d005a179da7d8dc1adae6409d47b39c369e992b 127.0.0.1:7380 slave - 0 1401258245007 2 disconnected 8000-11999\n"
                + "c37ab8396be428403d4e55c0d317348be27ed973 127.0.0.1:7381 master - 111 1401258245007 222 connected 7000 12000 12002-16383\n";

        String nodes2 = "3d005a179da7d8dc1adae6409d47b39c369e992b 127.0.0.1:7380 master - 0 1401258245007 2 disconnected 8000-11999\n"
                + "c37ab8396be428403d4e55c0d317348be27ed973 127.0.0.1:7381 master - 111 1401258245007 222 connected 7000 12000 12002-16383\n";

        Partitions partitions1 = ClusterPartitionParser.parse(nodes1);
        Partitions partitions2 = ClusterPartitionParser.parse(nodes2);
        assertThat(isChanged(partitions1, partitions2)).isTrue();
    }

    @Test
    public void getNodeSpecificViews_1_2() throws Exception {

        Map<RedisURI, ClusterTopologyRefresh.TimedAsyncCommand<String, String, String>> commands = Maps.newHashMap();

        String nodes1 = "1 127.0.0.1:7380 master,myself - 0 1401258245007 2 disconnected 8000-11999\n"
                + "2 127.0.0.1:7381 master - 111 1401258245007 222 connected 7000 12000 12002-16383\n";
        createCommand(commands, 1, nodes1);

        String nodes2 = "1 127.0.0.1:7380 master - 0 1401258245007 2 disconnected 8000-11999\n"
                + "2 127.0.0.1:7381 master,myself - 111 1401258245007 222 connected 7000 12000 12002-16383\n";
        createCommand(commands, 2, nodes2);

        List<Partitions> values = Lists.newArrayList(sut.getNodeSpecificViews(commands).values());

        assertThat(values).hasSize(2);

        for (Partitions value : values) {
            assertThat(value).extracting("nodeId").containsExactly("1", "2");
        }
    }

    @Test
    public void getNodeSpecificViews_2_1() throws Exception {

        Map<RedisURI, ClusterTopologyRefresh.TimedAsyncCommand<String, String, String>> commands = Maps.newHashMap();

        String nodes1 = "1 127.0.0.1:7380 master,myself - 0 1401258245007 2 disconnected 8000-11999\n"
                + "2 127.0.0.1:7381 master - 111 1401258245007 222 connected 7000 12000 12002-16383\n";
        createCommand(commands, 5, nodes1);

        String nodes2 = "1 127.0.0.1:7380 master - 0 1401258245007 2 disconnected 8000-11999\n"
                + "2 127.0.0.1:7381 master,myself - 111 1401258245007 222 connected 7000 12000 12002-16383\n";
        createCommand(commands, 1, nodes2);

        List<Partitions> values = Lists.newArrayList(sut.getNodeSpecificViews(commands).values());

        assertThat(values).hasSize(2);

        for (Partitions value : values) {
            assertThat(value).extracting("nodeId").containsExactly("2", "1");
        }
    }

    protected void createCommand(Map<RedisURI, ClusterTopologyRefresh.TimedAsyncCommand<String, String, String>> commands,
            int duration, String nodes) {
        ClusterTopologyRefresh.TimedAsyncCommand<String, String, String> command1 = sut.createClusterNodesCommand();
        command1.getOutput().set(ByteBuffer.wrap(nodes.getBytes()));
        command1.complete();
        command1.encodedAtNs = 0;
        command1.completedAtNs = duration;

        commands.put(RedisURI.create("redis://localhost:" + duration), command1);
    }
}