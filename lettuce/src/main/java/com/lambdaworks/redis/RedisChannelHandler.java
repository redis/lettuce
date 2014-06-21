package com.lambdaworks.redis;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import com.lambdaworks.redis.protocol.RedisCommand;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Abstract base for every redis connection. Provides basic connection functionality and tracks open resources.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 15.05.14 16:09
 */
public abstract class RedisChannelHandler<K, V> extends ChannelInboundHandlerAdapter implements Closeable {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(RedisChannelHandler.class);

    protected long timeout;
    protected TimeUnit unit;

    private CloseEvents closeEvents = new CloseEvents();
    private boolean closed;
    private final RedisChannelWriter<K, V> channelWriter;
    private boolean active = true;

    public RedisChannelHandler(RedisChannelWriter<K, V> writer, long timeout, TimeUnit unit) {
        this.unit = unit;
        this.timeout = timeout;
        this.channelWriter = writer;
        writer.setRedisChannelHandler(this);

    }

    /**
     * Set the command timeout for this connection.
     * 
     * @param timeout Command timeout.
     * @param unit Unit of time for the timeout.
     */
    public void setTimeout(long timeout, TimeUnit unit) {
        this.timeout = timeout;
        this.unit = unit;
    }

    /**
     * Close the connection.
     */
    public synchronized void close() {
        logger.debug("close()");

        if (closed) {
            logger.warn("Client is already closed");
            return;
        }

        if (!closed) {
            active = false;
            closed = true;
            closeEvents.fireEventClosed(this);
            closeEvents = null;
        }

    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        channelRead(msg);
    }

    public void channelRead(Object msg) {

    }

    public <T> RedisCommand<K, V, T> dispatch(RedisCommand<K, V, T> cmd) {

        return channelWriter.write(cmd);
    }

    public void registerCloseables(final Collection<Closeable> registry, final Closeable... closeables) {
        registry.addAll(Arrays.asList(closeables));

        addListener(new CloseEvents.CloseListener() {
            @Override
            public void resourceClosed(Object resource) {
                for (Closeable closeable : closeables) {
                    if (closeable == RedisChannelHandler.this) {
                        continue;
                    }

                    try {
                        closeable.close();
                    } catch (IOException e) {
                        logger.debug(e.toString(), e);
                    }
                }

                registry.removeAll(Arrays.asList(closeables));
            }
        });
    }

    public void addListener(CloseEvents.CloseListener listener) {
        closeEvents.addListener(listener);
    }

    public void removeListener(CloseEvents.CloseListener listener) {
        closeEvents.removeListener(listener);
    }

    public boolean isClosed() {
        return closed;
    }

    public void activated() {
        active = true;

    }

    public void deactivated() {
        active = false;
    }

    public RedisChannelWriter<K, V> getChannelWriter() {
        return channelWriter;
    }

    public boolean isOpen() {
        return active;
    }
}
