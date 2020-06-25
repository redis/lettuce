/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.output;

import java.util.Collection;

import io.lettuce.core.output.StreamingOutput.Subscriber;

/**
 * Simple subscriber feeding a {@link Collection} {@link #onNext(Collection, Object)}. Does not support {@link #onNext(Object)
 * plain onNext}.
 *
 * @author Mark Paluch
 * @author Julien Ruaux
 * @since 4.2
 */
public class ListSubscriber<T> extends Subscriber<T> {

    private static final ListSubscriber<Object> INSTANCE = new ListSubscriber<>();

    private ListSubscriber() {
    }

    @SuppressWarnings("unchecked")
    public static <T> ListSubscriber<T> instance() {
        return (ListSubscriber<T>) INSTANCE;
    }

    @Override
    public void onNext(T t) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onNext(Collection<T> outputTarget, T t) {
        outputTarget.add(t);
    }

}
