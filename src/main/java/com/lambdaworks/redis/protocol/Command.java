// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.protocol;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.util.concurrent.AbstractFuture;
import com.lambdaworks.redis.RedisCommandInterruptedException;
import io.netty.buffer.ByteBuf;

/**
 * A redis command and its result. All successfully executed commands will eventually return a {@link CommandOutput} object.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @param <T> Command output type.
 * 
 * @author Will Glozer
 */
public class Command<K, V, T> extends AbstractFuture<T> implements RedisCommand<K, V, T> {

    private static final byte[] CRLF = "\r\n".getBytes(LettuceCharsets.ASCII);

    protected CommandArgs<K, V> args;
    protected CommandOutput<K, V, T> output;
    protected CountDownLatch latch;

    private final ProtocolKeyword type;
    private boolean multi;
    private Throwable exception;
    private boolean cancelled = false;

    /**
     * Create a new command with the supplied type and args.
     *
     * @param type Command type.
     * @param output Command output.
     * @param args Command args, if any.
     */
    public Command(ProtocolKeyword type, CommandOutput<K, V, T> output, CommandArgs<K, V> args) {
        this(type, output, args, false);
    }

    /**
     * Create a new command with the supplied type and args.
     *
     * @param type Command type.
     * @param output Command output.
     * @param args Command args, if any.
     * @param multi Flag indicating if MULTI active.
     */
    public Command(ProtocolKeyword type, CommandOutput<K, V, T> output, CommandArgs<K, V> args, boolean multi) {
        this.type = type;
        this.output = output;
        this.args = args;
        setMulti(multi);
    }

    public void setMulti(boolean multi) {
        this.latch = new CountDownLatch(multi ? 2 : 1);
        this.multi = multi;
    }

    public boolean isMulti() {
        return multi;
    }

    @Override
    protected void interruptTask() {
        cancelled = true;

        if (latch.getCount() == 1) {
            latch.countDown();
        }
    }

    /**
     * Check if the command has been cancelled.
     * 
     * @return True if the command was cancelled.
     */
    @Override
    public boolean isCancelled() {
        return latch.getCount() == 0 && cancelled;
    }

    /**
     * Check if the command has completed.
     * 
     * @return true if the command has completed.
     */
    @Override
    public boolean isDone() {
        return latch.getCount() == 0;
    }

    /**
     * Get the command output and if the command hasn't completed yet, wait until it does.
     * 
     * @return The command output.
     */
    @Override
    public T get() throws ExecutionException {
        try {
            latch.await();
            if (exception != null) {
                throw new ExecutionException(exception);
            }

            return output.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RedisCommandInterruptedException(e);
        }
    }

    /**
     * Get the command output and if the command hasn't completed yet, wait up to the specified time until it does.
     * 
     * @param timeout Maximum time to wait for a result.
     * @param unit Unit of time for the timeout.
     * 
     * @return The command output.
     * 
     * @throws TimeoutException if the wait timed out.
     */
    @Override
    public T get(long timeout, TimeUnit unit) throws TimeoutException, ExecutionException {
        try {
            if (!latch.await(timeout, unit)) {
                throw new TimeoutException("Command timed out");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RedisCommandInterruptedException(e);
        }
        if (exception != null) {
            throw new ExecutionException(exception);
        }
        return output.get();
    }

    /**
     * Wait up to the specified time for the command output to become available.
     * 
     * @param timeout Maximum time to wait for a result.
     * @param unit Unit of time for the timeout.
     * 
     * @return true if the output became available.
     */
    @Override
    public boolean await(long timeout, TimeUnit unit) {
        try {
            return latch.await(timeout, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RedisCommandInterruptedException(e);
        }
    }

    /**
     * Get the object that holds this command's output.
     * 
     * @return The command output object.
     */
    @Override
    public CommandOutput<K, V, T> getOutput() {
        return output;
    }

    /**
     * Mark this command complete and notify all waiting threads.
     */
    @Override
    public void complete() {
        latch.countDown();
        if (latch.getCount() == 0) {
            if (output == null) {
                set(null);
            } else {
                set(output.get());
            }
        }
    }

    /**
     * Encode and write this command to the supplied buffer using the new <a href="http://redis.io/topics/protocol">Unified
     * Request Protocol</a>.
     * 
     * @param buf Buffer to write to.
     */
    @Override
    public void encode(ByteBuf buf) {
        buf.writeByte('*');
        writeInt(buf, 1 + (args != null ? args.count() : 0));
        buf.writeBytes(CRLF);
        buf.writeByte('$');
        writeInt(buf, type.getBytes().length);
        buf.writeBytes(CRLF);
        buf.writeBytes(type.getBytes());
        buf.writeBytes(CRLF);
        if (args != null) {
            buf.writeBytes(args.buffer());
        }
    }

    /**
     * Write the textual value of a positive integer to the supplied buffer.
     *
     * @param buf Buffer to write to.
     * @param value Value to write.
     */
    protected static void writeInt(ByteBuf buf, int value) {

        if (value < 10) {
            buf.writeByte((byte) ('0' + value));
            return;
        }

        String asString = Integer.toString(value);
        for (int i = 0; i < asString.length(); i++) {
            buf.writeByte((byte) asString.charAt(i));
        }
    }

    @Override
    public String getError() {
        return output.getError();
    }

    @Override
    public CommandArgs<K, V> getArgs() {
        return args;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [type=").append(type);
        sb.append(", output=").append(output);
        sb.append(']');
        return sb.toString();
    }

    public void setOutput(CommandOutput<K, V, T> output) {
        this.output = output;
    }

    public Throwable getException() {
        return exception;
    }

    @Override
    public boolean setException(Throwable exception) {
        this.exception = exception;
        return true;
    }

    @Override
    public ProtocolKeyword getType() {
        return type;
    }
}
