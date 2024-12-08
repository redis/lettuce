package io.lettuce.core.event.connection;

import io.lettuce.core.event.Event;

/**
 * Interface for Connection authentication events
 *
 * @author Ivo Gaydajiev
 * @since 6.6.0
 */
public interface AuthenticateEvent extends Event {

    String getEpId();

}
