package com.lambdaworks.redis.server;

import java.util.concurrent.TimeUnit;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * Tiny netty server to generate random base64 data on message reception.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class RandomResponseServer {

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel channel;

    public void initialize(int port) throws InterruptedException {

        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).option(ChannelOption.SO_BACKLOG, 100)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline p = ch.pipeline();
                        // p.addLast(new LoggingHandler(LogLevel.INFO));
                        p.addLast(new RandomServerHandler());
                    }
                });

        // Start the server.
        ChannelFuture f = b.bind(port).sync();

        channel = f.channel();
    }

    public void shutdown() {
        channel.close();
        bossGroup.shutdownGracefully(100, 100, TimeUnit.MILLISECONDS);
        workerGroup.shutdownGracefully(100, 100, TimeUnit.MILLISECONDS);
    }
}
