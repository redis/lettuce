package io.lettuce.core.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.lettuce.core.protocol.CommandKeyword;
import io.lettuce.core.protocol.ProtocolKeyword;
import io.netty.channel.local.LocalAddress;

/**
 * Unit tests for {@link CommandLatencyId}.
 *
 * @author Mark Paluch
 */
class CommandLatencyIdUnitTests {

    private CommandLatencyId sut = CommandLatencyId.create(LocalAddress.ANY, new LocalAddress("me"), CommandKeyword.ADDR);

    @Test
    void testToString() {
        assertThat(sut.toString()).contains("local:any -> local:me");
    }

    @Test
    void testValues() {
        assertThat(sut.localAddress()).isEqualTo(LocalAddress.ANY);
        assertThat(sut.remoteAddress()).isEqualTo(new LocalAddress("me"));
    }

    @Test
    void testEquality() {
        assertThat(sut).isEqualTo(CommandLatencyId.create(LocalAddress.ANY, new LocalAddress("me"), new MyCommand("ADDR")));
        assertThat(sut).isNotEqualTo(CommandLatencyId.create(LocalAddress.ANY, new LocalAddress("me"), new MyCommand("FOO")));
    }

    @Test
    void testHashCode() {
        assertThat(sut)
                .hasSameHashCodeAs(CommandLatencyId.create(LocalAddress.ANY, new LocalAddress("me"), new MyCommand("ADDR")));
    }

    static class MyCommand implements ProtocolKeyword {

        final String name;

        public MyCommand(String name) {
            this.name = name;
        }

        @Override
        public byte[] getBytes() {
            return name.getBytes();
        }

        @Override
        public String toString() {
            return name;
        }

    }

}
