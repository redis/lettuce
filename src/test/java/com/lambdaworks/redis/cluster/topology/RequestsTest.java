package com.lambdaworks.redis.cluster.topology;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.codec.Utf8StringCodec;
import com.lambdaworks.redis.output.StatusOutput;
import com.lambdaworks.redis.protocol.Command;
import com.lambdaworks.redis.protocol.CommandType;

/**
 * @author Mark Paluch
 */
public class RequestsTest {

    @Test
    public void shouldCreateTopologyView() throws Exception {

        RedisURI redisURI = RedisURI.create("localhost", 6379);

        Requests clusterNodesRequests = new Requests();
        String clusterNodesOutput = "1 127.0.0.1:7380 master,myself - 0 1401258245007 2 disconnected 8000-11999\n";
        clusterNodesRequests.addRequest(redisURI, getCommand(clusterNodesOutput));

        Requests clientListRequests = new Requests();
        String clientListOutput = "id=2 addr=127.0.0.1:58919 fd=6 name= age=3 idle=0 flags=N db=0 sub=0 psub=0 multi=-1 qbuf=0 qbuf-free=32768 obl=0 oll=0 omem=0 events=r cmd=client\n";
        clientListRequests.addRequest(redisURI, getCommand(clientListOutput));

        NodeTopologyView nodeTopologyView = NodeTopologyView.from(redisURI, clusterNodesRequests, clientListRequests);

        assertThat(nodeTopologyView.isAvailable()).isTrue();
        assertThat(nodeTopologyView.getConnectedClients()).isEqualTo(1);
        assertThat(nodeTopologyView.getPartitions()).hasSize(1);
        assertThat(nodeTopologyView.getClusterNodes()).isEqualTo(clusterNodesOutput);
        assertThat(nodeTopologyView.getClientList()).isEqualTo(clientListOutput);
    }

    @Test
    public void shouldCreateTopologyViewWithoutClientCount() throws Exception {

        RedisURI redisURI = RedisURI.create("localhost", 6379);

        Requests clusterNodesRequests = new Requests();
        String clusterNodesOutput = "1 127.0.0.1:7380 master,myself - 0 1401258245007 2 disconnected 8000-11999\n";
        clusterNodesRequests.addRequest(redisURI, getCommand(clusterNodesOutput));

        Requests clientListRequests = new Requests();

        NodeTopologyView nodeTopologyView = NodeTopologyView.from(redisURI, clusterNodesRequests, clientListRequests);

        assertThat(nodeTopologyView.isAvailable()).isFalse();
        assertThat(nodeTopologyView.getConnectedClients()).isEqualTo(0);
        assertThat(nodeTopologyView.getPartitions()).isEmpty();
        assertThat(nodeTopologyView.getClusterNodes()).isNull();
    }

    @Test
    public void awaitShouldReturnAwaitedTime() throws Exception {

        RedisURI redisURI = RedisURI.create("localhost", 6379);
        Requests requests = new Requests();
        Command<String, String, String> command = new Command<String, String, String>(CommandType.TYPE,
                new StatusOutput<>(new Utf8StringCodec()));
        TimedAsyncCommand timedAsyncCommand = new TimedAsyncCommand(command);

        requests.addRequest(redisURI, timedAsyncCommand);

        assertThat(requests.await(100, TimeUnit.MILLISECONDS)).isGreaterThan(TimeUnit.MILLISECONDS.toNanos(90));
    }

    @Test
    public void awaitShouldReturnAwaitedTimeIfNegative() throws Exception {

        RedisURI redisURI = RedisURI.create("localhost", 6379);
        Requests requests = new Requests();
        Command<String, String, String> command = new Command<String, String, String>(CommandType.TYPE,
                new StatusOutput<>(new Utf8StringCodec()));
        TimedAsyncCommand timedAsyncCommand = new TimedAsyncCommand(command);

        requests.addRequest(redisURI, timedAsyncCommand);

        assertThat(requests.await(-1, TimeUnit.MILLISECONDS)).isEqualTo(0);

    }

    private TimedAsyncCommand getCommand(String response) {
        Command<String, String, String> command = new Command<String, String, String>(CommandType.TYPE,
                new StatusOutput<>(new Utf8StringCodec()));
        TimedAsyncCommand timedAsyncCommand = new TimedAsyncCommand(command);

        command.getOutput().set(ByteBuffer.wrap(response.getBytes()));
        timedAsyncCommand.complete();
        return timedAsyncCommand;
    }
}