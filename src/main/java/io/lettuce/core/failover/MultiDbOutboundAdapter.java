package io.lettuce.core.failover;

import java.util.List;

import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.protocol.CompleteableCommand;
import io.lettuce.core.protocol.RedisCommand;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

/**
 * Channel handler for tracking command results and recording them to the circuit breaker.
 * <p>
 * This handler intercepts write operations and attaches completion listeners to track the success or failure of each command.
 *
 * @author Ali Takavci
 * @since 7.4
 */

class MultiDbOutboundAdapter extends ChannelOutboundHandlerAdapter {

    public final static String HANDLER_NAME = MultiDbOutboundAdapter.class.getSimpleName() + "#0";

    private CircuitBreaker circuitBreaker;

    /**
     * Create a new MultiDbOutboundAdapter.
     *
     * @param circuitBreaker the circuit breaker instance
     */
    public MultiDbOutboundAdapter(CircuitBreaker circuitBreaker) {
        this.circuitBreaker = circuitBreaker;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (circuitBreaker != null) {

            if (msg instanceof RedisCommand<?, ?, ?>) {
                promise.addListener(recorder(circuitBreaker.getGeneration(), (RedisCommand<?, ?, ?>) msg));
            }

            if (msg instanceof List) {
                List<?> list = (List<?>) msg;
                for (Object o : list) {
                    if (o instanceof RedisCommand<?, ?, ?>) {
                        promise.addListener(recorder(circuitBreaker.getGeneration(), (RedisCommand<?, ?, ?>) o));
                    }
                }
            }
        }
        // Pass the write operation down the pipeline
        super.write(ctx, msg, promise);

    }

    private GenericFutureListener<? extends Future<? super Void>> recorder(CircuitBreakerGeneration generation,
            RedisCommand<?, ?, ?> command) {
        return future -> {
            // Record failure if write failed
            if (!future.isSuccess()) {
                generation.recordResult(future.cause());
            } else {
                // Track command completion if write succeeded
                recordOnCommandComplete(generation, command);
            }
        };
    }

    private void recordOnCommandComplete(CircuitBreakerGeneration generation, RedisCommand<?, ?, ?> command) {
        // Attach completion callback to track success/failure
        if (command instanceof CompleteableCommand) {
            CompleteableCommand<?> completeable = (CompleteableCommand<?>) command;
            completeable.onComplete((o, e) -> {
                // Record failures except for RedisCommandTimeoutException
                // Timeouts are recorded by DatabaseCommandTracker
                if (!(e instanceof RedisCommandTimeoutException)) {
                    generation.recordResult(e);
                }
            });
        }
    }

}
