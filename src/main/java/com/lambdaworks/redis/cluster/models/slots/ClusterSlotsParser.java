package com.lambdaworks.redis.cluster.models.slots;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.net.HostAndPort;
import com.google.common.primitives.Ints;

/**
 * Parser for redis <a href="http://redis.io/commands/cluster-slots">CLUSTER SLOTS</a> command output.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.0
 */
public class ClusterSlotsParser {

    /**
     * Utility constructor.
     */
    private ClusterSlotsParser() {

    }

    /**
     * Parse the output of the redis CLUSTER SLOTS command and convert it to a list of
     * {@link com.lambdaworks.redis.cluster.models.slots.ClusterSlotRange}
     * 
     * @param clusterSlotsOutput output of CLUSTER SLOTS command
     * @return List&gt;ClusterSlotRange&gt;
     */
    public static List<ClusterSlotRange> parse(List<?> clusterSlotsOutput) {
        List<ClusterSlotRange> result = Lists.newArrayList();

        for (Object o : clusterSlotsOutput) {

            if (!(o instanceof List)) {
                continue;
            }

            List<?> range = (List<?>) o;
            if (range.size() < 2) {
                continue;
            }

            ClusterSlotRange clusterSlotRange = parseRange(range);
            result.add(clusterSlotRange);
        }

        Collections.sort(result, new Comparator<ClusterSlotRange>() {
            @Override
            public int compare(ClusterSlotRange o1, ClusterSlotRange o2) {
                return o1.getFrom() - o2.getFrom();
            }
        });

        return Collections.unmodifiableList(result);
    }

    private static ClusterSlotRange parseRange(List<?> range) {
        Iterator<?> iterator = range.iterator();

        int from = Ints.checkedCast(getLongFromIterator(iterator, 0));
        int to = Ints.checkedCast(getLongFromIterator(iterator, 0));
        HostAndPort master = null;

        List<HostAndPort> slaves = Lists.newArrayList();
        if (iterator.hasNext()) {
            master = getHostAndPort(iterator);
        }

        while (iterator.hasNext()) {
            HostAndPort slave = getHostAndPort(iterator);
            if (slave != null) {
                slaves.add(slave);
            }
        }

        return new ClusterSlotRange(from, to, master, Collections.unmodifiableList(slaves));
    }

    private static HostAndPort getHostAndPort(Iterator<?> iterator) {
        Object element = iterator.next();
        if (element instanceof List) {
            List<?> hostAndPortList = (List<?>) element;
            if (hostAndPortList.size() != 2) {
                return null;
            }

            Iterator<?> hostAndPortIterator = hostAndPortList.iterator();
            String host = (String) hostAndPortIterator.next();
            int port = Ints.checkedCast(getLongFromIterator(hostAndPortIterator, 0));
            HostAndPort hostAndPort = HostAndPort.fromParts(host, port);

            return hostAndPort;

        }
        return null;
    }

    private static long getLongFromIterator(Iterator<?> iterator, long defaultValue) {
        if (iterator.hasNext()) {
            Object object = iterator.next();
            if (object instanceof String) {
                return Long.parseLong((String) object);
            }

            if (object instanceof Number) {
                return ((Number) object).longValue();
            }
        }
        return defaultValue;
    }
}
