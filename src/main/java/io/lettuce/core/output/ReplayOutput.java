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
    public void setSingle(ByteBuffer bytes) {
        recordBytes(bytes, new SimpleString(copy(bytes)));
    }

    @Override
    public void setBigNumber(ByteBuffer bytes) {
        recordBytes(bytes, new BigNumber(copy(bytes)));
    }

    private static ByteBuffer copy(ByteBuffer bytes) {
        return bytes == null ? null : bytes.duplicate();
    }

    private void recordBytes(ByteBuffer bytes, Signal signal) {
        int index = output.size();
        set(bytes);
        replaceLast(index, signal);
    }

    private void recordMulti(int count, Multi.Type type) {
        int index = output.size();
        multi(count);
        replaceLast(index, new Multi(count, type));
    }

    private void replaceLast(int index, Signal signal) {
        if (output.size() > index) {
            output.set(index, signal);
        } else {
            output.add(signal);
        }
    }

    @Override
    public void set(long integer) {
        output.add(new Integer(integer));
    }

    @Override
    public void set(double number) {
        output.add(new Double(number));
    }

    @Override
    public void set(boolean value) {
        output.add(new BooleanValue(value));
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
        output.add(new Multi(count, Multi.Type.MULTI));
    }

    @Override
    public void multiArray(int count) {
        recordMulti(count, Multi.Type.ARRAY);
    }

    @Override
    public void multiPush(int count) {
        recordMulti(count, Multi.Type.PUSH);
    }

    @Override
    public void multiMap(int count) {
        recordMulti(count, Multi.Type.MAP);
    }

    @Override
    public void multiSet(int count) {
        recordMulti(count, Multi.Type.SET);
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

    static class SimpleString extends BulkStringSupport {

        SimpleString(ByteBuffer message) {
            super(message);
        }

        @Override
        protected void replay(CommandOutput<?, ?, ?> target) {
            target.setSingle(message);
        }

    }

    static class BigNumber extends BulkStringSupport {

        BigNumber(ByteBuffer message) {
            super(message);
        }

        @Override
        protected void replay(CommandOutput<?, ?, ?> target) {
            target.setBigNumber(message);
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

    static class Double extends Signal {

        final double message;

        Double(double message) {
            this.message = message;
        }

        @Override
        protected void replay(CommandOutput<?, ?, ?> target) {
            target.set(message);
        }

    }

    static class BooleanValue extends Signal {

        final boolean message;

        BooleanValue(boolean message) {
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

        final Type type;

        Multi(int count, Type type) {
            this.count = count;
            this.type = type;
        }

        @Override
        protected void replay(CommandOutput<?, ?, ?> target) {
            switch (type) {
                case MULTI:
                    target.multi(count);
                    break;
                case ARRAY:
                    target.multiArray(count);
                    break;
                case PUSH:
                    target.multiPush(count);
                    break;
                case MAP:
                    target.multiMap(count);
                    break;
                case SET:
                    target.multiSet(count);
                    break;
                default:
                    throw new IllegalStateException("Unsupported multi signal type " + type);
            }
        }

        enum Type {
            MULTI, ARRAY, PUSH, MAP, SET
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
