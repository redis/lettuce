/*
 * Copyright 2017-2020 the original author or authors.
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
package io.lettuce.core.protocol;

/**
 * {@link CommandWrapper} implementation to track {@link WithLatency command latency}.
 *
 * @author Mark Paluch
 * @since 4.4
 */
class LatencyMeteredCommand<K, V, T> extends CommandWrapper<K, V, T> implements WithLatency {

    private long sentNs = -1;

    private long firstResponseNs = -1;

    private long completedNs = -1;

    public LatencyMeteredCommand(RedisCommand<K, V, T> command) {
        super(command);
    }

    @Override
    public void sent(long timeNs) {
        sentNs = timeNs;
        firstResponseNs = -1;
        completedNs = -1;
    }

    @Override
    public void firstResponse(long timeNs) {
        firstResponseNs = timeNs;
    }

    @Override
    public void completed(long timeNs) {
        completedNs = timeNs;
    }

    @Override
    public long getSent() {
        return sentNs;
    }

    @Override
    public long getFirstResponse() {
        return firstResponseNs;
    }

    @Override
    public long getCompleted() {
        return completedNs;
    }

}
