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
package io.lettuce.core.resource;

import java.time.Duration;

/**
 * {@link Delay} with a constant delay for each attempt.
 *
 * @author Mark Paluch
 */
class ConstantDelay extends Delay {

    private final Duration delay;

    ConstantDelay(Duration delay) {
        this.delay = delay;
    }

    @Override
    public Duration createDelay(long attempt) {
        return delay;
    }
}
