/*
 * Copyright 2018-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lambdaworks.redis.protocol;

import java.util.Map;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;

/**
 * @author Grzegorz Szpak
 */
public class EmptyConfig implements ChannelConfig {

    @Override
    public Map<ChannelOption<?>, Object> getOptions() {
        return null;
    }

    @Override
    public boolean setOptions(Map<ChannelOption<?>, ?> map) {
        return false;
    }

    @Override
    public <T> T getOption(ChannelOption<T> channelOption) {
        return null;
    }

    @Override
    public <T> boolean setOption(ChannelOption<T> channelOption, T t) {
        return false;
    }

    @Override
    public int getConnectTimeoutMillis() {
        return 0;
    }

    @Override
    public ChannelConfig setConnectTimeoutMillis(int i) {
        return null;
    }

    @Override
    public int getMaxMessagesPerRead() {
        return 0;
    }

    @Override
    public ChannelConfig setMaxMessagesPerRead(int i) {
        return null;
    }

    @Override
    public int getWriteSpinCount() {
        return 0;
    }

    @Override
    public ChannelConfig setWriteSpinCount(int i) {
        return null;
    }

    @Override
    public ByteBufAllocator getAllocator() {
        return null;
    }

    @Override
    public ChannelConfig setAllocator(ByteBufAllocator byteBufAllocator) {
        return null;
    }

    @Override
    public <T extends RecvByteBufAllocator> T getRecvByteBufAllocator() {
        return null;
    }

    @Override
    public ChannelConfig setRecvByteBufAllocator(RecvByteBufAllocator recvByteBufAllocator) {
        return null;
    }

    @Override
    public boolean isAutoRead() {
        return false;
    }

    @Override
    public ChannelConfig setAutoRead(boolean b) {
        return null;
    }

    @Override
    public boolean isAutoClose() {
        return false;
    }

    @Override
    public ChannelConfig setAutoClose(boolean b) {
        return null;
    }

    @Override
    public int getWriteBufferHighWaterMark() {
        return 0;
    }

    @Override
    public ChannelConfig setWriteBufferHighWaterMark(int i) {
        return null;
    }

    @Override
    public int getWriteBufferLowWaterMark() {
        return 0;
    }

    @Override
    public ChannelConfig setWriteBufferLowWaterMark(int i) {
        return null;
    }

    @Override
    public MessageSizeEstimator getMessageSizeEstimator() {
        return null;
    }

    @Override
    public ChannelConfig setMessageSizeEstimator(MessageSizeEstimator messageSizeEstimator) {
        return null;
    }

    @Override
    public WriteBufferWaterMark getWriteBufferWaterMark() {
        return null;
    }

    @Override
    public ChannelConfig setWriteBufferWaterMark(WriteBufferWaterMark writeBufferWaterMark) {
        return null;
    }
}
