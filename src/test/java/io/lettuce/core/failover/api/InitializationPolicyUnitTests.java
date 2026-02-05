package io.lettuce.core.failover.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.lettuce.core.failover.api.InitializationPolicy.Decision;

/**
 * Unit tests for {@link InitializationPolicy} implementations.
 * <p>
 * Tests verify the decision logic for different combinations of available, failed, and pending connections for each built-in
 * policy:
 * <ul>
 * <li>{@link InitializationPolicy.BuiltIn#ALL_AVAILABLE}</li>
 * <li>{@link InitializationPolicy.BuiltIn#MAJORITY_AVAILABLE}</li>
 * <li>{@link InitializationPolicy.BuiltIn#ONE_AVAILABLE}</li>
 * </ul>
 *
 * @author Ali Takavci
 * @since 7.4
 */
@DisplayName("InitializationPolicy Unit Tests")
class InitializationPolicyUnitTests {

    /**
     * Test implementation of InitializationContext for unit testing.
     */
    private static class TestInitializationContext implements InitializationPolicy.InitializationContext {

        private final int available;

        private final int failed;

        private final int pending;

        TestInitializationContext(int available, int failed, int pending) {
            this.available = available;
            this.failed = failed;
            this.pending = pending;
        }

        @Override
        public int getAvailableConnections() {
            return available;
        }

        @Override
        public int getFailedConnections() {
            return failed;
        }

        @Override
        public int getPendingConnections() {
            return pending;
        }

    }

    @Nested
    @DisplayName("ALL_AVAILABLE Policy Tests")
    class AllAvailablePolicyTests {

        private final InitializationPolicy policy = InitializationPolicy.BuiltIn.ALL_AVAILABLE;

        @Test
        @DisplayName("Should return SUCCESS when all connections are available")
        void shouldSucceedWhenAllAvailable() {
            TestInitializationContext ctx = new TestInitializationContext(3, 0, 0);
            assertThat(policy.evaluate(ctx)).isEqualTo(Decision.SUCCESS);
        }

        @Test
        @DisplayName("Should return SUCCESS when single connection is available and no others")
        void shouldSucceedWithSingleConnection() {
            TestInitializationContext ctx = new TestInitializationContext(1, 0, 0);
            assertThat(policy.evaluate(ctx)).isEqualTo(Decision.SUCCESS);
        }

        @Test
        @DisplayName("Should return FAIL when any connection fails")
        void shouldFailWhenAnyConnectionFails() {
            TestInitializationContext ctx = new TestInitializationContext(2, 1, 0);
            assertThat(policy.evaluate(ctx)).isEqualTo(Decision.FAIL);
        }

        @Test
        @DisplayName("Should return FAIL when all connections fail")
        void shouldFailWhenAllConnectionsFail() {
            TestInitializationContext ctx = new TestInitializationContext(0, 3, 0);
            assertThat(policy.evaluate(ctx)).isEqualTo(Decision.FAIL);
        }

        @Test
        @DisplayName("Should return FAIL when single connection fails")
        void shouldFailWithSingleFailure() {
            TestInitializationContext ctx = new TestInitializationContext(0, 1, 0);
            assertThat(policy.evaluate(ctx)).isEqualTo(Decision.FAIL);
        }

        @Test
        @DisplayName("Should return CONTINUE when connections are pending")
        void shouldContinueWhenPending() {
            TestInitializationContext ctx = new TestInitializationContext(2, 0, 1);
            assertThat(policy.evaluate(ctx)).isEqualTo(Decision.CONTINUE);
        }

        @Test
        @DisplayName("Should return CONTINUE when all connections are pending")
        void shouldContinueWhenAllPending() {
            TestInitializationContext ctx = new TestInitializationContext(0, 0, 3);
            assertThat(policy.evaluate(ctx)).isEqualTo(Decision.CONTINUE);
        }

        @Test
        @DisplayName("Should return FAIL when some available but one failed")
        void shouldFailWithMixedAvailableAndFailed() {
            TestInitializationContext ctx = new TestInitializationContext(1, 1, 0);
            assertThat(policy.evaluate(ctx)).isEqualTo(Decision.FAIL);
        }

    }

    @Nested
    @DisplayName("MAJORITY_AVAILABLE Policy Tests")
    class MajorityAvailablePolicyTests {

        private final InitializationPolicy policy = InitializationPolicy.BuiltIn.MAJORITY_AVAILABLE;

        @Test
        @DisplayName("Should return SUCCESS when majority is reached (3 of 5)")
        void shouldSucceedWithMajority() {
            TestInitializationContext ctx = new TestInitializationContext(3, 1, 1);
            assertThat(policy.evaluate(ctx)).isEqualTo(Decision.SUCCESS);
        }

        @Test
        @DisplayName("Should return SUCCESS when all are available")
        void shouldSucceedWhenAllAvailable() {
            TestInitializationContext ctx = new TestInitializationContext(5, 0, 0);
            assertThat(policy.evaluate(ctx)).isEqualTo(Decision.SUCCESS);
        }

        @Test
        @DisplayName("Should return SUCCESS with exact majority (2 of 3)")
        void shouldSucceedWithExactMajority() {
            TestInitializationContext ctx = new TestInitializationContext(2, 1, 0);
            assertThat(policy.evaluate(ctx)).isEqualTo(Decision.SUCCESS);
        }

        @Test
        @DisplayName("Should return SUCCESS with single connection out of one")
        void shouldSucceedWithSingleConnection() {
            TestInitializationContext ctx = new TestInitializationContext(1, 0, 0);
            assertThat(policy.evaluate(ctx)).isEqualTo(Decision.SUCCESS);
        }

        @Test
        @DisplayName("Should return FAIL when majority is impossible (2 failed of 3)")
        void shouldFailWhenMajorityImpossible() {
            TestInitializationContext ctx = new TestInitializationContext(0, 2, 1);
            assertThat(policy.evaluate(ctx)).isEqualTo(Decision.FAIL);
        }

        @Test
        @DisplayName("Should return FAIL when all connections fail")
        void shouldFailWhenAllFail() {
            TestInitializationContext ctx = new TestInitializationContext(0, 5, 0);
            assertThat(policy.evaluate(ctx)).isEqualTo(Decision.FAIL);
        }

        @Test
        @DisplayName("Should return FAIL when majority not reached and no pending (1 of 3)")
        void shouldFailWhenMajorityNotReachedNoPending() {
            TestInitializationContext ctx = new TestInitializationContext(1, 2, 0);
            assertThat(policy.evaluate(ctx)).isEqualTo(Decision.FAIL);
        }

        @Test
        @DisplayName("Should return CONTINUE when majority possible but not yet reached")
        void shouldContinueWhenMajorityPossible() {
            TestInitializationContext ctx = new TestInitializationContext(1, 1, 3);
            assertThat(policy.evaluate(ctx)).isEqualTo(Decision.CONTINUE);
        }

        @Test
        @DisplayName("Should return CONTINUE when all pending")
        void shouldContinueWhenAllPending() {
            TestInitializationContext ctx = new TestInitializationContext(0, 0, 5);
            assertThat(policy.evaluate(ctx)).isEqualTo(Decision.CONTINUE);
        }

        @Test
        @DisplayName("Should return SUCCESS early when majority reached with pending (3 of 5, 2 pending)")
        void shouldSucceedEarlyWithMajority() {
            TestInitializationContext ctx = new TestInitializationContext(3, 0, 2);
            assertThat(policy.evaluate(ctx)).isEqualTo(Decision.SUCCESS);
        }

        @Test
        @DisplayName("Should return FAIL early when majority impossible (3 failed of 5, 2 pending)")
        void shouldFailEarlyWhenMajorityImpossible() {
            TestInitializationContext ctx = new TestInitializationContext(0, 3, 2);
            assertThat(policy.evaluate(ctx)).isEqualTo(Decision.FAIL);
        }

        @Test
        @DisplayName("Should handle even number of connections (2 of 4 required)")
        void shouldHandleEvenNumberOfConnections() {
            // For 4 connections, majority is 3 (4/2 + 1)
            TestInitializationContext ctx = new TestInitializationContext(3, 1, 0);
            assertThat(policy.evaluate(ctx)).isEqualTo(Decision.SUCCESS);
        }

        @Test
        @DisplayName("Should fail with even split (2 of 4)")
        void shouldFailWithEvenSplit() {
            // For 4 connections, 2 available is not majority (need 3)
            TestInitializationContext ctx = new TestInitializationContext(2, 2, 0);
            assertThat(policy.evaluate(ctx)).isEqualTo(Decision.FAIL);
        }

        @Test
        @DisplayName("Should handle two connections (2 of 2)")
        void shouldHandleTwoConnections() {
            TestInitializationContext ctx = new TestInitializationContext(2, 0, 0);
            assertThat(policy.evaluate(ctx)).isEqualTo(Decision.SUCCESS);
        }

        @Test
        @DisplayName("Should fail with one of two connections")
        void shouldFailWithOneOfTwo() {
            TestInitializationContext ctx = new TestInitializationContext(1, 1, 0);
            assertThat(policy.evaluate(ctx)).isEqualTo(Decision.FAIL);
        }

    }

    @Nested
    @DisplayName("ONE_AVAILABLE Policy Tests")
    class OneAvailablePolicyTests {

        private final InitializationPolicy policy = InitializationPolicy.BuiltIn.ONE_AVAILABLE;

        @Test
        @DisplayName("Should return SUCCESS when one connection is available")
        void shouldSucceedWithOneAvailable() {
            TestInitializationContext ctx = new TestInitializationContext(1, 2, 0);
            assertThat(policy.evaluate(ctx)).isEqualTo(Decision.SUCCESS);
        }

        @Test
        @DisplayName("Should return SUCCESS when all connections are available")
        void shouldSucceedWhenAllAvailable() {
            TestInitializationContext ctx = new TestInitializationContext(5, 0, 0);
            assertThat(policy.evaluate(ctx)).isEqualTo(Decision.SUCCESS);
        }

        @Test
        @DisplayName("Should return SUCCESS early with one available and pending")
        void shouldSucceedEarlyWithOneAvailable() {
            TestInitializationContext ctx = new TestInitializationContext(1, 0, 4);
            assertThat(policy.evaluate(ctx)).isEqualTo(Decision.SUCCESS);
        }

        @Test
        @DisplayName("Should return SUCCESS with multiple available")
        void shouldSucceedWithMultipleAvailable() {
            TestInitializationContext ctx = new TestInitializationContext(3, 2, 0);
            assertThat(policy.evaluate(ctx)).isEqualTo(Decision.SUCCESS);
        }

        @Test
        @DisplayName("Should return FAIL when all connections fail")
        void shouldFailWhenAllFail() {
            TestInitializationContext ctx = new TestInitializationContext(0, 5, 0);
            assertThat(policy.evaluate(ctx)).isEqualTo(Decision.FAIL);
        }

        @Test
        @DisplayName("Should return FAIL when single connection fails")
        void shouldFailWithSingleFailure() {
            TestInitializationContext ctx = new TestInitializationContext(0, 1, 0);
            assertThat(policy.evaluate(ctx)).isEqualTo(Decision.FAIL);
        }

        @Test
        @DisplayName("Should return CONTINUE when all connections are pending")
        void shouldContinueWhenAllPending() {
            TestInitializationContext ctx = new TestInitializationContext(0, 0, 5);
            assertThat(policy.evaluate(ctx)).isEqualTo(Decision.CONTINUE);
        }

        @Test
        @DisplayName("Should return CONTINUE when some failed but some pending")
        void shouldContinueWithFailedAndPending() {
            TestInitializationContext ctx = new TestInitializationContext(0, 2, 3);
            assertThat(policy.evaluate(ctx)).isEqualTo(Decision.CONTINUE);
        }

    }

}
