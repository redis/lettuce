/*
 * Copyright 2011-2020 the original author or authors.
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
package io.lettuce.core.models.role;

import java.util.*;

import io.lettuce.core.internal.HostAndPort;
import io.lettuce.core.internal.LettuceAssert;

/**
 * Parser for Redis <a href="http://redis.io/commands/role">ROLE</a> command output.
 *
 * @author Mark Paluch
 * @since 3.0
 */
@SuppressWarnings("serial")
public class RoleParser {

    protected static final Map<String, RedisInstance.Role> ROLE_MAPPING;

    protected static final Map<String, RedisSlaveInstance.State> REPLICA_STATE_MAPPING;

    static {
        Map<String, RedisInstance.Role> roleMap = new HashMap<>();
        roleMap.put("master", RedisInstance.Role.UPSTREAM);
        roleMap.put("slave", RedisInstance.Role.REPLICA);
        roleMap.put("sentinel", RedisInstance.Role.SENTINEL);

        ROLE_MAPPING = Collections.unmodifiableMap(roleMap);

        Map<String, RedisReplicaInstance.State> replicas = new HashMap<>();
        replicas.put("connect", RedisReplicaInstance.State.CONNECT);
        replicas.put("connected", RedisReplicaInstance.State.CONNECTED);
        replicas.put("connecting", RedisReplicaInstance.State.CONNECTING);
        replicas.put("sync", RedisReplicaInstance.State.SYNC);

        REPLICA_STATE_MAPPING = Collections.unmodifiableMap(replicas);
    }

    /**
     * Utility constructor.
     */
    private RoleParser() {

    }

    /**
     * Parse the output of the Redis ROLE command and convert to a RedisInstance.
     *
     * @param roleOutput output of the Redis ROLE command.
     * @return RedisInstance
     */
    public static RedisInstance parse(List<?> roleOutput) {
        LettuceAssert.isTrue(roleOutput != null && !roleOutput.isEmpty(), "Empty role output");
        LettuceAssert.isTrue(roleOutput.get(0) instanceof String && ROLE_MAPPING.containsKey(roleOutput.get(0)),
                () -> "First role element must be a string (any of " + ROLE_MAPPING.keySet() + ")");

        RedisInstance.Role role = ROLE_MAPPING.get(roleOutput.get(0));

        switch (role) {
            case MASTER:
            case UPSTREAM:
                return parseUpstream(roleOutput);

            case SLAVE:
            case REPLICA:
                return parseReplica(roleOutput);

            case SENTINEL:
                return parseSentinel(roleOutput);
        }

        return null;

    }

    private static RedisInstance parseUpstream(List<?> roleOutput) {

        long replicationOffset = getUpstreamReplicationOffset(roleOutput);
        List<ReplicationPartner> replicas = getUpstreamReplicaReplicationPartners(roleOutput);

        RedisMasterInstance redisUpstreamInstanceRole = new RedisMasterInstance(replicationOffset,
                Collections.unmodifiableList(replicas));
        return redisUpstreamInstanceRole;
    }

    private static RedisInstance parseReplica(List<?> roleOutput) {

        Iterator<?> iterator = roleOutput.iterator();
        iterator.next(); // skip first element

        String ip = getStringFromIterator(iterator, "");
        long port = getLongFromIterator(iterator, 0);

        String stateString = getStringFromIterator(iterator, null);
        long replicationOffset = getLongFromIterator(iterator, 0);

        ReplicationPartner master = new ReplicationPartner(HostAndPort.of(ip, Math.toIntExact(port)), replicationOffset);

        RedisReplicaInstance.State state = REPLICA_STATE_MAPPING.get(stateString);

        return new RedisReplicaInstance(master, state);
    }

    private static RedisInstance parseSentinel(List<?> roleOutput) {

        Iterator<?> iterator = roleOutput.iterator();
        iterator.next(); // skip first element

        List<String> monitoredMasters = getMonitoredUpstreams(iterator);

        RedisSentinelInstance result = new RedisSentinelInstance(Collections.unmodifiableList(monitoredMasters));
        return result;
    }

    private static List<String> getMonitoredUpstreams(Iterator<?> iterator) {
        List<String> monitoredUpstreams = new ArrayList<>();

        if (!iterator.hasNext()) {
            return monitoredUpstreams;
        }

        Object upstreams = iterator.next();

        if (!(upstreams instanceof Collection)) {
            return monitoredUpstreams;
        }

        for (Object upstream : (Collection) upstreams) {
            if (upstream instanceof String) {
                monitoredUpstreams.add((String) upstream);
            }
        }

        return monitoredUpstreams;
    }

    private static List<ReplicationPartner> getUpstreamReplicaReplicationPartners(List<?> roleOutput) {

        List<ReplicationPartner> replicas = new ArrayList<>();
        if (roleOutput.size() > 2 && roleOutput.get(2) instanceof Collection) {
            Collection<?> segments = (Collection<?>) roleOutput.get(2);

            for (Object output : segments) {
                if (!(output instanceof Collection<?>)) {
                    continue;
                }

                ReplicationPartner replicationPartner = getReplicationPartner((Collection<?>) output);
                replicas.add(replicationPartner);
            }
        }
        return replicas;
    }

    private static ReplicationPartner getReplicationPartner(Collection<?> segments) {

        Iterator<?> iterator = segments.iterator();

        String ip = getStringFromIterator(iterator, "");
        long port = getLongFromIterator(iterator, 0);
        long replicationOffset = getLongFromIterator(iterator, 0);

        return new ReplicationPartner(HostAndPort.of(ip, Math.toIntExact(port)), replicationOffset);
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

    private static long getUpstreamReplicationOffset(List<?> roleOutput) {
        long replicationOffset = 0;

        if (roleOutput.size() > 1 && roleOutput.get(1) instanceof Number) {
            Number number = (Number) roleOutput.get(1);
            replicationOffset = number.longValue();
        }
        return replicationOffset;
    }

}
