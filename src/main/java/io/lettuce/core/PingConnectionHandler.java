package io.lettuce.core;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.AsyncCommand;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.resource.ClientResources;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * A netty {@link ChannelHandler} responsible for monitoring (By periodically sending PING commands) Redis alive.
 *
 * @author Bodong Yang
 * @date 2022/11/11
 */
@ChannelHandler.Sharable
public class PingConnectionHandler extends ChannelInboundHandlerAdapter {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(PingConnectionHandler.class);

    private final RedisCommandBuilder<String, String> commandBuilder = new RedisCommandBuilder<>(StringCodec.UTF8);

    private static final int MAX_PING_FAILED_TIMES = 3;

    private final AtomicInteger pingFailed = new AtomicInteger(0);

    private final ClientResources clientResources;

    private final ClientOptions clientOptions;

    public PingConnectionHandler(ClientResources clientResources, ClientOptions clientOptions) {
        this.clientResources = clientResources;
        this.clientOptions = clientOptions;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        sendPing(ctx);
        ctx.fireChannelActive();
    }

    /**
     * Periodically send the PING command, if it fails three times in a row, it will initiate a reconnection.
     * Ignore exceptions returned by the following PING commands: {@link RedisLoadingException}
     * {@link RedisBusyException}
     *
     * @param ctx the ctx
     */
    private void sendPing(ChannelHandlerContext ctx) {
        AsyncCommand<String, String, String> dispatch = dispatch(ctx.channel(), commandBuilder.ping());

        clientResources.timer().newTimeout(timeout -> {
            if (ctx.isRemoved() || !ctx.channel().isActive()) {
                return;
            }

            if (dispatch.cancel(false) || cause(dispatch) != null) {

                Throwable cause = cause(dispatch);

                if (!(cause instanceof RedisLoadingException
                    || cause instanceof RedisBusyException)) {
                    if (!dispatch.isCancelled()) {
                        logger.error("Unable to send PING command over channel: " + ctx.channel(), cause);
                    }

                    if (pingFailed.incrementAndGet() == MAX_PING_FAILED_TIMES) {
                        logger.error("channel: {} closed due to {} consecutive PING response timeout set in {} ms",
                            ctx.channel(), MAX_PING_FAILED_TIMES, clientOptions.getPingConnectionInterval());
                        pingFailed.set(0);
                        ctx.channel().close();
                    } else {
                        sendPing(ctx);
                    }
                } else {
                    pingFailed.set(0);
                    sendPing(ctx);
                }
            } else {
                pingFailed.set(0);
                sendPing(ctx);
            }
        }, clientOptions.getPingConnectionInterval(), TimeUnit.MILLISECONDS);
    }

    private <T> AsyncCommand<String, String, T> dispatch(Channel channel, Command<String, String, T> command) {

        AsyncCommand<String, String, T> future = new AsyncCommand<>(command);

        channel.writeAndFlush(future).addListener(writeFuture -> {

            if (!writeFuture.isSuccess()) {
                future.completeExceptionally(writeFuture.cause());
            }
        });

        return future;
    }

    protected Throwable cause(CompletableFuture<?> future) {
        try {
            future.getNow(null);
            return null;
        } catch (CompletionException ex2) {
            return ex2.getCause();
        } catch (CancellationException ex1) {
            return ex1;
        }
    }
}
