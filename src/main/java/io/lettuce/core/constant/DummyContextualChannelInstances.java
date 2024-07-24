package io.lettuce.core.constant;

import io.lettuce.core.ContextualChannel;
import io.lettuce.core.context.ConnectionContext;

/**
 * @author chenxiaofan
 */
public class DummyContextualChannelInstances {

    private DummyContextualChannelInstances() {
    }

    public static final ContextualChannel CHANNEL_WILL_RECONNECT = new ContextualChannel(null,
            ConnectionContext.State.WILL_RECONNECT);

    public static final ContextualChannel CHANNEL_CONNECTING = new ContextualChannel(null, ConnectionContext.State.CONNECTING);

    public static final ContextualChannel CHANNEL_RECONNECT_FAILED = new ContextualChannel(null,
            ConnectionContext.State.RECONNECT_FAILED);

    public static final ContextualChannel CHANNEL_ENDPOINT_CLOSED = new ContextualChannel(null,
            ConnectionContext.State.ENDPOINT_CLOSED);

}
