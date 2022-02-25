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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.internal.LettuceLists;
import io.lettuce.core.internal.LettuceStrings;
import io.lettuce.core.models.role.RedisNodeDescription;
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
     * Read from the node with the lowest latency during topology discovery. Note that latency measurements are momentary
     * snapshots that can change in rapid succession. Requires dynamic refresh sources to obtain topologies and latencies from
     * all nodes in the cluster.
     */
    static final class ReadFromLowestCommandLatency extends ReadFrom {

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
     * {@link RedisURI} i.e. unavailable when using {@link io.lettuce.core.masterreplica.MasterReplica} with static setup
     * (provided hosts) and Redis Sentinel with {@literal announce-hostname yes}. Both IPv4 and IPv6 style subnets are supported
     * but they never match with IP addresses of different version.
     *
     * @since 6.1
     */
    static final class ReadFromSubnet extends ReadFrom {

        private static final int CACHE_SIZE = 48;

        private final List<SubnetRule> rules = new ArrayList<>();

        private final ConcurrentLruCache<String, Integer> ipv4AddressCache = new ConcurrentLruCache<>(CACHE_SIZE,
                Ipv4SubnetRule::toInt);

        private final ConcurrentLruCache<String, BigInteger> ipv6AddressCache = new ConcurrentLruCache<>(CACHE_SIZE,
                Ipv6SubnetRule::toBigInteger);

        /**
         * @param cidrNotations CIDR-block notation strings, e.g., "192.168.0.0/16" or "2001:db8:abcd:0000::/52".
         */
        ReadFromSubnet(String... cidrNotations) {

            LettuceAssert.notEmpty(cidrNotations, "CIDR notations must not be empty");

            for (String cidrNotation : cidrNotations) {
                rules.add(createSubnetRule(cidrNotation));
            }
        }

        @Override
        public List<RedisNodeDescription> select(Nodes nodes) {

            List<RedisNodeDescription> result = new ArrayList<>(nodes.getNodes().size());

            for (RedisNodeDescription node : nodes) {

                if (test(node)) {
                    result.add(node);

                }
            }

            return result;
        }

        private boolean test(RedisNodeDescription node) {

            for (SubnetRule rule : rules) {
                if (rule.isInSubnet(node.getUri().getHost())) {
                    return true;
                }
            }

            return false;
        }

        interface SubnetRule {

            boolean isInSubnet(String ipAddress);

        }

        SubnetRule createSubnetRule(String cidrNotation) {

            String[] parts = cidrNotation.split("/");

            LettuceAssert.isTrue(parts.length == 2, "CIDR notation must have exact one '/'");

            String ipAddress = parts[0];
            int cidrPrefix = Integer.parseInt(parts[1]);

            if (NetUtil.isValidIpV4Address(ipAddress)) {
                return new Ipv4SubnetRule(ipAddress, cidrPrefix, ipv4AddressCache);
            } else if (NetUtil.isValidIpV6Address(ipAddress)) {
                return new Ipv6SubnetRule(ipAddress, cidrPrefix, ipv6AddressCache);
            } else {
                throw new IllegalArgumentException("Invalid CIDR notation " + cidrNotation);
            }
        }

        static class Ipv4SubnetRule implements SubnetRule {

            private static final int IPV4_BYTE_COUNT = 4;

            private final int networkAddress;

            private final int subnetMask;

            private final ConcurrentLruCache<String, Integer> ipv4AddressCache;

            Ipv4SubnetRule(String ipAddress, int cidrPrefix, ConcurrentLruCache<String, Integer> ipv4AddressCache) {

                LettuceAssert.isTrue(NetUtil.isValidIpV4Address(ipAddress),
                        () -> String.format("Invalid IPv4 IP address %s", ipAddress));
                LettuceAssert.isTrue(0 <= cidrPrefix && cidrPrefix <= 32,
                        () -> String.format("Invalid CIDR prefix %d", cidrPrefix));

                this.subnetMask = toSubnetMask(cidrPrefix);
                this.networkAddress = toNetworkAddress(ipAddress, this.subnetMask);
                this.ipv4AddressCache = ipv4AddressCache;
            }

            /**
             * return {@code true} if the {@code ipAddress} is in this subnet. If {@code ipAddress} is not valid IPv4 style
             * (e.g., IPv6 style) {@code false} is always returned.
             */
            @Override
            public boolean isInSubnet(String ipAddress) {

                if (LettuceStrings.isEmpty(ipAddress) || !NetUtil.isValidIpV4Address(ipAddress)) {
                    return false;
                }

                Integer address = ipv4AddressCache.get(ipAddress);

                return (address & subnetMask) == networkAddress;
            }

            private int toSubnetMask(int cidrPrefix) {
                return (int) (-1L << (32 - cidrPrefix));
            }

            private int toNetworkAddress(String ipAddress, int subnetMask) {
                return toInt(ipAddress) & subnetMask;
            }

            static int toInt(String ipAddress) {

                byte[] octets = NetUtil.createByteArrayFromIpAddressString(ipAddress);

                LettuceAssert.isTrue(octets != null && octets.length == IPV4_BYTE_COUNT,
                        () -> String.format("Invalid IP address %s", ipAddress));

                return ((octets[0] & 0xff) << 24) | ((octets[1] & 0xff) << 16) | ((octets[2] & 0xff) << 8) | (octets[3] & 0xff);
            }

        }

        static class Ipv6SubnetRule implements SubnetRule {

            private static final int IPV6_BYTE_COUNT = 16;

            private final BigInteger networkAddress;

            private final BigInteger subnetMask;

            private final ConcurrentLruCache<String, BigInteger> ipv6AddressCache;

            public Ipv6SubnetRule(String ipAddress, int cidrPrefix, ConcurrentLruCache<String, BigInteger> ipv6AddressCache) {

                LettuceAssert.isTrue(NetUtil.isValidIpV6Address(ipAddress),
                        () -> String.format("Invalid IPv6 IP address %s", ipAddress));
                LettuceAssert.isTrue(0 <= cidrPrefix && cidrPrefix <= 128,
                        () -> String.format("Invalid CIDR prefix %d", cidrPrefix));

                this.subnetMask = toSubnetMask(cidrPrefix);
                this.networkAddress = toNetworkAddress(ipAddress, this.subnetMask);
                this.ipv6AddressCache = ipv6AddressCache;
            }

            /**
             * return {@code true} if the {@code ipAddress} is in this subnet. If {@code ipAddress} is not valid IPv6 style
             * (e.g., IPv4 style) {@code false} is always returned.
             */
            @Override
            public boolean isInSubnet(String ipAddress) {

                if (LettuceStrings.isEmpty(ipAddress) || !NetUtil.isValidIpV6Address(ipAddress)) {
                    return false;
                }

                BigInteger address = ipv6AddressCache.get(ipAddress);
                return address.and(subnetMask).equals(networkAddress);
            }

            private static BigInteger toSubnetMask(int cidrPrefix) {
                return BigInteger.valueOf(-1).shiftLeft(128 - cidrPrefix);
            }

            private static BigInteger toNetworkAddress(String ipAddress, BigInteger subnetMask) {
                return toBigInteger(ipAddress).and(subnetMask);
            }

            static BigInteger toBigInteger(String ipAddress) {

                byte[] octets = NetUtil.createByteArrayFromIpAddressString(ipAddress);

                LettuceAssert.isTrue(octets != null && octets.length == IPV6_BYTE_COUNT,
                        () -> String.format("Invalid IP address %s", ipAddress));

                return new BigInteger(octets);
            }

        }

    }

    /**
     * Read from any node that has {@link RedisURI} matching with the given pattern.
     *
     * @since 6.1
     */
    static class ReadFromRegex extends ReadFrom {

        private final ReadFrom delegate;

        private final boolean orderSensitive;

        public ReadFromRegex(Pattern pattern, boolean orderSensitive) {

            LettuceAssert.notNull(pattern, "Pattern must not be null");

            this.orderSensitive = orderSensitive;

            delegate = new UnorderedPredicateReadFromAdapter(redisNodeDescription -> {
                String host = redisNodeDescription.getUri().getHost();
                if (LettuceStrings.isEmpty(host)) {
                    return false;
                }
                return pattern.matcher(host).matches();
            });
        }

        @Override
        public List<RedisNodeDescription> select(Nodes nodes) {
            return delegate.select(nodes);
        }

        @Override
        protected boolean isOrderSensitive() {
            return orderSensitive;
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
