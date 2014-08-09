package com.lambdaworks.redis.models.role;

import static com.google.common.base.Preconditions.*;

import java.util.*;

import com.google.common.collect.Lists;
import com.google.common.net.HostAndPort;
import com.google.common.primitives.Ints;

/**
 * Parser for redis ROLE command output.
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 03.08.14 10:52
 */
@SuppressWarnings("serial")
public class RoleParser {
    private static final Map<String, RedisInstance.Role> ROLE_MAPPING = new HashMap<String, RedisInstance.Role>() {
        {
            put("master", RedisInstance.Role.MASTER);
            put("slave", RedisInstance.Role.SLAVE);
            put("sentinel", RedisInstance.Role.SENTINEL);
        }
    };

    private static final Map<String, RedisSlaveInstance.State> SLAVE_STATE_MAPPING = new HashMap<String, RedisSlaveInstance.State>() {
        {
            put("connect", RedisSlaveInstance.State.CONNECT);
            put("connected", RedisSlaveInstance.State.CONNECTED);
            put("connecting", RedisSlaveInstance.State.CONNECTING);
            put("sync", RedisSlaveInstance.State.SYNC);
        }
    };

    /**
     * Utility constructor.
     */
    private RoleParser() {

    }

	/**
	 * Parse the output of the redis ROLE command and convert to a RedisInstance.
	 * @param roleOutput
	 * @return RedisInstance
	 */
    public static RedisInstance parse(List<?> roleOutput) {
        checkArgument(roleOutput != null && !roleOutput.isEmpty(), "Empty role output");
        checkArgument(roleOutput.get(0) instanceof String && ROLE_MAPPING.containsKey(roleOutput.get(0)),
                "First role element must be a string (any of " + ROLE_MAPPING.keySet() + ")");

        RedisInstance.Role role = ROLE_MAPPING.get(roleOutput.get(0));

        switch (role) {
            case MASTER: {
                return parseMaster(roleOutput);
            }

            case SLAVE: {
                return parseSlave(roleOutput);
            }

            case SENTINEL: {
                return parseSentinel(roleOutput);
            }
        }

        return null;

    }

    private static RedisInstance parseMaster(List<?> roleOutput) {

        long replicationOffset = getMasterReplicationOffset(roleOutput);
        List<ReplicationPartner> slaves = getMasterSlaveReplicationPartners(roleOutput);

        RedisMasterInstance redisMasterInstanceRole = new RedisMasterInstance(replicationOffset,
                Collections.unmodifiableList(slaves));
        return redisMasterInstanceRole;
    }

    private static RedisInstance parseSlave(List<?> roleOutput) {

        Iterator<?> iterator = roleOutput.iterator();
        iterator.next(); // skip first element

        String ip = getStringFromIterator(iterator, "");
        long port = getLongFromIterator(iterator, 0);

        String stateString = getStringFromIterator(iterator, null);
        long replicationOffset = getLongFromIterator(iterator, 0);

        ReplicationPartner master = new ReplicationPartner(HostAndPort.fromParts(ip, Ints.checkedCast(port)), replicationOffset);

        RedisSlaveInstance.State state = SLAVE_STATE_MAPPING.get(stateString);

        RedisSlaveInstance redisSlaveInstanceRole = new RedisSlaveInstance(master, state);
        return redisSlaveInstanceRole;
    }

    private static RedisInstance parseSentinel(List<?> roleOutput) {

        Iterator<?> iterator = roleOutput.iterator();
        iterator.next(); // skip first element

        List<String> monitoredMasters = getMonitoredMasters(iterator);

        RedisSentinelInstance result = new RedisSentinelInstance(Collections.unmodifiableList(monitoredMasters));
        return result;
    }

    private static List<String> getMonitoredMasters(Iterator<?> iterator) {
        List<String> monitoredMasters = Lists.newArrayList();

        if (!iterator.hasNext()) {
            return monitoredMasters;
        }

        Object masters = iterator.next();

        if (!(masters instanceof Collection)) {
            return monitoredMasters;
        }

        for (Object monitoredMaster : (Collection) masters) {
            if (monitoredMaster instanceof String) {
                monitoredMasters.add((String) monitoredMaster);
            }
        }

        return monitoredMasters;
    }

    private static List<ReplicationPartner> getMasterSlaveReplicationPartners(List<?> roleOutput) {
        List<ReplicationPartner> slaves = Lists.newArrayList();
        if (roleOutput.size() > 2 && roleOutput.get(2) instanceof Collection) {
            Collection<?> slavesOutput = (Collection<?>) roleOutput.get(2);

            for (Object slaveOutput : slavesOutput) {
                if (!(slaveOutput instanceof Collection<?>)) {
                    continue;
                }

                ReplicationPartner replicationPartner = getMasterSlaveReplicationPartner((Collection<?>) slaveOutput);
                slaves.add(replicationPartner);
            }
        }
        return slaves;
    }

    private static ReplicationPartner getMasterSlaveReplicationPartner(Collection<?> slaveOutput) {
        Iterator<?> iterator = slaveOutput.iterator();

        String ip = getStringFromIterator(iterator, "");
        long port = getLongFromIterator(iterator, 0);
        long replicationOffset = getLongFromIterator(iterator, 0);

        return new ReplicationPartner(HostAndPort.fromParts(ip, Ints.checkedCast(port)), replicationOffset);
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

    private static String getStringFromIterator(Iterator<?> iterator, String defaultValue) {
        if (iterator.hasNext()) {
            Object object = iterator.next();
            if (object instanceof String) {
                return (String) object;
            }
        }
        return defaultValue;
    }

    private static long getMasterReplicationOffset(List<?> roleOutput) {
        long replicationOffset = 0;

        if (roleOutput.size() > 1 && roleOutput.get(1) instanceof Number) {
            Number number = (Number) roleOutput.get(1);
            replicationOffset = number.longValue();
        }
        return replicationOffset;
    }
}
