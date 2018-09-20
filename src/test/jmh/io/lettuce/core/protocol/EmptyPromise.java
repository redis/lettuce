/*
 * Copyright 2011-2018 the original author or authors.
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
package io.lettuce.core.protocol;

import java.util.concurrent.TimeUnit;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

/**
 * @author Mark Paluch
 */
class EmptyPromise implements ChannelPromise {

    @Override
    public Channel channel() {
        return null;
    }

    @Override
    public ChannelPromise setSuccess(Void result) {
        return null;
    }

    @Override
    public boolean trySuccess(Void result) {
        return false;
    }

    @Override
    public ChannelPromise setSuccess() {
        return null;
    }

    @Override
    public boolean trySuccess() {
        return false;
    }

    @Override
    public ChannelPromise setFailure(Throwable cause) {
        return null;
    }

    @Override
    public boolean tryFailure(Throwable cause) {
        return false;
    }

    @Override
    public boolean setUncancellable() {
        return false;
    }

    @Override
    public boolean isSuccess() {
        return true;
    }

    @Override
    public boolean isCancellable() {
        return false;
    }

    @Override
    public Throwable cause() {
        return null;
    }

    @Override
    public ChannelPromise addListener(GenericFutureListener<? extends Future<? super Void>> listener) {

        try {
            ((GenericFutureListener) listener).operationComplete(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    public ChannelPromise addListeners(GenericFutureListener<? extends Future<? super Void>>... listeners) {
for (GenericFutureListener<? extends Future<? super Void>> listener : listeners) {
            addListener(listener);
        }        return this;
    }

    @Override
    public ChannelPromise removeListener(GenericFutureListener<? extends Future<? super Void>> listener) {
        return null;
    }

    @Override
    public ChannelPromise removeListeners(GenericFutureListener<? extends Future<? super Void>>... listeners) {
        return null;
    }

    @Override
    public ChannelPromise sync() {
        return null;
    }

    @Override
    public ChannelPromise syncUninterruptibly() {
        return null;
    }

    @Override
    public ChannelPromise await() {
        return null;
    }

    @Override
    public ChannelPromise awaitUninterruptibly() {
        return null;
    }

    @Override
    public boolean isVoid() {
        return false;
    }

    @Override
    public ChannelPromise unvoid() {
        return null;
    }

    @Override
    public boolean await(long timeout, TimeUnit unit) {
        return false;
    }

    @Override
    public boolean await(long timeoutMillis) {
        return false;
    }

    @Override
    public boolean awaitUninterruptibly(long timeout, TimeUnit unit) {
        return false;
    }

    @Override
    public boolean awaitUninterruptibly(long timeoutMillis) {
        return false;
    }

    @Override
    public Void getNow() {
        return null;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public Void get() {
        return null;
    }

    @Override
    public Void get(long timeout, TimeUnit unit) {
        return null;
    }
}
