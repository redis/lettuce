package com.lambdaworks.redis.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import com.lambdaworks.redis.protocol.CommandKeyword;
import io.netty.channel.local.LocalAddress;
import org.junit.Test;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class CommandLatencyIdTest {

    private CommandLatencyId sut = CommandLatencyId.create(LocalAddress.ANY, new LocalAddress("me"), CommandKeyword.ADDR);

    @Test
    public void testToString() throws Exception {
        assertThat(sut.toString()).contains("local:any -> local:me");
    }

    @Test
    public void testValues() throws Exception {
        assertThat(sut.localAddress()).isEqualTo(LocalAddress.ANY);
        assertThat(sut.remoteAddress()).isEqualTo(new LocalAddress("me"));
    }
}
