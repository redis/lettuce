package io.lettuce.core.protocol;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.netty.buffer.ByteBuf;

/**
 * Unit tests for {@link DecodeBufferPolicies}.
 *
 * @author Mark Paluch
 */
@Tag(UNIT_TEST)
@ExtendWith(MockitoExtension.class)
class DecodeBufferPoliciesUnitTests {

    @Mock
    ByteBuf byteBuf;

    @Test
    void shouldAlwaysDiscard() {

        DecodeBufferPolicy policy = DecodeBufferPolicies.always();

        policy.afterDecoding(byteBuf);
        policy.afterPartialDecode(byteBuf);
        policy.afterCommandDecoded(byteBuf);

        verify(byteBuf, never()).discardSomeReadBytes();
        verify(byteBuf, times(3)).discardReadBytes();
    }

    @Test
    void shouldAlwaysDiscardSomeReadBytes() {

        DecodeBufferPolicy policy = DecodeBufferPolicies.alwaysSome();

        policy.afterDecoding(byteBuf);
        policy.afterPartialDecode(byteBuf);
        policy.afterCommandDecoded(byteBuf);

        verify(byteBuf, never()).discardReadBytes();
        verify(byteBuf, times(3)).discardSomeReadBytes();
    }

}
