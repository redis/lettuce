/*
 * Copyright 2011-2022 the original author or authors.
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
package io.lettuce.core;

import java.util.List;
import java.util.regex.Pattern;

import io.lettuce.core.internal.LettuceStrings;
import io.lettuce.core.models.role.RedisNodeDescription;

/**
 * Defines from which Redis nodes data is read.
 *
 * @author Mark Paluch
 * @author Ryosuke Hasebe
 * @author Omer Cilingir
 * @author Yohei Ueki
 * @since 4.0
 */
public abstract class ReadFrom {

    /**
     * Setting to read from the upstream only.
     */
    public static final ReadFrom MASTER = new ReadFromImpl.ReadFromUpstream();

    /**
     * Setting to read preferred from the upstream and fall back to a replica if the master is not available.
     */
    public static final ReadFrom MASTER_PREFERRED = new ReadFromImpl.ReadFromUpstreamPreferred();

    /**
     * Setting to read from the upstream only.
     *
     * @since 6.0
     */
    public static final ReadFrom UPSTREAM = new ReadFromImpl.ReadFromUpstream();

    /**
     * Setting to read preferred from the upstream and fall back to a replica if the upstream is not available.
     *
     * @since 6.0
     */
    public static final ReadFrom UPSTREAM_PREFERRED = new ReadFromImpl.ReadFromUpstreamPreferred();

    /**
     * Setting to read preferred from replica and fall back to upstream if no replica is available.
     *
     * @since 5.2
     */
    public static final ReadFrom REPLICA_PREFERRED = new ReadFromImpl.ReadFromReplicaPreferred();

    /**
     * Setting to read preferred from replicas and fall back to upstream if no replica is available.
     *
     * @since 4.4
     * @deprecated Renamed to {@link #REPLICA_PREFERRED}.
     */
    @Deprecated
    public static final ReadFrom SLAVE_PREFERRED = REPLICA_PREFERRED;

    /**
     * Setting to read from the replica only.
     *
     * @since 5.2
     */
    public static final ReadFrom REPLICA = new ReadFromImpl.ReadFromReplica();

    /**
     * Setting to read from the replica only.
     *
     * @deprecated renamed to {@link #REPLICA}.
     */
    @Deprecated
    public static final ReadFrom SLAVE = REPLICA;

    /**
     * Setting to read from the node with the lowest latency during topology discovery. Note that latency measurements are
     * momentary snapshots that can change in rapid succession. Requires dynamic refresh sources to obtain topologies and
     * latencies from all nodes in the cluster.
     *
     * @since 6.1.7
     */
    public static final ReadFrom LOWEST_LATENCY = new ReadFromImpl.ReadFromLowestCommandLatency();

    /**
     * Setting to read from the node with the lowest latency during topology discovery. Note that latency measurements are
     * momentary snapshots that can change in rapid succession. Requires dynamic refresh sources to obtain topologies and
     * latencies from all nodes in the cluster.
     *
     * @deprecated since 6.1.7 as we're renaming this setting to {@link #LOWEST_LATENCY} for more clarity what this setting
     *             actually represents.
     */
    @Deprecated
    public static final ReadFrom NEAREST = LOWEST_LATENCY;

    /**
     * Setting to read from any node.
     *
     * @since 5.2
     */
    public static final ReadFrom ANY = new ReadFromImpl.ReadFromAnyNode();

    /**
     * Setting to read from any replica node.
     *
     * @since 6.0.1
     */
    public static final ReadFrom ANY_REPLICA = new ReadFromImpl.ReadFromAnyReplica();

    /**
     * Setting to read from any node in the subnets.
     *
     * @param cidrNotations CIDR-block notation strings, e.g., "192.168.0.0/16", "2001:db8:abcd:0000::/52". Must not be
     *        {@code null}.
     * @return an instance of {@link ReadFromImpl.ReadFromSubnet}.
     * @since 6.1
     */
    public static ReadFrom subnet(String... cidrNotations) {
        return new ReadFromImpl.ReadFromSubnet(cidrNotations);
    }

    /**
     * Read from any node that has {@link RedisURI} matching with the given pattern.
     *
     * @param pattern regex pattern, e.g., {@code Pattern.compile(".*region-1.*")}. Must not be {@code null}.
     * @return an instance of {@link ReadFromImpl.ReadFromRegex}.
     * @since 6.1
     */
    public static ReadFrom regex(Pattern pattern) {
        return regex(pattern, false);
    }

    /**
     * Read from any node that has {@link RedisURI} matching with the given pattern.
     *
     * @param pattern regex pattern, e.g., {@code Pattern.compile(".*region-1.*")}. Must not be {@code null}.
     * @param orderSensitive {@code true} to attempt reads in the order of hosts returned by {@link ReadFrom#select(Nodes)};
     *        {@code false} to apply randomization.
     * @return an instance of {@link ReadFromImpl.ReadFromRegex}.
     * @since 6.1
     */
    public static ReadFrom regex(Pattern pattern, boolean orderSensitive) {
        return new ReadFromImpl.ReadFromRegex(pattern, orderSensitive);
    }

    /**
     * Chooses the nodes from the matching Redis nodes that match this read selector.
     *
     * @param nodes set of nodes that are suitable for reading
     * @return List of {@link RedisNodeDescription}s that are selected for reading
     */
    public abstract List<RedisNodeDescription> select(Nodes nodes);

    /**
     * Returns whether this {@link ReadFrom} requires ordering of the resulting {@link RedisNodeDescription nodes}.
     *
     * @return {@code true} if code using {@link ReadFrom} should retain ordering or {@code false} to allow reordering of
     *         {@link RedisNodeDescription nodes}.
     * @since 5.2
     */
    protected boolean isOrderSensitive() {
        return false;
    }

    /**
     * Retrieve the {@link ReadFrom} preset by name.
     *
     * @param name the name of the read from setting
     * @return the {@link ReadFrom} preset
     * @throws IllegalArgumentException if {@code name} is empty, {@code null} or the {@link ReadFrom} preset is unknown.
     */
    public static ReadFrom valueOf(String name) {

        if (LettuceStrings.isEmpty(name)) {
            throw new IllegalArgumentException("Name must not be empty");
        }

        if (name.equalsIgnoreCase("master")) {
            return UPSTREAM;
        }

        if (name.equalsIgnoreCase("masterPreferred")) {
            return UPSTREAM_PREFERRED;
        }

        if (name.equalsIgnoreCase("upstream")) {
            return UPSTREAM;
        }

        if (name.equalsIgnoreCase("upstreamPreferred")) {
            return UPSTREAM_PREFERRED;
        }

        if (name.equalsIgnoreCase("slave") || name.equalsIgnoreCase("replica")) {
            return REPLICA;
        }

        if (name.equalsIgnoreCase("slavePreferred") || name.equalsIgnoreCase("replicaPreferred")) {
            return REPLICA_PREFERRED;
        }

        if (name.equalsIgnoreCase("nearest") || name.equalsIgnoreCase("lowestLatency")) {
            return LOWEST_LATENCY;
        }

        if (name.equalsIgnoreCase("any")) {
            return ANY;
        }

        if (name.equalsIgnoreCase("anyReplica")) {
            return ANY_REPLICA;
        }

        if (name.equalsIgnoreCase("subnet")) {
            throw new IllegalArgumentException("subnet must be created via ReadFrom#subnet");
        }

        if (name.equalsIgnoreCase("regex")) {
            throw new IllegalArgumentException("regex must be created via ReadFrom#regex");
        }

        throw new IllegalArgumentException("ReadFrom " + name + " not supported");
    }

    /**
     * Descriptor of nodes that are available for the current read operation.
     */
    public interface Nodes extends Iterable<RedisNodeDescription> {

        /**
         * Returns the list of nodes that are applicable for the read operation. The list is ordered by latency.
         *
         * @return the collection of nodes that are applicable for reading.
         *
         */
        List<RedisNodeDescription> getNodes();

    }

}
