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
package io.lettuce.core;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.internal.LettuceLists;
import io.lettuce.core.internal.LettuceStrings;
import io.lettuce.core.models.role.RedisNodeDescription;
import io.netty.handler.ipfilter.IpFilterRuleType;
import io.netty.handler.ipfilter.IpSubnetFilterRule;
import io.netty.util.NetUtil;

/**
 * Collection of common read setting implementations.
 *
 * @author Mark Paluch
 * @author Omer Cilingir
 * @author Yohei Ueki
 * @since 4.0
 */
class ReadFromImpl {

    private static final Predicate<RedisNodeDescription> IS_UPSTREAM = node -> node.getRole().isUpstream();

    private static final Predicate<RedisNodeDescription> IS_REPLICA = node -> node.getRole().isReplica();

    /**
     * Read from upstream only.
     */
    static final class ReadFromUpstream extends ReadFrom {

        @Override
        public List<RedisNodeDescription> select(Nodes nodes) {

            for (RedisNodeDescription node : nodes) {
                if (node.getRole().isUpstream()) {
                    return LettuceLists.newList(node);
                }
            }

            return Collections.emptyList();
        }

    }

    /**
     * Read from upstream and replicas. Prefer upstream reads and fall back to replicas if the upstream is not available.
     */
    static final class ReadFromUpstreamPreferred extends OrderedPredicateReadFromAdapter {

        ReadFromUpstreamPreferred() {
            super(IS_UPSTREAM, IS_REPLICA);
        }

    }

    /**
     * Read from replica only.
     */
    static final class ReadFromReplica extends OrderedPredicateReadFromAdapter {

        ReadFromReplica() {
            super(IS_REPLICA);
        }

    }

    /**
     * Read from upstream and replicas. Prefer replica reads and fall back to upstream if the no replica is not available.
     */
    static final class ReadFromReplicaPreferred extends OrderedPredicateReadFromAdapter {

        ReadFromReplicaPreferred() {
            super(IS_REPLICA, IS_UPSTREAM);
        }

    }

    /**
     * Read from nearest node.
     */
    static final class ReadFromNearest extends ReadFrom {

        @Override
        public List<RedisNodeDescription> select(Nodes nodes) {
            return nodes.getNodes();
        }

        @Override
        protected boolean isOrderSensitive() {
            return true;
        }

    }

    /**
     * Read from any node.
     */
    static final class ReadFromAnyNode extends UnorderedPredicateReadFromAdapter {

        public ReadFromAnyNode() {
            super(x -> true);
        }

    }

    /**
     * Read from any replica node.
     *
     * @since 6.0.1
     */
    static final class ReadFromAnyReplica extends UnorderedPredicateReadFromAdapter {

        public ReadFromAnyReplica() {
            super(IS_REPLICA);
        }

    }

    /**
     * Read from any node in the subnets. This class does not provide DNS resolution and supports only IP address style
     * {@link RedisURI} i.e. unavailable when using {@link io.lettuce.core.masterreplica.MasterReplica} with
     * static setup (provided hosts) and Redis Sentinel with {@literal announce-hostname yes}.
     *
     * @since x.x.x
     */
    static final class ReadFromSubnet extends ReadFrom {

        private final List<IpSubnetFilterRule> rules = new ArrayList<>();

        /**
         * @param cidrNotations CIDR-block notation strings, e.g., "192.168.0.0/16".
         */
        ReadFromSubnet(String... cidrNotations) {
            LettuceAssert.notEmpty(cidrNotations, "cidrNotations must not be empty");

            for (String cidrNotation : cidrNotations) {
                // parts[0]: ipAddress (e.g., "192.168.0.0")
                // parts[1]: cidrPrefix (e.g., "16")
                String[] parts = cidrNotation.split("/");
                LettuceAssert.isTrue(parts.length == 2, "cidrNotation must have exact one '/'");

                rules.add(new IpSubnetFilterRule(parts[0], Integer.parseInt(parts[1]), IpFilterRuleType.ACCEPT));
            }
        }

        @Override
        public List<RedisNodeDescription> select(Nodes nodes) {
            List<RedisNodeDescription> result = new ArrayList<>(nodes.getNodes().size());
            for (RedisNodeDescription node : nodes) {
                if (isInSubnet(node.getUri())) {
                    result.add(node);
                }
            }

            return result;
        }

        private boolean isInSubnet(RedisURI redisURI) {
            String host = redisURI.getHost();
            if (LettuceStrings.isEmpty(host)) {
                return false;
            }

            LettuceAssert.isTrue(NetUtil.isValidIpV4Address(host) || NetUtil.isValidIpV6Address(host),
                    "ReadFromSubnet supports only IP address-style redisURI. host=" + host);

            InetSocketAddress address;
            try {
                // This won't make DNS lookup because host is already validated as an IP address.
                address = new InetSocketAddress(InetAddress.getByName(host), redisURI.getPort());
            } catch (UnknownHostException e) {
                throw new IllegalStateException("Should not reach here. host=" + host, e);
            }

            for (IpSubnetFilterRule rule : rules) {
                if (rule.matches(address)) {
                    return true;
                }
            }
            return false;
        }

    }

    /**
     * {@link Predicate}-based {@link ReadFrom} implementation.
     *
     * @since 5.2
     */
    static class OrderedPredicateReadFromAdapter extends ReadFrom {

        private final Predicate<RedisNodeDescription> predicates[];

        @SafeVarargs
        OrderedPredicateReadFromAdapter(Predicate<RedisNodeDescription>... predicates) {
            this.predicates = predicates;
        }

        @Override
        public List<RedisNodeDescription> select(Nodes nodes) {

            List<RedisNodeDescription> result = new ArrayList<>(nodes.getNodes().size());

            for (Predicate<RedisNodeDescription> predicate : predicates) {

                for (RedisNodeDescription node : nodes) {
                    if (predicate.test(node)) {
                        result.add(node);
                    }
                }
            }

            return result;
        }

        @Override
        protected boolean isOrderSensitive() {
            return true;
        }

    }

    /**
     * Unordered {@link Predicate}-based {@link ReadFrom} implementation.
     *
     * @since 5.2
     */
    static class UnorderedPredicateReadFromAdapter extends OrderedPredicateReadFromAdapter {

        @SafeVarargs
        UnorderedPredicateReadFromAdapter(Predicate<RedisNodeDescription>... predicates) {
            super(predicates);
        }

        @Override
        protected boolean isOrderSensitive() {
            return false;
        }

    }

}
