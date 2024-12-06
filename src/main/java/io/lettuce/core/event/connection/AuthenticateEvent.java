package io.lettuce.core.event.connection;

import io.lettuce.core.event.Event;

/**
 * Interface for Connection authentication events
 *
 * @author Ivo Gaydajiev
 * @since 6.5.2
 */
public interface AuthenticateEvent extends Event {

    String getEpId();

}
