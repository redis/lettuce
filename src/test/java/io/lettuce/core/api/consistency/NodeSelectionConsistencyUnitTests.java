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
 * Verify that the cluster node-selection interfaces of every eligible command group mirror the sync interface: every sync
 * method (except the {@link KnownApiDeviations#NODE_SELECTION_EXCLUDED excluded} connection-control methods) must exist with
 * its return type wrapped in {@code Executions} (sync flavor) or {@code AsyncExecutions} (async flavor), and node-selection
 * interfaces must not declare methods unknown to the sync API.
 * <p>
 * The exclusion tables themselves are kept honest by {@link DeviationTableStalenessUnitTests}.
 */
@Tag(UNIT_TEST)
class NodeSelectionConsistencyUnitTests {

    @ParameterizedTest
    @EnumSource(CommandInterfaces.class)
    void syncMethodsExistOnNodeSelectionApisWithWrappedReturnType(CommandInterfaces group) {

        if (!group.hasNodeSelection()) {
            return;
        }

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(TypeSignatures.typeParameterNames(group.nodeSelectionSync()))
                .as("type parameters of %s", group.nodeSelectionSync().getSimpleName())
                .isEqualTo(TypeSignatures.typeParameterNames(group.sync()));

        for (Method syncMethod : TypeSignatures.apiMethods(group.sync())) {

            boolean excluded = KnownApiDeviations.contains(KnownApiDeviations.NODE_SELECTION_EXCLUDED, syncMethod,
                    group.sync());
            boolean excludedOnSync = excluded
                    || KnownApiDeviations.contains(KnownApiDeviations.NOT_ON_NODE_SELECTION_SYNC, syncMethod, group.sync());

            assertCounterpart(softly, group, group.nodeSelectionSync(), syncMethod, "Executions", excludedOnSync);
            assertCounterpart(softly, group, group.nodeSelectionAsync(), syncMethod, "AsyncExecutions", excluded);
        }

        softly.assertAll();
    }

    private void assertCounterpart(SoftAssertions softly, CommandInterfaces group, Class<?> nodeSelection, Method syncMethod,
            String wrapper, boolean excluded) {

        Method counterpart = TypeSignatures.findCounterpart(nodeSelection, syncMethod);

        if (excluded) {
            softly.assertThat(counterpart).as("%s is excluded from node-selection APIs but present on %s",
                    TypeSignatures.describe(group.sync(), syncMethod), nodeSelection.getSimpleName()).isNull();
            return;
        }

        if (counterpart == null) {
            softly.fail("%s is missing on %s", TypeSignatures.describe(group.sync(), syncMethod),
                    nodeSelection.getSimpleName());
            return;
        }

        softly.assertThat(TypeSignatures.normalize(counterpart.getGenericReturnType()))
                .as("return type of %s", TypeSignatures.describe(nodeSelection, counterpart))
                .isEqualTo(TypeSignatures.expectedNodeSelectionReturnType(syncMethod, wrapper));

        softly.assertThat(TypeSignatures.parameterSignature(counterpart))
                .as("parameter types of %s", TypeSignatures.describe(nodeSelection, counterpart))
                .isEqualTo(TypeSignatures.parameterSignature(syncMethod));

        boolean expectDeprecated = syncMethod.isAnnotationPresent(Deprecated.class)
                || KnownApiDeviations.contains(KnownApiDeviations.NODE_SELECTION_EXTRA_DEPRECATED, syncMethod, group.sync());
        softly.assertThat(counterpart.isAnnotationPresent(Deprecated.class))
                .as("@Deprecated parity of %s", TypeSignatures.describe(nodeSelection, counterpart))
                .isEqualTo(expectDeprecated);
    }

    @ParameterizedTest
    @EnumSource(CommandInterfaces.class)
    void nodeSelectionMethodsExistOnSyncApi(CommandInterfaces group) {

        if (!group.hasNodeSelection()) {
            return;
        }

        SoftAssertions softly = new SoftAssertions();

        for (Class<?> nodeSelection : new Class<?>[] { group.nodeSelectionSync(), group.nodeSelectionAsync() }) {
            for (Method method : TypeSignatures.apiMethods(nodeSelection)) {
                if (nodeSelection == group.nodeSelectionAsync()
                        && KnownApiDeviations.contains(KnownApiDeviations.NOT_ON_NODE_SELECTION_SYNC, method, group.sync())) {
                    // dispatch exists on the async node-selection API only, incl. Supplier overloads without sync counterpart
                    continue;
                }
                if (TypeSignatures.findCounterpart(group.sync(), method) == null) {
                    softly.fail("%s is missing on %s", TypeSignatures.describe(nodeSelection, method),
                            group.sync().getSimpleName());
                }
            }
        }

        softly.assertAll();
    }

}
