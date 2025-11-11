package io.lettuce.core.failover;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        assertThatThrownBy(() -> MultiDbClient.create(null, Collections.singletonList(MultiDbTestSupport.DB1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

}
