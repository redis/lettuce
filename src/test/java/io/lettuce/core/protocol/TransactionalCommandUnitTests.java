package io.lettuce.core.protocol;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.lettuce.core.RedisException;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.StatusOutput;

/**
 * @author Mark Paluch
 */
class TransactionalCommandUnitTests {

    @Test
    void shouldCompleteOnException() {

        RedisCommand<String, String, String> inner = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8));

        TransactionalCommand<String, String, String> command = new TransactionalCommand<>(new AsyncCommand<>(inner));

        command.completeExceptionally(new RedisException("foo"));

        assertThat(command).isCompletedExceptionally();
    }

}
