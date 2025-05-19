/*
 * Copyright 2017-2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.api;

import io.lettuce.core.VAddArgs;
import io.lettuce.core.VSimArgs;
import io.lettuce.core.annotations.Experimental;
import io.lettuce.core.json.JsonPath;
import io.lettuce.core.json.JsonType;
import io.lettuce.core.json.JsonValue;
import io.lettuce.core.json.arguments.JsonGetArgs;
import io.lettuce.core.json.arguments.JsonMsetArgs;
import io.lettuce.core.json.arguments.JsonRangeArgs;
import io.lettuce.core.json.arguments.JsonSetArgs;
import io.lettuce.core.vector.RawVector;
import io.lettuce.core.vector.Vector;
import io.lettuce.core.vector.VectorMetadata;

import java.util.List;
import java.util.Map;

/**
 * ${intent} for Vector Sets
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Tihomir Mateev
 * @see <a href="https://redis.io/docs/latest/develop/data-types/vector-sets/">Redis Vector Sets</a>
 * @since 6.7
 */
public interface RedisVectorSetCommands<K, V> {

    @Experimental
    Boolean vadd(K key, Vector vector, V element);

    @Experimental
    Boolean vadd(K key, Vector vector, int dimensionality, V element);

    @Experimental
    Boolean vadd(K key, Vector vector, V element, VAddArgs args);

    @Experimental
    Boolean vadd(K key, Vector vector, int dimensionality, V element, VAddArgs args);

    @Experimental
    Integer vcard(K key);

    @Experimental
    List<V> vdim(K key);

    @Experimental
    Vector vemb(K key, V element);

    @Experimental
    RawVector vembRaw(K key, V element);

    @Experimental
    String vgetattr(K key, V element);

    @Experimental
    VectorMetadata vinfo(K key);

    @Experimental
    List<V> vlinks(K key, V element);

    @Experimental
    List<V> vlinksWithScores(K key, V element);

    @Experimental
    List<V> vrandmember(K key);

    @Experimental
    List<V> vrandmember(K key, int count);

    @Experimental
    Boolean vrem(K key, V element);

    @Experimental
    Boolean vsetattr(K key, V element, String json);

    @Experimental
    List<V> vsim(K key, Vector vector);

    @Experimental
    List<V> vsim(K key, Vector vector, VSimArgs args);

    @Experimental
    Map<V, Integer> vsimWithScore(K key, Vector vector);

    @Experimental
    Map<V, Integer> vsimWithScore(K key, Vector vector, VSimArgs args);


}
