package com.lambdaworks.redis.protocol;

import java.net.SocketAddress;
import java.util.ArrayDeque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.openjdk.jmh.annotations.*;

import com.lambdaworks.redis.ClientOptions;
import com.lambdaworks.redis.codec.ByteArrayCodec;
import com.lambdaworks.redis.output.ValueOutput;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

/**
 * Benchmark for {@link Command}. Test cases:
 * <ul>
 * <li>user command writes</li>
 * <li>netty (in-eventloop) writes</li>
 * </ul>
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
@State(Scope.Benchmark)
public class CommandHandlerBenchmark {

    private final static ByteArrayCodec CODEC = new ByteArrayCodec();
    private final static ClientOptions CLIENT_OPTIONS = ClientOptions.create();
    private final static EmptyChannelHandlerContext CHANNEL_HANDLER_CONTEXT = new EmptyChannelHandlerContext();
    private final static byte[] KEY = "key".getBytes();
    private final static ChannelFuture EMPTY = new EmptyFuture();

    private CommandHandler commandHandler;
    private Command command;

    @Setup
    public void setup() {

        commandHandler = new CommandHandler(CLIENT_OPTIONS, EmptyClientResources.INSTANCE, new ArrayDeque<>(512));
        command = new Command(CommandType.GET, new ValueOutput<>(CODEC), new CommandArgs(CODEC).addKey(KEY));

        commandHandler.setState(CommandHandler.LifecycleState.CONNECTED);

        commandHandler.channel = new MyLocalChannel();
    }

    @TearDown(Level.Iteration)
    public void tearDown() {
        commandHandler.reset();
    }

    @Benchmark
    public void userWrite() {
        commandHandler.write(command);
    }

    @Benchmark
    public void nettyWrite() throws Exception {
        commandHandler.write(CHANNEL_HANDLER_CONTEXT, command, null);
    }

    private final static class EmptyChannelHandlerContext implements ChannelHandlerContext {

        @Override
        public Channel channel() {
            return null;
        }

        @Override
        public EventExecutor executor() {
            return null;
        }

        @Override
        public String name() {
            return null;
        }

        @Override
        public ChannelHandler handler() {
            return null;
        }

        @Override
        public boolean isRemoved() {
            return false;
        }

        @Override
        public ChannelHandlerContext fireChannelRegistered() {
            return null;
        }

        @Override
        public ChannelHandlerContext fireChannelUnregistered() {
            return null;
        }

        @Override
        public ChannelHandlerContext fireChannelActive() {
            return null;
        }

        @Override
        public ChannelHandlerContext fireChannelInactive() {
            return null;
        }

        @Override
        public ChannelHandlerContext fireExceptionCaught(Throwable cause) {
            return null;
        }

        @Override
        public ChannelHandlerContext fireUserEventTriggered(Object event) {
            return null;
        }

        @Override
        public ChannelHandlerContext fireChannelRead(Object msg) {
            return null;
        }

        @Override
        public ChannelHandlerContext fireChannelReadComplete() {
            return null;
        }

        @Override
        public ChannelHandlerContext fireChannelWritabilityChanged() {
            return null;
        }

        @Override
        public ChannelFuture bind(SocketAddress localAddress) {
            return null;
        }

        @Override
        public ChannelFuture connect(SocketAddress remoteAddress) {
            return null;
        }

        @Override
        public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress) {
            return null;
        }

        @Override
        public ChannelFuture disconnect() {
            return null;
        }

        @Override
        public ChannelFuture close() {
            return null;
        }

        @Override
        public ChannelFuture deregister() {
            return null;
        }

        @Override
        public ChannelFuture bind(SocketAddress localAddress, ChannelPromise promise) {
            return null;
        }

        @Override
        public ChannelFuture connect(SocketAddress remoteAddress, ChannelPromise promise) {
            return null;
        }

        @Override
        public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
            return null;
        }

        @Override
        public ChannelFuture disconnect(ChannelPromise promise) {
            return null;
        }

        @Override
        public ChannelFuture close(ChannelPromise promise) {
            return null;
        }

        @Override
        public ChannelFuture deregister(ChannelPromise promise) {
            return null;
        }

        @Override
        public ChannelHandlerContext read() {
            return null;
        }

        @Override
        public ChannelFuture write(Object msg) {
            return null;
        }

        @Override
        public ChannelFuture write(Object msg, ChannelPromise promise) {
            return null;
        }

        @Override
        public ChannelHandlerContext flush() {
            return null;
        }

        @Override
        public ChannelFuture writeAndFlush(Object msg, ChannelPromise promise) {
            return null;
        }

        @Override
        public ChannelFuture writeAndFlush(Object msg) {
            return null;
        }

        @Override
        public ChannelPipeline pipeline() {
            return null;
        }

        @Override
        public ByteBufAllocator alloc() {
            return null;
        }

        @Override
        public ChannelPromise newPromise() {
            return null;
        }

        @Override
        public ChannelProgressivePromise newProgressivePromise() {
            return null;
        }

        @Override
        public ChannelFuture newSucceededFuture() {
            return null;
        }

        @Override
        public ChannelFuture newFailedFuture(Throwable cause) {
            return null;
        }

        @Override
        public ChannelPromise voidPromise() {
            return null;
        }

        @Override
        public <T> Attribute<T> attr(AttributeKey<T> key) {
            return null;
        }
    }

    private final static class MyLocalChannel extends EmbeddedChannel {
        @Override
        public boolean isActive() {
            return true;
        }

        @Override
        public boolean isOpen() {
            return true;
        }

        @Override
        public ChannelFuture write(Object msg) {
            return EMPTY;
        }

        @Override
        public ChannelFuture write(Object msg, ChannelPromise promise) {
            return promise;
        }

        @Override
        public ChannelFuture writeAndFlush(Object msg) {
            return EMPTY;
        }

        @Override
        public ChannelFuture writeAndFlush(Object msg, ChannelPromise promise) {
            return promise;
        }
    }

    private static class EmptyFuture implements ChannelFuture {
        @Override
        public Channel channel() {
            return null;
        }

        @Override
        public ChannelFuture addListener(GenericFutureListener<? extends Future<? super Void>> listener) {
            return null;
        }

        @Override
        public ChannelFuture addListeners(GenericFutureListener<? extends Future<? super Void>>... listeners) {
            return null;
        }

        @Override
        public ChannelFuture removeListener(GenericFutureListener<? extends Future<? super Void>> listener) {
            return null;
        }

        @Override
        public ChannelFuture removeListeners(GenericFutureListener<? extends Future<? super Void>>... listeners) {
            return null;
        }

        @Override
        public ChannelFuture sync() throws InterruptedException {
            return null;
        }

        @Override
        public ChannelFuture syncUninterruptibly() {
            return null;
        }

        @Override
        public ChannelFuture await() throws InterruptedException {
            return null;
        }

        @Override
        public ChannelFuture awaitUninterruptibly() {
            return null;
        }

        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public boolean isCancellable() {
            return false;
        }

        @Override
        public Throwable cause() {
            return null;
        }

        @Override
        public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
            return false;
        }

        @Override
        public boolean await(long timeoutMillis) throws InterruptedException {
            return false;
        }

        @Override
        public boolean awaitUninterruptibly(long timeout, TimeUnit unit) {
            return false;
        }

        @Override
        public boolean awaitUninterruptibly(long timeoutMillis) {
            return false;
        }

        @Override
        public Void getNow() {
            return null;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return false;
        }

        @Override
        public Void get() throws InterruptedException, ExecutionException {
            return null;
        }

        @Override
        public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return null;
        }
    }
}
