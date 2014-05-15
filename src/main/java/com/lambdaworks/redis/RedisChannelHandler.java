package com.lambdaworks.redis;

import static com.lambdaworks.redis.protocol.CommandType.AUTH;
import static com.lambdaworks.redis.protocol.CommandType.SELECT;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.output.StatusOutput;
import com.lambdaworks.redis.protocol.Command;
import com.lambdaworks.redis.protocol.CommandArgs;
import com.lambdaworks.redis.protocol.ConnectionWatchdog;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:mark.paluch@1und1.de">Mark Paluch</a>
 * @since 15.05.14 16:09
 */
public class RedisChannelHandler<K, V> extends ChannelInboundHandlerAdapter {
    protected BlockingQueue<Command<K, V, ?>> queue;
    protected Channel channel;
    protected long timeout;
    protected TimeUnit unit;

    private boolean closed;

    public RedisChannelHandler(BlockingQueue<Command<K, V, ?>> queue, long timeout, TimeUnit unit) {
        this.queue = queue;
        this.unit = unit;
        this.timeout = timeout;
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
        if (!closed && channel != null) {
            ConnectionWatchdog watchdog = channel.pipeline().get(ConnectionWatchdog.class);
            if (watchdog != null) {
                watchdog.setReconnect(false);
            }
            closed = true;
            channel.close();
        }
    }

    @Override
    public synchronized void channelActive(ChannelHandlerContext ctx) throws Exception {
        channel = ctx.channel();

        List<Command<K, V, ?>> tmp = new ArrayList<Command<K, V, ?>>(queue.size() + 2);

        tmp.addAll(queue);
        queue.clear();

        for (Command<K, V, ?> cmd : tmp) {
            if (!cmd.isCancelled()) {
                queue.add(cmd);
                channel.writeAndFlush(cmd);
            }
        }

        tmp.clear();
    }

    @Override
    public synchronized void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (closed) {
            for (Command<K, V, ?> cmd : queue) {
                if (cmd.getOutput() != null) {
                    cmd.getOutput().setError("Connection closed");
                }
                cmd.complete();
            }
            queue.clear();
            queue = null;
            channel = null;
        }
    }

    public synchronized <T> Command<K, V, T> dispatch(Command<K, V, T> cmd) {

        try {

            queue.put(cmd);

            if (channel != null) {
                channel.writeAndFlush(cmd);
            }
        } catch (NullPointerException e) {
            throw new RedisException("Connection is closed");
        } catch (InterruptedException e) {
            throw new RedisCommandInterruptedException(e);
        }

        return cmd;
    }
}
