/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.api.consistency;

import static io.lettuce.TestTags.UNIT_TEST;

import java.lang.reflect.Method;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Verify that the sync and reactive command interfaces of every command group declare the same methods and that the reactive
 * return types follow the mapping rules: {@code Mono<T>} for scalars, {@code Flux<E>} for {@code List}/{@code Set} results,
 * plus the deviations recorded in {@link KnownApiDeviations}. Streaming-channel variants must be deprecated on the reactive API
 * in favor of consuming the {@code Publisher}.
 */
@Tag(UNIT_TEST)
class SyncReactiveConsistencyUnitTests {

    @ParameterizedTest
    @EnumSource(CommandInterfaces.class)
    void syncMethodsExistOnReactiveApiWithMappedReturnType(CommandInterfaces group) {

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(TypeSignatures.typeParameterNames(group.reactive()))
                .as("type parameters of %s", group.reactive().getSimpleName())
                .isEqualTo(TypeSignatures.typeParameterNames(group.sync()));

        for (Method syncMethod : TypeSignatures.apiMethods(group.sync())) {

            Method reactiveMethod = TypeSignatures.findCounterpart(group.reactive(), syncMethod);
            if (reactiveMethod == null) {
                softly.fail("%s is missing on %s", TypeSignatures.describe(group.sync(), syncMethod),
                        group.reactive().getSimpleName());
                continue;
            }

            softly.assertThat(TypeSignatures.normalize(reactiveMethod.getGenericReturnType()))
                    .as("return type of %s", TypeSignatures.describe(group.reactive(), reactiveMethod))
                    .isEqualTo(TypeSignatures.expectedReactiveReturnType(syncMethod, group.sync()));

            if (!KnownApiDeviations.contains(KnownApiDeviations.REACTIVE_PARAMETER_FLAVOR_SPECIFIC, syncMethod, group.sync())) {
                softly.assertThat(TypeSignatures.parameterSignature(reactiveMethod))
                        .as("parameter types of %s", TypeSignatures.describe(group.reactive(), reactiveMethod))
                        .isEqualTo(TypeSignatures.parameterSignature(syncMethod));
            }

            // streaming-channel variants are deprecated on the reactive API in favor of consuming the Publisher
            boolean expectDeprecated = syncMethod.isAnnotationPresent(Deprecated.class)
                    || TypeSignatures.isStreamingChannelMethod(syncMethod);
            softly.assertThat(reactiveMethod.isAnnotationPresent(Deprecated.class))
                    .as("@Deprecated parity of %s", TypeSignatures.describe(group.reactive(), reactiveMethod))
                    .isEqualTo(expectDeprecated);
        }

        softly.assertAll();
    }

    @ParameterizedTest
    @EnumSource(CommandInterfaces.class)
    void reactiveMethodsExistOnSyncApi(CommandInterfaces group) {

        SoftAssertions softly = new SoftAssertions();

        for (Method reactiveMethod : TypeSignatures.apiMethods(group.reactive())) {

            if (KnownApiDeviations.contains(KnownApiDeviations.NOT_ON_SYNC_API, reactiveMethod, group.sync())
                    || KnownApiDeviations.contains(KnownApiDeviations.REACTIVE_ONLY, reactiveMethod, group.sync())) {
                continue;
            }

            if (TypeSignatures.findCounterpart(group.sync(), reactiveMethod) == null) {
                softly.fail("%s is missing on %s", TypeSignatures.describe(group.reactive(), reactiveMethod),
                        group.sync().getSimpleName());
            }
        }

        softly.assertAll();
    }

}
