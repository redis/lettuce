/*
 * Copyright 2020 the original author or authors.
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
package io.lettuce.core.protocol;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.netty.buffer.ByteBuf;

/**
 * Unit tests for {@link DecodeBufferPolicies}.
 *
 * @author Mark Paluch
 */
@ExtendWith(MockitoExtension.class)
class DecodeBufferPoliciesUnitTests {

    @Mock
    ByteBuf byteBuf;

    @Test
    void shouldAlwaysDiscard() {

        DecodeBufferPolicy policy = DecodeBufferPolicies.always();

        policy.afterDecoding(byteBuf);
        policy.afterPartialDecode(byteBuf);
        policy.afterCommandDecoded(byteBuf);

        verify(byteBuf, never()).discardSomeReadBytes();
        verify(byteBuf, times(3)).discardReadBytes();
    }

    @Test
    void shouldAlwaysDiscardSomeReadBytes() {

        DecodeBufferPolicy policy = DecodeBufferPolicies.alwaysSome();

        policy.afterDecoding(byteBuf);
        policy.afterPartialDecode(byteBuf);
        policy.afterCommandDecoded(byteBuf);

        verify(byteBuf, never()).discardReadBytes();
        verify(byteBuf, times(3)).discardSomeReadBytes();
    }

}
