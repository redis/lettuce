package io.lettuce.core.protocol;

import java.util.Collection;

/**
 * Interface to be implemented by classes that queue commands. Implementors of this class need to expose their queue to control
 * all queues by maintenance and cleanup processes.
 *
 * @author Mark Paluch
 */
interface HasQueuedCommands {

    Collection<RedisCommand<?, ?, ?>> drainQueue();

}
