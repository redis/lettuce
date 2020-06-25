/*
 * Copyright 2018-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.output;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;

/**
 * Replayable {@link CommandOutput} capturing output signals to replay these on a target {@link CommandOutput}. Replay is useful
 * when the response requires inspection prior to dispatching the actual output to a command target.
 *
 * @author Mark Paluch
 * @since 5.0.3
 */
public class ReplayOutput<K, V> extends CommandOutput<K, V, List<ReplayOutput.Signal>> {

    /**
     * Initialize a new instance that encodes and decodes keys and values using the supplied codec.
     */
    public ReplayOutput() {
        super((RedisCodec) StringCodec.ASCII, new ArrayList<>());
    }

    @Override
    public void set(ByteBuffer bytes) {
        output.add(new BulkString(bytes));
    }

    @Override
    public void set(long integer) {
        output.add(new Integer(integer));
    }

    @Override
    public void setError(ByteBuffer error) {
        error.mark();
        output.add(new ErrorBytes(error));
        error.reset();
        super.setError(error);
    }

    @Override
    public void setError(String error) {
        output.add(new ErrorString(error));
        super.setError(error);
    }

    @Override
    public void complete(int depth) {
        output.add(new Complete(depth));
    }

    @Override
    public void multi(int count) {
        output.add(new Multi(count));
    }

    /**
     * Replay all captured signals on a {@link CommandOutput}.
     *
     * @param target the target {@link CommandOutput}.
     */
    public void replay(CommandOutput<?, ?, ?> target) {

        for (Signal signal : output) {
            signal.replay(target);
        }
    }

    /**
     * Encapsulates a replayable decoding signal.
     */
    public static abstract class Signal {

        /**
         * Replay the signal on a {@link CommandOutput}.
         *
         * @param target
         */
        protected abstract void replay(CommandOutput<?, ?, ?> target);

    }

    abstract static class BulkStringSupport extends Signal {

        final ByteBuffer message;

        BulkStringSupport(ByteBuffer message) {

            if (message != null) {

                // need to copy the buffer to prevent buffer lifecycle mismatch
                this.message = ByteBuffer.allocate(message.remaining());
                this.message.put(message);
                this.message.rewind();
            } else {
                this.message = null;
            }
        }

    }

    public static class BulkString extends BulkStringSupport {

        BulkString(ByteBuffer message) {
            super(message);
        }

        @Override
        protected void replay(CommandOutput<?, ?, ?> target) {
            target.set(message);
        }

    }

    static class Integer extends Signal {

        final long message;

        Integer(long message) {
            this.message = message;
        }

        @Override
        protected void replay(CommandOutput<?, ?, ?> target) {
            target.set(message);
        }

    }

    public static class ErrorBytes extends BulkStringSupport {

        ErrorBytes(ByteBuffer message) {
            super(message);
        }

        @Override
        protected void replay(CommandOutput<?, ?, ?> target) {
            target.setError(message);
        }

    }

    static class ErrorString extends Signal {

        final String message;

        ErrorString(String message) {
            this.message = message;
        }

        @Override
        protected void replay(CommandOutput<?, ?, ?> target) {
            target.setError(message);
        }

    }

    static class Multi extends Signal {

        final int count;

        Multi(int count) {
            this.count = count;
        }

        @Override
        protected void replay(CommandOutput<?, ?, ?> target) {
            target.multi(count);
        }

    }

    static class Complete extends Signal {

        final int depth;

        public Complete(int depth) {
            this.depth = depth;
        }

        @Override
        protected void replay(CommandOutput<?, ?, ?> target) {
            target.complete(depth);
        }

    }

}
