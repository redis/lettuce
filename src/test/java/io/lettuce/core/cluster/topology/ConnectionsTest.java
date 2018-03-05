package io.lettuce.core.cluster.topology;

import static org.mockito.Mockito.verify;

import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ConnectionsTest {

    @Mock
    private StatefulRedisConnection<String, String> connection1;

    @Mock
    private StatefulRedisConnection<String, String> connection2;

    @Test
    public void shouldCloseAllConnections() {
        final Connections iut = new Connections();
        iut.addConnection(RedisURI.create("127.0.0.1", 7380), connection1);
        iut.addConnection(RedisURI.create("127.0.0.1", 7381), connection2);

        iut.close();

        verify(connection1).close();
        verify(connection2).close();
    }
}