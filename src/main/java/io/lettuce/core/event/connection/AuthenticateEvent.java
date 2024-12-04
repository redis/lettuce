package io.lettuce.core.event.connection;

import io.lettuce.core.event.Event;

/**
 * Interface for Connection authentication events
 *
 * @author Ivo Gaydajiev
 * @since 3.4
 */
public interface AuthenticateEvent extends Event {

    String getEpId();

}
