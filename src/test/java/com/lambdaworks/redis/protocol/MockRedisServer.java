package com.lambdaworks.redis.protocol;

import java.io.Closeable;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.redis.RedisArrayAggregator;
import io.netty.handler.codec.redis.RedisBulkStringAggregator;
import io.netty.handler.codec.redis.RedisDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public class MockRedisServer implements Closeable {

    protected static final InternalLogger logger = InternalLoggerFactory.getInstance(MockRedisServer.class);

    private int port;
    private EventLoopGroup eventLoopGroup;
    private ChannelFuture closeFuture;

    private Iterator<byte[]> mockResponseIterator;

    public int port() {
        return port;
    }

    @Override
    public void close() {

        try {
            eventLoopGroup.shutdownGracefully().sync();
            logger.info("MockRedisServer: closed: port={}", port);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void sync() throws InterruptedException {

        closeFuture.sync();
    }

    public void setResponses(String... mockResponses) {

        mockResponseIterator = Arrays.stream(mockResponses)
                                     .map(s -> s.getBytes(StandardCharsets.UTF_8))
                                     .iterator();
    }

    public void setResponses(byte[]... mockResponses) {

        mockResponseIterator = Arrays.stream(mockResponses).iterator();
    }

    public void startServer() {

        this.mockResponseIterator = Collections.emptyIterator();
        this.eventLoopGroup = new NioEventLoopGroup(1);
        ServerBootstrap b = new ServerBootstrap();
        b.group(eventLoopGroup)
         .channel(NioServerSocketChannel.class)
         .option(ChannelOption.SO_BACKLOG, 100)
         .handler(new LoggingHandler(LogLevel.INFO))
         .childHandler(new ChannelInitializer<SocketChannel>() {
             @Override
             public void initChannel(SocketChannel ch) throws Exception {
                 ChannelPipeline p = ch.pipeline();
                 p.addLast(new RedisDecoder());
                 p.addLast(new RedisBulkStringAggregator());
                 p.addLast(new RedisArrayAggregator());
                 //p.addLast(new RedisEncoder()); // This is correct encoder, so we cannot make wrong responses.
                 p.addLast(new MockRedisServerHandler());
             }
         });

        try {
            // Start the server.
            ChannelFuture f = b.bind("localhost", 0).sync();

            // store ephemeral listen port
            this.port = ((InetSocketAddress) f.channel().localAddress()).getPort();

            // and set closeFuture.
            this.closeFuture = f.channel().closeFuture();

            logger.info("MockRedisServer: started: port={}", port);
        } catch (InterruptedException e) {
            logger.error("MockRedisServer: failed", e);
            throw new RuntimeException(e);
        } catch (Throwable e) {
            logger.error("MockRedisServer: failed", e);
            throw e;
        }
    }

    private class MockRedisServerHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            if (!mockResponseIterator.hasNext()) {
                throw new NoSuchElementException("No more mock responses. Set your responses first.");
            }
            final byte[] currentResponse = mockResponseIterator.next();
            final ByteBuf buf = ctx.alloc().ioBuffer(currentResponse.length);
            buf.writeBytes(currentResponse);
            ctx.writeAndFlush(buf);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            logger.warn("exceptionCaught", cause);
            ctx.close();
        }
    }
}
