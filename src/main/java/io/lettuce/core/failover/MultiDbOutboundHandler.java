package io.lettuce.core.failover;

import java.util.Collection;

import io.lettuce.core.failover.DatabaseCommandTracker.CircuitBreakerAwareCommand;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

/**
 * Channel handler for tracking command results and recording them to the circuit breaker.
 * <p>
 * This handler intercepts write operations and attaches completion listeners to track the success or failure of each command.
 *
 * @author Ali Takavci
 * @since 7.4
 */

class MultiDbOutboundHandler extends ChannelOutboundHandlerAdapter {

    public final static String HANDLER_NAME = MultiDbOutboundHandler.class.getSimpleName() + "#0";

    private CircuitBreaker circuitBreaker;

    /**
     * Create a new MultiDbOutboundHandler.
     *
     * @param circuitBreaker the circuit breaker instance
     */
    public MultiDbOutboundHandler(CircuitBreaker circuitBreaker) {
        this.circuitBreaker = circuitBreaker;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (circuitBreaker != null) {

            if (msg instanceof CircuitBreakerAwareCommand<?, ?, ?>) {
                promise.addListener(((CircuitBreakerAwareCommand<?, ?, ?>) msg));
            }

            if (msg instanceof Collection) {
                Collection<?> collection = (Collection<?>) msg;
                for (Object cmd : collection) {
                    if (cmd instanceof CircuitBreakerAwareCommand<?, ?, ?>) {
                        // we want to use the generation that is active at the time of write.
                        // Using a lamda which acces directly to circuitBreaker.getGeneration() here would delay capturing the
                        // generation and use a potentially wrong generation, a later one.
                        promise.addListener(((CircuitBreakerAwareCommand<?, ?, ?>) cmd));
                    }
                }
            }
        }
        // Pass the write operation down the pipeline
        super.write(ctx, msg, promise);

    }

}
