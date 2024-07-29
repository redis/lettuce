package io.lettuce.core.output;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.json.JsonValue;
import io.lettuce.core.json.JsonParser;
import io.lettuce.core.json.JsonParserRegistry;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

/**
 * {@link List} of string output.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Will Glozer
 * @author Mark Paluch
 */
public class JsonValueListOutput<K, V> extends CommandOutput<K, V, List<JsonValue<V>>> {

    private boolean initialized;

    private final JsonParser<K, V> parser;

    public JsonValueListOutput(RedisCodec<K, V> codec) {
        super(codec, Collections.emptyList());

        parser = JsonParserRegistry.getJsonParser(codec);
    }

    @Override
    public void set(ByteBuffer bytes) {
        if (!initialized) {
            multi(1);
        }

        output.add(parser.createJsonValue(bytes));
    }

    @Override
    public void multi(int count) {
        if (!initialized) {
            output = OutputFactory.newList(count);
            initialized = true;
        }
    }

}
