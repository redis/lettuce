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
package io.lettuce.core.protocol;

import io.netty.buffer.ByteBuf;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class})
class RatioReadBytesDiscardPolicyTest {
    @Mock
    private ByteBuf buffer;

    private RatioReadBytesDiscardPolicy policy = new RatioReadBytesDiscardPolicy(3);

    @Test
    void shouldNotDiscardReadBytesWhenDidntReachUsageRatio() {
        when(buffer.readerIndex()).thenReturn(7);
        when(buffer.capacity()).thenReturn(10);

        policy.discardReadBytesIfNecessary(buffer);

        verifyNoMoreInteractions(buffer);
    }

    @Test
    void shouldDiscardReadBytesWhenReachedUsageRatio() {
        when(buffer.refCnt()).thenReturn(1);
        when(buffer.readerIndex()).thenReturn(9);
        when(buffer.capacity()).thenReturn(10);

        policy.discardReadBytesIfNecessary(buffer);

        verify(buffer).discardReadBytes();
    }

    @Test
    void shouldNotDiscardReadBytesWhenReachedUsageRatioButBufferReleased() {
        when(buffer.refCnt()).thenReturn(0);
        when(buffer.readerIndex()).thenReturn(9);
        when(buffer.capacity()).thenReturn(10);

        policy.discardReadBytesIfNecessary(buffer);

        verifyNoMoreInteractions(buffer);
    }
}