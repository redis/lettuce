package io.lettuce.core.masterreplica;

import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import reactor.util.function.Tuples;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;

/**
 * @author Mark Paluch
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConnectionsUnitTests {

    @Mock
    private StatefulRedisConnection<String, String> connection1;

    @BeforeEach
    void before() {
        when(connection1.closeAsync()).thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    void shouldCloseConnectionCompletingAfterCloseSignal() {

        Connections connections = new Connections(5, Collections.emptyList());
        connections.closeAsync();

        verifyNoInteractions(connection1);

        connections.onAccept(Tuples.of(RedisURI.create("localhost", 6379), connection1));

        verify(connection1).closeAsync();
    }

}
