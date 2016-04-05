package com.lambdaworks.redis.protocol;

import static org.assertj.core.api.Assertions.assertThat;

import com.lambdaworks.redis.AbstractRedisClientTest;
import com.lambdaworks.redis.ReactiveCommandDispatcher;
import org.junit.Test;

import rx.Observable;

import com.lambdaworks.redis.codec.Utf8StringCodec;
import com.lambdaworks.redis.output.StatusOutput;
import com.lambdaworks.redis.protocol.AsyncCommand;
import com.lambdaworks.redis.protocol.Command;
import com.lambdaworks.redis.protocol.CommandType;
import com.lambdaworks.redis.protocol.RedisCommand;

/**
 * @author Mark Paluch
 */
public class CustomCommandTest extends AbstractRedisClientTest {

    @Test
    public void asyncPing() throws Exception {

        RedisCommand<String, String, String> command = new Command<>(CommandType.PING,
                new StatusOutput<>(new Utf8StringCodec()), null);

        AsyncCommand<String, String, String> async = new AsyncCommand<>(command);
        redis.getStatefulConnection().dispatch(async);

        assertThat(async.get()).isEqualTo("PONG");
    }

    @Test
    public void fireAndForget() throws Exception {

        RedisCommand<String, String, String> command = new Command<>(CommandType.PING,
                new StatusOutput<>(new Utf8StringCodec()), null);
        redis.getStatefulConnection().dispatch(command);
        assertThat(command.isCancelled()).isFalse();

    }

    @Test
    public void reactivePing() throws Exception {

        RedisCommand<String, String, String> command = new Command<>(CommandType.PING,
                new StatusOutput<>(new Utf8StringCodec()), null);
        ReactiveCommandDispatcher<String, String, String> dispatcher = new ReactiveCommandDispatcher<>(command,
                redis.getStatefulConnection(), false);

        String result = Observable.create(dispatcher).toBlocking().first();

        assertThat(result).isEqualTo("PONG");
    }
}
