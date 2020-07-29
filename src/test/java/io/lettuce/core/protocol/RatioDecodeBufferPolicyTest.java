/*
 * Copyright 2020 the original author or authors.
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
package io.lettuce.core.protocol;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.netty.buffer.ByteBuf;

/**
 * Unit tests for {@link RatioDecodeBufferPolicy}.
 *
 * @author Shaphan
 */
@ExtendWith(MockitoExtension.class)
class RatioDecodeBufferPolicyTest {

    @Mock
    ByteBuf buffer;

    RatioDecodeBufferPolicy policy = new RatioDecodeBufferPolicy(3);

    @Test
    void shouldNotDiscardReadBytesWhenDidntReachUsageRatio() {

        when(buffer.capacity()).thenReturn(10);

        policy.afterCommandDecoded(buffer);

        verify(buffer).capacity();
        verify(buffer).readerIndex();
        verifyNoMoreInteractions(buffer);
    }

    @Test
    void shouldDiscardReadBytesWhenReachedUsageRatio() {

        when(buffer.readerIndex()).thenReturn(9);
        when(buffer.capacity()).thenReturn(10);

        policy.afterCommandDecoded(buffer);

        verify(buffer).capacity();
        verify(buffer).readerIndex();
        verify(buffer).discardReadBytes();
    }
}
