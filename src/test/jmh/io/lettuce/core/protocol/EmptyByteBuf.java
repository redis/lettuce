/*
 * Copyright 2011-2018 the original author or authors.
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

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.util.ByteProcessor;

/**
 * @author Mark Paluch
 */
class EmptyByteBuf extends ByteBuf {

    private static final EmptyByteBuf INSTANCE = new EmptyByteBuf();

    @Override
    public int capacity() {
        return 0;
    }

    @Override
    public ByteBuf capacity(int i) {
        return null;
    }

    @Override
    public int maxCapacity() {
        return 0;
    }

    @Override
    public ByteBufAllocator alloc() {
        return EmptyByteBufAllocator.INSTANCE;
    }

    @Override
    public ByteOrder order() {
        return null;
    }

    @Override
    public ByteBuf order(ByteOrder byteOrder) {
        return null;
    }

    @Override
    public ByteBuf unwrap() {
        return null;
    }

    @Override
    public boolean isDirect() {
        return false;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public ByteBuf asReadOnly() {
        return null;
    }

    @Override
    public int readerIndex() {
        return 0;
    }

    @Override
    public ByteBuf readerIndex(int i) {
        return null;
    }

    @Override
    public int writerIndex() {
        return 0;
    }

    @Override
    public ByteBuf writerIndex(int i) {
        return null;
    }

    @Override
    public ByteBuf setIndex(int i, int i1) {
        return null;
    }

    @Override
    public int readableBytes() {
        return 0;
    }

    @Override
    public int writableBytes() {
        return 0;
    }

    @Override
    public int maxWritableBytes() {
        return 0;
    }

    @Override
    public boolean isReadable() {
        return false;
    }

    @Override
    public boolean isReadable(int i) {
        return false;
    }

    @Override
    public boolean isWritable() {
        return false;
    }

    @Override
    public boolean isWritable(int i) {
        return false;
    }

    @Override
    public ByteBuf clear() {
        return null;
    }

    @Override
    public ByteBuf markReaderIndex() {
        return null;
    }

    @Override
    public ByteBuf resetReaderIndex() {
        return null;
    }

    @Override
    public ByteBuf markWriterIndex() {
        return null;
    }

    @Override
    public ByteBuf resetWriterIndex() {
        return null;
    }

    @Override
    public ByteBuf discardReadBytes() {
        return null;
    }

    @Override
    public ByteBuf discardSomeReadBytes() {
        return null;
    }

    @Override
    public ByteBuf ensureWritable(int i) {
        return null;
    }

    @Override
    public int ensureWritable(int i, boolean b) {
        return 0;
    }

    @Override
    public boolean getBoolean(int i) {
        return false;
    }

    @Override
    public byte getByte(int i) {
        return 0;
    }

    @Override
    public short getUnsignedByte(int i) {
        return 0;
    }

    @Override
    public short getShort(int i) {
        return 0;
    }

    @Override
    public short getShortLE(int i) {
        return 0;
    }

    @Override
    public int getUnsignedShort(int i) {
        return 0;
    }

    @Override
    public int getUnsignedShortLE(int i) {
        return 0;
    }

    @Override
    public int getMedium(int i) {
        return 0;
    }

    @Override
    public int getMediumLE(int i) {
        return 0;
    }

    @Override
    public int getUnsignedMedium(int i) {
        return 0;
    }

    @Override
    public int getUnsignedMediumLE(int i) {
        return 0;
    }

    @Override
    public int getInt(int i) {
        return 0;
    }

    @Override
    public int getIntLE(int i) {
        return 0;
    }

    @Override
    public long getUnsignedInt(int i) {
        return 0;
    }

    @Override
    public long getUnsignedIntLE(int i) {
        return 0;
    }

    @Override
    public long getLong(int i) {
        return 0;
    }

    @Override
    public long getLongLE(int i) {
        return 0;
    }

    @Override
    public char getChar(int i) {
        return 0;
    }

    @Override
    public float getFloat(int i) {
        return 0;
    }

    @Override
    public double getDouble(int i) {
        return 0;
    }

    @Override
    public ByteBuf getBytes(int i, ByteBuf byteBuf) {
        return null;
    }

    @Override
    public ByteBuf getBytes(int i, ByteBuf byteBuf, int i1) {
        return null;
    }

    @Override
    public ByteBuf getBytes(int i, ByteBuf byteBuf, int i1, int i2) {
        return null;
    }

    @Override
    public ByteBuf getBytes(int i, byte[] bytes) {
        return null;
    }

    @Override
    public ByteBuf getBytes(int i, byte[] bytes, int i1, int i2) {
        return null;
    }

    @Override
    public ByteBuf getBytes(int i, ByteBuffer byteBuffer) {
        return null;
    }

    @Override
    public ByteBuf getBytes(int i, OutputStream outputStream, int i1) {
        return null;
    }

    @Override
    public int getBytes(int i, GatheringByteChannel gatheringByteChannel, int i1) {
        return 0;
    }

    @Override
    public int getBytes(int i, FileChannel fileChannel, long l, int i1) {
        return 0;
    }

    @Override
    public CharSequence getCharSequence(int i, int i1, Charset charset) {
        return null;
    }

    @Override
    public ByteBuf setBoolean(int i, boolean b) {
        return null;
    }

    @Override
    public ByteBuf setByte(int i, int i1) {
        return null;
    }

    @Override
    public ByteBuf setShort(int i, int i1) {
        return null;
    }

    @Override
    public ByteBuf setShortLE(int i, int i1) {
        return null;
    }

    @Override
    public ByteBuf setMedium(int i, int i1) {
        return null;
    }

    @Override
    public ByteBuf setMediumLE(int i, int i1) {
        return null;
    }

    @Override
    public ByteBuf setInt(int i, int i1) {
        return null;
    }

    @Override
    public ByteBuf setIntLE(int i, int i1) {
        return null;
    }

    @Override
    public ByteBuf setLong(int i, long l) {
        return null;
    }

    @Override
    public ByteBuf setLongLE(int i, long l) {
        return null;
    }

    @Override
    public ByteBuf setChar(int i, int i1) {
        return null;
    }

    @Override
    public ByteBuf setFloat(int i, float v) {
        return null;
    }

    @Override
    public ByteBuf setDouble(int i, double v) {
        return null;
    }

    @Override
    public ByteBuf setBytes(int i, ByteBuf byteBuf) {
        return null;
    }

    @Override
    public ByteBuf setBytes(int i, ByteBuf byteBuf, int i1) {
        return null;
    }

    @Override
    public ByteBuf setBytes(int i, ByteBuf byteBuf, int i1, int i2) {
        return null;
    }

    @Override
    public ByteBuf setBytes(int i, byte[] bytes) {
        return null;
    }

    @Override
    public ByteBuf setBytes(int i, byte[] bytes, int i1, int i2) {
        return null;
    }

    @Override
    public ByteBuf setBytes(int i, ByteBuffer byteBuffer) {
        return null;
    }

    @Override
    public int setBytes(int i, InputStream inputStream, int i1) {
        return 0;
    }

    @Override
    public int setBytes(int i, ScatteringByteChannel scatteringByteChannel, int i1) {
        return 0;
    }

    @Override
    public int setBytes(int i, FileChannel fileChannel, long l, int i1) {
        return 0;
    }

    @Override
    public ByteBuf setZero(int i, int i1) {
        return null;
    }

    @Override
    public int setCharSequence(int i, CharSequence charSequence, Charset charset) {
        return 0;
    }

    @Override
    public boolean readBoolean() {
        return false;
    }

    @Override
    public byte readByte() {
        return 0;
    }

    @Override
    public short readUnsignedByte() {
        return 0;
    }

    @Override
    public short readShort() {
        return 0;
    }

    @Override
    public short readShortLE() {
        return 0;
    }

    @Override
    public int readUnsignedShort() {
        return 0;
    }

    @Override
    public int readUnsignedShortLE() {
        return 0;
    }

    @Override
    public int readMedium() {
        return 0;
    }

    @Override
    public int readMediumLE() {
        return 0;
    }

    @Override
    public int readUnsignedMedium() {
        return 0;
    }

    @Override
    public int readUnsignedMediumLE() {
        return 0;
    }

    @Override
    public int readInt() {
        return 0;
    }

    @Override
    public int readIntLE() {
        return 0;
    }

    @Override
    public long readUnsignedInt() {
        return 0;
    }

    @Override
    public long readUnsignedIntLE() {
        return 0;
    }

    @Override
    public long readLong() {
        return 0;
    }

    @Override
    public long readLongLE() {
        return 0;
    }

    @Override
    public char readChar() {
        return 0;
    }

    @Override
    public float readFloat() {
        return 0;
    }

    @Override
    public double readDouble() {
        return 0;
    }

    @Override
    public ByteBuf readBytes(int i) {
        return null;
    }

    @Override
    public ByteBuf readSlice(int i) {
        return null;
    }

    @Override
    public ByteBuf readRetainedSlice(int i) {
        return null;
    }

    @Override
    public ByteBuf readBytes(ByteBuf byteBuf) {
        return null;
    }

    @Override
    public ByteBuf readBytes(ByteBuf byteBuf, int i) {
        return null;
    }

    @Override
    public ByteBuf readBytes(ByteBuf byteBuf, int i, int i1) {
        return null;
    }

    @Override
    public ByteBuf readBytes(byte[] bytes) {
        return null;
    }

    @Override
    public ByteBuf readBytes(byte[] bytes, int i, int i1) {
        return null;
    }

    @Override
    public ByteBuf readBytes(ByteBuffer byteBuffer) {
        return null;
    }

    @Override
    public ByteBuf readBytes(OutputStream outputStream, int i) {
        return null;
    }

    @Override
    public int readBytes(GatheringByteChannel gatheringByteChannel, int i) {
        return 0;
    }

    @Override
    public CharSequence readCharSequence(int i, Charset charset) {
        return null;
    }

    @Override
    public int readBytes(FileChannel fileChannel, long l, int i) {
        return 0;
    }

    @Override
    public ByteBuf skipBytes(int i) {
        return null;
    }

    @Override
    public ByteBuf writeBoolean(boolean b) {
        return null;
    }

    @Override
    public ByteBuf writeByte(int i) {
        return null;
    }

    @Override
    public ByteBuf writeShort(int i) {
        return null;
    }

    @Override
    public ByteBuf writeShortLE(int i) {
        return null;
    }

    @Override
    public ByteBuf writeMedium(int i) {
        return null;
    }

    @Override
    public ByteBuf writeMediumLE(int i) {
        return null;
    }

    @Override
    public ByteBuf writeInt(int i) {
        return null;
    }

    @Override
    public ByteBuf writeIntLE(int i) {
        return null;
    }

    @Override
    public ByteBuf writeLong(long l) {
        return null;
    }

    @Override
    public ByteBuf writeLongLE(long l) {
        return null;
    }

    @Override
    public ByteBuf writeChar(int i) {
        return null;
    }

    @Override
    public ByteBuf writeFloat(float v) {
        return null;
    }

    @Override
    public ByteBuf writeDouble(double v) {
        return null;
    }

    @Override
    public ByteBuf writeBytes(ByteBuf byteBuf) {
        return null;
    }

    @Override
    public ByteBuf writeBytes(ByteBuf byteBuf, int i) {
        return null;
    }

    @Override
    public ByteBuf writeBytes(ByteBuf byteBuf, int i, int i1) {
        return null;
    }

    @Override
    public ByteBuf writeBytes(byte[] bytes) {
        return null;
    }

    @Override
    public ByteBuf writeBytes(byte[] bytes, int i, int i1) {
        return null;
    }

    @Override
    public ByteBuf writeBytes(ByteBuffer byteBuffer) {
        return null;
    }

    @Override
    public int writeBytes(InputStream inputStream, int i) {
        return 0;
    }

    @Override
    public int writeBytes(ScatteringByteChannel scatteringByteChannel, int i) {
        return 0;
    }

    @Override
    public int writeBytes(FileChannel fileChannel, long l, int i) {
        return 0;
    }

    @Override
    public ByteBuf writeZero(int i) {
        return null;
    }

    @Override
    public int writeCharSequence(CharSequence charSequence, Charset charset) {
        return 0;
    }

    @Override
    public int indexOf(int i, int i1, byte b) {
        return 0;
    }

    @Override
    public int bytesBefore(byte b) {
        return 0;
    }

    @Override
    public int bytesBefore(int i, byte b) {
        return 0;
    }

    @Override
    public int bytesBefore(int i, int i1, byte b) {
        return 0;
    }

    @Override
    public int forEachByte(ByteProcessor byteProcessor) {
        return 0;
    }

    @Override
    public int forEachByte(int i, int i1, ByteProcessor byteProcessor) {
        return 0;
    }

    @Override
    public int forEachByteDesc(ByteProcessor byteProcessor) {
        return 0;
    }

    @Override
    public int forEachByteDesc(int i, int i1, ByteProcessor byteProcessor) {
        return 0;
    }

    @Override
    public ByteBuf copy() {
        return null;
    }

    @Override
    public ByteBuf copy(int i, int i1) {
        return null;
    }

    @Override
    public ByteBuf slice() {
        return null;
    }

    @Override
    public ByteBuf retainedSlice() {
        return null;
    }

    @Override
    public ByteBuf slice(int i, int i1) {
        return null;
    }

    @Override
    public ByteBuf retainedSlice(int i, int i1) {
        return null;
    }

    @Override
    public ByteBuf duplicate() {
        return null;
    }

    @Override
    public ByteBuf retainedDuplicate() {
        return null;
    }

    @Override
    public int nioBufferCount() {
        return 0;
    }

    @Override
    public ByteBuffer nioBuffer() {
        return null;
    }

    @Override
    public ByteBuffer nioBuffer(int i, int i1) {
        return null;
    }

    @Override
    public ByteBuffer internalNioBuffer(int i, int i1) {
        return null;
    }

    @Override
    public ByteBuffer[] nioBuffers() {
        return new ByteBuffer[0];
    }

    @Override
    public ByteBuffer[] nioBuffers(int i, int i1) {
        return new ByteBuffer[0];
    }

    @Override
    public boolean hasArray() {
        return false;
    }

    @Override
    public byte[] array() {
        return new byte[0];
    }

    @Override
    public int arrayOffset() {
        return 0;
    }

    @Override
    public boolean hasMemoryAddress() {
        return false;
    }

    @Override
    public long memoryAddress() {
        return 0;
    }

    @Override
    public String toString(Charset charset) {
        return null;
    }

    @Override
    public String toString(int i, int i1, Charset charset) {
        return null;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        return false;
    }

    @Override
    public int compareTo(ByteBuf byteBuf) {
        return 0;
    }

    @Override
    public String toString() {
        return null;
    }

    @Override
    public ByteBuf retain(int i) {
        return null;
    }

    @Override
    public int refCnt() {
        return 0;
    }

    @Override
    public ByteBuf retain() {
        return null;
    }

    @Override
    public ByteBuf touch() {
        return null;
    }

    @Override
    public ByteBuf touch(Object o) {
        return null;
    }

    @Override
    public boolean release() {
        return false;
    }

    @Override
    public boolean release(int i) {
        return false;
    }

    private enum EmptyByteBufAllocator implements ByteBufAllocator {
        INSTANCE;

        @Override
        public ByteBuf buffer() {
            return EmptyByteBuf.INSTANCE;
        }

        @Override
        public ByteBuf buffer(int i) {
            return EmptyByteBuf.INSTANCE;
        }

        @Override
        public ByteBuf buffer(int i, int i1) {
            return EmptyByteBuf.INSTANCE;
        }

        @Override
        public ByteBuf ioBuffer() {
            return EmptyByteBuf.INSTANCE;
        }

        @Override
        public ByteBuf ioBuffer(int i) {
            return EmptyByteBuf.INSTANCE;
        }

        @Override
        public ByteBuf ioBuffer(int i, int i1) {
            return EmptyByteBuf.INSTANCE;
        }

        @Override
        public ByteBuf heapBuffer() {
            return EmptyByteBuf.INSTANCE;
        }

        @Override
        public ByteBuf heapBuffer(int i) {
            return EmptyByteBuf.INSTANCE;
        }

        @Override
        public ByteBuf heapBuffer(int i, int i1) {
            return EmptyByteBuf.INSTANCE;
        }

        @Override
        public ByteBuf directBuffer() {
            return EmptyByteBuf.INSTANCE;
        }

        @Override
        public ByteBuf directBuffer(int i) {
            return EmptyByteBuf.INSTANCE;
        }

        @Override
        public ByteBuf directBuffer(int i, int i1) {
            return EmptyByteBuf.INSTANCE;
        }

        @Override
        public CompositeByteBuf compositeBuffer() {
            return null;
        }

        @Override
        public CompositeByteBuf compositeBuffer(int i) {
            return null;
        }

        @Override
        public CompositeByteBuf compositeHeapBuffer() {
            return null;
        }

        @Override
        public CompositeByteBuf compositeHeapBuffer(int i) {
            return null;
        }

        @Override
        public CompositeByteBuf compositeDirectBuffer() {
            return null;
        }

        @Override
        public CompositeByteBuf compositeDirectBuffer(int i) {
            return null;
        }

        @Override
        public boolean isDirectBufferPooled() {
            return false;
        }

        @Override
        public int calculateNewCapacity(int i, int i1) {
            return 0;
        }
    }
}
