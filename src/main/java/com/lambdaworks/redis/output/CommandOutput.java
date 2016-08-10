// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.output;

import java.nio.ByteBuffer;

import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.internal.LettuceAssert;

/**
 * Abstract representation of the output of a redis command.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @param <T> Output type.
 * 
 * @author Will Glozer
 */
public abstract class CommandOutput<K, V, T> {
    protected final RedisCodec<K, V> codec;
    protected T output;
    protected String error;

    /**
     * Initialize a new instance that encodes and decodes keys and values using the supplied codec.
     * 
     * @param codec Codec used to encode/decode keys and values, must not be {@literal null}.
     * @param output Initial value of output.
     */
    public CommandOutput(RedisCodec<K, V> codec, T output) {
        LettuceAssert.notNull(codec, "RedisCodec must not be null");
        this.codec = codec;
        this.output = output;
    }

    /**
     * Get the command output.
     * 
     * @return The command output.
     */
    public T get() {
        return output;
    }

    /**
     * Set the command output to a sequence of bytes, or null. Concrete {@link CommandOutput} implementations must override this
     * method unless they only receive an integer value which cannot be null.
     * 
     * @param bytes The command output, or null.
     */
    public void set(ByteBuffer bytes) {
        throw new IllegalStateException();
    }

    /**
     * Set the command output to a 64-bit signed integer. Concrete {@link CommandOutput} implementations must override this
     * method unless they only receive a byte array value.
     * 
     * @param integer The command output.
     */
    public void set(long integer) {
        throw new IllegalStateException();
    }

    /**
     * Set command output to an error message from the server.
     * 
     * @param error Error message.
     */
    public void setError(ByteBuffer error) {
        this.error = decodeAscii(error);
    }

    /**
     * Set command output to an error message from the client.
     * 
     * @param error Error message.
     */
    public void setError(String error) {
        this.error = error;
    }

    /**
     * Check if the command resulted in an error.
     * 
     * @return true if command resulted in an error.
     */
    public boolean hasError() {
        return this.error != null;
    }

    /**
     * Get the error that occurred.
     * 
     * @return The error.
     */
    public String getError() {
        return error;
    }

    /**
     * Mark the command output complete.
     * 
     * @param depth Remaining depth of output queue.
     * 
     */
    public void complete(int depth) {
        // nothing to do by default
    }

    protected String decodeAscii(ByteBuffer bytes) {
        if (bytes == null) {
            return null;
        }

        char[] chars = new char[bytes.remaining()];
        for (int i = 0; i < chars.length; i++) {
            chars[i] = (char) bytes.get();
        }
        return new String(chars);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [output=").append(output);
        sb.append(", error='").append(error).append('\'');
        sb.append(']');
        return sb.toString();
    }

    /**
     * Mark the beginning of a multi sequence (array).
     * 
     * @param count expected number of elements in this multi sequence.
     */
    public void multi(int count) {

    }
}
