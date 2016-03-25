package com.lambdaworks.redis.event.connection;

import com.lambdaworks.redis.ConnectionId;
import com.lambdaworks.redis.event.Event;

/**
 * Interface for Connection-related events
 * 
 * @author Mark Paluch
 * @since 3.4
 */
public interface ConnectionEvent extends ConnectionId, Event {

}
