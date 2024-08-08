package io.lettuce.core.context;

import java.util.Deque;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.lettuce.core.RedisException;
import io.lettuce.core.protocol.RedisCommand;

/**
 * Should be accessed by the event loop thread only.
 *
 * @author chenxiaofan
 */
public class ConnectionContext {

    public static class CloseStatus {

        private final boolean willReconnect;

        private Deque<RedisCommand<?, ?, ?>> retryablePendingCommands;

        private final RedisException channelClosed;

        public CloseStatus(boolean willReconnect, Deque<RedisCommand<?, ?, ?>> retryablePendingCommands,
                RedisException channelClosed) {
            this.willReconnect = willReconnect;
            this.retryablePendingCommands = retryablePendingCommands;
            this.channelClosed = channelClosed;
        }

        public boolean isWillReconnect() {
            return willReconnect;
        }

        public @Nullable Deque<RedisCommand<?, ?, ?>> getAndClearRetryablePendingCommands() {
            final Deque<RedisCommand<?, ?, ?>> old = this.retryablePendingCommands;
            this.retryablePendingCommands = null;
            return old;
        }

        public Exception getErr() {
            return channelClosed;
        }

        @Override
        public String toString() {
            return "CloseStatus{willReconnect=" + willReconnect + ", clientCloseReason=" + channelClosed + '}';
        }

    }

    public enum State {

        WILL_RECONNECT, CONNECTING, CONNECTED,
        /**
         * The client is closed. NOTE: this is different from connection closed.
         */
        ENDPOINT_CLOSED, RECONNECT_FAILED;

        public boolean isConnected() {
            return this == CONNECTED;
        }

    }

    public final State initialState;

    public final AutoBatchFlushEndPointContext autoBatchFlushEndPointContext;

    public ConnectionContext(State initialState) {
        this.initialState = initialState;
        this.autoBatchFlushEndPointContext = new AutoBatchFlushEndPointContext();
    }

    /* below fields must be accessed by the event loop thread only */
    @Nullable
    private CloseStatus closeStatus = null;

    public void setCloseStatus(@Nonnull CloseStatus closeStatus) {
        this.closeStatus = closeStatus;
    }

    public @Nullable CloseStatus getCloseStatus() {
        return closeStatus;
    }

    public boolean isChannelInactiveEventFired() {
        return closeStatus != null;
    }

    private boolean channelQuiescent = false;

    public boolean setChannelQuiescentOnce() {
        if (channelQuiescent) {
            return false;
        }
        channelQuiescent = true;
        return true;
    }

}
