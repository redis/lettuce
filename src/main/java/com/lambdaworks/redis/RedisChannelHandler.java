package com.lambdaworks.redis;

import java.util.concurrent.TimeUnit;

import com.lambdaworks.redis.internal.RedisChannelWriter;
import com.lambdaworks.redis.protocol.Command;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 15.05.14 16:09
 */
public class RedisChannelHandler<K, V> extends ChannelInboundHandlerAdapter {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(RedisChannelHandler.class);

    protected long timeout;
    protected TimeUnit unit;

    private CloseEvents closeEvents = new CloseEvents();
    private boolean closed;
    private RedisChannelWriter<K, V> channelWriter;

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
            closed = true;
            channelWriter.close();
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

    public synchronized <T> Command<K, V, T> dispatch(Command<K, V, T> cmd) {

        channelWriter.write(cmd);
        return cmd;
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

    }

    public void deactivated() {

    }

    public RedisChannelWriter<K, V> getChannelWriter() {
        return channelWriter;
    }
}
