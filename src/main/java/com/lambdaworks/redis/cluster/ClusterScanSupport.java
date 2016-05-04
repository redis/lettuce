package com.lambdaworks.redis.cluster;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import rx.Observable;
import rx.functions.Func1;

import com.lambdaworks.redis.*;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;
import com.lambdaworks.redis.models.role.RedisNodeDescription;

/**
 * Methods to support a Cluster-wide SCAN operation over multiple hosts.
 * 
 * @author Mark Paluch
 */
class ClusterScanSupport {

    /**
     * Map a {@link RedisFuture} of {@link KeyScanCursor} to a {@link RedisFuture} of {@link ClusterKeyScanCursor}.
     */
    final static ScanCursorMapper<RedisFuture<KeyScanCursor<?>>> futureKeyScanCursorMapper = new ScanCursorMapper<RedisFuture<KeyScanCursor<?>>>() {
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

    /**
     * Map a {@link RedisFuture} of {@link StreamScanCursor} to a {@link RedisFuture} of {@link ClusterStreamScanCursor}.
     */
    final static ScanCursorMapper<RedisFuture<StreamScanCursor>> futureStreamScanCursorMapper = new ScanCursorMapper<RedisFuture<StreamScanCursor>>() {
        @Override
        public RedisFuture<StreamScanCursor> map(List<String> nodeIds, String currentNodeId,
                RedisFuture<StreamScanCursor> cursor) {
            return new PipelinedRedisFuture<>(cursor, new Function<StreamScanCursor, StreamScanCursor>() {
                @Override
                public StreamScanCursor apply(StreamScanCursor result) {
                    return new ClusterStreamScanCursor(nodeIds, currentNodeId, result);
                }
            });
        }
    };

    /**
     * Map a {@link Observable} of {@link KeyScanCursor} to a {@link Observable} of {@link ClusterKeyScanCursor}.
     */
    final static ScanCursorMapper<Observable<KeyScanCursor<?>>> reactiveKeyScanCursorMapper = new ScanCursorMapper<Observable<KeyScanCursor<?>>>() {
        @Override
        public Observable<KeyScanCursor<?>> map(List<String> nodeIds, String currentNodeId, Observable<KeyScanCursor<?>> cursor) {
            return cursor.map(new Func1<KeyScanCursor<?>, KeyScanCursor<?>>() {
                @Override
                public KeyScanCursor<?> call(KeyScanCursor<?> keyScanCursor) {
                    return new ClusterKeyScanCursor<>(nodeIds, currentNodeId, keyScanCursor);
                }
            });
        }
    };

    /**
     * Map a {@link Observable} of {@link StreamScanCursor} to a {@link Observable} of {@link ClusterStreamScanCursor}.
     */
    final static ScanCursorMapper<Observable<StreamScanCursor>> reactiveStreamScanCursorMapper = new ScanCursorMapper<Observable<StreamScanCursor>>() {
        @Override
        public Observable<StreamScanCursor> map(List<String> nodeIds, String currentNodeId, Observable<StreamScanCursor> cursor) {
            return cursor.map(new Func1<StreamScanCursor, StreamScanCursor>() {
                @Override
                public StreamScanCursor call(StreamScanCursor streamScanCursor) {
                    return new ClusterStreamScanCursor(nodeIds, currentNodeId, streamScanCursor);
                }
            });
        }
    };

    /**
     * Retrieve the cursor to continue the scan.
     * 
     * @param scanCursor can be {@literal null}.
     * @return
     */
    static ScanCursor getContinuationCursor(ScanCursor scanCursor) {
        if (ScanCursor.INITIAL.equals(scanCursor)) {
            return scanCursor;
        }

        assertClusterScanCursor(scanCursor);

        ClusterScanCursor clusterScanCursor = (ClusterScanCursor) scanCursor;
        if (clusterScanCursor.isScanOnCurrentNodeFinished()) {
            return ScanCursor.INITIAL;
        }

        return scanCursor;
    }

    static <K, V> List<String> getNodeIds(StatefulRedisClusterConnection<K, V> connection, ScanCursor cursor) {
        if (ScanCursor.INITIAL.equals(cursor)) {
            List<String> nodeIds = getNodeIds(connection);

            assertHasNodes(nodeIds);

            return nodeIds;
        }

        assertClusterScanCursor(cursor);

        ClusterScanCursor clusterScanCursor = (ClusterScanCursor) cursor;
        return clusterScanCursor.getNodeIds();
    }

    static String getCurrentNodeId(ScanCursor cursor, List<String> nodeIds) {

        if (ScanCursor.INITIAL.equals(cursor)) {
            assertHasNodes(nodeIds);
            return nodeIds.get(0);
        }

        assertClusterScanCursor(cursor);
        return getNodeIdForNextScanIteration(nodeIds, (ClusterScanCursor) cursor);
    }

    /**
     * Retrieve a list of node Ids to use for the SCAN operation.
     * 
     * @param connection
     * @return
     */
    private static List<String> getNodeIds(StatefulRedisClusterConnection<?, ?> connection) {
        List<String> nodeIds = new ArrayList<>();

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

    private static String getNodeIdForNextScanIteration(List<String> nodeIds, ClusterScanCursor clusterKeyScanCursor) {
        if (clusterKeyScanCursor.isScanOnCurrentNodeFinished()) {
            if (clusterKeyScanCursor.isFinished()) {
                throw new IllegalStateException("Cluster scan is finished");
            }

            int nodeIndex = nodeIds.indexOf(clusterKeyScanCursor.getCurrentNodeId());
            return nodeIds.get(nodeIndex + 1);
        }

        return clusterKeyScanCursor.getCurrentNodeId();
    }

    private static void assertClusterScanCursor(ScanCursor cursor) {
        if (!(cursor instanceof ClusterScanCursor)) {
            throw new IllegalArgumentException(
                    "A scan in Redis Cluster mode requires to reuse the resulting cursor from the previous scan invocation");
        }
    }

    private static void assertHasNodes(List<String> nodeIds) {
        if (nodeIds.isEmpty()) {
            throw new RedisException("No available nodes for a scan");
        }
    }

    static <K> ScanCursorMapper<RedisFuture<KeyScanCursor<K>>> asyncClusterKeyScanCursorMapper() {
        return (ScanCursorMapper) futureKeyScanCursorMapper;
    }

    static ScanCursorMapper<RedisFuture<StreamScanCursor>> asyncClusterStreamScanCursorMapper() {
        return futureStreamScanCursorMapper;
    }

    static <K> ScanCursorMapper<Observable<KeyScanCursor<K>>> reactiveClusterKeyScanCursorMapper() {
        return (ScanCursorMapper) reactiveKeyScanCursorMapper;
    }

    static ScanCursorMapper<Observable<StreamScanCursor>> reactiveClusterStreamScanCursorMapper() {
        return reactiveStreamScanCursorMapper;
    }

    /**
     * Mapper between the node operation cursor and the cluster scan cursor.
     *
     * @param <T>
     */
    interface ScanCursorMapper<T> {
        T map(List<String> nodeIds, String currentNodeId, T cursor);
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

    /**
     * State object for a cluster-wide SCAN using Key results.
     * 
     * @param <K>
     */
    private static class ClusterKeyScanCursor<K> extends KeyScanCursor<K> implements ClusterScanCursor {
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

    /**
     * State object for a cluster-wide SCAN using streaming.
     */
    private static class ClusterStreamScanCursor extends StreamScanCursor implements ClusterScanCursor {
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
