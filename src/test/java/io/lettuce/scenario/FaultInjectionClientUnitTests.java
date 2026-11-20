package io.lettuce.scenario;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.scenario.FaultInjectionClient.ActionStatus;

/**
 * Unit tests for how {@link FaultInjectionClient} reads the {@code /action/{id}} status response.
 * <p>
 * The scenario tests poll that endpoint until an action finishes. These pin the one fact that decides whether a poll loop ends:
 * a {@code failed} status is terminal and must not be mistaken for "still running".
 */
@Tag(UNIT_TEST)
class FaultInjectionClientUnitTests {

    private final FaultInjectionClient client = new FaultInjectionClient();

    @Test
    void runningStatusIsNeitherSuccessNorFailure() {
        ActionStatus status = client
                .parseActionStatus("{\"status\":\"running\",\"error\":null,\"traceback\":null,\"output\":null}");

        assertThat(status.isSuccess()).isFalse();
        assertThat(status.isFailure()).isFalse();
    }

    @Test
    void successStatusIsSuccess() {
        ActionStatus status = client
                .parseActionStatus("{\"status\":\"success\",\"error\":null,\"traceback\":null,\"output\":{\"total_nodes\":2}}");

        assertThat(status.isSuccess()).isTrue();
        assertThat(status.isFailure()).isFalse();
    }

    @Test
    void failedStatusIsFailureAndKeepsTheError() {
        ActionStatus status = client.parseActionStatus("{\"status\":\"failed\",\"error\":\"ProcessRestartException: "
                + "Failed to restart process 'dmcproxy' on node 1: Process did not restart after kill\",\"traceback\":\"...\"}");

        assertThat(status.isFailure()).isTrue();
        assertThat(status.isSuccess()).isFalse();
        assertThat(status.getError()).contains("dmcproxy").contains("did not restart");
    }

    @Test
    void errorWithoutFailedStatusIsStillFailure() {
        ActionStatus status = client.parseActionStatus("{\"status\":\"running\",\"error\":\"boom\"}");

        assertThat(status.isFailure()).isTrue();
    }

    @Test
    void successOutputMentioningFailureIsNotFailure() {
        ActionStatus status = client.parseActionStatus(
                "{\"status\":\"success\",\"error\":null,\"output\":{\"command\":\"failover shard 1\",\"output\":\"failed over\"}}");

        assertThat(status.isSuccess()).isTrue();
        assertThat(status.isFailure()).isFalse();
    }

    @Test
    void unparsableBodyKeepsPolling() {
        assertThat(client.parseActionStatus("<html>502 Bad Gateway</html>").isFailure()).isFalse();
        assertThat(client.parseActionStatus("<html>502 Bad Gateway</html>").isSuccess()).isFalse();
        assertThat(client.parseActionStatus("").isFailure()).isFalse();
        assertThat(client.parseActionStatus("[]").isFailure()).isFalse();
    }

    @Test
    void actionFailedExceptionCarriesTheDetails() {
        ActionStatus status = client.parseActionStatus("{\"status\":\"failed\",\"error\":\"boom\"}");

        FaultInjectionClient.ActionFailedException e = new FaultInjectionClient.ActionFailedException("abc-123", status);

        assertThat(e.getActionId()).isEqualTo("abc-123");
        assertThat(e.getStatus()).isEqualTo("failed");
        assertThat(e.getError()).isEqualTo("boom");
        assertThat(e).hasMessageContaining("abc-123").hasMessageContaining("boom");
    }

}
