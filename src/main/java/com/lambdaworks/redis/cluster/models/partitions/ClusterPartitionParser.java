package com.lambdaworks.redis.cluster.models.partitions;

import java.util.*;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.net.HostAndPort;
import com.lambdaworks.redis.LettuceStrings;
import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.RedisURI;

/**
 * Parser for node information output of {@code CLUSTER NODES} and {@code CLUSTER SLAVES}.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.0
 */
public class ClusterPartitionParser {
    public static final String CONNECTED = "connected";

    private static final String TOKEN_SLOT_IN_TRANSITION = "[";
    private static final char TOKEN_NODE_SEPARATOR = '\n';
    private static final Map<String, RedisClusterNode.NodeFlag> FLAG_MAPPING;

    static {
        ImmutableMap.Builder<String, RedisClusterNode.NodeFlag> builder = ImmutableMap.builder();

        builder.put("noflags", RedisClusterNode.NodeFlag.NOFLAGS);
        builder.put("myself", RedisClusterNode.NodeFlag.MYSELF);
        builder.put("master", RedisClusterNode.NodeFlag.MASTER);
        builder.put("slave", RedisClusterNode.NodeFlag.SLAVE);
        builder.put("fail?", RedisClusterNode.NodeFlag.EVENTUAL_FAIL);
        builder.put("fail", RedisClusterNode.NodeFlag.FAIL);
        builder.put("handshake", RedisClusterNode.NodeFlag.HANDSHAKE);
        builder.put("noaddr", RedisClusterNode.NodeFlag.NOADDR);
        FLAG_MAPPING = builder.build();
    }

    /**
     * Utility constructor.
     */
    private ClusterPartitionParser() {

    }

    /**
     * Parse partition lines into Partitions object.
     * 
     * @param nodes output of CLUSTER NODES
     * @return the partitions object.
     */
    public static Partitions parse(String nodes) {
        Partitions result = new Partitions();

        Iterator<String> iterator = Splitter.on(TOKEN_NODE_SEPARATOR).omitEmptyStrings().split(nodes).iterator();

        try {
            while (iterator.hasNext()) {
                String node = iterator.next();
                RedisClusterNode partition = parseNode(node);
                result.addPartition(partition);
            }

        } catch (Exception e) {
            throw new RedisException("Cannot parse " + nodes, e);
        }

        result.updateCache();
        return result;
    }

    private static RedisClusterNode parseNode(String nodeInformation) {

        Iterable<String> split = Splitter.on(' ').split(nodeInformation);
        Iterator<String> iterator = split.iterator();

        String nodeId = iterator.next();
        boolean connected = false;
        RedisURI uri = null;

        HostAndPort hostAndPort = HostAndPort.fromString(iterator.next());

        if (LettuceStrings.isNotEmpty(hostAndPort.getHostText())) {
            uri = RedisURI.Builder.redis(hostAndPort.getHostText(), hostAndPort.getPort()).build();
        }

        String flags = iterator.next();
        List<String> flagStrings = Lists.newArrayList(Splitter.on(',').trimResults().split(flags).iterator());

        Set<RedisClusterNode.NodeFlag> nodeFlags = readFlags(flagStrings);

        String slaveOfString = iterator.next(); // (nodeId or -)
        String slaveOf = "-".equals(slaveOfString) ? null : slaveOfString;

        long pingSentTs = getLongFromIterator(iterator, 0);
        long pongReceivedTs = getLongFromIterator(iterator, 0);
        long configEpoch = getLongFromIterator(iterator, 0);

        String connectedFlags = iterator.next(); // "connected" : "disconnected"

        if (CONNECTED.equals(connectedFlags)) {
            connected = true;
        }

        List<String> slotStrings = Lists.newArrayList(iterator); // slot, from-to [slot->-nodeID] [slot-<-nodeID]
        List<Integer> slots = readSlots(slotStrings);

        RedisClusterNode partition = new RedisClusterNode(uri, nodeId, connected, slaveOf, pingSentTs, pongReceivedTs,
                configEpoch, slots, nodeFlags);

        return partition;

    }

    private static Set<RedisClusterNode.NodeFlag> readFlags(List<String> flagStrings) {

        Set<RedisClusterNode.NodeFlag> flags = Sets.newHashSet();
        for (String flagString : flagStrings) {
            if (FLAG_MAPPING.containsKey(flagString)) {
                flags.add(FLAG_MAPPING.get(flagString));
            }
        }
        return Collections.unmodifiableSet(flags);
    }

    private static List<Integer> readSlots(List<String> slotStrings) {

        List<Integer> slots = Lists.newArrayList();
        for (String slotString : slotStrings) {

            if (slotString.startsWith(TOKEN_SLOT_IN_TRANSITION)) {
                // not interesting
                continue;

            }

            if (slotString.contains("-")) {
                // slot range
                Iterable<String> split = Splitter.on('-').split(slotString);
                Iterator<String> it = split.iterator();
                int from = Integer.parseInt(it.next());
                int to = Integer.parseInt(it.next());

                for (int slot = from; slot <= to; slot++) {
                    slots.add(slot);

                }
                continue;
            }

            slots.add(Integer.parseInt(slotString));
        }

        return Collections.unmodifiableList(slots);
    }

    private static long getLongFromIterator(Iterator<?> iterator, long defaultValue) {
        if (iterator.hasNext()) {
            Object object = iterator.next();
            if (object instanceof String) {
                return Long.parseLong((String) object);
            }
        }
        return defaultValue;
    }

}
