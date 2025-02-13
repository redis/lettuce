package io.lettuce.core.output;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import io.lettuce.core.codec.RedisCodec;

/**
 * {@link EnumSet} output.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mikhael Sokolov
 * @author Mark Paluch
 * @since 6.1
 */
public class EnumSetOutput<K, V, E extends Enum<E>> extends CommandOutput<K, V, Set<E>> {

    private boolean initialized;

    private final Class<E> enumClass;

    private final UnaryOperator<String> enumValuePreprocessor;

    private final Function<String, E> onUnknownValue;

    /**
     * Create a new {@link EnumSetOutput}.
     *
     * @param codec Codec used to encode/decode keys and values, must not be {@code null}.
     * @param enumClass {@link Enum} type.
     * @param enumValuePreprocessor pre-processor for {@link String} values before looking up the enum value.
     * @param onUnknownValue fallback {@link Function} to be called when an enum value cannot be looked up.
     */
    public EnumSetOutput(RedisCodec<K, V> codec, Class<E> enumClass, UnaryOperator<String> enumValuePreprocessor,
            Function<String, E> onUnknownValue) {
        super(codec, Collections.emptySet());
        this.enumClass = enumClass;
        this.enumValuePreprocessor = enumValuePreprocessor;
        this.onUnknownValue = onUnknownValue;
    }

    @Override
    public void set(ByteBuffer bytes) {

        if (bytes == null) {
            return;
        }

        E enumConstant = resolve(enumValuePreprocessor.apply(decodeString(bytes)));

        if (enumConstant == null) {
            return;
        }

        output.add(enumConstant);
    }

    @Override
    public void multi(int count) {

        if (!initialized) {
            output = EnumSet.noneOf(enumClass);
            initialized = true;
        }
    }

    private E resolve(String value) {
        try {
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException e) {
            return onUnknownValue.apply(value);
        }
    }

}
