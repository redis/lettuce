/*
 * Copyright 2011-2016 the original author or authors.
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

import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import com.lambdaworks.redis.GeoCoordinates;
import com.lambdaworks.redis.Value;
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

        List<Value<GeoCoordinates>> geopos = reactive.geopos(key, "Weinheim", "foobar", "Bahn").collectList().block();

        assertThat(geopos).hasSize(3);
        assertThat(geopos.get(0).getValue().getX().doubleValue()).isEqualTo(8.6638, offset(0.001));
        assertThat(geopos.get(1).hasValue()).isFalse();
        assertThat(geopos.get(2).hasValue()).isTrue();
    }

    @Test
    @Ignore("API differences")
    @Override
    public void geoposWithTransaction() throws Exception {
    }
}
