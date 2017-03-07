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
package io.lettuce.core.output;

import java.util.List;

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.output.StreamingOutput.Subscriber;

/**
 * Simple subscriber
 * @author Mark Paluch
 * @since 4.2
 */
class ListSubscriber<T> implements Subscriber<T> {

    private List<T> target;

    private ListSubscriber(List<T> target) {

        LettuceAssert.notNull(target, "Target must not be null");
		this.target = target;
    }

    @Override
    public void onNext(T t) {
        target.add(t);
    }

    static <T> ListSubscriber<T> of(List<T> target) {
		return new ListSubscriber<>(target);
	}
}
