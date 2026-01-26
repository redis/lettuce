package io.lettuce.core.failover;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.*;

import java.util.Collections;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.test.resource.FastShutdown;
import io.lettuce.test.resource.TestClientResources;

/**
 * @author Ali Takavci
 * @since 7.1
 */
@Tag(UNIT_TEST)
class RedisMultiDbClientFactoryUnitTests {

    @Test
    void plain() {
        FastShutdown.shutdown(MultiDbClient.create(MultiDbTestSupport.DBs));
    }

    @Test
    void withStringUri() {
        FastShutdown.shutdown(MultiDbClient.create(MultiDbTestSupport.DBs));
    }

    @Test
    void withUri() {
        FastShutdown.shutdown(MultiDbClient.create(MultiDbTestSupport.DBs));
    }

    @Test
    void withUriNull() {
        assertThatThrownBy(() -> MultiDbClient.create(Collections.singletonList((DatabaseConfig) null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void clientResourcesWithUri() {
        FastShutdown
                .shutdown(MultiDbClient.create(TestClientResources.get(), Collections.singletonList(MultiDbTestSupport.DB1)));
    }

    @Test
    void clientResourcesWithUriNull() {
        assertThatThrownBy(
                () -> MultiDbClient.create(TestClientResources.get(), Collections.singletonList((DatabaseConfig) null)))
                        .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void clientResourcesNullWithUri() {
        FastShutdown.shutdown(MultiDbClient.create(null, Collections.singletonList(MultiDbTestSupport.DB1)));
    }

    @Test
    void withMultiDbOptions() {
        MultiDbOptions options = MultiDbOptions.builder().failbackSupported(true).failbackCheckInterval(60000L).build();
        FastShutdown.shutdown(MultiDbClient.create(MultiDbTestSupport.DBs, options));
    }

    @Test
    void withMultiDbOptionsNull() {
        assertThatThrownBy(() -> MultiDbClient.create(MultiDbTestSupport.DBs, (MultiDbOptions) null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void withClientResourcesAndMultiDbOptions() {
        MultiDbOptions options = MultiDbOptions.builder().failbackSupported(false).build();
        FastShutdown.shutdown(MultiDbClient.create(TestClientResources.get(), MultiDbTestSupport.DBs, options));
    }

    @Test
    void withClientResourcesAndMultiDbOptionsNull() {
        assertThatThrownBy(() -> MultiDbClient.create(TestClientResources.get(), MultiDbTestSupport.DBs, (MultiDbOptions) null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void withClientResourcesNullAndMultiDbOptions() {
        MultiDbOptions options = MultiDbOptions.create();
        FastShutdown.shutdown(MultiDbClient.create(null, MultiDbTestSupport.DBs, options));
    }

    @Test
    void getMultiDbOptions() {
        MultiDbOptions options = MultiDbOptions.builder().failbackSupported(false).failbackCheckInterval(30000L).build();
        MultiDbClient client = MultiDbClient.create(MultiDbTestSupport.DBs, options);

        assertThat(client.getMultiDbOptions()).isNotNull();
        assertThat(client.getMultiDbOptions().isFailbackSupported()).isFalse();
        assertThat(client.getMultiDbOptions().getFailbackCheckInterval()).isEqualTo(30000L);

        FastShutdown.shutdown(client);
    }

    @Test
    void getMultiDbOptionsWithDefaults() {
        MultiDbClient client = MultiDbClient.create(MultiDbTestSupport.DBs);

        assertThat(client.getMultiDbOptions()).isNotNull();
        assertThat(client.getMultiDbOptions().isFailbackSupported()).isTrue();
        assertThat(client.getMultiDbOptions().getFailbackCheckInterval()).isEqualTo(120000L);

        FastShutdown.shutdown(client);
    }

}
