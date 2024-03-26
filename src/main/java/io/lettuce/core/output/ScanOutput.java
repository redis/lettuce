package io.lettuce.core.output;

import java.nio.ByteBuffer;

import io.lettuce.core.internal.LettuceStrings;
import io.lettuce.core.ScanCursor;
import io.lettuce.core.codec.RedisCodec;

/**
 * Cursor handling output.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @param <T> Cursor type.
 * @author Mark Paluch
 */
public abstract class ScanOutput<K, V, T extends ScanCursor> extends CommandOutput<K, V, T> {

    public ScanOutput(RedisCodec<K, V> codec, T cursor) {
        super(codec, cursor);
    }

    @Override
    public void set(ByteBuffer bytes) {

        if (output.getCursor() == null) {
            output.setCursor(decodeAscii(bytes));
            if (LettuceStrings.isNotEmpty(output.getCursor()) && "0".equals(output.getCursor())) {
                output.setFinished(true);
            }
            return;
        }

        setOutput(bytes);

    }

    protected abstract void setOutput(ByteBuffer bytes);

}
