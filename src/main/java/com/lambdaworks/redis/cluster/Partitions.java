package com.lambdaworks.redis.cluster;

import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Lists;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 26.05.14 17:11
 */
class Partitions implements Iterable<RedisClusterPartition> {
    private List<RedisClusterPartition> partitions = Lists.newArrayList();

    public RedisClusterPartition getPartitionByHash(int hash) {
        return null;
    }

    @Override
    public Iterator<RedisClusterPartition> iterator() {
        return Lists.newArrayList(partitions).iterator();
    }
}
