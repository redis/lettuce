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
