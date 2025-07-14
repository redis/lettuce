/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.BaseRedisCommandBuilder;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;
import io.lettuce.core.protocol.RedisCommand;
import io.lettuce.core.search.arguments.CreateArgs;
import io.lettuce.core.search.Field;

import java.util.List;

import static io.lettuce.core.protocol.CommandType.*;

/**
 * Command builder for RediSearch commands.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @since 6.6
 */
class RediSearchCommandBuilder<K, V> extends BaseRedisCommandBuilder<K, V> {

    RediSearchCommandBuilder(RedisCodec<K, V> codec) {
        super(codec);
    }

    /**
     * Create a new index with the given name, index options and fields.
     *
     * @param index the index name
     * @param createArgs the index options
     * @param fields the fields
     * @return the result of the create command
     */
    public Command<K, V, String> ftCreate(K index, CreateArgs<K, V> createArgs, List<Field<K>> fields) {
        notNullKey(index);
        notEmpty(fields.toArray());

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(index);

        if (createArgs != null) {
            createArgs.build(args);
        }

        args.add(CommandKeyword.SCHEMA);

        for (Field<K> field : fields) {
            field.build(args);
        }

        return createCommand(FT_CREATE, new StatusOutput<>(codec), args);

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
