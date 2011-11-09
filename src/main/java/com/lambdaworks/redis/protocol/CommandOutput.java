// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.protocol;

import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.codec.RedisCodec;

import java.nio.ByteBuffer;

/**
 * Abstract representation of the output of a redis command.
 *
 * @param <T> Output type.
 *
 * @author Will Glozer
 */
public abstract class CommandOutput<K, V, T> {
    protected RedisCodec<K, V> codec;
    protected String error;

    /**
     * Initialize a new instance that encodes and decodes keys and
     * values using the supplied codec.
     *
     * @param codec Codec used to encode/decode keys and values.
     */
    public CommandOutput(RedisCodec<K, V> codec) {
        this.codec = codec;
    }

    /**
     * Get the command output.
     *
     * @return The command output.
     *
     * @throws RedisException if the command failed.
     */
    public abstract T get() throws RedisException;

    /**
     * Set the command output to a sequence of bytes, or null. Concrete
     * {@link CommandOutput} implementations must override this method
     * unless they only receive an integer value which cannot be null.
     *
     * @param bytes The command output, or null.
     */
    public void set(ByteBuffer bytes) {
        throw new IllegalStateException();
    }

    /**
     * Set the command output to a 64-bit signed integer. Concrete
     * {@link CommandOutput} implementations must override this method
     * unless they only receive a byte array value.
     *
     * @param integer The command output.
     */
    public void set(long integer) {
        throw new IllegalStateException();
    }

    /**
     * Check if the redis server returned an error and if so throw a
     * {@link RedisException} containing the error message.
     *
     * @throws RedisException if the server returned an error.
     */
    protected void errorCheck() throws RedisException {
        if (error != null) throw new RedisException(error);
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
     * Mark the command output complete.
     *
     * @param depth Remaining depth of output queue.
     */
    public void complete(int depth) {
        // nothing to do by default
    }

    protected String decodeAscii(ByteBuffer bytes) {
        char[] chars = new char[bytes.remaining()];
        for (int i = 0; i < chars.length; i++) {
            chars[i] = (char) bytes.get();
        }
        return new String(chars);
    }
}
