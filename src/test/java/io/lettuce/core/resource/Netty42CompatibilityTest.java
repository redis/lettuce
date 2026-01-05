package io.lettuce.core.resource;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.test.resource.FastShutdown;
import io.lettuce.test.settings.TestSettings;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.epoll.EpollIoHandler;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.uring.IoUringIoHandler;

/**
 * Test to reproduce Netty 4.2 compatibility issues between application event loops and Lettuce's internal event loops.
 * 
 * This test simulates a real-world scenario where an application (e.g., a Netty server) uses Netty event loops and also uses
 * Lettuce to connect to Redis.
 */
class Netty42CompatibilityTest {

    private static final String host = TestSettings.host();

    private static final int port = TestSettings.port();

    private EventLoopGroup appBossGroup;

    private EventLoopGroup appWorkerGroup;

    private Channel serverChannel;

    private RedisClient redisClient;

    private StatefulRedisConnection<String, String> connection;

    @BeforeEach
    void setUp() {
        // Clean up any previous state
        tearDown();
    }

    @AfterEach
    void tearDown() {
        if (connection != null) {
            connection.close();
            connection = null;
        }
        if (redisClient != null) {
            FastShutdown.shutdown(redisClient);
            redisClient = null;
        }
        if (serverChannel != null) {
            serverChannel.close().syncUninterruptibly();
            serverChannel = null;
        }
        if (appWorkerGroup != null) {
            appWorkerGroup.shutdownGracefully(0, 1, TimeUnit.SECONDS).syncUninterruptibly();
            appWorkerGroup = null;
        }
        if (appBossGroup != null) {
            appBossGroup.shutdownGracefully(0, 1, TimeUnit.SECONDS).syncUninterruptibly();
            appBossGroup = null;
        }
    }

    /**
     * Test scenario: Application uses OLD deprecated NioEventLoopGroup API, Lettuce also uses old API internally.
     * 
     * This reproduces the issue reported in https://github.com/lettuce-io/lettuce-core/issues/3584
     */
    @Test
    void testApplicationWithOldNioEventLoopGroupAPI() throws Exception {
        // Simulate an application using the old deprecated Netty 4.1 API
        // This is still common in existing applications that haven't migrated to Netty 4.2
        appBossGroup = new NioEventLoopGroup(1);
        appWorkerGroup = new NioEventLoopGroup(2);

        // Start a simple Netty server (simulating the application's server)
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(appBossGroup, appWorkerGroup).channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 128).childHandler(new ChannelInitializer<SocketChannel>() {

                    @Override
                    protected void initChannel(SocketChannel ch) {
                        // Minimal handler
                    }

                });

        // Bind to a random port
        serverChannel = bootstrap.bind(0).sync().channel();

        // Now the application tries to use Lettuce to connect to Redis
        // This should work without "incompatible event loop type" errors
        redisClient = RedisClient.create(RedisURI.create(host, port));
        connection = redisClient.connect();

        // Perform a simple operation
        String result = connection.sync().ping();
        assertThat(result).isEqualTo("PONG");
    }

    /**
     * Test scenario: Application uses NEW Netty 4.2 MultiThreadIoEventLoopGroup API, Lettuce should be compatible.
     */
    @Test
    void testApplicationWithNewNetty42API() throws Exception {
        // Simulate an application using the new Netty 4.2 API
        appBossGroup = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
        appWorkerGroup = new MultiThreadIoEventLoopGroup(2, NioIoHandler.newFactory());

        // Start a simple Netty server
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(appBossGroup, appWorkerGroup).channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 128).childHandler(new ChannelInitializer<SocketChannel>() {

                    @Override
                    protected void initChannel(SocketChannel ch) {
                        // Minimal handler
                    }

                });

        serverChannel = bootstrap.bind(0).sync().channel();

        // Application uses Lettuce
        redisClient = RedisClient.create(RedisURI.create(host, port));
        connection = redisClient.connect();

        // Perform a simple operation
        String result = connection.sync().ping();
        assertThat(result).isEqualTo("PONG");
    }

    /**
     * Test scenario: Both Epoll and IOUring are available (Linux), verify Lettuce works without incompatibility errors.
     * <p>
     * This is a regression test for the bug where the priority order mismatch caused:
     * <ul>
     * <li>Event loop created with EpollIoHandler (DefaultEventLoopGroupProvider priority: Epoll > Kqueue > IOUring)</li>
     * <li>Channel class selected as IoUringSocketChannel (old Transports priority: Kqueue > IOUring > Epoll)</li>
     * <li>Result: "incompatible event loop type" error when trying to register the channel</li>
     * </ul>
     * <p>
     * With the fix, both event loop and channel use Epoll (highest priority).
     */
    @Test
    void testEpollAndIOUringBothAvailable() throws Exception {
        // This test only runs on Linux systems where both Epoll and IOUring are available
        assumeTrue(EpollProvider.isAvailable() && IOUringProvider.isAvailable(),
                "Test requires both Epoll and IOUring to be available (Linux only)");

        // Simulate an application using Epoll and IOUring event loops
        // Before the fix, this would cause "incompatible event loop type" error
        // because Lettuce would create event loop with Epoll but select IOUring channel
        appBossGroup = new MultiThreadIoEventLoopGroup(1, EpollIoHandler.newFactory());
        appWorkerGroup = new MultiThreadIoEventLoopGroup(2, IoUringIoHandler.newFactory());

        // Start a simple Netty server
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(appBossGroup, appWorkerGroup).channel(EpollServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 128).childHandler(new ChannelInitializer<SocketChannel>() {

                    @Override
                    protected void initChannel(SocketChannel ch) {
                        // Minimal handler
                    }

                });

        serverChannel = bootstrap.bind(0).sync().channel();

        // Application uses Lettuce - this should work without "incompatible event loop type" error
        redisClient = RedisClient.create(RedisURI.create(host, port));
        connection = redisClient.connect();

        // Perform a simple operation to verify the connection works
        String result = connection.sync().ping();
        assertThat(result).isEqualTo("PONG");

        // Verify that Epoll is being used (higher priority than IOUring)
        assertThat(Transports.socketChannelClass().getSimpleName()).isEqualTo("EpollSocketChannel");
    }

}
