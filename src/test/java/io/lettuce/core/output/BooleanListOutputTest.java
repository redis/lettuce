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
package io.lettuce.core.output;

import static org.assertj.core.api.Assertions.*;

import java.nio.ByteBuffer;

import org.junit.Test;

import io.lettuce.core.codec.Utf8StringCodec;

/**
 * @author Mark Paluch
 */
public class BooleanListOutputTest {

    private BooleanListOutput<?, ?> sut = new BooleanListOutput<>(new Utf8StringCodec());

    @Test
    public void defaultSubscriberIsSet() throws Exception {
        assertThat(sut.getSubscriber()).isNotNull().isInstanceOf(ListSubscriber.class);
    }

    @Test
    public void commandOutputCorrectlyDecoded() throws Exception {

		sut.set(1L);
        sut.set(0L);
        sut.set(2L);

        assertThat(sut.get()).contains(true, false, false);
    }

    @Test(expected = IllegalStateException.class)
    public void setByteNotImplemented() throws Exception {
        sut.set(ByteBuffer.wrap("4.567".getBytes()));
    }
}
