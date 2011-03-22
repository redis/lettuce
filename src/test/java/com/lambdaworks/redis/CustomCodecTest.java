// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

import com.lambdaworks.redis.codec.RedisCodec;
import org.junit.Test;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class CustomCodecTest extends AbstractCommandTest {
    @Test
    public void test() throws Exception {
        RedisConnection<String, Object> connection = client.connect(new SerializedObjectCodec());
        List<String> list = list("one", "two");
        connection.set(key, list);
        assertEquals(list, connection.get(key));
    }

    public class SerializedObjectCodec extends RedisCodec<String, Object> {
        private Charset charset = Charset.forName("UTF-8");

        @Override
        public String decodeKey(ByteBuffer bytes) {
            return charset.decode(bytes).toString();
        }

        @Override
        public Object decodeValue(ByteBuffer bytes) {
            try {
                byte[] array = new byte[bytes.remaining()];
                bytes.get(array);
                ObjectInputStream is = new ObjectInputStream(new ByteArrayInputStream(array));
                return is.readObject();
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        public byte[] encodeKey(String key) {
            return charset.encode(key).array();
        }

        @Override
        public byte[] encodeValue(Object value) {
            try {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                ObjectOutputStream os = new ObjectOutputStream(bytes);
                os.writeObject(value);
                return bytes.toByteArray();
            } catch (IOException e) {
                return null;
            }
        }
    }
}
