package io.lettuce.core.output;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import io.lettuce.core.codec.RedisCodec;

/**
 * Output capturing a hostname and port (both string elements) into a {@link SocketAddress}.
 *
 * @author Mark Paluch
 * @since 5.0.1
 */
public class SocketAddressOutput<K, V> extends CommandOutput<K, V, SocketAddress> {

    private String hostname;

    private boolean hasHostname;

    public SocketAddressOutput(RedisCodec<K, V> codec) {
        super(codec, null);
    }

    @Override
    public void set(ByteBuffer bytes) {

        if (!hasHostname) {
            hostname = decodeUtf8(bytes);
            hasHostname = true;
            return;
        }

        int port = Integer.parseInt(decodeUtf8(bytes));
        output = InetSocketAddress.createUnresolved(hostname, port);
    }

}
