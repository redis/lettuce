package com.lambdaworks.redis.cluster;

import java.util.List;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 26.05.14 17:20
 */
class ClusterPartitionParser {
    private ClusterPartitionParser() {

    }

    /**
     * Parse partition lines into Partitions object.
     * 
     * @param partitions
     * @return
     */
    public static Partitions parse(List<String> partitions) {
        Partitions result = new Partitions();

        return result;
    }
}
