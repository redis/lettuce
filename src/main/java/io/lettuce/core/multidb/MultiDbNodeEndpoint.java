package io.lettuce.core.multidb;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisChannelWriter;
import io.lettuce.core.RedisException;
import io.lettuce.core.protocol.DefaultEndpoint;
import io.lettuce.core.protocol.RedisCommand;
import io.lettuce.core.resource.ClientResources;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * Command handler for node connections within the Redis MultiDb context. This handler can requeue commands if it is
 * disconnected and closed but has commands in the queue. If the handler was connected it would retry commands using the
 * {@literal MOVED} or {@literal ASK} redirection.
 */
class MultiDbNodeEndpoint extends DefaultEndpoint {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(MultiDbNodeEndpoint.class);

    private final RedisChannelWriter multiDbChannelWriter;

    /**
     * Initialize a new instance that handles commands from the supplied queue.
     *
     * @param clientOptions client options for this connection.
     * @param clientResources client resources for this connection.
     * @param multiDbChannelWriter top-most channel writer.
     */
    public MultiDbNodeEndpoint(ClientOptions clientOptions, ClientResources clientResources,
            RedisChannelWriter multiDbChannelWriter) {

        super(clientOptions, clientResources);

        this.multiDbChannelWriter = multiDbChannelWriter;
    }

    /**
     * Move queued and buffered commands from the inactive connection to the upstream command writer. This is done only if the
     * current connection is disconnected and auto-reconnect is enabled (command-retries). If the connection would be open, we
     * could get into a race that the commands we're moving are right now in processing. Alive connections can handle redirects
     * and retries on their own.
     */
    @Override
    public CompletableFuture<Void> closeAsync() {

        logger.debug("{} closeAsync()", logPrefix());

        if (multiDbChannelWriter != null) {
            retriggerCommands(doExclusive(this::drainCommands));
        }

        return super.closeAsync();
    }

    protected void retriggerCommands(Collection<RedisCommand<?, ?, ?>> commands) {

        for (RedisCommand<?, ?, ?> queuedCommand : commands) {
            if (queuedCommand == null || queuedCommand.isCancelled()) {
                continue;
            }

            try {
                multiDbChannelWriter.write(queuedCommand);
            } catch (RedisException e) {
                queuedCommand.completeExceptionally(e);
            }
        }
    }

}
