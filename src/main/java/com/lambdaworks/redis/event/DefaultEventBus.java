package com.lambdaworks.redis.event;

import reactor.core.publisher.Flux;
import reactor.core.publisher.TopicProcessor;
import reactor.core.scheduler.Scheduler;

/**
 * Default implementation for an {@link EventBus}. Events are published using a {@link Scheduler}.
 * 
 * @author Mark Paluch
 * @since 3.4
 */
public class DefaultEventBus implements EventBus {

    private final TopicProcessor<Event> bus;
    private final Scheduler scheduler;

    public DefaultEventBus(Scheduler scheduler) {
        this.bus = TopicProcessor.create();
        this.scheduler = scheduler;
    }

    @Override
    public Flux<Event> get() {
        return bus.onBackpressureDrop().publishOn(scheduler);
    }

    @Override
    public void publish(Event event) {
        bus.onNext(event);
    }
}
