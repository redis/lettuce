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
package io.lettuce.core.cluster;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;

import org.junit.jupiter.api.Test;

import io.lettuce.test.Futures;

/**
 * @author Mark Paluch
 */
class PipelinedRedisFutureUnitTests {

    private PipelinedRedisFuture<String> sut;

    @Test
    void testComplete() {

        String other = "other";

        sut = new PipelinedRedisFuture<>(new HashMap<>(), o -> other);

        sut.complete("");
        assertThat(Futures.get(sut.toCompletableFuture())).isEqualTo(other);
        assertThat(sut.getError()).isNull();
    }

    @Test
    void testCompleteExceptionally() {

        String other = "other";

        sut = new PipelinedRedisFuture<>(new HashMap<>(), o -> other);

        sut.completeExceptionally(new Exception());
        assertThat(Futures.get(sut.toCompletableFuture())).isEqualTo(other);
        assertThat(sut.getError()).isNull();
    }

}
