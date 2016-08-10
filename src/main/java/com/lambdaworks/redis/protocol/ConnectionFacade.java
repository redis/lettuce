package com.lambdaworks.redis.protocol;

/**
 * Represents a stateful connection facade. Connections can be activated and deactivated and particular actions can be executed
 * upon connection activation/deactivation.
 * 
 * @author Mark Paluch
 */
public interface ConnectionFacade {

    /**
     * Callback for a connection activated event. This method may invoke non-blocking connection operations to prepare the
     * connection after the connection was established.
     */
    void activated();

    /**
     * Callback for a connection deactivated event. This method may invoke non-blocking operations to cleanup the connection
     * after disconnection.
     */
    void deactivated();

    /**
     * Reset the connection state.
     */
    void reset();
}
