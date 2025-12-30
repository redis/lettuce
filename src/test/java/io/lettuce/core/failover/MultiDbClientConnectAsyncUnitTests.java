/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
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
package io.lettuce.core.failover;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.ConnectionFuture;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.failover.api.StatefulRedisMultiDbConnection;

/**
 * Unit tests for {@link MultiDbClient#connectAsync(io.lettuce.core.codec.RedisCodec)} method.
 * 
 * @author Ali Takavci
 * @since 7.4
 */
@Tag(UNIT_TEST)
class MultiDbClientConnectAsyncUnitTests {

    @Test
    void connectAsyncShouldRejectNullCodec() {
        MultiDbClient client = MultiDbClient.create(MultiDbTestSupport.DBs);

        try {
            assertThatThrownBy(() -> client.connectAsync(null)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("codec must not be null");
        } finally {
            client.shutdown();
        }
    }

    @Test
    void connectAsyncShouldReturnConnectionFuture() {
        MultiDbClient client = MultiDbClient.create(MultiDbTestSupport.DBs);

        try {
            ConnectionFuture<StatefulRedisMultiDbConnection<String, String>> future = client.connectAsync(StringCodec.UTF8);

            assertThat((Object) future).isNotNull();
            assertThat((Object) future).isInstanceOf(ConnectionFuture.class);

            // Clean up - close the connection if it completes
            future.whenComplete((conn, throwable) -> {
                if (conn != null) {
                    conn.closeAsync();
                }
            });
        } finally {
            client.shutdown();
        }
    }

    @Test
    void connectAsyncShouldReturnCompletableFuture() {
        MultiDbClient client = MultiDbClient.create(MultiDbTestSupport.DBs);

        try {
            ConnectionFuture<StatefulRedisMultiDbConnection<String, String>> future = client.connectAsync(StringCodec.UTF8);

            assertThat(future.toCompletableFuture()).isNotNull();

            // Clean up
            future.whenComplete((conn, throwable) -> {
                if (conn != null) {
                    conn.closeAsync();
                }
            });
        } finally {
            client.shutdown();
        }
    }

    @Test
    void connectAsyncShouldSupportChaining() {
        MultiDbClient client = MultiDbClient.create(MultiDbTestSupport.DBs);

        try {
            ConnectionFuture<StatefulRedisMultiDbConnection<String, String>> future = client.connectAsync(StringCodec.UTF8);

            ConnectionFuture<String> chainedFuture = future.thenApply(conn -> {
                String result = "connected";
                conn.closeAsync();
                return result;
            });

            assertThat((Object) chainedFuture).isNotNull();
        } finally {
            client.shutdown();
        }
    }

}
