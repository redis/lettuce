package com.lambdaworks.redis.protocol;

import java.util.Queue;

/**
 * Interface to be implemented by classes that queue commands. Implementors of this class need to expose their queue to control
 * all queues by maintenance and cleanup processes.
 *
 * @author Mark Paluch
 */
interface HasQueuedCommands {

    /**
     * The queue holding commands.
     * 
     * @return the queue
     */
    Queue<RedisCommand<?, ?, ?>> getQueue();
}
