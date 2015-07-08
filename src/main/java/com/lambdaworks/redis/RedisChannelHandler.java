package com.lambdaworks.redis;

import static com.google.common.base.Preconditions.checkArgument;

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
 * @since 3.0
 */
public abstract class RedisChannelHandler<K, V> extends ChannelInboundHandlerAdapter implements Closeable {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(RedisChannelHandler.class);

    protected long timeout;
    protected TimeUnit unit;

    private CloseEvents closeEvents = new CloseEvents();
    private boolean closed;
    private final RedisChannelWriter<K, V> channelWriter;
    private boolean active = true;
    private ClientOptions clientOptions;

    /**
     * @param writer the channel writer
     * @param timeout timeout value
     * @param unit unit of the timeout
     */
    public RedisChannelHandler(RedisChannelWriter<K, V> writer, long timeout, TimeUnit unit) {
        this.channelWriter = writer;
        writer.setRedisChannelHandler(this);
        setTimeout(timeout, unit);
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        closed = false;
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
    @Override
    public synchronized void close() {
        logger.debug("close()");

        if (closed) {
            logger.warn("Client is already closed");
            return;
        }

        if (!closed) {
            active = false;
            closed = true;
            channelWriter.close();
            closeEvents.fireEventClosed(this);
            closeEvents = new CloseEvents();
        }

    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        channelRead(msg);
    }

    /**
     * Invoked on a channel read.
     * 
     * @param msg channel message
     */
    public void channelRead(Object msg) {

    }

    protected <T> RedisCommand<K, V, T> dispatch(RedisCommand<K, V, T> cmd) {

        return channelWriter.write(cmd);
    }

    /**
     * Register Closeable resources. Internal access only.
     * 
     * @param registry registry of closeables
     * @param closeables closeables to register
     */
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

    protected void addListener(CloseEvents.CloseListener listener) {
        closeEvents.addListener(listener);
    }

    /**
     * 
     * @return true if the connection is closed (final state in the connection lifecyle).
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Notification when the connection becomes active (connected).
     */
    public void activated() {
        active = true;
        closed = false;

    }

    /**
     * Notification when the connection becomes inactive (disconnected).
     */
    public void deactivated() {
        active = false;
    }

    /**
     * 
     * @return the channel writer
     */
    public RedisChannelWriter<K, V> getChannelWriter() {
        return channelWriter;
    }

    /**
     * 
     * @return true if the connection is active and not closed.
     */
    public boolean isOpen() {
        return active;
    }

    public void reset() {
        channelWriter.reset();
    }

    public ClientOptions getOptions() {
        return clientOptions;
    }

    public void setOptions(ClientOptions clientOptions) {
        checkArgument(clientOptions != null, "clientOptions must not be null");
        this.clientOptions = clientOptions;
    }

    public long getTimeout() {
        return timeout;
    }

    public TimeUnit getTimeoutUnit() {
        return unit;
    }

    public void setAutoFlushCommands(boolean autoFlush) {
        getChannelWriter().setAutoFlushCommands(autoFlush);
    }

    public void flushCommands() {
        getChannelWriter().flushCommands();
    }
}
