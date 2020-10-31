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
package io.lettuce.core.output;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.StringCodec;

/**
 * @author Mark Paluch
 */
class NestedMultiOutputUnitTests {

    @Test
    void nestedMultiError() {

        NestedMultiOutput<String, String> output = new NestedMultiOutput<>(StringCodec.UTF8);
        output.setError(StandardCharsets.US_ASCII.encode("Oops!"));
        assertThat(output.getError()).isNotNull();
    }

    @Test
    void nestedMultiDouble() {
        NestedMultiOutput<String, String> output = new NestedMultiOutput<>(StringCodec.UTF8);
        double value = 123.456;
        output.set(value);
        assertThat(output.get()).isNotNull();
        assertThat(output.get()).size().isEqualTo(1);
        assertThat(output.get().get(0)).isEqualTo(value);
    }

}
