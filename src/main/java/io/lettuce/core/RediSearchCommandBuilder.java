/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core;

import java.util.List;
import java.util.Map;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.output.BooleanOutput;
import io.lettuce.core.output.ComplexOutput;
import io.lettuce.core.output.EncodedComplexOutput;
import io.lettuce.core.output.IntegerOutput;
import io.lettuce.core.output.ProfileResultOutput;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.output.ValueListOutput;
import io.lettuce.core.protocol.BaseRedisCommandBuilder;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;
import io.lettuce.core.search.AggregateReplyParser;
import io.lettuce.core.search.AggregationReply;
import io.lettuce.core.search.ProfileResult;
import io.lettuce.core.search.SearchReply;
import io.lettuce.core.search.SearchReplyParser;
import io.lettuce.core.search.SpellCheckResult;
import io.lettuce.core.search.SpellCheckResultParser;
import io.lettuce.core.search.Suggestion;
import io.lettuce.core.search.SuggestionParser;
import io.lettuce.core.search.SynonymMapParser;
import io.lettuce.core.search.arguments.AggregateArgs;
import io.lettuce.core.search.arguments.CreateArgs;
import io.lettuce.core.search.arguments.ExplainArgs;
import io.lettuce.core.search.arguments.FieldArgs;
import io.lettuce.core.search.arguments.ProfileArgs;
import io.lettuce.core.search.arguments.SearchArgs;
import io.lettuce.core.search.arguments.SpellCheckArgs;
import io.lettuce.core.search.arguments.SugAddArgs;
import io.lettuce.core.search.arguments.SugGetArgs;
import io.lettuce.core.search.arguments.SynUpdateArgs;

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
     * Perform spelling correction on a query.
     *
     * @param index the index name
     * @param query the search query
     * @return the result of the spellcheck command
     */
    public Command<K, V, SpellCheckResult<V>> ftSpellcheck(K index, V query) {
        return ftSpellcheck(index, query, null);
    }

    /**
     * Perform spelling correction on a query.
     *
     * @param index the index name
     * @param query the search query
     * @param args the spellcheck arguments
     * @return the result of the spellcheck command
     */
    public Command<K, V, SpellCheckResult<V>> ftSpellcheck(K index, V query, SpellCheckArgs<K, V> args) {
        notNullKey(index);
        LettuceAssert.notNull(query, "Query must not be null");

        CommandArgs<K, V> commandArgs = new CommandArgs<>(codec).addKey(index).addValue(query);

        if (args != null) {
            args.build(commandArgs);
        }

        SpellCheckResultParser<K, V> parser = new SpellCheckResultParser<>(codec);
        return createCommand(FT_SPELLCHECK, new EncodedComplexOutput<>(codec, parser), commandArgs);
    }

    /**
     * Add terms to a dictionary.
     *
     * @param dict the dictionary name
     * @param terms the terms to add to the dictionary
     * @return the result of the dictadd command
     */
    @SafeVarargs
    public final Command<K, V, Long> ftDictadd(K dict, V... terms) {
        notNullKey(dict);
        LettuceAssert.notNull(terms, "Terms must not be null");
        LettuceAssert.isTrue(terms.length > 0, "At least one term must be provided");

        CommandArgs<K, V> commandArgs = new CommandArgs<>(codec).addKey(dict);
        for (V term : terms) {
            LettuceAssert.notNull(term, "Term must not be null");
            commandArgs.addValue(term);
        }

        return createCommand(FT_DICTADD, new IntegerOutput<>(codec), commandArgs);
    }

    /**
     * Delete terms from a dictionary.
     *
     * @param dict the dictionary name
     * @param terms the terms to delete from the dictionary
     * @return the result of the dictdel command
     */
    @SafeVarargs
    public final Command<K, V, Long> ftDictdel(K dict, V... terms) {
        notNullKey(dict);
        LettuceAssert.notNull(terms, "Terms must not be null");
        LettuceAssert.isTrue(terms.length > 0, "At least one term must be provided");

        CommandArgs<K, V> commandArgs = new CommandArgs<>(codec).addKey(dict);
        for (V term : terms) {
            LettuceAssert.notNull(term, "Term must not be null");
            commandArgs.addValue(term);
        }

        return createCommand(FT_DICTDEL, new IntegerOutput<>(codec), commandArgs);
    }

    /**
     * Dump all terms in a dictionary.
     *
     * @param dict the dictionary name
     * @return the result of the dictdump command
     */
    public Command<K, V, List<V>> ftDictdump(K dict) {
        notNullKey(dict);

        CommandArgs<K, V> commandArgs = new CommandArgs<>(codec).addKey(dict);

        return createCommand(FT_DICTDUMP, new ValueListOutput<>(codec), commandArgs);
    }

    /**
     * Return the execution plan for a complex query.
     *
     * @param index the index name
     * @param query the search query
     * @return the execution plan as a string
     */
    public Command<K, V, String> ftExplain(K index, V query) {
        return ftExplain(index, query, null);
    }

    /**
     * Return the execution plan for a complex query.
     *
     * @param index the index name
     * @param query the search query
     * @param args the explain arguments
     * @return the execution plan as a string
     */
    public Command<K, V, String> ftExplain(K index, V query, ExplainArgs<K, V> args) {
        notNullKey(index);
        LettuceAssert.notNull(query, "Query must not be null");

        CommandArgs<K, V> commandArgs = new CommandArgs<>(codec).addKey(index).addValue(query);

        if (args != null) {
            args.build(commandArgs);
        }

        return createCommand(FT_EXPLAIN, new StatusOutput<>(codec), commandArgs);
    }

    /**
     * Return a list of all existing indexes.
     *
     * @return the list of index names
     */
    public Command<K, V, List<V>> ftList() {
        CommandArgs<K, V> commandArgs = new CommandArgs<>(codec);
        return createCommand(FT_LIST, new ValueListOutput<>(codec), commandArgs);
    }

    /**
     * Profile the execution of a search or aggregate query.
     *
     * @param index the index name
     * @param profileArgs the profile arguments (SEARCH or AGGREGATE, with optional LIMITED)
     * @param query the query string
     * @return the profile result containing search results and profiling information
     */
    public Command<K, V, ProfileResult> ftProfile(K index, ProfileArgs<K, V> profileArgs, V query) {
        notNullKey(index);
        LettuceAssert.notNull(profileArgs, "Profile arguments must not be null");
        LettuceAssert.notNull(query, "Query must not be null");

        CommandArgs<K, V> commandArgs = new CommandArgs<>(codec).addKey(index);
        profileArgs.build(commandArgs);
        commandArgs.addValue(query);

        return createCommand(FT_PROFILE, new ProfileResultOutput<>(codec), commandArgs);
    }

    /**
     * Profile the execution of a search or aggregate query with additional parameters.
     *
     * @param index the index name
     * @param profileArgs the profile arguments (SEARCH or AGGREGATE, with optional LIMITED)
     * @param query the query string
     * @param searchArgs additional search/aggregate arguments
     * @return the profile result containing search results and profiling information
     */
    public Command<K, V, ProfileResult> ftProfile(K index, ProfileArgs<K, V> profileArgs, V query,
            SearchArgs<K, V> searchArgs) {
        notNullKey(index);
        LettuceAssert.notNull(profileArgs, "Profile arguments must not be null");
        LettuceAssert.notNull(query, "Query must not be null");

        CommandArgs<K, V> commandArgs = new CommandArgs<>(codec).addKey(index);
        profileArgs.build(commandArgs);
        commandArgs.addValue(query);

        if (searchArgs != null) {
            searchArgs.build(commandArgs);
        }

        return createCommand(FT_PROFILE, new ProfileResultOutput<>(codec), commandArgs);
    }

    /**
     * Profile the execution of a search or aggregate query with additional parameters.
     *
     * @param index the index name
     * @param profileArgs the profile arguments (SEARCH or AGGREGATE, with optional LIMITED)
     * @param query the query string
     * @param aggregateArgs additional aggregate arguments
     * @return the profile result containing search results and profiling information
     */
    public Command<K, V, ProfileResult> ftProfile(K index, ProfileArgs<K, V> profileArgs, V query,
            AggregateArgs<K, V> aggregateArgs) {
        notNullKey(index);
        LettuceAssert.notNull(profileArgs, "Profile arguments must not be null");
        LettuceAssert.notNull(query, "Query must not be null");

        CommandArgs<K, V> commandArgs = new CommandArgs<>(codec).addKey(index);
        profileArgs.build(commandArgs);
        commandArgs.addValue(query);

        if (aggregateArgs != null) {
            aggregateArgs.build(commandArgs);
        }

        return createCommand(FT_PROFILE, new ProfileResultOutput<>(codec), commandArgs);
    }

    /**
     * Dump synonym group contents.
     *
     * @param index the index name
     * @return a map where keys are synonym terms and values are lists of group IDs containing that synonym
     */
    public Command<K, V, Map<V, List<V>>> ftSyndump(K index) {
        notNullKey(index);

        CommandArgs<K, V> commandArgs = new CommandArgs<>(codec).addKey(index);

        return createCommand(FT_SYNDUMP, new EncodedComplexOutput<>(codec, new SynonymMapParser<>(codec)), commandArgs);
    }

    /**
     * Update a synonym group with additional terms.
     *
     * @param index the index name
     * @param synonymGroupId the synonym group ID
     * @param terms the terms to add to the synonym group
     * @return the result of the synupdate command
     */
    @SafeVarargs
    public final Command<K, V, String> ftSynupdate(K index, V synonymGroupId, V... terms) {
        return ftSynupdate(index, synonymGroupId, null, terms);
    }

    /**
     * Update a synonym group with additional terms.
     *
     * @param index the index name
     * @param synonymGroupId the synonym group ID
     * @param args the synupdate arguments
     * @param terms the terms to add to the synonym group
     * @return the result of the synupdate command
     */
    @SafeVarargs
    public final Command<K, V, String> ftSynupdate(K index, V synonymGroupId, SynUpdateArgs<K, V> args, V... terms) {
        notNullKey(index);
        LettuceAssert.notNull(synonymGroupId, "Synonym group ID must not be null");
        LettuceAssert.notNull(terms, "Terms must not be null");
        LettuceAssert.isTrue(terms.length > 0, "At least one term must be provided");

        CommandArgs<K, V> commandArgs = new CommandArgs<>(codec).addKey(index).addValue(synonymGroupId);

        if (args != null) {
            args.build(commandArgs);
        }

        for (V term : terms) {
            LettuceAssert.notNull(term, "Term must not be null");
            commandArgs.addValue(term);
        }

        return createCommand(FT_SYNUPDATE, new StatusOutput<>(codec), commandArgs);
    }

    /**
     * Add a suggestion string to an auto-complete suggestion dictionary.
     *
     * @param key the suggestion dictionary key
     * @param string the suggestion string to index
     * @param score the floating point number of the suggestion string's weight
     * @return the result of the sugadd command
     */
    public Command<K, V, Long> ftSugadd(K key, V string, double score) {
        return ftSugadd(key, string, score, null);
    }

    /**
     * Add a suggestion string to an auto-complete suggestion dictionary.
     *
     * @param key the suggestion dictionary key
     * @param string the suggestion string to index
     * @param score the floating point number of the suggestion string's weight
     * @param args the suggestion add arguments
     * @return the result of the sugadd command
     */
    public Command<K, V, Long> ftSugadd(K key, V string, double score, SugAddArgs<K, V> args) {
        notNullKey(key);
        LettuceAssert.notNull(string, "String must not be null");

        CommandArgs<K, V> commandArgs = new CommandArgs<>(codec).addKey(key).addValue(string).add(score);

        if (args != null) {
            args.build(commandArgs);
        }

        return createCommand(FT_SUGADD, new IntegerOutput<>(codec), commandArgs);
    }

    /**
     * Delete a string from a suggestion dictionary.
     *
     * @param key the suggestion dictionary key
     * @param string the suggestion string to delete
     * @return the result of the sugdel command
     */
    public Command<K, V, Boolean> ftSugdel(K key, V string) {
        notNullKey(key);
        LettuceAssert.notNull(string, "String must not be null");

        CommandArgs<K, V> commandArgs = new CommandArgs<>(codec).addKey(key).addValue(string);

        return createCommand(FT_SUGDEL, new BooleanOutput<>(codec), commandArgs);
    }

    /**
     * Get completion suggestions for a prefix.
     *
     * @param key the suggestion dictionary key
     * @param prefix the prefix to complete on
     * @return the result of the sugget command
     */
    public Command<K, V, List<Suggestion<V>>> ftSugget(K key, V prefix) {
        return ftSugget(key, prefix, null);
    }

    /**
     * Get completion suggestions for a prefix.
     *
     * @param key the suggestion dictionary key
     * @param prefix the prefix to complete on
     * @param args the suggestion get arguments
     * @return the result of the sugget command
     */
    public Command<K, V, List<Suggestion<V>>> ftSugget(K key, V prefix, SugGetArgs<K, V> args) {
        notNullKey(key);
        LettuceAssert.notNull(prefix, "Prefix must not be null");

        CommandArgs<K, V> commandArgs = new CommandArgs<>(codec).addKey(key).addValue(prefix);

        boolean withScores = false;
        boolean withPayloads = false;

        if (args != null) {
            withScores = args.isWithScores();
            withPayloads = args.isWithPayloads();
            args.build(commandArgs);
        }

        SuggestionParser<V> parser = new SuggestionParser<>(withScores, withPayloads);
        return createCommand(FT_SUGGET, new ComplexOutput<>(codec, parser), commandArgs);
    }

    /**
     * Get the size of an auto-complete suggestion dictionary.
     *
     * @param key the suggestion dictionary key
     * @return the result of the suglen command
     */
    public Command<K, V, Long> ftSuglen(K key) {
        notNullKey(key);

        CommandArgs<K, V> commandArgs = new CommandArgs<>(codec).addKey(key);

        return createCommand(FT_SUGLEN, new IntegerOutput<>(codec), commandArgs);
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
