package io.lettuce.core.output;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Verifies that {@link CommandOutput}s which do not build a result themselves but route or record the decoder's callbacks (
 * {@link MultiOutput}, {@link ReplayOutput}, {@link TransformingOutput} ) override every callback that {@link CommandOutput}
 * declares. A callback that is not overridden silently falls back to the default implementation, which either throws or
 * converts the call into a different one (e.g. {@code multiMap(n)} into {@code multi(2n)}), so a wrapper that misses one breaks
 * RESP3 replies. New callbacks added to {@link CommandOutput} are picked up automatically by this test.
 */
@Tag(UNIT_TEST)
class WrapperOutputCallbackCoverageUnitTests {

    @Test
    void wrappersOverrideEveryCommandOutputCallback() {

        List<Method> callbacks = Arrays.stream(CommandOutput.class.getMethods())
                .filter(m -> m.getDeclaringClass() == CommandOutput.class).filter(m -> m.getReturnType() == void.class)
                .collect(Collectors.toList());

        assertThat(callbacks).hasSize(14);

        for (Class<?> wrapper : Arrays.asList(MultiOutput.class, ReplayOutput.class, TransformingOutput.class)) {
            for (Method callback : callbacks) {
                Method declared;
                try {
                    declared = wrapper.getMethod(callback.getName(), callback.getParameterTypes());
                } catch (NoSuchMethodException e) {
                    throw new AssertionError(
                            String.format("%s does not implement %s", wrapper.getSimpleName(), callback.getName()), e);
                }
                assertThat(declared.getDeclaringClass()).as("%s must override %s", wrapper.getSimpleName(), callback.getName())
                        .isNotEqualTo(CommandOutput.class);
            }
        }
    }

}
