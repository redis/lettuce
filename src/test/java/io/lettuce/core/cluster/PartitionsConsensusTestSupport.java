package io.lettuce.core.cluster;

import java.util.*;

import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import org.junit.jupiter.api.Tag;

import static io.lettuce.TestTags.INTEGRATION_TEST;

/**
 * @author Mark Paluch
 */
@Tag(INTEGRATION_TEST)
class PartitionsConsensusTestSupport {

    static RedisClusterNode createNode(int nodeId) {
        return new RedisClusterNode(RedisURI.create("localhost", 6379 - 2020 + nodeId), "" + nodeId, true, "", 0, 0, 0,
                Collections.emptyList(), new HashSet<>());
    }

    static Partitions createPartitions(RedisClusterNode... nodes) {

        Partitions partitions = new Partitions();
        partitions.addAll(Arrays.asList(nodes));
        return partitions;
    }

    static Map<RedisURI, Partitions> createMap(Partitions... partitionses) {

        Map<RedisURI, Partitions> partitionsMap = new HashMap<>();

        int counter = 0;
        for (Partitions partitions : partitionses) {
            partitionsMap.put(createNode(counter++).getUri(), partitions);
        }

        return partitionsMap;
    }

}
