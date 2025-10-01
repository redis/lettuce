package io.lettuce.core.cluster;

import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.ProtocolKeyword;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Hardcoded set of commands that are considered keyless for routing purposes.
 *
 * This is an interim solution until COMMAND tips are integrated. Only includes commands that are safe to route without
 * hash-slot affinity.
 */
class KeylessCommands {

    private static final Set<CommandType> KEYLESS = EnumSet.noneOf(CommandType.class);

    static {
        add(CommandType.FT_CREATE);
        add(CommandType.FT_SEARCH);
        add(CommandType.FT_AGGREGATE);
        add(CommandType.FT_CURSOR);
        add(CommandType.FT_ALIASADD);
        add(CommandType.FT_ALIASUPDATE);
        add(CommandType.FT_ALIASDEL);
        add(CommandType.FT_ALTER);
        add(CommandType.FT_TAGVALS);
        add(CommandType.FT_SPELLCHECK);
        add(CommandType.FT_DICTADD);
        add(CommandType.FT_DICTDEL);
        add(CommandType.FT_DICTDUMP);
        add(CommandType.FT_EXPLAIN);
        add(CommandType.FT_LIST);
        add(CommandType.FT_SYNDUMP);
        add(CommandType.FT_SYNUPDATE);
        add(CommandType.FT_DROPINDEX);
    }

    private static void add(CommandType t) {
        KEYLESS.add(t);
    }

    private static void tryAdd(String name) {
        try {
            KEYLESS.add(CommandType.valueOf(name));
        } catch (IllegalArgumentException ignore) {
        }
    }

    static boolean isKeyless(ProtocolKeyword type) {
        if (!(type instanceof CommandType)) {
            return false;
        }
        return KEYLESS.contains(type);
    }

    static Set<CommandType> getKeyless() {
        return Collections.unmodifiableSet(KEYLESS);
    }

}
