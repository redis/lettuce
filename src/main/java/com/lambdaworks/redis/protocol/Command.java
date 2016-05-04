// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.protocol;

import com.lambdaworks.redis.internal.LettuceAssert;
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
 * @author Mark Paluch
 */
public class Command<K, V, T> implements RedisCommand<K, V, T>, WithLatency{

    private final ProtocolKeyword type;

    protected CommandArgs<K, V> args;
    protected CommandOutput<K, V, T> output;
    protected Throwable exception;
    protected boolean cancelled = false;
    protected boolean completed = false;
    protected long sentNs = -1;
    protected long firstResponseNs = -1;
    protected long completedNs = -1;

    /**
     * Create a new command with the supplied type.
     *
     * @param type Command type, must not be {@literal null}.
     * @param output Command output, can be {@literal null}.
     */
    public Command(ProtocolKeyword type, CommandOutput<K, V, T> output) {
        this(type, output, null);
    }

    /**
     * Create a new command with the supplied type and args.
     *
     * @param type Command type, must not be {@literal null}.
     * @param output Command output, can be {@literal null}.
     * @param args Command args, can be {@literal null}
     */
    public Command(ProtocolKeyword type, CommandOutput<K, V, T> output, CommandArgs<K, V> args) {
        LettuceAssert.notNull(type, "Command type must not be null");
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
        CommandArgs.IntegerArgument.writeInteger(buf, 1 + (args != null ? args.count() : 0));

        buf.writeBytes(CommandArgs.CRLF);

        CommandArgs.BytesArgument.writeBytes(buf, type.getBytes());

        if (args != null) {
            args.encode(buf);
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

	@Override
	public void sent(long timeNs) {
		sentNs = timeNs;
		firstResponseNs = -1;
		completedNs = -1;
	}

	@Override
	public void firstResponse(long timeNs) {
		firstResponseNs = timeNs;
	}

	@Override
	public void completed(long timeNs) {
		completedNs = timeNs; 
	}

	@Override
	public long getSent() {
		return sentNs;
	}

	@Override
	public long getFirstResponse() {
		return firstResponseNs;
	}

	@Override
	public long getCompleted() {
		return completedNs;
	}
}