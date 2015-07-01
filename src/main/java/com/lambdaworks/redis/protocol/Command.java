// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.protocol;

import com.lambdaworks.redis.output.CommandOutput;
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
public class Command<K, V, T> implements RedisCommand<K, V, T> {

    private static final byte[] CRLF = "\r\n".getBytes(LettuceCharsets.ASCII);

    private final ProtocolKeyword type;

    protected CommandArgs<K, V> args;
    protected CommandOutput<K, V, T> output;
    protected Throwable exception;
    protected boolean cancelled = false;
    protected boolean completed = false;

    /**
     * Create a new command with the supplied type and args.
     *
     * @param type Command type.
     * @param output Command output.
     * @param args Command args, if any.
     */
    public Command(ProtocolKeyword type, CommandOutput<K, V, T> output, CommandArgs<K, V> args) {
        this.type = type;
        this.output = output;
        this.args = args;
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

    @Override
    public boolean completeExceptionally(Throwable throwable) {
        if (output != null) {
            output.setError(throwable.getMessage());
        }

        exception = throwable;
        return true;
    }

    /**
     * Mark this command complete and notify all waiting threads.
     */
    @Override
    public void complete() {
        completed = true;
    }

    @Override
    public void cancel() {
        cancelled = true;
    }

    /**
     * Encode and write this command to the supplied buffer using the new <a href="http://redis.io/topics/protocol">Unified
     * Request Protocol</a>.
     *
     * @param buf Buffer to write to.
     */
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
            buf.writeByte('0' + value);
            return;
        }

        StringBuilder sb = new StringBuilder(8);
        while (value > 0) {
            int digit = value % 10;
            sb.append((char) ('0' + digit));
            value /= 10;
        }

        for (int i = sb.length() - 1; i >= 0; i--) {
            buf.writeByte(sb.charAt(i));
        }
    }

    public String getError() {
        return output.getError();
    }

    @Override
    public CommandArgs<K, V> getArgs() {
        return args;
    }

    /**
     *
     * @return the resut from the output.
     */
    public T get() {
        if (output != null) {
            return output.get();
        }
        return null;
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
        if (isCancelled() || completed) {
            throw new IllegalStateException("Command is completed/cancelled. Cannot set a new output");
        }
        this.output = output;
    }

    @Override
    public ProtocolKeyword getType() {
        return type;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public boolean isDone() {
        return completed;
    }
}