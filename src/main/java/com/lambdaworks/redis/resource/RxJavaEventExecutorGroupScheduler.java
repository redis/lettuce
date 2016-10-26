/*
 * Copyright 2011-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lambdaworks.redis.resource;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;
import rx.internal.schedulers.ScheduledAction;
import rx.internal.util.SubscriptionList;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.EventExecutorGroup;

/**
 * A scheduler that uses a provided {@link EventExecutorGroup} instance to schedule tasks. This should typically be used as a
 * computation scheduler or any other scheduler that do not schedule blocking tasks. See also
 * https://github.com/ReactiveX/RxNetty
 * /blob/0.5.x/rxnetty-common/src/main/java/io/reactivex/netty/threads/RxJavaEventloopScheduler.java
 */
public class RxJavaEventExecutorGroupScheduler extends Scheduler {

    private final EventExecutorGroup eventLoopGroup;

    public RxJavaEventExecutorGroupScheduler(EventExecutorGroup eventLoopGroup) {
        this.eventLoopGroup = eventLoopGroup;
    }

    @Override
    public Worker createWorker() {
        final EventExecutor eventLoop = eventLoopGroup.next();
        return new ScheduledExecutorServiceWorker(eventLoop);
    }

    /**
     * This code is more or less copied from rx-netty's EventloopWorker worker code.
     **/
    private static class ScheduledExecutorServiceWorker extends Worker {

        /**
         * Why are there two subscription holders?
         *
         * The serial subscriptions are used for non-delayed schedules which are always executed (and hence removed) in order.
         * Since SubscriptionList holds the subs as a linked list, removals are optimal for serial removes. OTOH, delayed
         * schedules are executed (and hence removed) out of order and hence a CompositeSubscription, that stores the subs in a
         * hash structure is more optimal for removals.
         */
        private final SubscriptionList serial;
        private final CompositeSubscription timed;
        private final SubscriptionList both;
        private final ScheduledExecutorService scheduledExecutor;

        public ScheduledExecutorServiceWorker(EventExecutor scheduledExecutor) {
            this.scheduledExecutor = scheduledExecutor;
            serial = new SubscriptionList();
            timed = new CompositeSubscription();
            both = new SubscriptionList(serial, timed);
        }

        @Override
        public Subscription schedule(final Action0 action) {
            return schedule(action, 0, TimeUnit.DAYS);
        }

        @Override
        public Subscription schedule(final Action0 action, long delayTime, TimeUnit unit) {

            if (isUnsubscribed()) {
                return Subscriptions.unsubscribed();
            }

            final ScheduledAction sa;

            if (delayTime <= 0) {
                sa = new ScheduledAction(action, serial);
                serial.add(sa);
            } else {
                sa = new ScheduledAction(action, timed);
                timed.add(sa);
            }

            final Future<?> result = scheduledExecutor.schedule(sa, delayTime, unit);
            Subscription cancelFuture = Subscriptions.create(new Action0() {
                @Override
                public void call() {
                    result.cancel(false);
                }
            });
            sa.add(cancelFuture); /* An unsubscribe of the returned sub should cancel the future */
            return sa;
        }

        @Override
        public void unsubscribe() {
            both.unsubscribe();
        }

        @Override
        public boolean isUnsubscribed() {
            return both.isUnsubscribed();
        }

    }
}
