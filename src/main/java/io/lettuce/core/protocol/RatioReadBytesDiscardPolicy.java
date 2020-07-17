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

import io.lettuce.core.internal.LettuceAssert;
import io.netty.buffer.ByteBuf;

/**
 * A {@link ReadBytesDiscardPolicy} that tries to discard read bytes when buffer usage reaches a higher ratio
 */
public class RatioReadBytesDiscardPolicy implements ReadBytesDiscardPolicy {
    private final int bufferUsageRatio;
    private final float discardReadBytesRatio;

    public RatioReadBytesDiscardPolicy(int bufferUsageRatio) {
        LettuceAssert.isTrue(bufferUsageRatio > 0 && bufferUsageRatio < Integer.MAX_VALUE,
                "BufferUsageRatio must be greater than 0");

        this.bufferUsageRatio = bufferUsageRatio;
        this.discardReadBytesRatio = (float)bufferUsageRatio / (bufferUsageRatio + 1);
    }

    public int getBufferUsageRatio() {
        return bufferUsageRatio;
    }

    @Override
    public void discardReadBytesIfNecessary(ByteBuf buffer) {
        float usedRatio = (float) buffer.readerIndex() / buffer.capacity();

        if (usedRatio >= discardReadBytesRatio && buffer.refCnt() != 0) {
            buffer.discardReadBytes();
        }
    }
}
