// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;

import com.lambdaworks.redis.codec.ByteArrayCodec;
import org.junit.Test;

import com.lambdaworks.redis.codec.RedisCodec;

public class CustomCodecTest extends AbstractRedisClientTest {
    @Test
    public void test() throws Exception {
        RedisConnection<String, Object> connection = client.connect(new SerializedObjectCodec());
        List<String> list = list("one", "two");
        connection.set(key, list);
        assertThat(connection.get(key)).isEqualTo(list);
    }

    @Test
    public void testByteCodec() throws Exception {
        RedisConnection<byte[], byte[]> connection = client.connect(new ByteArrayCodec());
        String value = "üöäü+#";
        connection.set(key.getBytes(), value.getBytes());
        assertThat(connection.get(key.getBytes())).isEqualTo(value.getBytes());

        List<byte[]> keys = connection.keys(key.getBytes());
        assertThat(keys).contains(key.getBytes());
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
