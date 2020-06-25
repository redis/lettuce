/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
