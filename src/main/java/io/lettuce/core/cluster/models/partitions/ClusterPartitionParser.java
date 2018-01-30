/*
 * Copyright 2011-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.cluster.models.partitions;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.lettuce.core.LettuceStrings;
import io.lettuce.core.RedisException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.internal.HostAndPort;
import io.lettuce.core.internal.LettuceLists;

/**
 * Parser for node information output of {@code CLUSTER NODES} and {@code CLUSTER SLAVES}.
 *
 * @author Mark Paluch
 * @since 3.0
 */
public class ClusterPartitionParser {

    public static final String CONNECTED = "connected";

    private static final String TOKEN_SLOT_IN_TRANSITION = "[";
    private static final char TOKEN_NODE_SEPARATOR = '\n';
    private static final Pattern TOKEN_PATTERN = Pattern.compile(Character.toString(TOKEN_NODE_SEPARATOR));
    private static final Pattern SPACE_PATTERN = Pattern.compile(" ");
    private static final Pattern DASH_PATTERN = Pattern.compile("\\-");
    private static final Map<String, RedisClusterNode.NodeFlag> FLAG_MAPPING;

    static {
        Map<String, RedisClusterNode.NodeFlag> map = new HashMap<>();

        map.put("noflags", RedisClusterNode.NodeFlag.NOFLAGS);
        map.put("myself", RedisClusterNode.NodeFlag.MYSELF);
        map.put("master", RedisClusterNode.NodeFlag.MASTER);
        map.put("slave", RedisClusterNode.NodeFlag.SLAVE);
        map.put("fail?", RedisClusterNode.NodeFlag.EVENTUAL_FAIL);
        map.put("fail", RedisClusterNode.NodeFlag.FAIL);
        map.put("handshake", RedisClusterNode.NodeFlag.HANDSHAKE);
        map.put("noaddr", RedisClusterNode.NodeFlag.NOADDR);
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
     * @param nodes output of CLUSTER NODES
     * @return the partitions object.
     */
    public static Partitions parse(String nodes) {
        Partitions result = new Partitions();

        try {
            List<RedisClusterNode> mappedNodes = TOKEN_PATTERN.splitAsStream(nodes).filter(s -> !s.isEmpty())
                    .map(ClusterPartitionParser::parseNode)
                    .collect(Collectors.toList());
            result.addAll(mappedNodes);
        } catch (Exception e) {
            throw new RedisException("Cannot parse " + nodes, e);
        }

        return result;
    }

    private static RedisClusterNode parseNode(String nodeInformation) {

        Iterator<String> iterator = SPACE_PATTERN.splitAsStream(nodeInformation).iterator();

        String nodeId = iterator.next();
        boolean connected = false;
        RedisURI uri = null;

        String hostAndPortPart = iterator.next();
        if(hostAndPortPart.contains("@")) {
            hostAndPortPart = hostAndPortPart.substring(0, hostAndPortPart.indexOf('@'));
        }

        HostAndPort hostAndPort = HostAndPort.parseCompat(hostAndPortPart);

        if (LettuceStrings.isNotEmpty(hostAndPort.getHostText())) {
            uri = RedisURI.Builder.redis(hostAndPort.getHostText(), hostAndPort.getPort()).build();
        }

        String flags = iterator.next();
        List<String> flagStrings = LettuceLists.newList(flags.split("\\,"));

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

        List<String> slotStrings = LettuceLists.newList(iterator); // slot, from-to [slot->-nodeID] [slot-<-nodeID]
        List<Integer> slots = readSlots(slotStrings);

        RedisClusterNode partition = new RedisClusterNode(uri, nodeId, connected, slaveOf, pingSentTs, pongReceivedTs,
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
        return Collections.unmodifiableSet(flags);
    }

    private static List<Integer> readSlots(List<String> slotStrings) {

        List<Integer> slots = new ArrayList<>();
        for (String slotString : slotStrings) {

            if (slotString.startsWith(TOKEN_SLOT_IN_TRANSITION)) {
                // not interesting
                continue;

            }

            if (slotString.contains("-")) {
                // slot range
                Iterator<String> it = DASH_PATTERN.splitAsStream(slotString).iterator();
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
