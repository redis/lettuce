/*
 * Copyright 2011-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.cluster.models.partitions;

import java.util.*;

import io.lettuce.core.RedisException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.SlotHash;
import io.lettuce.core.internal.HostAndPort;
import io.lettuce.core.internal.LettuceLists;
import io.lettuce.core.internal.LettuceStrings;

/**
 * Parser for node information output of {@code CLUSTER NODES}, {@code CLUSTER SLAVES}, and {@code CLUSTER SHARDS}.
 *
 * @author Mark Paluch
 * @since 3.0
 */
public class ClusterPartitionParser {

    public static final String CONNECTED = "connected";

    private static final String TOKEN_SLOT_IN_TRANSITION = "[";

    private static final char TOKEN_NODE_SEPARATOR = '\n';

    private static final Map<String, RedisClusterNode.NodeFlag> FLAG_MAPPING;

    static {
        Map<String, RedisClusterNode.NodeFlag> map = new HashMap<>();

        map.put("noflags", RedisClusterNode.NodeFlag.NOFLAGS);
        map.put("myself", RedisClusterNode.NodeFlag.MYSELF);
        map.put("master", RedisClusterNode.NodeFlag.MASTER);
        map.put("slave", RedisClusterNode.NodeFlag.SLAVE);
        map.put("replica", RedisClusterNode.NodeFlag.REPLICA);
        map.put("fail?", RedisClusterNode.NodeFlag.EVENTUAL_FAIL);
        map.put("fail", RedisClusterNode.NodeFlag.FAIL);
        map.put("handshake", RedisClusterNode.NodeFlag.HANDSHAKE);
        map.put("noaddr", RedisClusterNode.NodeFlag.NOADDR);
        map.put("loading", RedisClusterNode.NodeFlag.LOADING);
        map.put("online", RedisClusterNode.NodeFlag.ONLINE);
        FLAG_MAPPING = Collections.unmodifiableMap(map);
    }

    /**
     * Utility constructor.
     */
    private ClusterPartitionParser() {

    }

    /**
     * Parse partition lines into Partitions object.
     *
     * @param clusterShards output of CLUSTER SHARDS
     * @return the partitions object.
     * @since 6.2
     */
    public static Partitions parse(List<Object> clusterShards) {

        Partitions partitions = new Partitions();

        try {

            Map<String, RedisClusterNode> nodeMap = new LinkedHashMap<>();

            for (Object s : clusterShards) {

                List<Object> shard = (List<Object>) s;

                if (shard.size() < 4) {
                    continue;
                }

                KeyValueMap shardMap = toMap(shard);
                List<Integer> slotRanges = shardMap.get("slots");
                List<List<Object>> nodes = shardMap.get("nodes");
                BitSet bitSet = readSlotRanges(slotRanges);

                List<RedisClusterNode> parsedNodes = new ArrayList<>(nodes.size());
                for (List<Object> node : nodes) {
                    RedisClusterNode clusterNode = parseNode(node, (BitSet) bitSet.clone());
                    nodeMap.putIfAbsent(clusterNode.getNodeId(), clusterNode);
                    parsedNodes.add(clusterNode);
                }

                RedisClusterNode master = findMaster(parsedNodes);

                if (master != null) {
                    associateMasterWithReplicas(master, parsedNodes);
                }
            }

            partitions.addAll(nodeMap.values());

        } catch (Exception e) {
            throw new RedisException("Cannot parse " + clusterShards, e);
        }

        return partitions;
    }

    private static RedisClusterNode findMaster(List<RedisClusterNode> nodes) {

        for (RedisClusterNode parsedNode : nodes) {
            if (parsedNode.is(RedisClusterNode.NodeFlag.UPSTREAM) || parsedNode.is(RedisClusterNode.NodeFlag.MASTER)) {
                return parsedNode;
            }
        }

        return null;
    }

    private static void associateMasterWithReplicas(RedisClusterNode master, List<RedisClusterNode> nodes) {

        for (RedisClusterNode parsedNode : nodes) {
            if (parsedNode.is(RedisClusterNode.NodeFlag.REPLICA) || parsedNode.is(RedisClusterNode.NodeFlag.SLAVE)) {
                parsedNode.setSlaveOf(master.getNodeId());
            }
        }
    }

    private static RedisClusterNode parseNode(List<Object> kvlist, BitSet slots) {

        KeyValueMap nodeMap = toMap(kvlist);

        RedisClusterNode node = new RedisClusterNode();
        node.setNodeId(nodeMap.get("id"));

        RedisURI uri;
        int port = ((Long) nodeMap.get("port")).intValue();
        if (LettuceStrings.isNotEmpty(nodeMap.get("hostname"))) {
            uri = RedisURI.create(nodeMap.get("hostname"), port);
        } else {
            uri = RedisURI.create(nodeMap.get("endpoint"), port);
        }

        node.setUri(uri);

        Set<RedisClusterNode.NodeFlag> flags = new HashSet<>();

        flags.add(FLAG_MAPPING.get(nodeMap.<String> get("role")));
        flags.add(FLAG_MAPPING.get(nodeMap.<String> get("health")));

        if (flags.contains(RedisClusterNode.NodeFlag.SLAVE)) {
            flags.add(RedisClusterNode.NodeFlag.REPLICA);
        } else if (flags.contains(RedisClusterNode.NodeFlag.REPLICA)) {
            flags.add(RedisClusterNode.NodeFlag.SLAVE);
        }

        if (flags.contains(RedisClusterNode.NodeFlag.MASTER)) {
            flags.add(RedisClusterNode.NodeFlag.UPSTREAM);
        } else if (flags.contains(RedisClusterNode.NodeFlag.UPSTREAM)) {
            flags.add(RedisClusterNode.NodeFlag.MASTER);
        }

        node.setFlags(flags);
        node.setReplOffset(nodeMap.get("replication-offset"));
        node.setSlots(slots);

        return node;
    }

    /**
     * Parse partition lines into Partitions object.
     *
     * @param nodes output of CLUSTER NODES
     * @return the partitions object.
     */
    public static Partitions parse(String nodes) {

        Partitions partitions = new Partitions();

        try {

            String[] lines = nodes.split(Character.toString(TOKEN_NODE_SEPARATOR));
            List<RedisClusterNode> mappedNodes = new ArrayList<>(lines.length);

            for (String line : lines) {

                if (line.isEmpty()) {
                    continue;

                }
                mappedNodes.add(ClusterPartitionParser.parseNode(line));
            }
            partitions.addAll(mappedNodes);
        } catch (Exception e) {
            throw new RedisException("Cannot parse " + nodes, e);
        }

        return partitions;
    }

    private static RedisClusterNode parseNode(String nodeInformation) {

        Iterator<String> iterator = Arrays.asList(nodeInformation.split(" ")).iterator();

        String nodeId = iterator.next();
        boolean connected = false;
        RedisURI uri = null;

        String hostAndPortPart = iterator.next();
        if (hostAndPortPart.contains("@")) {
            hostAndPortPart = hostAndPortPart.substring(0, hostAndPortPart.indexOf('@'));
        }

        HostAndPort hostAndPort = HostAndPort.parseCompat(hostAndPortPart);

        if (LettuceStrings.isNotEmpty(hostAndPort.getHostText())) {
            uri = RedisURI.Builder.redis(hostAndPort.getHostText(), hostAndPort.getPort()).build();
        }

        String flags = iterator.next();
        List<String> flagStrings = LettuceLists.newList(flags.split("\\,"));

        Set<RedisClusterNode.NodeFlag> nodeFlags = readFlags(flagStrings);

        String replicaOfString = iterator.next(); // (nodeId or -)
        String replicaOf = "-".equals(replicaOfString) ? null : replicaOfString;

        long pingSentTs = getLongFromIterator(iterator, 0);
        long pongReceivedTs = getLongFromIterator(iterator, 0);
        long configEpoch = getLongFromIterator(iterator, 0);

        String connectedFlags = iterator.next(); // "connected" : "disconnected"

        if (CONNECTED.equals(connectedFlags)) {
            connected = true;
        }

        List<String> slotStrings = LettuceLists.newList(iterator); // slot, from-to [slot->-nodeID] [slot-<-nodeID]
        BitSet slots = readSlots(slotStrings);

        RedisClusterNode partition = new RedisClusterNode(uri, nodeId, connected, replicaOf, pingSentTs, pongReceivedTs,
                configEpoch, slots, nodeFlags);

        return partition;

    }

    private static Set<RedisClusterNode.NodeFlag> readFlags(List<String> flagStrings) {

        Set<RedisClusterNode.NodeFlag> flags = new HashSet<>();
        for (String flagString : flagStrings) {
            if (FLAG_MAPPING.containsKey(flagString)) {
                flags.add(FLAG_MAPPING.get(flagString));
            }
        }

        if (flags.contains(RedisClusterNode.NodeFlag.SLAVE)) {
            flags.add(RedisClusterNode.NodeFlag.REPLICA);
        }

        return Collections.unmodifiableSet(flags);
    }

    private static BitSet readSlots(List<String> slotStrings) {

        BitSet slots = new BitSet(SlotHash.SLOT_COUNT);
        for (String slotString : slotStrings) {

            if (slotString.startsWith(TOKEN_SLOT_IN_TRANSITION)) {
                // not interesting
                continue;

            }

            if (slotString.contains("-")) {
                // slot range
                Iterator<String> it = Arrays.asList(slotString.split("\\-")).iterator();
                int from = Integer.parseInt(it.next());
                int to = Integer.parseInt(it.next());

                addSlots(slots, from, to);
                continue;
            }

            slots.set(Integer.parseInt(slotString));
        }

        return slots;
    }

    private static BitSet readSlotRanges(List<Integer> slotRanges) {

        BitSet slots = new BitSet(SlotHash.SLOT_COUNT);

        for (int i = 0; i < slotRanges.size(); i += 2) {

            Number from = slotRanges.get(i);
            Number to = slotRanges.get(i + 1);

            addSlots(slots, from.intValue(), to.intValue());
        }

        return slots;
    }

    private static void addSlots(BitSet slots, int from, int to) {
        for (int slot = from; slot <= to; slot++) {
            slots.set(slot);

        }
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

    private static KeyValueMap toMap(List<Object> kvlist) {

        if (kvlist.size() % 2 != 0) {
            throw new IllegalArgumentException("Key-Value list must contain an even number of key-value tuples");
        }

        Map<String, Object> map = new LinkedHashMap<>(kvlist.size() / 2);
        for (int i = 0; i < kvlist.size(); i += 2) {

            String key = (String) kvlist.get(i);
            Object value = kvlist.get(i + 1);

            map.put(key, value);
        }

        return new KeyValueMap(map);

    }

    static class KeyValueMap {

        private final Map<String, Object> map;

        public KeyValueMap(Map<String, Object> map) {
            this.map = map;
        }

        public <T> T get(String key) {
            return (T) map.get(key);
        }

    }

}
