package io.lettuce.core;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.resource.ClientResources;
import io.lettuce.test.resource.FastShutdown;
import io.lettuce.test.resource.TestClientResources;
import io.lettuce.test.settings.TestSettings;

/**
 * @author Mark Paluch
 */
@Tag(UNIT_TEST)
class RedisClientFactoryUnitTests {

    private static final String URI = "redis://" + TestSettings.host() + ":" + TestSettings.port();

    private static final RedisURI REDIS_URI = RedisURI.create(URI);

    @Test
    void plain() {
        FastShutdown.shutdown(RedisClient.create());
    }

    @Test
    void withStringUri() {
        FastShutdown.shutdown(RedisClient.create(URI));
    }

    @Test
    void withStringUriNull() {
        assertThatThrownBy(() -> RedisClient.create((String) null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void withUri() {
        FastShutdown.shutdown(RedisClient.create(REDIS_URI));
    }

    @Test
    void withUriNull() {
        assertThatThrownBy(() -> RedisClient.create((RedisURI) null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void clientResources() {
        FastShutdown.shutdown(RedisClient.create(TestClientResources.get()));
    }

    @Test
    void clientResourcesNull() {
        assertThatThrownBy(() -> RedisClient.create((ClientResources) null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void clientResourcesWithStringUri() {
        FastShutdown.shutdown(RedisClient.create(TestClientResources.get(), URI));
    }

    @Test
    void clientResourcesWithStringUriNull() {
        assertThatThrownBy(() -> RedisClient.create(TestClientResources.get(), (String) null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void clientResourcesNullWithStringUri() {
        assertThatThrownBy(() -> RedisClient.create(null, URI)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void clientResourcesWithUri() {
        FastShutdown.shutdown(RedisClient.create(TestClientResources.get(), REDIS_URI));
    }

    @Test
    void clientResourcesWithUriNull() {
        assertThatThrownBy(() -> RedisClient.create(TestClientResources.get(), (RedisURI) null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void clientResourcesNullWithUri() {
        assertThatThrownBy(() -> RedisClient.create(null, REDIS_URI)).isInstanceOf(IllegalArgumentException.class);
    }

}
