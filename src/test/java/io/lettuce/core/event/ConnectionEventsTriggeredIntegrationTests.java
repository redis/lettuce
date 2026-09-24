package io.lettuce.core.event;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.MyStreamingRedisCredentialsProvider;
import io.lettuce.core.Subscription;
import io.lettuce.core.event.connection.AuthenticationEvent;
import io.lettuce.core.event.connection.ReauthenticationEvent;
import io.lettuce.core.event.connection.ReauthenticationFailedEvent;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.WithPassword;
import io.lettuce.test.settings.TestSettings;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.TestSupport;
import io.lettuce.core.event.connection.ConnectionEvent;
import io.lettuce.test.resource.FastShutdown;
import io.lettuce.test.resource.TestClientResources;

/**
 * @author Mark Paluch
 * @author Ivo Gaydajiev
 */
@Tag(INTEGRATION_TEST)
@ExtendWith(LettuceExtension.class)
class ConnectionEventsTriggeredIntegrationTests extends TestSupport {

    @Test
    void testConnectionEvents() throws InterruptedException {

        RedisClient client = RedisClient.create(TestClientResources.get(), RedisURI.Builder.redis(host, port).build());

        BlockingQueue<ConnectionEvent> events = new LinkedBlockingQueue<>();
        Subscription subscription = client.getResources().eventBus().subscribe(ConnectionEvent.class, events::add);

        client.connect().close();

        ConnectionEvent first = events.poll(5, TimeUnit.SECONDS);
        assertThat(first).isNotNull();
        assertThat(first.remoteAddress()).isNotNull();
        assertThat(first.localAddress()).isNotNull();
        assertThat(first.toString()).contains("->");

        // expect at least three more connection events (connected/activated/disconnected/deactivated)
        for (int i = 0; i < 3; i++) {
            assertThat(events.poll(5, TimeUnit.SECONDS)).isNotNull();
        }

        subscription.close();
        FastShutdown.shutdown(client);
    }

    @Test
    void testReauthenticateEvents() {

        MyStreamingRedisCredentialsProvider credentialsProvider = new MyStreamingRedisCredentialsProvider();
        credentialsProvider.emitCredentials(TestSettings.username(), TestSettings.password().toString().toCharArray());

        RedisClient client = RedisClient.create(RedisURI.create(TestSettings.host(), TestSettings.port()));
        client.setOptions(ClientOptions.builder()
                .reauthenticateBehavior(ClientOptions.ReauthenticateBehavior.ON_NEW_CREDENTIALS).build());
        RedisURI uri = RedisURI.Builder.redis(host, port).withAuthentication(credentialsProvider).build();

        WithPassword.run(client, () -> {
            BlockingQueue<AuthenticationEvent> events = new LinkedBlockingQueue<>();
            Subscription subscription = client.getResources().eventBus().subscribe(AuthenticationEvent.class, events::add);

            client.connect(uri);
            AuthenticationEvent first = events.poll(1, TimeUnit.SECONDS);
            assertThat(first).asInstanceOf(InstanceOfAssertFactories.type(ReauthenticationEvent.class))
                    .extracting(ReauthenticationEvent::getEpId).isNotNull();

            credentialsProvider.emitCredentials(TestSettings.username(), "invalid".toCharArray());
            AuthenticationEvent second = events.poll(1, TimeUnit.SECONDS);
            assertThat(second).asInstanceOf(InstanceOfAssertFactories.type(ReauthenticationFailedEvent.class))
                    .extracting(ReauthenticationFailedEvent::getEpId).isNotNull();

            subscription.close();
        });

        FastShutdown.shutdown(client);
    }

}
