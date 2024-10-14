package io.lettuce.core.event;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.TestSupport;
import io.lettuce.core.event.connection.ConnectionEvent;
import io.lettuce.test.resource.FastShutdown;
import io.lettuce.test.resource.TestClientResources;

/**
 * @author Mark Paluch
 */
@Tag(INTEGRATION_TEST)
class ConnectionEventsTriggeredIntegrationTests extends TestSupport {

    @Test
    void testConnectionEvents() {

        RedisClient client = RedisClient.create(TestClientResources.get(), RedisURI.Builder.redis(host, port).build());

        Flux<ConnectionEvent> publisher = client.getResources().eventBus().get()
                .filter(event -> event instanceof ConnectionEvent).cast(ConnectionEvent.class);

        StepVerifier.create(publisher).then(() -> client.connect().close()).assertNext(event -> {
            assertThat(event.remoteAddress()).isNotNull();
            assertThat(event.localAddress()).isNotNull();
            assertThat(event.toString()).contains("->");
        }).expectNextCount(3).thenCancel().verify(Duration.of(5, ChronoUnit.SECONDS));

        FastShutdown.shutdown(client);
    }

}
