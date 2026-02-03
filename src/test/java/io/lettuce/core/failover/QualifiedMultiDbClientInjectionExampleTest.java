/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.failover;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.test.LettuceExtension;
import io.lettuce.test.NoFailback;

/**
 * Example test demonstrating how to inject multiple {@link MultiDbClient} instances with different configurations using
 * qualifier annotations.
 * <p>
 * This test shows:
 * <ul>
 * <li>Injecting a default MultiDbClient with failback enabled</li>
 * <li>Injecting a MultiDbClient with failback disabled using {@code @NoFailback}</li>
 * </ul>
 *
 * @author Ali Takavci
 */
@ExtendWith(LettuceExtension.class)
@Tag(INTEGRATION_TEST)
class QualifiedMultiDbClientInjectionExampleTest {

    private final MultiDbClient defaultClient;

    private final MultiDbClient noFailbackClient;

    /**
     * Constructor demonstrating qualified injection.
     *
     * @param defaultClient the default MultiDbClient with failback enabled
     * @param noFailbackClient the MultiDbClient with failback disabled
     */
    @Inject
    QualifiedMultiDbClientInjectionExampleTest(MultiDbClient defaultClient, @NoFailback MultiDbClient noFailbackClient) {
        this.defaultClient = defaultClient;
        this.noFailbackClient = noFailbackClient;
    }

    @Test
    void shouldInjectDefaultClientWithFailbackEnabled() {
        assertThat(defaultClient).isNotNull();
        assertThat(defaultClient.getMultiDbOptions()).isNotNull();
        assertThat(defaultClient.getMultiDbOptions().isFailbackSupported()).isTrue();
    }

    @Test
    void shouldInjectNoFailbackClientWithFailbackDisabled() {
        assertThat(noFailbackClient).isNotNull();
        assertThat(noFailbackClient.getMultiDbOptions()).isNotNull();
        assertThat(noFailbackClient.getMultiDbOptions().isFailbackSupported()).isFalse();
    }

    @Test
    void shouldInjectDifferentInstances() {
        // The two clients should be different instances
        assertThat(defaultClient).isNotSameAs(noFailbackClient);
    }

    @Test
    void shouldHaveDifferentConfigurations() {
        // Verify they have different configurations
        assertThat(defaultClient.getMultiDbOptions().isFailbackSupported())
                .isNotEqualTo(noFailbackClient.getMultiDbOptions().isFailbackSupported());
    }

}
