package com.lambdaworks.redis.protocol;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import io.netty.util.internal.ConcurrentSet;

/**
 * Registry for all queued commands. This class allows to register/unregister {@link HasQueuedCommands queue holders}. It
 * provides queue maintenance commands.
 * 
 * @author Mark Paluch
 */
class QueuedCommands {

    private Set<HasQueuedCommands> queues = new ConcurrentSet<>();

    /**
     * Register queue holder.
     * 
     * @param queueHolder the queue holder, must not be {@literal null}.
     */
    void register(HasQueuedCommands queueHolder) {
        queues.add(queueHolder);
    }

    /**
     * Unregister queue holder.
     *
     * @param queueHolder the queue holder, must not be {@literal null}.
     */
    void unregister(HasQueuedCommands queueHolder) {
        queues.remove(queueHolder);
    }

    /**
     * Remove a command from all registered queues.
     * 
     * @param command the command.
     */
    public void remove(RedisCommand<?, ?, ?> command) {

        for (HasQueuedCommands queue : queues) {
            queue.getQueue().remove(command);
        }
    }

    /**
     * Remove multiple commands from all registered queues.
     *
     * @param commands the commands.
     */
    void removeAll(Collection<? extends RedisCommand<?, ?, ?>> commands) {

        for (HasQueuedCommands queue : queues) {
            queue.getQueue().removeAll(commands);
        }
    }

    /**
     * Drain commands from all queues.
     * 
     * @return the commands.
     */
    List<RedisCommand<?, ?, ?>> drainCommands() {

        int size = 0;

        for (HasQueuedCommands queue : queues) {
            size += queue.getQueue().size();
        }

        List<RedisCommand<?, ?, ?>> target = new ArrayList<>(size);

        RedisCommand<?, ?, ?> cmd;
        for (HasQueuedCommands queue : queues) {
            while ((cmd = queue.getQueue().poll()) != null) {
                target.add(cmd);
            }
        }

        return target;
    }

    /**
     * Check whether the registered queues exceed a global sized of the {@code requestQueueSize}.
     * 
     * @param requestQueueSize the queue size limit.
     * @return {@literal true} if there are more queued commands than {@code requestQueueSize}
     */
    boolean exceedsLimit(int requestQueueSize) {

        if (requestQueueSize == Integer.MAX_VALUE) {
            return false;
        }

        int current = 0;

        for (HasQueuedCommands queue : queues) {
            current += queue.getQueue().size();
            if (current >= requestQueueSize) {
                return true;
            }
        }

        return false;
    }
}
