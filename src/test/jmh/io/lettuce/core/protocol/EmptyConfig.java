package io.lettuce.core.protocol;

import java.util.Map;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;

/**
 * @author Grzegorz Szpak
 */
class EmptyConfig implements ChannelConfig {

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
