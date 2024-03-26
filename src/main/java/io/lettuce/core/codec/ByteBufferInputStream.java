package io.lettuce.core.codec;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

class ByteBufferInputStream extends InputStream {

    private final ByteBuffer buffer;

    public ByteBufferInputStream(ByteBuffer b) {
        this.buffer = b;
    }

    @Override
    public int available() throws IOException {
        return buffer.remaining();
    }

    @Override
    public int read() throws IOException {
        if (buffer.remaining() > 0) {
            return (buffer.get() & 0xFF);
        }
        return -1;
    }

}
