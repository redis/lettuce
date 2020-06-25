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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.codec.Utf8StringCodec;

/**
 * @author Mark Paluch
 */
class BooleanListOutputUnitTests {

    private BooleanListOutput<?, ?> sut = new BooleanListOutput<>(StringCodec.UTF8);

    @Test
    void defaultSubscriberIsSet() {
        assertThat(sut.getSubscriber()).isNotNull().isInstanceOf(ListSubscriber.class);
    }

    @Test
    void commandOutputCorrectlyDecoded() {

        sut.multi(3);
        sut.set(1L);
        sut.set(0L);
        sut.set(2L);

        assertThat(sut.get()).contains(true, false, false);
    }

    @Test
    void setByteNotImplemented() {
        assertThatThrownBy(() -> sut.set(ByteBuffer.wrap("4.567".getBytes()))).isInstanceOf(IllegalStateException.class);
    }

}
