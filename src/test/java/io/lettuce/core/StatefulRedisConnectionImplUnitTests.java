package io.lettuce.core;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.AsyncCommand;
import io.lettuce.core.protocol.PushHandler;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.tracing.Tracing;
import io.lettuce.test.ReflectionTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class StatefulRedisConnectionImplUnitTests extends TestSupport {

    RedisCommandBuilder<String, String> commandBuilder = new RedisCommandBuilder<>(StringCodec.UTF8);
    StatefulRedisConnectionImpl<String,String> connection;

    @Mock
    RedisAsyncCommandsImpl<String, String> asyncCommands;

    @Mock
    PushHandler pushHandler;

    @Mock
    RedisChannelWriter writer;

    @Mock
    ClientResources clientResources;

    @Mock
    Tracing tracing;

    @BeforeEach
    void setup() throws NoSuchFieldException, IllegalAccessException {
        when(writer.getClientResources()).thenReturn(clientResources);
        when(clientResources.tracing()).thenReturn(tracing);
        when(tracing.isEnabled()).thenReturn(false);
        when(asyncCommands.auth(any(CharSequence.class)))
                .thenAnswer( invocation -> {
                    String pass = invocation.getArgument(0);
                    AsyncCommand<String, String, String> auth = new AsyncCommand<>(commandBuilder.auth(pass));
                    auth.complete();
                    return auth;
                });
        when(asyncCommands.auth(anyString(), any(CharSequence.class)))
                .thenAnswer( invocation -> {
                    String user = invocation.getArgument(0);  // Capture username
                    String pass = invocation.getArgument(1); // Capture password
                    AsyncCommand<String, String, String> auth = new AsyncCommand<>(commandBuilder.auth(user, pass));
                    auth.complete();
                    return auth;
                });

        Field asyncField = StatefulRedisConnectionImpl.class.getDeclaredField("async");
        asyncField.setAccessible(true);


        connection = new StatefulRedisConnectionImpl<>(writer, pushHandler, StringCodec.UTF8, Duration.ofSeconds(1));
        asyncField.set(connection,asyncCommands);
    }

    @Test
    public void testSetCredentialsWhenCredentialsAreNull() {
        connection.setCredentials(null);

        verify(asyncCommands, never()).auth(any(CharSequence.class));
        verify(asyncCommands, never()).auth(anyString(), any(CharSequence.class));
    }

    @Test
    void testSetCredentialsDispatchesAuthWhenNotInTransaction() {
        connection.setCredentials(new StaticRedisCredentials("user", "pass".toCharArray()));
        verify(asyncCommands).auth(eq("user"), eq("pass"));
    }


    @Test
    void testSetCredentialsDoesNotDispatchAuthIfInTransaction() {
        AtomicBoolean inTransaction = ReflectionTestUtils.getField(connection, "inTransaction");
        inTransaction.set(true);

        connection.setCredentials(new StaticRedisCredentials("user", "pass".toCharArray()));

        verify(asyncCommands, never()).auth(any(CharSequence.class));
        verify(asyncCommands, never()).auth(anyString(), any(CharSequence.class));
    }


    @Test
    void testSetCredentialsDispatchesAuthAfterTransaction() {
        AtomicBoolean inTransaction = ReflectionTestUtils.getField(connection, "inTransaction");

        connection.dispatch(commandBuilder.multi());
        assertThat(inTransaction.get()).isTrue();

        connection.setCredentials(new StaticRedisCredentials("user", "pass".toCharArray()));
        connection.dispatch(commandBuilder.discard());

        assertThat(inTransaction.get()).isFalse();

        verify(asyncCommands).auth(eq("user"), eq("pass"));
    }

    @Test
    void testSetCredentialsDispatchesAuthAfterTransactionInAnotherThread() throws InterruptedException {
        AtomicBoolean inTransaction = ReflectionTestUtils.getField(connection, "inTransaction");

        connection.dispatch(commandBuilder.multi());
        assertThat(inTransaction.get()).isTrue();

        Thread thread = new Thread(() -> {
            connection.setCredentials(new StaticRedisCredentials("user", "pass".toCharArray()));
        });
        thread.start();

        connection.dispatch(commandBuilder.discard());

        thread.join();

        assertThat(inTransaction.get()).isFalse();
        verify(asyncCommands).auth(eq("user"), eq("pass"));
    }

}
