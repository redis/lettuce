package io.lettuce.test.server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;

/**
 * Tiny netty server to generate a response.
 *
 * @author Mark Paluch
 */
public class MockTcpServer {

    private EventLoopGroup bossGroup;

    private EventLoopGroup workerGroup;

    private Channel channel;

    private List<Supplier<? extends ChannelHandler>> handlers = new ArrayList<>();

    public void addHandler(Supplier<? extends ChannelHandler> supplier) {
        handlers.add(supplier);
    }

    public void initialize(int port) throws InterruptedException {

        bossGroup = Resources.bossGroup;
        workerGroup = Resources.workerGroup;

        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).option(ChannelOption.SO_BACKLOG, 100)
                .childHandler(new ChannelInitializer<SocketChannel>() {

                    @Override
                    public void initChannel(SocketChannel ch) {
                        ChannelPipeline p = ch.pipeline();
                        // p.addLast(new LoggingHandler(LogLevel.INFO));

                        for (Supplier<? extends ChannelHandler> handler : handlers) {
                            p.addLast(handler.get());
                        }
                    }

                });

        // Start the server.
        ChannelFuture f = b.bind(port).sync();

        channel = f.channel();
    }

    public void shutdown() {
        channel.close();
    }

    private static class Resources {

        private static final EventLoopGroup bossGroup;

        private static final EventLoopGroup workerGroup;

        static {
            bossGroup = new NioEventLoopGroup(1, new DefaultThreadFactory(NioEventLoopGroup.class, true));
            workerGroup = new NioEventLoopGroup(5, new DefaultThreadFactory(NioEventLoopGroup.class, true));

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                bossGroup.shutdownGracefully(0, 0, TimeUnit.MILLISECONDS);
                workerGroup.shutdownGracefully(0, 0, TimeUnit.MILLISECONDS);

            }, "MockRedisServer-shutdown"));
        }

    }

}
