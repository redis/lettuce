// Copyright (C) 2013 - ze.  All rights reserved.
package com.lambdaworks.redis;

/**
 * Simple interface for Redis connection state monitoring.
 * 
 * @author ze
 */
public interface RedisConnectionStateListener {
    /**
     * Event handler for successful connection event.
     * 
     * @param connection Source connection.
     */
    public void onRedisConnected(RedisChannelHandler connection);

    /**
     * Event handler for disconnection event.
     * 
     * @param connection Source connection.
     */
    public void onRedisDisconnected(RedisChannelHandler connection);

    /**
     * 
     * Event handler for exceptions.
     * 
     * @param connection Source connection.
     * 
     * @param cause Caught exception.
     */
    public void onRedisExceptionCaught(RedisChannelHandler connection, Throwable cause);
}