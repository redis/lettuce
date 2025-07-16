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
import io.lettuce.core.output.ValueListOutput;
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

        boolean withCursor = false;

        if (aggregateArgs != null) {
            aggregateArgs.build(args);
            withCursor = aggregateArgs.getWithCursor() != null && aggregateArgs.getWithCursor().isPresent();
        }

        return createCommand(FT_AGGREGATE, new EncodedComplexOutput<>(codec, new AggregateReplyParser<>(codec, withCursor)),
                args);
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

        if (count >= 0) {
            args.add(CommandKeyword.COUNT);
            args.add(count);
        }

        return createCommand(FT_CURSOR, new EncodedComplexOutput<>(codec, new AggregateReplyParser<>(codec, true)), args);
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
     * Add an alias to an index.
     *
     * @param alias the alias name
     * @param index the index name
     * @return the result of the alias add command
     */
    public Command<K, V, String> ftAliasadd(K alias, K index) {
        notNullKey(alias);
        notNullKey(index);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(alias).addKey(index);

        return createCommand(FT_ALIASADD, new StatusOutput<>(codec), args);
    }

    /**
     * Update an alias to point to a different index.
     *
     * @param alias the alias name
     * @param index the index name
     * @return the result of the alias update command
     */
    public Command<K, V, String> ftAliasupdate(K alias, K index) {
        notNullKey(alias);
        notNullKey(index);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(alias).addKey(index);

        return createCommand(FT_ALIASUPDATE, new StatusOutput<>(codec), args);
    }

    /**
     * Remove an alias from an index.
     *
     * @param alias the alias name
     * @return the result of the alias delete command
     */
    public Command<K, V, String> ftAliasdel(K alias) {
        notNullKey(alias);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(alias);

        return createCommand(FT_ALIASDEL, new StatusOutput<>(codec), args);
    }

    /**
     * Add new attributes to an existing index.
     *
     * @param index the index name
     * @param skipInitialScan whether to skip the initial scan of existing documents
     * @param fieldArgs the field arguments for the new attributes to add
     * @return the result of the alter command
     */
    public Command<K, V, String> ftAlter(K index, boolean skipInitialScan, List<FieldArgs<K>> fieldArgs) {
        notNullKey(index);
        notEmpty(fieldArgs.toArray());

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(index);

        if (skipInitialScan) {
            args.add(CommandKeyword.SKIPINITIALSCAN);
        }

        args.add(CommandKeyword.SCHEMA);
        args.add(CommandKeyword.ADD);

        for (FieldArgs<K> arg : fieldArgs) {
            arg.build(args);
        }

        return createCommand(FT_ALTER, new StatusOutput<>(codec), args);
    }

    /**
     * Return distinct values indexed in a Tag field.
     *
     * @param index the index name
     * @param fieldName the name of a Tag field defined in the schema
     * @return the result of the tagvals command
     */
    public Command<K, V, List<V>> ftTagvals(K index, K fieldName) {
        notNullKey(index);
        notNullKey(fieldName);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(index).addKey(fieldName);

        return createCommand(FT_TAGVALS, new ValueListOutput<>(codec), args);
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
