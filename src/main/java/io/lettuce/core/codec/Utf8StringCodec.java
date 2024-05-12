package io.lettuce.core.codec;

import java.nio.charset.StandardCharsets;

/**
 * A {@link RedisCodec} that handles UTF-8 encoded keys and values.
 *
 * @author Will Glozer
 * @author Mark Paluch
 * @see StringCodec
 * @see StandardCharsets#UTF_8
 * @deprecated since 5.2, use {@link StringCodec#UTF8} instead.
 */
@Deprecated
public class Utf8StringCodec extends StringCodec implements RedisCodec<String, String> {

    /**
     * Initialize a new instance that encodes and decodes strings using the UTF-8 charset;
     */
    public Utf8StringCodec() {
        super(StandardCharsets.UTF_8);
    }

}
