package com.lambdaworks.redis.cluster;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.lambdaworks.redis.*;
import com.lambdaworks.redis.api.async.RedisKeyAsyncCommands;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;
import com.lambdaworks.redis.models.role.RedisNodeDescription;

/**
 * Methods to support a Cluster-wide SCAN operation over multiple hosts.
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
class ClusterScanSupport {

    final static ScanCursorMapper<KeyScanCursor<?>> keyScanCursorMapper = new ScanCursorMapper<KeyScanCursor<?>>() {
        @Override
        public RedisFuture<KeyScanCursor<?>> map(List<String> nodeIds, String currentNodeId,
                                                 RedisFuture<KeyScanCursor<?>> cursor) {
            return new PipelinedRedisFuture<>(cursor, new Function<KeyScanCursor<?>, KeyScanCursor<?>>() {
                @Override
                public KeyScanCursor<?> apply(KeyScanCursor<?> result) {
                    return new ClusterKeyScanCursor<>(nodeIds, currentNodeId, result);
                }
            });
        }
    };

    final static ScanCursorMapper<StreamScanCursor> streamScanCursorMapper = new ScanCursorMapper<StreamScanCursor>() {
        @Override
        public RedisFuture<StreamScanCursor> map(List<String> nodeIds, String currentNodeId,
                                                 RedisFuture<StreamScanCursor> cursor) {
            return new PipelinedRedisFuture<>(
                    cursor, new Function<StreamScanCursor, StreamScanCursor>() {
                @Override
                public StreamScanCursor apply(StreamScanCursor result) {
                    return new ClusterStreamScanCursor(nodeIds, currentNodeId, result);
                }
            });
        }
    };

    static <K> ScanCursorMapper<KeyScanCursor<K>> keyScanCursorMapper() {
        return (ScanCursorMapper) keyScanCursorMapper;
    }

    static <K> ScanCursorMapper<StreamScanCursor> streamScanCursorMapper() {
        return streamScanCursorMapper;
    }

    /**
     * Perform a SCAN in the cluster.
     * @param connection
     * @param cursor
     * @param scanFunction
     * @param mapper
     * @param <T>
     * @param <K>
     * @param <V>
     * @return
     */
    static <T extends ScanCursor, K, V> RedisFuture<T> clusterScan(StatefulRedisClusterConnection<K, V> connection, T cursor,
            BiFunction<RedisKeyAsyncCommands<K, V>, ScanCursor, RedisFuture<T>> scanFunction, ScanCursorMapper<T> mapper) {

        List<String> nodeIds;
        String currentNodeId;
        ScanCursor continuationCursor = null;

        if (cursor == null) {
            nodeIds = getNodeIds(connection);

            if (nodeIds.isEmpty()) {
                throw new RedisException("No available nodes for a scan");
            }

            currentNodeId = nodeIds.get(0);
        } else {
            if (!(cursor instanceof ClusterScanCursor)) {
                throw new IllegalArgumentException(
                        "A scan in Redis Cluster mode requires to reuse the resulting cursor from the previous scan invocation");
            }

            ClusterScanCursor clusterScanCursor = (ClusterScanCursor) cursor;
            nodeIds = clusterScanCursor.getNodeIds();
            if (clusterScanCursor.isScanOnCurrentNodeFinished()) {
                continuationCursor = null;
            }else{
                continuationCursor = cursor;
            }

            currentNodeId = getNodeIdForNextScanIteration(nodeIds, clusterScanCursor);
        }

        RedisFuture<T> scanCursor = scanFunction.apply(connection.getConnection(currentNodeId).async(), continuationCursor);
        return mapper.map(nodeIds, currentNodeId, scanCursor);
    }

    /**
     * Retrieve a list of node Ids to use for the SCAN operation.
     * @param connection
     * @return
     */
    private static  List<String> getNodeIds(StatefulRedisClusterConnection<?, ?> connection) {
        List<String> nodeIds;
        nodeIds = new ArrayList<>();

        PartitionAccessor partitionAccessor = new PartitionAccessor(connection.getPartitions());
        for (RedisClusterNode redisClusterNode : partitionAccessor.getMasters()) {

            if (connection.getReadFrom() != null) {

                List<RedisNodeDescription> readCandidates = (List) partitionAccessor.getReadCandidates(redisClusterNode);

                List<RedisNodeDescription> selection = connection.getReadFrom().select(new ReadFrom.Nodes() {
                    @Override
                    public List<RedisNodeDescription> getNodes() {
                        return readCandidates;
                    }

                    @Override
                    public Iterator<RedisNodeDescription> iterator() {
                        return readCandidates.iterator();
                    }
                });

                if (!selection.isEmpty()) {
                    RedisClusterNode selectedNode = (RedisClusterNode) selection.get(0);
                    nodeIds.add(selectedNode.getNodeId());
                    continue;
                }
            }
            nodeIds.add(redisClusterNode.getNodeId());
        }
        return nodeIds;
    }

    static String getNodeIdForNextScanIteration(List<String> nodeIds, ClusterScanCursor clusterKeyScanCursor) {
        if (clusterKeyScanCursor.isScanOnCurrentNodeFinished()) {
            if (clusterKeyScanCursor.isFinished()) {
                throw new IllegalStateException("Cluster scan is finished");
            }

            int nodeIndex = nodeIds.indexOf(clusterKeyScanCursor.getCurrentNodeId());
            return nodeIds.get(nodeIndex + 1);
        }

        return clusterKeyScanCursor.getCurrentNodeId();
    }

    /**
     * Mapper between the node operation cursor and the cluster scan cursor.
     * @param <T>
     */
    interface ScanCursorMapper<T> {
        RedisFuture<T> map(List<String> nodeIds, String currentNodeId, RedisFuture<T> cursor);
    }

    /**
     * Marker for a cluster scan cursor.
     */
    interface ClusterScanCursor {
        List<String> getNodeIds();

        String getCurrentNodeId();

        boolean isScanOnCurrentNodeFinished();

        boolean isFinished();
    }

    static class ClusterKeyScanCursor<K> extends KeyScanCursor<K> implements ClusterScanCursor {
        final List<String> nodeIds;
        final String currentNodeId;
        final KeyScanCursor<K> cursor;

        public ClusterKeyScanCursor(List<String> nodeIds, String currentNodeId, KeyScanCursor<K> cursor) {
            super();
            this.nodeIds = nodeIds;
            this.currentNodeId = currentNodeId;
            this.cursor = cursor;
            setCursor(cursor.getCursor());
            getKeys().addAll(cursor.getKeys());

            if (cursor.isFinished()) {
                int nodeIndex = nodeIds.indexOf(currentNodeId);
                if (nodeIndex == -1 || nodeIndex == nodeIds.size() - 1) {
                    setFinished(true);
                }
            }
        }

        @Override
        public List<String> getNodeIds() {
            return nodeIds;
        }

        @Override
        public String getCurrentNodeId() {
            return currentNodeId;
        }

        public boolean isScanOnCurrentNodeFinished() {
            return cursor.isFinished();
        }
    }

    static class ClusterStreamScanCursor extends StreamScanCursor implements ClusterScanCursor {
        final List<String> nodeIds;
        final String currentNodeId;
        final StreamScanCursor cursor;

        public ClusterStreamScanCursor(List<String> nodeIds, String currentNodeId, StreamScanCursor cursor) {
            super();
            this.nodeIds = nodeIds;
            this.currentNodeId = currentNodeId;
            this.cursor = cursor;
            setCursor(cursor.getCursor());
            setCount(cursor.getCount());

            if (cursor.isFinished()) {
                int nodeIndex = nodeIds.indexOf(currentNodeId);
                if (nodeIndex == -1 || nodeIndex == nodeIds.size() - 1) {
                    setFinished(true);
                }
            }
        }

        @Override
        public List<String> getNodeIds() {
            return nodeIds;
        }

        @Override
        public String getCurrentNodeId() {
            return currentNodeId;
        }

        public boolean isScanOnCurrentNodeFinished() {
            return cursor.isFinished();
        }
    }
}
