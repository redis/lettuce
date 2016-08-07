package com.lambdaworks.redis.output;

import static com.lambdaworks.redis.protocol.LettuceCharsets.buffer;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.codec.Utf8StringCodec;

/**
 * @author Mark Paluch
 */
public class NestedMultiOutputTest {

    private RedisCodec<String, String> codec = new Utf8StringCodec();

    @Test
    public void nestedMultiError() throws Exception {

        NestedMultiOutput<String, String> output = new NestedMultiOutput<String, String>(codec);
        output.setError(buffer("Oops!"));
        assertThat(output.getError()).isNotNull();
    }

}