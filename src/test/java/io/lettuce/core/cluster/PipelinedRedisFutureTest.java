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
package io.lettuce.core.cluster;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import java.util.HashMap;

/**
 * @author Mark Paluch
 */
public class PipelinedRedisFutureTest {

    private PipelinedRedisFuture<String> sut;

    @Test
    public void testComplete() throws Exception {

        String other = "other";

        sut = new PipelinedRedisFuture<>(new HashMap<>(), o -> other);

        sut.complete("");
        assertThat(sut.get()).isEqualTo(other);
        assertThat(sut.getError()).isNull();

    }

    @Test
    public void testCompleteExceptionally() throws Exception {

        String other = "other";

        sut = new PipelinedRedisFuture<>(new HashMap<>(), o -> other);

        sut.completeExceptionally(new Exception());
        assertThat(sut.get()).isEqualTo(other);
        assertThat(sut.getError()).isNull();

    }
}
