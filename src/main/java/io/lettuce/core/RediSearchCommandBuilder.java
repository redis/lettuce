/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.output.EncodedComplexOutput;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.BaseRedisCommandBuilder;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;
import io.lettuce.core.search.AggregateReplyParser;
import io.lettuce.core.search.AggregationReply;
import io.lettuce.core.search.SearchReply;
import io.lettuce.core.search.SearchReplyParser;
import io.lettuce.core.search.arguments.AggregateArgs;
import io.lettuce.core.search.arguments.CreateArgs;
import io.lettuce.core.search.arguments.FieldArgs;
import io.lettuce.core.search.arguments.SearchArgs;

import java.util.List;

import static io.lettuce.core.protocol.CommandType.*;

/**
 * Command builder for RediSearch commands.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @since 6.8
 */
class RediSearchCommandBuilder<K, V> extends BaseRedisCommandBuilder<K, V> {

    RediSearchCommandBuilder(RedisCodec<K, V> codec) {
        super(codec);
    }

    /**
     * Create a new index with the given name, index options and fieldArgs.
     *
     * @param index the index name
     * @param createArgs the index options
     * @param fieldArgs the fieldArgs
     * @return the result of the create command
     */
    public Command<K, V, String> ftCreate(K index, CreateArgs<K, V> createArgs, List<FieldArgs<K>> fieldArgs) {
        notNullKey(index);
        notEmpty(fieldArgs.toArray());

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(index);

        if (createArgs != null) {
            createArgs.build(args);
        }

        args.add(CommandKeyword.SCHEMA);

        for (FieldArgs<K> arg : fieldArgs) {
            arg.build(args);
        }

        return createCommand(FT_CREATE, new StatusOutput<>(codec), args);

    }

    /**
     * Search the index with the given name using the specified query and search arguments.
     *
     * @param index the index name
     * @param query the query
     * @param searchArgs the search arguments
     * @return the result of the search command
     */
    public Command<K, V, SearchReply<K, V>> ftSearch(K index, V query, SearchArgs<K, V> searchArgs) {
        notNullKey(index);
        notNullKey(query);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(index);
        args.addValue(query);

        if (searchArgs != null) {
            searchArgs.build(args);
        }

        return createCommand(FT_SEARCH, new EncodedComplexOutput<>(codec, new SearchReplyParser<>(codec, searchArgs)), args);
    }

    /**
     * Run a search query on an index and perform aggregate transformations on the results.
     *
     * @param index the index name
     * @param query the query
     * @param aggregateArgs the aggregate arguments
     * @return the result of the aggregate command
     */
    public Command<K, V, AggregationReply<K, V>> ftAggregate(K index, V query, AggregateArgs<K, V> aggregateArgs) {
        notNullKey(index);
        notNullKey(query);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(index);
        args.addValue(query);

        if (aggregateArgs != null) {
            aggregateArgs.build(args);
        }

        return createCommand(FT_AGGREGATE, new EncodedComplexOutput<>(codec, new AggregateReplyParser<>(codec, null)), args);
    }

    /**
     * Read next results from an existing cursor.
     *
     * @param index the index name
     * @param cursorId the cursor id
     * @return the result of the cursor read command
     */
    public Command<K, V, AggregationReply<K, V>> ftCursorread(K index, long cursorId) {
        notNullKey(index);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(CommandKeyword.READ).addKey(index);
        args.add(cursorId);

        return createCommand(FT_CURSOR, new EncodedComplexOutput<>(codec, new AggregateReplyParser<>(codec, null)), args);
    }

    /**
     * Read next results from an existing cursor.
     *
     * @param index the index name
     * @param cursorId the cursor id
     * @param count the number of results to read
     * @return the result of the cursor read command
     */
    public Command<K, V, AggregationReply<K, V>> ftCursorread(K index, long cursorId, int count) {
        notNullKey(index);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(CommandKeyword.READ).addKey(index);
        args.add(cursorId);
        args.add(CommandKeyword.COUNT);
        args.add(count);

        return createCommand(FT_CURSOR, new EncodedComplexOutput<>(codec, new AggregateReplyParser<>(codec, null)), args);
    }

    /**
     * Delete a cursor.
     *
     * @param index the index name
     * @param cursorId the cursor id
     * @return the result of the cursor delete command
     */
    public Command<K, V, String> ftCursordel(K index, long cursorId) {
        notNullKey(index);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(CommandKeyword.DEL).addKey(index);
        args.add(cursorId);

        return createCommand(FT_CURSOR, new StatusOutput<>(codec), args);
    }

    /**
     * Drop the index with the given name.
     *
     * @param index the index name
     * @param deleteDocumentKeys whether to delete the document keys
     * @return the result of the drop command
     */
    public Command<K, V, String> ftDropindex(K index, boolean deleteDocumentKeys) {
        notNullKey(index);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(index);

        if (deleteDocumentKeys) {
            args.add(CommandKeyword.DD);
        }

        return createCommand(FT_DROPINDEX, new StatusOutput<>(codec), args);
    }

}
