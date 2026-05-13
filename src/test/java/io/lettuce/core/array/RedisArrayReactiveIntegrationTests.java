/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.array;

import io.lettuce.test.ReactiveSyncInvocationHandler;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.lettuce.TestTags.INTEGRATION_TEST;

/**
 * Reactive integration tests for Redis Array commands. Re-runs all tests from {@link RedisArrayIntegrationTests} routing every
 * call through the reactive API via {@link ReactiveSyncInvocationHandler}.
 *
 * @author Aleksandar Todorov
 * @since 7.6
 */
@Tag(INTEGRATION_TEST)
public class RedisArrayReactiveIntegrationTests extends RedisArrayIntegrationTests {

    public RedisArrayReactiveIntegrationTests() {
        super();
        redis = ReactiveSyncInvocationHandler.sync(connection);
    }

    // armget and argetrange return arrays with null holes for empty slots.
    // Reactive Streams spec forbids onNext(null), so the dissolving Flux
    // cannot represent these responses. Skipped in the reactive overlay.

    @Test
    @Disabled("Reactive Streams cannot emit null elements; armget returns nulls for missing indices")
    @Override
    void armsetAndArmget() {
    }

    @Test
    @Disabled("Reactive Streams cannot emit null elements; argetrange returns nulls for empty slots")
    @Override
    void argetrange() {
    }

}
