package io.lettuce.scenario;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;
import reactor.netty.http.client.HttpClient;

/**
 * Async Fault Injection Client using reactor.netty.http.client.HttpClient.
 */
public class FaultInjectionClient {

    private static final String BASE_URL;

    static {
        BASE_URL = System.getenv().getOrDefault("FAULT_INJECTION_API_URL", "http://127.0.0.1:20324");
    }

    private static final Logger log = LoggerFactory.getLogger(FaultInjectionClient.class);

    private final HttpClient httpClient;

    private final ObjectMapper objectMapper;

    public FaultInjectionClient() {
        this.httpClient = HttpClient.create().responseTimeout(Duration.ofSeconds(10));

        this.objectMapper = new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

    public static class TriggerActionResponse {

        @JsonProperty("action_id")
        private String actionId;

        // Default constructor for Jackson
        public TriggerActionResponse() {
        }

        public TriggerActionResponse(String actionId) {
            this.actionId = actionId;
        }

        public String getActionId() {
            return actionId;
        }

        public void setActionId(String actionId) {
            this.actionId = actionId;
        }

    }

    /**
     * Triggers an action with the specified type and parameters.
     *
     * @param actionType the type of action to trigger
     * @param parameters the parameters for the action
     * @return a Mono that emits the TriggerActionResponse when the action is triggered
     */
    public Mono<TriggerActionResponse> triggerAction(String actionType, Map<String, Object> parameters) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", actionType);
        payload.put("parameters", parameters);

        try {
            String jsonString = objectMapper.writeValueAsString(payload);
            byte[] bytes = jsonString.getBytes();
            ByteBuf byteBuf = Unpooled.wrappedBuffer(bytes);

            return httpClient.headers(h -> h.add("Content-Type", "application/json")).post().uri(BASE_URL + "/action")
                    .send(ByteBufFlux.fromInbound(Mono.just(byteBuf))).responseSingle((response, body) -> body.asString())
                    .map(result -> {
                        log.info("Trigger action response: {}", result);
                        try {
                            return objectMapper.readValue(result, TriggerActionResponse.class);
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to parse response", e);
                        }
                    }).onErrorResume(e -> {
                        log.error("Failed to trigger action", e);
                        return Mono.error(new RuntimeException("Failed to trigger action", e));
                    });
        } catch (Exception e) {
            log.error("Failed to serialize request", e);
            return Mono.error(new RuntimeException("Failed to serialize request", e));
        }
    }

    /**
     * Checks the status of an action.
     *
     * @param actionId the ID of the action to check
     * @return Mono that emits true if the action is completed, empty if still in progress
     */
    private Mono<Boolean> checkActionStatus(String actionId) {
        return httpClient.get().uri(BASE_URL + "/action/" + actionId).responseSingle((response, body) -> body.asString())
                .flatMap(result -> {
                    log.info("Action status: {}", result);
                    if (result.contains("success")) {
                        return Mono.just(true);
                    }
                    // Return empty to trigger retry
                    return Mono.empty();
                })
                .retryWhen(reactor.util.retry.Retry.backoff(3, Duration.ofMillis(300)).maxBackoff(Duration.ofSeconds(2))
                        .filter(throwable -> !(throwable instanceof RuntimeException && throwable.getMessage() != null
                                && throwable.getMessage().contains("Fault injection proxy error")))
                        .doBeforeRetry(retrySignal -> log.warn("Retrying action status check after error, attempt: {}",
                                retrySignal.totalRetries() + 1)))
                .onErrorResume(e -> {
                    log.error("Fault injection proxy error after retries", e);
                    return Mono.error(new RuntimeException("Fault injection proxy error", e));
                });
    }

    /**
     * Waits for an action to complete by polling the status endpoint. Uses Reactor's retry capabilities for a more idiomatic
     * approach.
     *
     * @param actionId the ID of the action to wait for
     * @param checkInterval interval between status checks
     * @param delayAfter delay after completion before returning true
     * @param timeout maximum time to wait for completion
     * @return Mono that completes with true when the action is completed and the delay has passed
     */
    public Mono<Boolean> waitForCompletion(String actionId, Duration checkInterval, Duration delayAfter, Duration timeout) {
        return Mono.defer(() -> checkActionStatus(actionId)).flatMap(completed -> {
            if (completed) {
                // If we need to wait after completion, delay and then return true
                if (!delayAfter.isZero()) {
                    return Mono.delay(delayAfter).thenReturn(true);
                }
                return Mono.just(true);
            }
            return Mono.just(false);
        }).repeatWhenEmpty(repeat -> repeat.delayElements(checkInterval).timeout(timeout)
                .doOnError(e -> log.error("Timeout waiting for action to complete", e)));
    }

    /**
     * Triggers an action and waits for it to complete.
     *
     * @param actionType the type of action to trigger
     * @param parameters the parameters for the action
     * @param checkInterval interval between status checks
     * @param delayAfter delay after completion before returning true
     * @param timeout maximum time to wait for completion
     * @return a Mono that emits true when the action is completed
     */
    public Mono<Boolean> triggerActionAndWait(String actionType, Map<String, Object> parameters, Duration checkInterval,
            Duration delayAfter, Duration timeout) {
        return triggerAction(actionType, parameters)
                .flatMap(response -> waitForCompletion(response.getActionId(), checkInterval, delayAfter, timeout));
    }

    /**
     * Executes an rladmin command via the fault injector with enhanced error handling.
     *
     * @param bdbId the BDB ID to execute the command on
     * @param rladminCommand the rladmin command to execute
     * @param checkInterval interval between status checks
     * @param timeout maximum time to wait for completion
     * @return a Mono that emits true when the command is completed
     */
    public Mono<Boolean> executeRladminCommand(String bdbId, String rladminCommand, Duration checkInterval, Duration timeout) {
        // Validate parameters
        if (bdbId == null || bdbId.trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("BDB ID cannot be null or empty"));
        }
        if (rladminCommand == null || rladminCommand.trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("Rladmin command cannot be null or empty"));
        }

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("bdb_id", bdbId);
        parameters.put("rladmin_command", rladminCommand);

        log.info("Executing rladmin command: {} on BDB: {}", rladminCommand, bdbId);

        return triggerAction("execute_rladmin_command", parameters)
                .flatMap(response -> waitForRladminCompletion(response.getActionId(), rladminCommand, checkInterval, timeout))
                .onErrorResume(e -> {
                    log.error("Failed to execute rladmin command '{}' on BDB '{}': {}", rladminCommand, bdbId, e.getMessage());
                    return Mono.error(new RuntimeException("Rladmin command execution failed: " + e.getMessage(), e));
                });
    }

    /**
     * Executes an rladmin command with default timing parameters.
     */
    public Mono<Boolean> executeRladminCommand(String bdbId, String rladminCommand) {
        return executeRladminCommand(bdbId, rladminCommand, Duration.ofSeconds(2), Duration.ofSeconds(60));
    }

    /**
     * Enhanced status checking for rladmin operations with command-specific validation.
     */
    private Mono<Boolean> waitForRladminCompletion(String actionId, String rladminCommand, Duration checkInterval,
            Duration timeout) {
        return Mono.defer(() -> checkRladminActionStatus(actionId, rladminCommand)).flatMap(completed -> {
            if (completed) {
                log.info("Rladmin command '{}' completed successfully", rladminCommand);
                return Mono.just(true);
            }
            return Mono.just(false);
        }).repeatWhenEmpty(repeat -> repeat.delayElements(checkInterval).timeout(timeout)
                .doOnError(e -> log.error("Timeout waiting for rladmin command '{}' to complete", rladminCommand)));
    }

    /**
     * Checks the status of an rladmin action with command-specific validation.
     */
    private Mono<Boolean> checkRladminActionStatus(String actionId, String rladminCommand) {
        return httpClient.get().uri(BASE_URL + "/action/" + actionId).responseSingle((response, body) -> body.asString())
                .flatMap(result -> {
                    log.debug("Rladmin action '{}' status: {}", rladminCommand, result);

                    try {
                        // Properly parse JSON response using ObjectMapper
                        com.fasterxml.jackson.databind.JsonNode statusResponse = objectMapper.readTree(result);
                        String status = statusResponse.has("status") ? statusResponse.get("status").asText() : null;
                        String error = statusResponse.has("error") && !statusResponse.get("error").isNull() 
                            ? statusResponse.get("error").asText() : null;

                        log.debug("Parsed status: {}, error: {}", status, error);

                        if ("success".equals(status) || "completed".equals(status)) {
                            return validateRladminCommandCompletion(rladminCommand, result);
                        }

                        if ("failed".equals(status) || error != null) {
                            return Mono.error(new RuntimeException("Rladmin command failed: status=" + status + ", error=" + error));
                        }

                        if ("pending".equals(status)) {
                            log.debug("Command '{}' still pending, will retry...", rladminCommand);
                            return Mono.empty(); // Trigger retry
                        }

                        // Unknown status, log and retry
                        log.warn("Unknown status '{}' for command '{}', will retry...", status, rladminCommand);
                        return Mono.empty();

                    } catch (Exception e) {
                        log.error("Failed to parse action status response: {}", result, e);
                        return Mono.error(new RuntimeException("Failed to parse action status: " + e.getMessage()));
                    }
                })
                .retryWhen(reactor.util.retry.Retry.backoff(5, Duration.ofMillis(500)).maxBackoff(Duration.ofSeconds(5))
                        .filter(throwable -> !(throwable instanceof RuntimeException && throwable.getMessage() != null
                                && throwable.getMessage().contains("failed")))
                        .doBeforeRetry(retrySignal -> log.debug("Retrying rladmin status check for '{}', attempt: {}",
                                rladminCommand, retrySignal.totalRetries() + 1)));
    }

    /**
     * Validates command-specific completion criteria.
     */
    private Mono<Boolean> validateRladminCommandCompletion(String rladminCommand, String statusResult) {
        // Command-specific validation logic
        if (rladminCommand.contains("migrate")) {
            // For migration commands, verify migration status
            if (statusResult.contains("migration_completed") || statusResult.contains("success")) {
                return Mono.just(true);
            }
        } else if (rladminCommand.contains("failover")) {
            // For failover commands, verify failover status
            if (statusResult.contains("failover_completed") || statusResult.contains("success")) {
                return Mono.just(true);
            }
        } else if (rladminCommand.contains("bind endpoint")) {
            // For endpoint bind commands, verify endpoint status
            if (statusResult.contains("endpoint_bound") || statusResult.contains("success")) {
                return Mono.just(true);
            }
        }

        // Default success check
        return Mono.just(statusResult.contains("success"));
    }

    /**
     * Triggers endpoint rebind to generate MOVING notification with comprehensive validation. This operation migrates shards
     * out of primary node and rebinds the endpoint.
     *
     * @param bdbId the BDB ID
     * @param endpointId the endpoint ID to rebind
     * @param policy the policy to use for rebinding (single, all-master-shards, etc.)
     * @return a Mono that emits true when the operation is completed
     */
    public Mono<Boolean> triggerEndpointRebind(String bdbId, String endpointId, String policy) {
        // Enhanced parameter validation
        if (endpointId == null || endpointId.trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("Endpoint ID cannot be null or empty"));
        }
        if (policy == null || policy.trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("Policy cannot be null or empty"));
        }

        // Validate policy is one of the expected values
        if (!policy.matches("single|all-master-shards|all-nodes")) {
            log.warn("Policy '{}' may not be valid. Expected: single, all-master-shards, or all-nodes", policy);
        }

        String rebindCommand = String.format("bind endpoint %s policy %s", endpointId, policy);
        log.info("Triggering endpoint rebind: endpoint {} with policy {} on BDB {}", endpointId, policy, bdbId);

        // Endpoint operations typically need more time (up to 2 minutes)
        return executeRladminCommand(bdbId, rebindCommand, Duration.ofSeconds(3), Duration.ofSeconds(120))
                .doOnSuccess(success -> log.info("Endpoint rebind completed for endpoint {} on BDB {}", endpointId, bdbId))
                .doOnError(error -> log.error("Endpoint rebind failed for endpoint {} on BDB {}: {}", endpointId, bdbId,
                        error.getMessage()));
    }

    /**
     * Triggers shard migration to generate MIGRATING/MIGRATED notifications with enhanced tracking.
     *
     * @param bdbId the BDB ID
     * @param shardId the shard ID to migrate (optional, if null migrates all shards)
     * @return a Mono that emits true when the migration is initiated
     */
    public Mono<Boolean> triggerShardMigration(String bdbId, String shardId) {
        String migrateCommand;
        Duration operationTimeout;

        if (shardId != null && !shardId.trim().isEmpty()) {
            // Validate shard ID format (should be numeric)
            try {
                Integer.parseInt(shardId);
            } catch (NumberFormatException e) {
                return Mono.error(new IllegalArgumentException("Shard ID must be numeric: " + shardId));
            }

            migrateCommand = String.format("migrate shard %s", shardId);
            operationTimeout = Duration.ofSeconds(120); // Single shard migration (up to 2 minutes)
            log.info("Triggering migration for shard {} on BDB {}", shardId, bdbId);
        } else {
            migrateCommand = "migrate";
            operationTimeout = Duration.ofSeconds(300); // All shards migration takes longer
            log.info("Triggering migration for all shards on BDB {}", bdbId);
        }

        return executeRladminCommand(bdbId, migrateCommand, Duration.ofSeconds(5), operationTimeout)
                .doOnSuccess(success -> log.info("Shard migration completed for {} on BDB {}",
                        shardId != null ? "shard " + shardId : "all shards", bdbId))
                .doOnError(error -> log.error("Shard migration failed for {} on BDB {}: {}",
                        shardId != null ? "shard " + shardId : "all shards", bdbId, error.getMessage()));
    }

    /**
     * Triggers shard failover to generate FAILING_OVER/FAILED_OVER notifications with enhanced monitoring.
     *
     * @param bdbId the BDB ID
     * @param shardId the shard ID to failover (optional, if null fails over all shards)
     * @return a Mono that emits true when the failover is initiated
     */
    public Mono<Boolean> triggerShardFailover(String bdbId, String shardId) {
        String failoverCommand;
        Duration operationTimeout;

        if (shardId != null && !shardId.trim().isEmpty()) {
            // Validate shard ID format
            try {
                Integer.parseInt(shardId);
            } catch (NumberFormatException e) {
                return Mono.error(new IllegalArgumentException("Shard ID must be numeric: " + shardId));
            }

            failoverCommand = String.format("failover shard %s", shardId);
            operationTimeout = Duration.ofSeconds(120); // Single shard failover (up to 2 minutes)
            log.info("Triggering failover for shard {} on BDB {}", shardId, bdbId);
        } else {
            failoverCommand = "failover";
            operationTimeout = Duration.ofSeconds(360); // All shards failover takes longer
            log.info("Triggering failover for all shards on BDB {}", bdbId);
        }

        return executeRladminCommand(bdbId, failoverCommand, Duration.ofSeconds(3), operationTimeout)
                .doOnSuccess(success -> log.info("Shard failover completed for {} on BDB {}",
                        shardId != null ? "shard " + shardId : "all shards", bdbId))
                .doOnError(error -> log.error("Shard failover failed for {} on BDB {}: {}",
                        shardId != null ? "shard " + shardId : "all shards", bdbId, error.getMessage()));
    }

    /**
     * Advanced method to trigger a sequence of maintenance operations for comprehensive testing.
     *
     * @param bdbId the BDB ID
     * @param operations list of operations to execute in sequence
     * @return a Mono that emits true when all operations complete
     */
    public Mono<Boolean> triggerMaintenanceSequence(String bdbId, List<MaintenanceOperation> operations) {
        if (operations == null || operations.isEmpty()) {
            return Mono.error(new IllegalArgumentException("Operations list cannot be null or empty"));
        }

        log.info("Starting maintenance sequence with {} operations on BDB {}", operations.size(), bdbId);

        return Flux.fromIterable(operations).concatMap(operation -> {
            log.info("Executing maintenance operation: {}", operation);
            return executeMaintenanceOperation(bdbId, operation).delayElement(Duration.ofSeconds(2)); // Brief delay between
                                                                                                      // operations
        }).then(Mono.just(true)).doOnSuccess(success -> log.info("Maintenance sequence completed on BDB {}", bdbId))
                .doOnError(error -> log.error("Maintenance sequence failed on BDB {}: {}", bdbId, error.getMessage()));
    }

    /**
     * Executes a single maintenance operation based on its type.
     */
    private Mono<Boolean> executeMaintenanceOperation(String bdbId, MaintenanceOperation operation) {
        switch (operation.getType()) {
            case ENDPOINT_REBIND:
                return triggerEndpointRebind(bdbId, operation.getEndpointId(), operation.getPolicy());
            case SHARD_MIGRATION:
                return triggerShardMigration(bdbId, operation.getShardId());
            case SHARD_FAILOVER:
                return triggerShardFailover(bdbId, operation.getShardId());
            default:
                return Mono.error(new IllegalArgumentException("Unknown operation type: " + operation.getType()));
        }
    }

    /**
     * Enum for maintenance operation types.
     */
    public enum MaintenanceOperationType {
        ENDPOINT_REBIND, SHARD_MIGRATION, SHARD_FAILOVER
    }

    /**
     * Class representing a maintenance operation.
     */
    public static class MaintenanceOperation {

        private final MaintenanceOperationType type;

        private final String endpointId;

        private final String policy;

        private final String shardId;

        public MaintenanceOperation(MaintenanceOperationType type, String endpointId, String policy) {
            this.type = type;
            this.endpointId = endpointId;
            this.policy = policy;
            this.shardId = null;
        }

        public MaintenanceOperation(MaintenanceOperationType type, String shardId) {
            this.type = type;
            this.endpointId = null;
            this.policy = null;
            this.shardId = shardId;
        }

        public MaintenanceOperationType getType() {
            return type;
        }

        public String getEndpointId() {
            return endpointId;
        }

        public String getPolicy() {
            return policy;
        }

        public String getShardId() {
            return shardId;
        }

        @Override
        public String toString() {
            switch (type) {
                case ENDPOINT_REBIND:
                    return String.format("EndpointRebind(endpoint=%s, policy=%s)", endpointId, policy);
                case SHARD_MIGRATION:
                    return String.format("ShardMigration(shard=%s)", shardId);
                case SHARD_FAILOVER:
                    return String.format("ShardFailover(shard=%s)", shardId);
                default:
                    return type.toString();
            }
        }

    }

}
