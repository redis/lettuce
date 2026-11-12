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
 * Verify that the sync and async command interfaces of every command group declare the same methods and that the async return
 * types wrap the sync return types in {@code RedisFuture}.
 * <p>
 * This parity is load-bearing at runtime: the sync API is a dynamic proxy that translates each sync method to the async method
 * with the same name and parameter types (see {@code FutureSyncInvocationHandler}), so a mismatch throws at runtime.
 */
@Tag(UNIT_TEST)
class SyncAsyncConsistencyUnitTests {

    @ParameterizedTest
    @EnumSource(CommandInterfaces.class)
    void syncMethodsExistOnAsyncApiWithWrappedReturnType(CommandInterfaces group) {

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(TypeSignatures.typeParameterNames(group.async()))
                .as("type parameters of %s", group.async().getSimpleName())
                .isEqualTo(TypeSignatures.typeParameterNames(group.sync()));

        for (Method syncMethod : TypeSignatures.apiMethods(group.sync())) {

            Method asyncMethod = TypeSignatures.findCounterpart(group.async(), syncMethod);
            if (asyncMethod == null) {
                softly.fail("%s is missing on %s", TypeSignatures.describe(group.sync(), syncMethod),
                        group.async().getSimpleName());
                continue;
            }

            softly.assertThat(TypeSignatures.normalize(asyncMethod.getGenericReturnType()))
                    .as("return type of %s", TypeSignatures.describe(group.async(), asyncMethod))
                    .isEqualTo(TypeSignatures.expectedAsyncReturnType(syncMethod, group.sync()));

            softly.assertThat(TypeSignatures.parameterSignature(asyncMethod))
                    .as("parameter types of %s", TypeSignatures.describe(group.async(), asyncMethod))
                    .isEqualTo(TypeSignatures.parameterSignature(syncMethod));

            softly.assertThat(asyncMethod.isAnnotationPresent(Deprecated.class))
                    .as("@Deprecated parity of %s", TypeSignatures.describe(group.async(), asyncMethod))
                    .isEqualTo(syncMethod.isAnnotationPresent(Deprecated.class));
        }

        softly.assertAll();
    }

    @ParameterizedTest
    @EnumSource(CommandInterfaces.class)
    void asyncMethodsExistOnSyncApi(CommandInterfaces group) {

        SoftAssertions softly = new SoftAssertions();

        for (Method asyncMethod : TypeSignatures.apiMethods(group.async())) {

            if (KnownApiDeviations.contains(KnownApiDeviations.NOT_ON_SYNC_API, asyncMethod, group.sync())) {
                continue;
            }

            if (TypeSignatures.findCounterpart(group.sync(), asyncMethod) == null) {
                softly.fail("%s is missing on %s (this breaks the sync-over-async runtime proxy)",
                        TypeSignatures.describe(group.async(), asyncMethod), group.sync().getSimpleName());
            }
        }

        softly.assertAll();
    }

}
