package com.lambdaworks.redis.cluster;

import static org.assertj.core.api.Assertions.*;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.lambdaworks.redis.codec.Utf8StringCodec;
import com.lambdaworks.redis.output.StatusOutput;
import com.lambdaworks.redis.protocol.Command;
import com.lambdaworks.redis.protocol.CommandType;

public class ClusterCommandTest {

    private ClusterCommand sut;
    private Command command = new Command(CommandType.TYPE, new StatusOutput(new Utf8StringCodec()), null);

    @Before
    public void before() throws Exception {
        sut = new ClusterCommand(command, null, 1);
    }

    @Test
    public void testException() throws Exception {

        assertThat(command.getException()).isNull();

        sut.setException(new Exception());

        assertThat(command.getException()).isNotNull();
    }

    @Test
    public void testCancel() throws Exception {

        assertThat(command.isCancelled()).isFalse();

        sut.cancel(true);

        assertThat(command.isCancelled()).isTrue();
    }

    @Test
    public void testComplete() throws Exception {

        command.complete();
        sut.await(1, TimeUnit.MINUTES);
    }
}
