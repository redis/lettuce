/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed to Redis, Inc. under one or more contributor license agreements.
 */
package io.lettuce.core.masterreplica;

import io.lettuce.core.KeyScanCursor;
import io.lettuce.core.MapScanCursor;
import io.lettuce.core.RedisURI;
import io.lettuce.core.ScanCursor;
import io.lettuce.core.ScoredValueScanCursor;
import io.lettuce.core.StreamScanCursor;
import io.lettuce.core.ValueScanCursor;
import io.lettuce.core.models.role.RedisInstance;

/**
 * Support for node-affine {@code SCAN} on Master/Replica connections.
 * <p>
 * Scan cursors are node-local state: a continuation must return to the node that served the initial request. Under
 * {@code ReadFrom} the reads would otherwise spread across nodes, so continuations would be interpreted against the wrong
 * keyspace, yielding undefined results (<a href="https://github.com/redis/lettuce/issues/3287">#3287</a>).
 * <p>
 * This mirrors {@code ClusterScanSupport}: the node that served a scan is carried on a dedicated cursor <b>subtype</b> (rather
 * than mutating the public {@link ScanCursor}), and a {@link SourceScanCursorFactory} wraps the per-node result into that
 * subtype. Unlike the cluster variant — which aggregates across all masters — Master/Replica <em>sticks</em> to one node, so
 * the wrapped cursor's {@code finished} flag mirrors the single node's cursor with no cross-node advancement.
 *
 * @author Sanghun Lee
 */
class MasterReplicaScanSupport {

    private MasterReplicaScanSupport() {
    }

    /**
     * @return {@code cursor} as a {@link SourceAwareScanCursor} if it carries a source node (i.e. it is a continuation of a
     *         previous Master/Replica scan), otherwise {@code null} (an initial request).
     */
    static SourceAwareScanCursor asSourceAware(ScanCursor cursor) {
        return cursor instanceof SourceAwareScanCursor ? (SourceAwareScanCursor) cursor : null;
    }

    static <K> SourceScanCursorFactory<KeyScanCursor<K>> keyScanCursorFactory() {
        return MasterReplicaKeyScanCursor::new;
    }

    static <K, V> SourceScanCursorFactory<MapScanCursor<K, V>> mapScanCursorFactory() {
        return MasterReplicaMapScanCursor::new;
    }

    static <V> SourceScanCursorFactory<ValueScanCursor<V>> valueScanCursorFactory() {
        return MasterReplicaValueScanCursor::new;
    }

    static <V> SourceScanCursorFactory<ScoredValueScanCursor<V>> scoredValueScanCursorFactory() {
        return MasterReplicaScoredValueScanCursor::new;
    }

    static SourceScanCursorFactory<StreamScanCursor> streamScanCursorFactory() {
        return MasterReplicaStreamScanCursor::new;
    }

    /**
     * Wraps a per-node scan result into a cursor that remembers the issuing node.
     *
     * @param <T> the concrete {@link ScanCursor} type.
     */
    interface SourceScanCursorFactory<T extends ScanCursor> {

        T create(RedisURI source, RedisInstance.Role sourceRole, T delegate);

    }

    /**
     * A scan cursor that remembers the node that issued it so a continuation can be pinned back to that node.
     */
    interface SourceAwareScanCursor {

        RedisURI getSource();

        RedisInstance.Role getSourceRole();

    }

    private static void copy(ScanCursor target, ScanCursor delegate) {
        target.setCursor(delegate.getCursor());
        target.setFinished(delegate.isFinished());
    }

    private static final class MasterReplicaKeyScanCursor<K> extends KeyScanCursor<K> implements SourceAwareScanCursor {

        private final RedisURI source;

        private final RedisInstance.Role sourceRole;

        MasterReplicaKeyScanCursor(RedisURI source, RedisInstance.Role sourceRole, KeyScanCursor<K> delegate) {
            this.source = source;
            this.sourceRole = sourceRole;
            copy(this, delegate);
            getKeys().addAll(delegate.getKeys());
        }

        @Override
        public RedisURI getSource() {
            return source;
        }

        @Override
        public RedisInstance.Role getSourceRole() {
            return sourceRole;
        }

    }

    private static final class MasterReplicaMapScanCursor<K, V> extends MapScanCursor<K, V> implements SourceAwareScanCursor {

        private final RedisURI source;

        private final RedisInstance.Role sourceRole;

        MasterReplicaMapScanCursor(RedisURI source, RedisInstance.Role sourceRole, MapScanCursor<K, V> delegate) {
            this.source = source;
            this.sourceRole = sourceRole;
            copy(this, delegate);
            getMap().putAll(delegate.getMap());
        }

        @Override
        public RedisURI getSource() {
            return source;
        }

        @Override
        public RedisInstance.Role getSourceRole() {
            return sourceRole;
        }

    }

    private static final class MasterReplicaValueScanCursor<V> extends ValueScanCursor<V> implements SourceAwareScanCursor {

        private final RedisURI source;

        private final RedisInstance.Role sourceRole;

        MasterReplicaValueScanCursor(RedisURI source, RedisInstance.Role sourceRole, ValueScanCursor<V> delegate) {
            this.source = source;
            this.sourceRole = sourceRole;
            copy(this, delegate);
            getValues().addAll(delegate.getValues());
        }

        @Override
        public RedisURI getSource() {
            return source;
        }

        @Override
        public RedisInstance.Role getSourceRole() {
            return sourceRole;
        }

    }

    private static final class MasterReplicaScoredValueScanCursor<V> extends ScoredValueScanCursor<V>
            implements SourceAwareScanCursor {

        private final RedisURI source;

        private final RedisInstance.Role sourceRole;

        MasterReplicaScoredValueScanCursor(RedisURI source, RedisInstance.Role sourceRole, ScoredValueScanCursor<V> delegate) {
            this.source = source;
            this.sourceRole = sourceRole;
            copy(this, delegate);
            getValues().addAll(delegate.getValues());
        }

        @Override
        public RedisURI getSource() {
            return source;
        }

        @Override
        public RedisInstance.Role getSourceRole() {
            return sourceRole;
        }

    }

    private static final class MasterReplicaStreamScanCursor extends StreamScanCursor implements SourceAwareScanCursor {

        private final RedisURI source;

        private final RedisInstance.Role sourceRole;

        MasterReplicaStreamScanCursor(RedisURI source, RedisInstance.Role sourceRole, StreamScanCursor delegate) {
            this.source = source;
            this.sourceRole = sourceRole;
            copy(this, delegate);
            setCount(delegate.getCount());
        }

        @Override
        public RedisURI getSource() {
            return source;
        }

        @Override
        public RedisInstance.Role getSourceRole() {
            return sourceRole;
        }

    }

}
