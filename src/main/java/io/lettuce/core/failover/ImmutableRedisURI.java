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

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import io.lettuce.core.DriverInfo;
import io.lettuce.core.RedisCredentialsProvider;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SslVerifyMode;
import io.lettuce.core.annotations.Experimental;

/**
 * An immutable implementation of {@link RedisURI} that prevents any modifications after construction. All setter methods throw
 * {@link UnsupportedOperationException}.
 */
@Experimental
public class ImmutableRedisURI extends RedisURI {

    private List<RedisURI> sentinels;

    public ImmutableRedisURI(RedisURI redisURI) {
        super(redisURI);
        this.sentinels = Collections
                .unmodifiableList(super.getSentinels().stream().map(ImmutableRedisURI::new).collect(Collectors.toList()));
    }

    @Override
    public void setHost(String host) {
        throw new UnsupportedOperationException("ImmutableRedisURI cannot be modified");
    }

    @Override
    public void setSentinelMasterId(String sentinelMasterId) {
        throw new UnsupportedOperationException("ImmutableRedisURI cannot be modified");
    }

    @Override
    public void setPort(int port) {
        throw new UnsupportedOperationException("ImmutableRedisURI cannot be modified");
    }

    @Override
    public void setSocket(String socket) {
        throw new UnsupportedOperationException("ImmutableRedisURI cannot be modified");
    }

    @Override
    public void applyAuthentication(RedisURI source) {
        throw new UnsupportedOperationException("ImmutableRedisURI cannot be modified");
    }

    @Override
    public void setAuthentication(CharSequence password) {
        throw new UnsupportedOperationException("ImmutableRedisURI cannot be modified");
    }

    @Override
    public void setAuthentication(char[] password) {
        throw new UnsupportedOperationException("ImmutableRedisURI cannot be modified");
    }

    @Override
    public void setAuthentication(String username, char[] password) {
        throw new UnsupportedOperationException("ImmutableRedisURI cannot be modified");
    }

    @Override
    public void setAuthentication(String username, CharSequence password) {
        throw new UnsupportedOperationException("ImmutableRedisURI cannot be modified");
    }

    @Override
    public void setCredentialsProvider(RedisCredentialsProvider credentialsProvider) {
        throw new UnsupportedOperationException("ImmutableRedisURI cannot be modified");
    }

    @Override
    public void setTimeout(Duration timeout) {
        throw new UnsupportedOperationException("ImmutableRedisURI cannot be modified");
    }

    @Override
    public void setDatabase(int database) {
        throw new UnsupportedOperationException("ImmutableRedisURI cannot be modified");
    }

    @Override
    public void setClientName(String clientName) {
        throw new UnsupportedOperationException("ImmutableRedisURI cannot be modified");
    }

    @Override
    public void setLibraryName(String libraryName) {
        throw new UnsupportedOperationException("ImmutableRedisURI cannot be modified");
    }

    @Override
    public void setDriverInfo(DriverInfo driverInfo) {
        throw new UnsupportedOperationException("ImmutableRedisURI cannot be modified");
    }

    @Override
    public void setLibraryVersion(String libraryVersion) {
        throw new UnsupportedOperationException("ImmutableRedisURI cannot be modified");
    }

    @Override
    public void applySsl(RedisURI source) {
        throw new UnsupportedOperationException("ImmutableRedisURI cannot be modified");
    }

    @Override
    public void setSsl(boolean ssl) {
        throw new UnsupportedOperationException("ImmutableRedisURI cannot be modified");
    }

    @Override
    public void setVerifyPeer(boolean verifyPeer) {
        throw new UnsupportedOperationException("ImmutableRedisURI cannot be modified");
    }

    @Override
    public void setVerifyPeer(SslVerifyMode verifyMode) {
        throw new UnsupportedOperationException("ImmutableRedisURI cannot be modified");
    }

    @Override
    public void setStartTls(boolean startTls) {
        throw new UnsupportedOperationException("ImmutableRedisURI cannot be modified");
    }

    @Override
    public List<RedisURI> getSentinels() {
        return sentinels;
    }

}
