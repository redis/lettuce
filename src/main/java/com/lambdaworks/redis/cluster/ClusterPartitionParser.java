package com.lambdaworks.redis.cluster;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.net.HostAndPort;
import com.lambdaworks.redis.LettuceStrings;
import com.lambdaworks.redis.RedisURI;

/**
 * Parser for node information output (CLUSTER NODES).
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 26.05.14 17:20
 */
class ClusterPartitionParser {

    private static final String TOKEN_MYSELF = "myself";
    private static final String TOKEN_SLOT_IMPORT = "-<-";
    private static final String TOKEN_SLOT_MIGRATING = "->-";
    private static final String TOKEN_SLOT_IN_TRANSITION = "[";
    private static final char TOKEN_NODE_SEPARATOR = '\n';
    private static final Map<String, RedisClusterNode.NodeFlag> FLAG_MAPPING = new HashMap<String, RedisClusterNode.NodeFlag>() {
        {
            put("noflags", RedisClusterNode.NodeFlag.NOFLAGS);
            put("myself", RedisClusterNode.NodeFlag.MYSELF);
            put("master", RedisClusterNode.NodeFlag.MASTER);
            put("slave", RedisClusterNode.NodeFlag.SLAVE);
            put("fail?", RedisClusterNode.NodeFlag.EVENTUAL_FAIL);
            put("fail", RedisClusterNode.NodeFlag.FAIL);
            put("handshake", RedisClusterNode.NodeFlag.HANDSHAKE);
            put("noaddr", RedisClusterNode.NodeFlag.NOADDR);
        }
    };

    private ClusterPartitionParser() {

    }

    /**
     * Parse partition lines into Partitions object.
     * 
     * @param nodes
     * @return
     */
    public static Partitions parse(String nodes) {
        Partitions result = new Partitions();

        Iterator<String> iterator = Splitter.on(TOKEN_NODE_SEPARATOR).omitEmptyStrings().split(nodes).iterator();

        while (iterator.hasNext()) {
            String node = iterator.next();
            RedisClusterNode partition = parseNode(node);
            result.addPartition(partition);
        }

        return result;
    }

    private static RedisClusterNode parseNode(String nodeInformation) {

        Iterable<String> split = Splitter.on(' ').split(nodeInformation);
        Iterator<String> iterator = split.iterator();

        RedisClusterNode partition = new RedisClusterNode();
        partition.setNodeId(iterator.next());

        HostAndPort hostAndPort = HostAndPort.fromString(iterator.next());

        if (LettuceStrings.isNotEmpty(hostAndPort.getHostText())) {
            partition.setUri(RedisURI.Builder.redis(hostAndPort.getHostText(), hostAndPort.getPort()).build());
        }

        String flags = iterator.next();
        List<String> flagStrings = Lists.newArrayList(Splitter.on(',').trimResults().split(flags).iterator());

        readFlags(flagStrings, partition);

        String slaveOf = iterator.next(); // (nodeId or -)
        long pingSentTs = Long.parseLong(iterator.next());
        long pongReceivedTs = Long.parseLong(iterator.next());
        long configEpoch = Long.parseLong(iterator.next());

        partition.setSlaveOf("-".equals(slaveOf) ? null : slaveOf);
        partition.setPingSentTimestamp(pingSentTs);
        partition.setPongReceivedTimestamp(pongReceivedTs);
        partition.setConfigEpoch(configEpoch);

        String connectedFlags = iterator.next(); // "connected" : "disconnected"

        if ("connected".equals(connectedFlags)) {
            partition.setConnected(true);
        }

        List<String> slotStrings = Lists.newArrayList(iterator); // slot, from-to [slot->-nodeID] [slot-<-nodeID]

        readSlots(slotStrings, partition);

        return partition;

    }

    private static void readFlags(List<String> flagStrings, RedisClusterNode partition) {

        for (String flagString : flagStrings) {
            if (FLAG_MAPPING.containsKey(flagString)) {
                partition.addFlag(FLAG_MAPPING.get(flagString));
            }
        }
    }

    private static void readSlots(List<String> slotStrings, RedisClusterNode partition) {

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
                    partition.getSlots().add(slot);

                }
                continue;
            }

            partition.getSlots().add(Integer.parseInt(slotString));
        }
    }

}
