/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.cluster.routing;

import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode.NodeFlag;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.RedisCommand;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Policy that routes keyless RediSearch commands.
 *
 * - FT.SEARCH / FT.AGGREGATE: SINGLE_NODE via round-robin over primaries - FT.CREATE / FT.ALTER / FT.DROPINDEX: BROADCAST to
 * primaries
 */
public class SearchKeylessRoutingPolicy implements KeylessRoutingPolicy {

    private volatile List<RedisClusterNode> lastPrimaries = Collections.emptyList();

    private int offset = 0; // simple RR; reset on topology shape change

    @Override
    public synchronized <K, V, T> Decision classify(RedisCommand<K, V, T> cmd, Partitions topology) {
        CommandType type = safeType(cmd);

        if (isSearchQuery(type, cmd)) {
            List<RedisClusterNode> primaries = primaries(topology);
            if (primaries.isEmpty())
                return null;
            List<RedisClusterNode> rr = roundRobinOnce(primaries);
            return Decision.singleNode(rr);
        }

        if (isSearchAdmin(type, cmd)) {
            List<RedisClusterNode> primaries = primaries(topology);
            if (primaries.isEmpty())
                return null;
            return Decision.broadcast(primaries);
        }

        return null;
    }

    private static <K, V, T> CommandType safeType(RedisCommand<K, V, T> cmd) {
        try {
            if (cmd.getType() instanceof CommandType) {
                return (CommandType) cmd.getType();
            }
            return null;
        } catch (Throwable t) {
            return null;
        }
    }

    private static <K, V, T> boolean isSearchQuery(CommandType type, RedisCommand<K, V, T> cmd) {
        if (type != null) {
            String n = type.name();
            if ("FT_SEARCH".equals(n) || "FT_AGGREGATE".equals(n))
                return true;
        }
        return tokenIs(cmd, "FT.SEARCH") || tokenIs(cmd, "FT.AGGREGATE");
    }

    private static <K, V, T> boolean isSearchAdmin(CommandType type, RedisCommand<K, V, T> cmd) {
        if (type != null) {
            String n = type.name();
            if ("FT_CREATE".equals(n) || "FT_ALTER".equals(n) || "FT_DROPINDEX".equals(n))
                return true;
        }
        return tokenIs(cmd, "FT.CREATE") || tokenIs(cmd, "FT.ALTER") || tokenIs(cmd, "FT.DROPINDEX");
    }

    private static <K, V, T> boolean tokenIs(RedisCommand<K, V, T> cmd, String expect) {
        try {
            if (cmd.getArgs() == null)
                return false;
            // First token for FT commands is the command literal (e.g., FT.SEARCH)
            String commandToken = firstToken(cmd);
            return expect.equalsIgnoreCase(commandToken);
        } catch (Throwable t) {
            return false;
        }
    }

    private static <K, V, T> String firstToken(RedisCommand<K, V, T> cmd) {
        // We don't have a direct accessor; we can use toCommandString() which is inexpensive for small arg lists
        String s = cmd.getArgs().toCommandString();
        int idx = s.indexOf(' ');
        return idx == -1 ? s : s.substring(0, idx);
    }

    private static List<RedisClusterNode> primaries(Partitions p) {
        List<RedisClusterNode> list = new ArrayList<>();
        for (RedisClusterNode n : p) {
            if (n != null && (n.is(NodeFlag.MASTER) || n.is(NodeFlag.UPSTREAM))) {
                list.add(n);
            }
        }
        return list;
    }

    private List<RedisClusterNode> roundRobinOnce(List<RedisClusterNode> primaries) {
        if (primaries.size() != lastPrimaries.size()) {
            lastPrimaries = new ArrayList<>(primaries);
            offset = 0;
        }
        if (primaries.isEmpty())
            return primaries;
        if (offset >= primaries.size())
            offset = 0;
        RedisClusterNode pick = primaries.get(offset++);
        return Collections.singletonList(pick);
    }

}
