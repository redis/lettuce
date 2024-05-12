package io.lettuce.core.event.connection;

import io.lettuce.core.ConnectionId;
import io.lettuce.core.event.Event;

/**
 * Interface for Connection-related events
 *
 * @author Mark Paluch
 * @since 3.4
 */
public interface ConnectionEvent extends ConnectionId, Event {

}
