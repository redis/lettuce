package com.lambdaworks.redis.event.connection;

import com.lambdaworks.redis.ConnectionId;
import com.lambdaworks.redis.event.Event;

/**
 * Interface for Connection-related events
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.4
 */
public interface ConnectionEvent extends ConnectionId, Event {

}
