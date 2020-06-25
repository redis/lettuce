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

/**
 * Implementors of this class support a streaming {@link CommandOutput} while the command is still processed. The receiving
 * {@link Subscriber} receives {@link Subscriber#onNext(Collection, Object)} calls while the command is active.
 *
 * @author Mark Paluch
 * @since 4.2
 */
public interface StreamingOutput<T> {

    /**
     * Sets the {@link Subscriber}.
     *
     * @param subscriber
     */
    void setSubscriber(Subscriber<T> subscriber);

    /**
     * Retrieves the {@link Subscriber}.
     *
     * @return
     */
    Subscriber<T> getSubscriber();

    /**
     * Subscriber to a {@link StreamingOutput}.
     *
     * @param <T>
     */
    abstract class Subscriber<T> {

        /**
         * Data notification sent by the {@link StreamingOutput}.
         *
         * @param t element.
         */
        public abstract void onNext(T t);

        /**
         * Data notification sent by the {@link StreamingOutput}.
         *
         * @param outputTarget target
         * @param t element.
         */
        public void onNext(Collection<T> outputTarget, T t) {
            onNext(t);
        }

    }

}
