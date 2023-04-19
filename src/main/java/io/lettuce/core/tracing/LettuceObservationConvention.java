/*
 * Copyright 2022-2023 the original author or authors.
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
package io.lettuce.core.tracing;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;

/**
 * {@link ObservationConvention} for {@link LettuceObservationContext}.
 *
 * @author Mark Paluch
 * @since 6.3
 */
public interface LettuceObservationConvention extends ObservationConvention<LettuceObservationContext> {

    @Override
    default boolean supportsContext(Observation.Context context) {
        return context instanceof LettuceObservationContext;
    }

    /**
     * @return whether to attach the full command into the trace. Use this flag with caution as sensitive arguments will be
     *         captured in the observation spans and metric tags.
     */
    boolean includeCommandArgsInSpanTags();

}
