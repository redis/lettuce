package com.lambdaworks.redis.commands.reactive;

import static com.lambdaworks.redis.BitFieldArgs.offset;
import static com.lambdaworks.redis.BitFieldArgs.signed;
import static com.lambdaworks.redis.BitFieldArgs.typeWidthBasedOffset;
import static com.lambdaworks.redis.BitFieldArgs.OverflowType.FAIL;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.Test;

import com.lambdaworks.redis.BitFieldArgs;
import com.lambdaworks.redis.Value;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.reactive.RedisStringReactiveCommands;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.commands.BitCommandTest;
import com.lambdaworks.util.ReactiveSyncInvocationHandler;

/**
 * @author Mark Paluch
 */
public class BitReactiveCommandTest extends BitCommandTest {

    private RedisStringReactiveCommands<String, String> reactive;

    @Override
    protected RedisCommands<String, String> connect() {
        bitstring = ReactiveSyncInvocationHandler.sync(client.connect(new BitStringCodec()));

        StatefulRedisConnection<String, String> connection = client.connect();
        reactive = connection.reactive();
        return ReactiveSyncInvocationHandler.sync(connection);
    }

    @Test
    public void bitfield() throws Exception {

        BitFieldArgs bitFieldArgs = BitFieldArgs.Builder.set(signed(8), 0, 1).set(5, 1).incrBy(2, 3).get().get(2);

        List<Value<Long>> values = reactive.bitfield(key, bitFieldArgs).collectList().block();

        assertThat(values).containsExactly(Value.just(0L), Value.just(32L), Value.just(3L), Value.just(0L), Value.just(3L));
        assertThat(bitstring.get(key)).isEqualTo("0000000000010011");
    }

    @Test
    public void bitfieldGetWithOffset() throws Exception {

        BitFieldArgs bitFieldArgs = BitFieldArgs.Builder.set(signed(8), 0, 1).get(signed(2), typeWidthBasedOffset(1));

        List<Value<Long>> values = reactive.bitfield(key, bitFieldArgs).collectList().block();

        assertThat(values).containsExactly(Value.just(0L), Value.just(0L));
        assertThat(bitstring.get(key)).isEqualTo("10000000");
    }

    @Test
    public void bitfieldSet() throws Exception {

        BitFieldArgs bitFieldArgs = BitFieldArgs.Builder.set(signed(8), 0, 5).set(5);

        List<Value<Long>> values = reactive.bitfield(key, bitFieldArgs).collectList().block();

        assertThat(values).containsExactly(Value.just(0L), Value.just(5L));
        assertThat(bitstring.get(key)).isEqualTo("10100000");
    }

    @Test
    public void bitfieldWithOffsetSet() throws Exception {

        reactive.bitfield(key, BitFieldArgs.Builder.set(signed(8), typeWidthBasedOffset(2), 5)).last().block();
        assertThat(bitstring.get(key)).isEqualTo("000000000000000010100000");

        redis.del(key);
        reactive.bitfield(key, BitFieldArgs.Builder.set(signed(8), offset(2), 5)).last().block();
        assertThat(bitstring.get(key)).isEqualTo("1000000000000010");
    }

    @Test
    public void bitfieldIncrBy() throws Exception {

        BitFieldArgs bitFieldArgs = BitFieldArgs.Builder.set(signed(8), 0, 5).incrBy(1);

        List<Value<Long>> values = reactive.bitfield(key, bitFieldArgs).collectList().block();

        assertThat(values).containsExactly(Value.just(0L), Value.just(6L));
        assertThat(bitstring.get(key)).isEqualTo("01100000");
    }

    @Test
    public void bitfieldOverflow() throws Exception {

        BitFieldArgs bitFieldArgs = BitFieldArgs.Builder.overflow(FAIL).set(signed(8), 9, 5).incrBy(signed(8),
                Integer.MAX_VALUE);

        List<Value<Long>> values = reactive.bitfield(key, bitFieldArgs).collectList().block();

        assertThat(values).containsExactly(Value.just(0L), Value.empty());
    }
}
