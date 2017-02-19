/*
 * Copyright 2011-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lambdaworks.redis.commands.reactive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.offset;

import org.junit.Ignore;
import org.junit.Test;

import reactor.test.StepVerifier;

import com.lambdaworks.redis.api.reactive.RedisReactiveCommands;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.commands.GeoCommandTest;
import com.lambdaworks.util.ReactiveSyncInvocationHandler;

/**
 * @author Mark Paluch
 */
public class GeoReactiveCommandTest extends GeoCommandTest {

    @Override
    protected RedisCommands<String, String> connect() {
        return ReactiveSyncInvocationHandler.sync(client.connect());
    }

    @Test
    @Override
    public void geopos() throws Exception {

        RedisReactiveCommands<String, String> reactive = client.connect().reactive();

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
    @Ignore("API differences")
    @Override
    public void geoposWithTransaction() throws Exception {
    }
}
