/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.json.JsonParser;
import io.lettuce.core.json.JsonValue;
import io.lettuce.core.output.*;
import io.lettuce.core.protocol.BaseRedisCommandBuilder;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.RedisCommand;
import io.lettuce.core.vector.RawVector;
import io.lettuce.core.vector.VectorMetadata;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static io.lettuce.core.protocol.CommandType.*;
import static io.lettuce.core.protocol.CommandKeyword.*;

/**
 * Implementation of the {@link BaseRedisCommandBuilder} handling Vector Set commands.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Tihomir Mateev
 * @since 6.7
 */
public class RedisVectorSetCommandBuilder<K, V> extends BaseRedisCommandBuilder<K, V> {

    private final Supplier<JsonParser> parser;

    /**
     * Initialize a new {@link RedisVectorSetCommandBuilder} with the specified codec.
     *
     * @param codec the codec used to encode/decode keys and values, must not be {@code null}
     * @param parser
     */
    public RedisVectorSetCommandBuilder(RedisCodec<K, V> codec, Supplier<JsonParser> parser) {
        super(codec);

        this.parser = parser;
    }

    /**
     * Create a new {@code VADD} command to add a vector to a vector set.
     *
     * @param key the key of the vector set, must not be {@code null}
     * @param element the name of the element being added to the vector set, must not be {@code null}
     * @param vectors the vector values as floating point numbers, must not be empty
     * @return a new {@link Command} that adds a vector to a vector set
     * @see <a href="https://redis.io/docs/latest/commands/vadd/">Redis Documentation: VADD</a>
     */
    public Command<K, V, Boolean> vadd(K key, V element, Double[] vectors) {
        return vadd(key, -1, element, null, vectors);
    }

    /**
     * Create a new {@code VADD} command to add a vector to a vector set with the specified dimensionality.
     *
     * @param key the key of the vector set, must not be {@code null}
     * @param dimensionality the number of dimensions for the vector
     * @param element the name of the element being added to the vector set, must not be {@code null}
     * @param vectors the vector values as floating point numbers, must not be empty
     * @return a new {@link Command} that adds a vector to a vector set with the specified dimensionality
     * @see <a href="https://redis.io/docs/latest/commands/vadd/">Redis Documentation: VADD</a>
     */
    public Command<K, V, Boolean> vadd(K key, int dimensionality, V element, Double[] vectors) {
        return vadd(key, dimensionality, element, null, vectors);
    }

    /**
     * Create a new {@code VADD} command to add a vector to a vector set with additional options.
     *
     * @param key the key of the vector set, must not be {@code null}
     * @param element the name of the element being added to the vector set, must not be {@code null}
     * @param vAddArgs the additional arguments for the VADD command
     * @param vectors the vector values as floating point numbers, must not be empty
     * @return a new {@link Command} that adds a vector to a vector set with additional options
     * @see <a href="https://redis.io/docs/latest/commands/vadd/">Redis Documentation: VADD</a>
     */
    public Command<K, V, Boolean> vadd(K key, V element, VAddArgs vAddArgs, Double[] vectors) {
        return vadd(key, -1, element, vAddArgs, vectors);
    }

    /**
     * Create a new {@code VADD} command to add a vector to a vector set with the specified dimensionality and additional
     * options.
     *
     * @param key the key of the vector set, must not be {@code null}
     * @param dimensionality the number of dimensions for the vector
     * @param element the name of the element being added to the vector set, must not be {@code null}
     * @param vAddArgs the additional arguments for the VADD command
     * @param vectors the vector values as floating point numbers, must not be empty
     * @return a new {@link Command} that adds a vector to a vector set with the specified dimensionality and additional options
     * @see <a href="https://redis.io/docs/latest/commands/vadd/">Redis Documentation: VADD</a>
     */
    public Command<K, V, Boolean> vadd(K key, int dimensionality, V element, VAddArgs vAddArgs, Double[] vectors) {
        notNullKey(key);
        notNullKey(element);
        notEmpty(vectors);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        if (dimensionality > 0) {
            args.add(CommandKeyword.REDUCE);
            args.add(dimensionality);
        }

        if (vectors.length > 1) {
            args.add(CommandKeyword.VALUES);
            args.add(vectors.length);
            Arrays.stream(vectors).map(Object::toString).forEach(args::add);
        } else {
            args.add(vectors[0]);
        }

        args.addValue(element);

        if (vAddArgs != null) {
            vAddArgs.build(args);
        }

        return createCommand(VADD, new BooleanOutput<>(codec), args);
    }

    /**
     * Create a new {@code VCARD} command to get the number of elements in a vector set.
     *
     * @param key the key of the vector set, must not be {@code null}
     * @return a new {@link Command} that returns the number of elements in the vector set
     * @see <a href="https://redis.io/docs/latest/commands/vcard/">Redis Documentation: VCARD</a>
     */
    public Command<K, V, Long> vcard(K key) {
        notNullKey(key);
        return createCommand(VCARD, new IntegerOutput<>(codec), new CommandArgs<>(codec).addKey(key));
    }

    /**
     * Create a new {@code VDIM} command to get the dimensionality of a vector set.
     *
     * @param key the key of the vector set, must not be {@code null}
     * @return a new {@link Command} that returns the dimensionality of the vector set
     * @see <a href="https://redis.io/docs/latest/commands/vdim/">Redis Documentation: VDIM</a>
     */
    public Command<K, V, Long> vdim(K key) {
        notNullKey(key);
        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        return createCommand(VDIM, new IntegerOutput<>(codec), args);
    }

    /**
     * Create a new {@code VEMB} command to get the vector values for an element in a vector set.
     *
     * @param key the key of the vector set, must not be {@code null}
     * @param element the name of the element in the vector set, must not be {@code null}
     * @return a new {@link Command} that returns the vector values for the specified element
     * @see <a href="https://redis.io/docs/latest/commands/vemb/">Redis Documentation: VEMB</a>
     */
    public Command<K, V, List<Double>> vemb(K key, V element) {
        notNullKey(key);
        notNullKey(element);
        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addValue(element);
        return createCommand(VEMB, new DoubleListOutput<>(codec), args);
    }

    /**
     * Create a new {@code VEMB} command with the RAW option to get the raw binary vector data for an element in a vector set.
     *
     * @param key the key of the vector set, must not be {@code null}
     * @param element the name of the element in the vector set, must not be {@code null}
     * @return a new {@link Command} that returns the raw vector data for the specified element
     * @see <a href="https://redis.io/docs/latest/commands/vemb/">Redis Documentation: VEMB</a>
     */
    public Command<K, V, RawVector> vembRaw(K key, V element) {
        notNullKey(key);
        notNullKey(element);
        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addValue(element).add(RAW);
        return createCommand(VEMB, new ComplexOutput<>(codec, RawVectorParser.INSTANCE), args);
    }

    /**
     * Create a new {@code VGETATTR} command to get the attributes associated with an element in a vector set.
     *
     * @param key the key of the vector set, must not be {@code null}
     * @param element the name of the element in the vector set, must not be {@code null}
     * @return a new {@link Command} that returns the attributes associated with the specified element
     * @see <a href="https://redis.io/docs/latest/commands/vgetattr/">Redis Documentation: VGETATTR</a>
     */
    public Command<K, V, String> vgetattr(K key, V element) {
        notNullKey(key);
        notNullKey(element);
        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addValue(element);
        return createCommand(VGETATTR, new StatusOutput<>(codec), args);
    }

    /**
     * Create a new {@code VGETATTR} command to get the attributes associated with an element in a vector set.
     *
     * @param key the key of the vector set, must not be {@code null}
     * @param element the name of the element in the vector set, must not be {@code null}
     * @return a new {@link Command} that returns the attributes associated with the specified element
     * @see <a href="https://redis.io/docs/latest/commands/vgetattr/">Redis Documentation: VGETATTR</a>
     */
    public Command<K, V, List<JsonValue>> vgetattrAsJsonValue(K key, V element) {

        notNullKey(key);
        notNullKey(element);
        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addValue(element);
        return createCommand(VGETATTR, new JsonValueListOutput<>(codec, parser.get()), args);
    }

    /**
     * Create a new {@code VINFO} command to get information about a vector set.
     *
     * @param key the key of the vector set, must not be {@code null}
     * @return a new {@link Command} that returns metadata about the vector set
     * @see <a href="https://redis.io/docs/latest/commands/vinfo/">Redis Documentation: VINFO</a>
     */
    public Command<K, V, VectorMetadata> vinfo(K key) {
        notNullKey(key);
        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        return createCommand(VINFO, new ComplexOutput<>(codec, VectorMetadataParser.INSTANCE), args);
    }

    /**
     * Create a new {@code VLINKS} command to get the links (connections) of an element in the HNSW graph.
     *
     * @param key the key of the vector set, must not be {@code null}
     * @param element the name of the element in the vector set, must not be {@code null}
     * @return a new {@link Command} that returns a list of elements that are linked to the specified element
     * @see <a href="https://redis.io/docs/latest/commands/vlinks/">Redis Documentation: VLINKS</a>
     */
    public Command<K, V, List<V>> vlinks(K key, V element) {
        notNullKey(key);
        notNullKey(element);
        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addValue(element);
        return createCommand(VLINKS, new ValueListOutput<>(codec), args);
    }

    /**
     * Create a new {@code VLINKS} command with the WITHSCORES option to get the links of an element along with their scores.
     *
     * @param key the key of the vector set, must not be {@code null}
     * @param element the name of the element in the vector set, must not be {@code null}
     * @return a new {@link Command} that returns a list of elements with their similarity scores
     * @see <a href="https://redis.io/docs/latest/commands/vlinks/">Redis Documentation: VLINKS</a>
     */
    public Command<K, V, Map<V, Double>> vlinksWithScores(K key, V element) {
        notNullKey(key);
        notNullKey(element);
        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addValue(element).add(WITHSCORES);
        return createCommand(VLINKS, new ValueDoubleMapOutput<>(codec), args);
    }

    /**
     * Create a new {@code VRANDMEMBER} command to get a random element from a vector set.
     *
     * @param key the key of the vector set, must not be {@code null}
     * @return a new {@link Command} that returns a random element from the vector set
     * @see <a href="https://redis.io/docs/latest/commands/vrandmember/">Redis Documentation: VRANDMEMBER</a>
     */
    public Command<K, V, V> vrandmember(K key) {
        notNullKey(key);
        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        return createCommand(VRANDMEMBER, new ValueOutput<>(codec), args);
    }

    /**
     * Create a new {@code VRANDMEMBER} command to get multiple random elements from a vector set.
     *
     * @param key the key of the vector set, must not be {@code null}
     * @param count the number of random elements to return
     * @return a new {@link Command} that returns a list of random elements from the vector set
     * @see <a href="https://redis.io/docs/latest/commands/vrandmember/">Redis Documentation: VRANDMEMBER</a>
     */
    public Command<K, V, List<V>> vrandmember(K key, int count) {
        notNullKey(key);
        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(count);
        return createCommand(VRANDMEMBER, new ValueListOutput<>(codec), args);
    }

    /**
     * Create a new {@code VREM} command to remove an element from a vector set.
     *
     * @param key the key of the vector set, must not be {@code null}
     * @param element the name of the element to remove from the vector set, must not be {@code null}
     * @return a new {@link Command} that returns {@literal true} if the element was removed, {@literal false} if the key or
     *         element does not exist
     * @see <a href="https://redis.io/docs/latest/commands/vrem/">Redis Documentation: VREM</a>
     */
    public Command<K, V, Boolean> vrem(K key, V element) {
        notNullKey(key);
        notNullKey(element);
        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addValue(element);
        return createCommand(VREM, new BooleanOutput<>(codec), args);
    }

    /**
     * Create a new {@code VSETATTR} command to set or update the attributes for an element in a vector set.
     *
     * @param key the key of the vector set, must not be {@code null}
     * @param element the name of the element in the vector set, must not be {@code null}
     * @param json the attributes as a JSON string, must not be {@code null}
     * @return a new {@link Command} that returns {@literal true} if the attributes were set, {@literal false} if the key or
     *         element does not exist
     * @see <a href="https://redis.io/docs/latest/commands/vsetattr/">Redis Documentation: VSETATTR</a>
     */
    public Command<K, V, Boolean> vsetattr(K key, V element, String json) {
        notNullKey(key);
        notNullKey(element);
        notNullKey(json);
        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addValue(element).add(json);
        return createCommand(VSETATTR, new BooleanOutput<>(codec), args);
    }

    /**
     * Create a new {@code VSETATTR} command to set or update the attributes for an element in a vector set.
     *
     * @param key the key of the vector set, must not be {@code null}
     * @param element the name of the element in the vector set, must not be {@code null}
     * @param json the attributes as a JSON string, must not be {@code null}
     * @return a new {@link Command} that returns {@literal true} if the attributes were set, {@literal false} if the key or
     *         element does not exist
     * @see <a href="https://redis.io/docs/latest/commands/vsetattr/">Redis Documentation: VSETATTR</a>
     */
    public Command<K, V, Boolean> vsetattr(K key, V element, JsonValue json) {
        notNullKey(key);
        notNullKey(element);
        notNullKey(json);
        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addValue(element).add(json.toString());
        return createCommand(VSETATTR, new BooleanOutput<>(codec), args);
    }

    /**
     * Create a new {@code VSIM} command to find the most similar vectors to the given query vector in a vector set.
     *
     * @param key the key of the vector set, must not be {@code null}
     * @param vectors the query vector values as floating point numbers, must not be empty
     * @return a new {@link Command} that returns a list of elements most similar to the query vector
     * @see <a href="https://redis.io/docs/latest/commands/vsim/">Redis Documentation: VSIM</a>
     */
    public Command<K, V, List<V>> vsim(K key, Double[] vectors) {
        return vsim(key, null, vectors);
    }

    /**
     * Create a new {@code VSIM} command to find the most similar vectors to the given element's vector in a vector set.
     *
     * @param key the key of the vector set, must not be {@code null}
     * @param element the name of the element whose vector will be used as the query, must not be {@code null}
     * @return a new {@link Command} that returns a list of elements most similar to the specified element
     * @see <a href="https://redis.io/docs/latest/commands/vsim/">Redis Documentation: VSIM</a>
     */
    public Command<K, V, List<V>> vsim(K key, V element) {
        return vsim(key, null, element);
    }

    /**
     * Create a new {@code VSIM} command to find the most similar vectors to the given query vector in a vector set with
     * additional options.
     *
     * @param key the key of the vector set, must not be {@code null}
     * @param vSimArgs the additional arguments for the VSIM command
     * @param vectors the query vector values as floating point numbers, must not be empty
     * @return a new {@link Command} that returns a list of elements most similar to the query vector
     * @see <a href="https://redis.io/docs/latest/commands/vsim/">Redis Documentation: VSIM</a>
     */
    public Command<K, V, List<V>> vsim(K key, VSimArgs vSimArgs, Double[] vectors) {
        notNullKey(key);
        notEmpty(vectors);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);

        if (vectors.length > 1) {
            args.add(CommandKeyword.VALUES);
            args.add(vectors.length);
            Arrays.stream(vectors).map(Object::toString).forEach(args::add);
        } else {
            args.add(vectors[0]);
        }

        if (vSimArgs != null) {
            vSimArgs.build(args);
        }

        return createCommand(VSIM, new ValueListOutput<>(codec), args);
    }

    /**
     * Create a new {@code VSIM} command to find the most similar vectors to the given element's vector in a vector set with
     * additional options.
     *
     * @param key the key of the vector set, must not be {@code null}
     * @param vSimArgs the additional arguments for the VSIM command
     * @param element the name of the element whose vector will be used as the query, must not be {@code null}
     * @return a new {@link Command} that returns a list of elements most similar to the specified element
     * @see <a href="https://redis.io/docs/latest/commands/vsim/">Redis Documentation: VSIM</a>
     */
    public Command<K, V, List<V>> vsim(K key, VSimArgs vSimArgs, V element) {
        notNullKey(key);
        notNullKey(element);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(ELE).addValue(element);

        if (vSimArgs != null) {
            vSimArgs.build(args);
        }

        return createCommand(VSIM, new ValueListOutput<>(codec), args);
    }

    /**
     * Create a new {@code VSIM} command with the WITHSCORES option to find the most similar vectors to the given query vector
     * and return them with their similarity scores.
     *
     * @param key the key of the vector set, must not be {@code null}
     * @param vectors the query vector values as floating point numbers, must not be empty
     * @return a new {@link Command} that returns a map of elements to their similarity scores
     * @see <a href="https://redis.io/docs/latest/commands/vsim/">Redis Documentation: VSIM</a>
     */
    public Command<K, V, Map<V, Double>> vsimWithScore(K key, Double[] vectors) {
        return vsimWithScore(key, null, vectors);
    }

    /**
     * Create a new {@code VSIM} command with the WITHSCORES option to find the most similar vectors to the given element's
     * vector and return them with their similarity scores.
     *
     * @param key the key of the vector set, must not be {@code null}
     * @param element the name of the element whose vector will be used as the query, must not be {@code null}
     * @return a new {@link Command} that returns a map of elements to their similarity scores
     * @see <a href="https://redis.io/docs/latest/commands/vsim/">Redis Documentation: VSIM</a>
     */
    public Command<K, V, Map<V, Double>> vsimWithScore(K key, V element) {
        return vsimWithScore(key, null, element);
    }

    /**
     * Create a new {@code VSIM} command with the WITHSCORES option to find the most similar vectors to the given query vector
     * with additional options and return them with their similarity scores.
     *
     * @param key the key of the vector set, must not be {@code null}
     * @param vSimArgs the additional arguments for the VSIM command
     * @param vectors the query vector values as floating point numbers, must not be empty
     * @return a new {@link Command} that returns a map of elements to their similarity scores
     * @see <a href="https://redis.io/docs/latest/commands/vsim/">Redis Documentation: VSIM</a>
     */
    public Command<K, V, Map<V, Double>> vsimWithScore(K key, VSimArgs vSimArgs, Double[] vectors) {
        notNullKey(key);
        notEmpty(vectors);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);

        if (vectors.length > 1) {
            args.add(CommandKeyword.VALUES);
            args.add(vectors.length);
            Arrays.stream(vectors).map(Object::toString).forEach(args::add);
        } else {
            args.add(vectors[0]);
        }

        args.add(WITHSCORES);

        if (vSimArgs != null) {
            vSimArgs.build(args);
        }

        return createCommand(VSIM, new ValueDoubleMapOutput<>(codec), args);
    }

    /**
     * Create a new {@code VSIM} command with the WITHSCORES option to find the most similar vectors to the given element's
     * vector with additional options and return them with their similarity scores.
     *
     * @param key the key of the vector set, must not be {@code null}
     * @param vSimArgs the additional arguments for the VSIM command
     * @param element the name of the element whose vector will be used as the query, must not be {@code null}
     * @return a new {@link Command} that returns a map of elements to their similarity scores
     * @see <a href="https://redis.io/docs/latest/commands/vsim/">Redis Documentation: VSIM</a>
     */
    public Command<K, V, Map<V, Double>> vsimWithScore(K key, VSimArgs vSimArgs, V element) {
        notNullKey(key);
        notNullKey(element);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(ELE).addValue(element).add(WITHSCORES);

        if (vSimArgs != null) {
            vSimArgs.build(args);
        }

        return createCommand(VSIM, new ValueDoubleMapOutput<>(codec), args);
    }

}
