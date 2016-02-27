package com.lambdaworks.redis.masterslave;

import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.TestSettings;
import com.lambdaworks.redis.models.role.RedisInstance;
import com.lambdaworks.redis.models.role.RedisNodeDescription;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class MasterSlaveTopologyProviderTest {

    private MasterSlaveTopologyProvider sut = new MasterSlaveTopologyProvider(null, RedisURI.Builder.redis(TestSettings.host(),
            TestSettings.port()).build());

    @Test
    public void testMaster() throws Exception {

        String info = "# Replication\r\n" + "role:master\r\n" + "connected_slaves:1\r\n" + "master_repl_offset:56276\r\n"
                + "repl_backlog_active:1\r\n";

        List<RedisNodeDescription> result = sut.getNodesFromInfo(info);
        assertThat(result).hasSize(1);

        RedisNodeDescription redisNodeDescription = result.get(0);

        assertThat(redisNodeDescription.getRole()).isEqualTo(RedisInstance.Role.MASTER);
        assertThat(redisNodeDescription.getUri().getHost()).isEqualTo(TestSettings.host());
        assertThat(redisNodeDescription.getUri().getPort()).isEqualTo(TestSettings.port());
    }

    @Test
    public void testMasterIsASlave() throws Exception {

        String info = "# Replication\r\n" + "role:slave\r\n" + "connected_slaves:1\r\n" + "master_repl_offset:56276\r\n"
                + "repl_backlog_active:1\r\n";

        List<RedisNodeDescription> result = sut.getNodesFromInfo(info);
        assertThat(result).hasSize(1);

        RedisNodeDescription redisNodeDescription = result.get(0);

        assertThat(redisNodeDescription.getRole()).isEqualTo(RedisInstance.Role.SLAVE);
    }

    @Test(expected = IllegalStateException.class)
    public void noRole() throws Exception {

        String info = "# Replication\r\n" + "connected_slaves:1\r\n" + "master_repl_offset:56276\r\n"
                + "repl_backlog_active:1\r\n";

        sut.getNodesFromInfo(info);
    }

    @Test(expected = IllegalStateException.class)
    public void noInvalidRole() throws Exception {

        String info = "# Replication\r\n" + "role:abc\r\n" + "master_repl_offset:56276\r\n" + "repl_backlog_active:1\r\n";

        sut.getNodesFromInfo(info);
    }

    @Test
    public void testSlaves() throws Exception {

        String info = "# Replication\r\n" + "role:master\r\n"
                + "slave0:ip=127.0.0.1,port=6483,state=online,offset=56276,lag=0\r\n"
                + "slave1:ip=127.0.0.1,port=6484,state=online,offset=56276,lag=0\r\n" + "master_repl_offset:56276\r\n"
                + "repl_backlog_active:1\r\n";

        List<RedisNodeDescription> result = sut.getNodesFromInfo(info);
        assertThat(result).hasSize(3);

        RedisNodeDescription slave1 = result.get(1);

        assertThat(slave1.getRole()).isEqualTo(RedisInstance.Role.SLAVE);
        assertThat(slave1.getUri().getHost()).isEqualTo("127.0.0.1");
        assertThat(slave1.getUri().getPort()).isEqualTo(6483);

        RedisNodeDescription slave2 = result.get(2);

        assertThat(slave2.getRole()).isEqualTo(RedisInstance.Role.SLAVE);
        assertThat(slave2.getUri().getHost()).isEqualTo("127.0.0.1");
        assertThat(slave2.getUri().getPort()).isEqualTo(6484);
    }
}
