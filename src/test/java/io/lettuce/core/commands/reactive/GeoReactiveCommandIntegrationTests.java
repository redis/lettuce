package io.lettuce.core.commands.reactive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.offset;

import javax.inject.Inject;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import reactor.test.StepVerifier;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.commands.GeoCommandIntegrationTests;
import io.lettuce.test.ReactiveSyncInvocationHandler;

/**
 * @author Mark Paluch
 */
class GeoReactiveCommandIntegrationTests extends GeoCommandIntegrationTests {

    private final StatefulRedisConnection<String, String> connection;

    @Inject
    GeoReactiveCommandIntegrationTests(StatefulRedisConnection<String, String> connection) {
        super(ReactiveSyncInvocationHandler.sync(connection));
        this.connection = connection;
    }

    @Test
    @Override
    public void geopos() {

        RedisReactiveCommands<String, String> reactive = connection.reactive();

        prepareGeo();

        StepVerifier.create(reactive.geopos(key, "Weinheim", "foobar", "Bahn")).consumeNextWith(actual -> {
            assertThat(actual.getValue().getX().doubleValue()).isEqualTo(8.6638, offset(0.001));

        }).consumeNextWith(actual -> {
            assertThat(actual.hasValue()).isFalse();
        }).consumeNextWith(actual -> {
            assertThat(actual.hasValue()).isTrue();
        }).verifyComplete();
    }

    @Test
    @Disabled("API differences")
    @Override
    public void geoposInTransaction() {
    }

}
