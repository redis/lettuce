package io.lettuce.scenario;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
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

    // Timeout constants
    private static final Duration HTTP_CLIENT_TIMEOUT = Duration.ofMinutes(5); // 300 seconds

    private static final Duration LONG_OPERATION_TIMEOUT = Duration.ofMinutes(5); // 300 seconds - for migrations/failovers

    private static final Duration MEDIUM_OPERATION_TIMEOUT = Duration.ofMinutes(3); // 180 seconds - for emptying nodes

    private static final Duration ENDPOINT_OPERATION_TIMEOUT = Duration.ofMinutes(2); // 120 seconds - for endpoint operations

    private static final Duration SHORT_OPERATION_TIMEOUT = Duration.ofMinutes(1); // 60 seconds - for simple commands

    private static final Duration STABILIZATION_DELAY = Duration.ofSeconds(10); // Wait for cluster to stabilize

    private static final Duration CHECK_INTERVAL_LONG = Duration.ofSeconds(1); // Check interval for long operations - reduced
                                                                               // for faster notification detection

    private static final Duration CHECK_INTERVAL_MEDIUM = Duration.ofSeconds(3); // Check interval for medium operations

    private static final Duration CHECK_INTERVAL_SHORT = Duration.ofSeconds(2); // Check interval for short operations

    private static final Duration OPERATION_DELAY = Duration.ofSeconds(2); // Brief delay between operations

    private static final Duration RETRY_MAX_BACKOFF = Duration.ofSeconds(5); // Maximum backoff for retries

    private final HttpClient httpClient;

    private final ObjectMapper objectMapper;

    public FaultInjectionClient() {
        // Increase HTTP client timeout to accommodate long-running operations like failover (up to 5 minutes)
        this.httpClient = HttpClient.create().responseTimeout(HTTP_CLIENT_TIMEOUT);

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
                .retryWhen(reactor.util.retry.Retry.backoff(3, Duration.ofMillis(300)).maxBackoff(RETRY_MAX_BACKOFF)
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
        }).repeatWhenEmpty(repeat -> repeat.delayElements(checkInterval)).timeout(timeout)
                .doOnError(e -> log.error("Timeout waiting for action to complete", e));
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
        return executeRladminCommand(bdbId, rladminCommand, CHECK_INTERVAL_SHORT, SHORT_OPERATION_TIMEOUT);
    }

    /**
     * Enhanced status checking for rladmin operations with command-specific validation.
     */
    private Mono<Boolean> waitForRladminCompletion(String actionId, String rladminCommand, Duration checkInterval,
            Duration timeout) {
        log.info("Starting polling for '{}' with timeout {}s", rladminCommand, timeout.getSeconds());

        return Mono.defer(() -> {
            log.debug("Checking status for '{}'", rladminCommand);
            return checkRladminActionStatus(actionId, rladminCommand);
        }).flatMap(completed -> {
            if (completed) {
                log.info("Command '{}' completed successfully!", rladminCommand);
                return Mono.just(true);
            }
            log.debug("Command '{}' not yet complete, will retry after {}s", rladminCommand, checkInterval.getSeconds());
            return Mono.just(false);
        }).repeatWhenEmpty(repeat -> repeat.delayElements(checkInterval)).timeout(timeout)
                .doOnError(e -> log.error("Timeout waiting for '{}' to complete: {}", rladminCommand, e.getMessage()));
    }

    /**
     * Checks the status of an rladmin action with command-specific validation.
     */
    private Mono<Boolean> checkRladminActionStatus(String actionId, String rladminCommand) {
        return httpClient.get().uri(BASE_URL + "/action/" + actionId).responseSingle((response, body) -> body.asString())
                .flatMap(result -> {
                    log.debug("Got HTTP response for '{}', raw result: '{}'", rladminCommand, result);

                    try {
                        // Properly parse JSON response using ObjectMapper
                        log.debug("Parsing JSON for '{}' - Raw response: '{}'", rladminCommand, result);
                        JsonNode statusResponse = objectMapper.readTree(result);
                        String status = statusResponse.has("status") ? statusResponse.get("status").asText() : null;
                        String error = statusResponse.has("error") && !statusResponse.get("error").isNull()
                                ? statusResponse.get("error").asText()
                                : null;

                        log.debug("Parsed JSON for '{}' - status='{}', error='{}'", rladminCommand, status, error);

                        if ("success".equals(status) || "completed".equals(status)) {
                            log.debug("Status indicates SUCCESS for '{}'", rladminCommand);
                            return Mono.just(true);
                        }

                        if ("failed".equals(status) || error != null) {
                            log.error("Status indicates FAILURE for '{}' - status='{}', error='{}'", rladminCommand, status,
                                    error);
                            return Mono.error(
                                    new RuntimeException("Rladmin command failed: status=" + status + ", error=" + error));
                        }

                        if ("running".equals(status)) {
                            log.debug("Status is {} for '{}', returning empty to trigger retry",
                                    status != null ? status.toUpperCase() : "NULL", rladminCommand);
                            return Mono.empty(); // Trigger retry
                        }

                        // Unknown status, log and retry
                        log.warn("UNKNOWN status '{}' for '{}', returning empty to trigger retry", status, rladminCommand);
                        return Mono.empty();

                    } catch (Exception e) {
                        log.error("EXCEPTION parsing JSON for '{}': {}", rladminCommand, e.getMessage(), e);
                        return Mono.error(new RuntimeException("Failed to parse action status: " + e.getMessage()));
                    }
                })
                .retryWhen(reactor.util.retry.Retry.backoff(5, Duration.ofMillis(500)).maxBackoff(RETRY_MAX_BACKOFF)
                        .filter(throwable -> !(throwable instanceof RuntimeException && throwable.getMessage() != null
                                && throwable.getMessage().contains("failed")))
                        .doBeforeRetry(retrySignal -> log.debug("Retrying rladmin status check for '{}', attempt: {}",
                                rladminCommand, retrySignal.totalRetries() + 1)));
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
        return executeRladminCommand(bdbId, rebindCommand, CHECK_INTERVAL_MEDIUM, ENDPOINT_OPERATION_TIMEOUT)
                .doOnSuccess(success -> log.info("Endpoint rebind completed for endpoint {} on BDB {}", endpointId, bdbId))
                .doOnError(error -> log.error("Endpoint rebind failed for endpoint {} on BDB {}: {}", endpointId, bdbId,
                        error.getMessage()));
    }

    /**
     * Triggers shard migration to generate MIGRATING/MIGRATED notifications with enhanced tracking.
     * 
     * @param bdbId the BDB ID
     * @param shardId the shard ID to migrate (used to determine which node to migrate from)
     * @param sourceNode the source node ID to migrate from
     * @param targetNode the target node ID to migrate to
     * @return a Mono that emits true when the migration is initiated
     */
    public Mono<Boolean> triggerShardMigration(String bdbId, String shardId, String sourceNode, String targetNode) {
        // Enhanced parameter validation
        if (sourceNode == null || sourceNode.trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("Source node cannot be null or empty"));
        }
        if (targetNode == null || targetNode.trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("Target node cannot be null or empty"));
        }

        if (shardId != null && !shardId.trim().isEmpty()) {
            // Validate shard ID format (should be numeric)
            try {
                Integer.parseInt(shardId);
            } catch (NumberFormatException e) {
                return Mono.error(new IllegalArgumentException("Shard ID must be numeric: " + shardId));
            }
        }

        // Use proper migrate command format: migrate node <source> all_shards target_node <target>
        String migrateCommand = String.format("migrate node %s all_shards target_node %s", sourceNode, targetNode);
        Duration operationTimeout = LONG_OPERATION_TIMEOUT; // Node migration (up to 5 minutes for complex operations)

        log.info("Triggering migration from node {} to node {} on BDB {} (triggered by shard {})", sourceNode, targetNode,
                bdbId, shardId != null ? shardId : "all");

        return executeRladminCommand(bdbId, migrateCommand, CHECK_INTERVAL_LONG, operationTimeout)
                .doOnSuccess(success -> log.info("Shard migration completed from node {} to node {} on BDB {}", sourceNode,
                        targetNode, bdbId))
                .doOnError(error -> log.error("Shard migration failed from node {} to node {} on BDB {}: {}", sourceNode,
                        targetNode, bdbId, error.getMessage()));
    }

    /**
     * Triggers shard failover to generate FAILING_OVER/FAILED_OVER notifications with enhanced monitoring and dynamic shard
     * discovery.
     * 
     * @param bdbId the BDB ID
     * @param shardId the shard ID to failover (used to determine which node to failover)
     * @param nodeId the specific node ID to failover
     * @param redisEnterpriseConfig the configuration to get shard information from
     * @return a Mono that emits true when the failover is initiated
     */
    public Mono<Boolean> triggerShardFailover(String bdbId, String shardId, String nodeId,
            RedisEnterpriseConfig redisEnterpriseConfig) {
        // Enhanced parameter validation
        if (nodeId == null || nodeId.trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("Node ID cannot be null or empty"));
        }

        if (redisEnterpriseConfig == null) {
            return Mono.error(new IllegalArgumentException("RedisEnterpriseConfig cannot be null"));
        }

        // Use proper failover command format: failover shard <shard_id>
        // Dynamically get the shard IDs for the target node
        List<String> shardIds = redisEnterpriseConfig.getShardsForNode("node:" + nodeId);

        log.info("*** DYNAMIC DISCOVERY DEBUG: nodeId='{}', looking for shards on 'node:{}'", nodeId, nodeId);
        log.info("*** DYNAMIC DISCOVERY DEBUG: getShardsForNode returned: {}", shardIds);

        if (shardIds.isEmpty()) {
            log.warn("No shards found for node:{}, cannot perform failover", nodeId);
            // Let's also debug the full config state
            log.warn("*** DEBUG: Full config state: {}", redisEnterpriseConfig.getSummary());
            return Mono.just(false);
        }

        // Build command with all shard IDs for the node
        String shardIdList = String.join(" ", shardIds);
        String failoverCommand = "failover shard " + shardIdList;
        Duration operationTimeout = LONG_OPERATION_TIMEOUT; // Node failover (up to 5 minutes for complex operations)

        log.info("*** FAILOVER COMMAND DEBUG: Final command = '{}'", failoverCommand);
        log.info("Triggering failover for node {} on BDB {} with {} shards: {}", nodeId, bdbId, shardIds.size(), shardIdList);

        return executeRladminCommand(bdbId, failoverCommand, CHECK_INTERVAL_MEDIUM, operationTimeout)
                .doOnSuccess(success -> log.info("Shard failover completed for node {} on BDB {}", nodeId, bdbId))
                .doOnError(error -> log.error("Shard failover failed for node {} on BDB {}: {}", nodeId, bdbId,
                        error.getMessage()));
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

    /**
     * Triggers a MOVING notification by automatically determining the optimal source and target nodes based on the endpoint's
     * current binding. This ensures the endpoint will need to be rebound after migration, triggering the MOVING notification.
     *
     * @param bdbId the BDB ID
     * @param endpointId the endpoint ID to rebind
     * @param policy the policy to use for rebinding (typically "single")
     * @param clusterConfig the cluster configuration to use for node selection
     * @return a Mono that emits true when the operation sequence is completed
     */
    public Mono<Boolean> triggerMovingNotification(String bdbId, String endpointId, String policy,
            RedisEnterpriseConfig clusterConfig) {
        // Enhanced parameter validation
        if (endpointId == null || endpointId.trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("Endpoint ID cannot be null or empty"));
        }
        if (policy == null || policy.trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("Policy cannot be null or empty"));
        }
        if (clusterConfig == null) {
            return Mono.error(new IllegalArgumentException("Cluster configuration cannot be null"));
        }

        // Use endpoint-aware node selection
        String sourceNode = clusterConfig.getOptimalSourceNodeForEndpoint(endpointId);
        String targetNode = clusterConfig.getOptimalTargetNode();

        log.info("Auto-selected nodes for MOVING notification: source={} (endpoint-bound), target={}", sourceNode, targetNode);

        return triggerMovingNotification(bdbId, endpointId, policy, sourceNode, targetNode);
    }

    /**
     * Triggers a MOVING notification by following the proper two-step process: 1. Find which node the endpoint is pointing
     * towards 2. Migrate all shards from that node to another node (making it an "empty node") 3. Bind endpoint to trigger the
     * MOVING notification
     *
     * @param bdbId the BDB ID
     * @param endpointId the endpoint ID to rebind
     * @param policy the policy to use for rebinding (typically "single")
     * @param sourceNode the source node ID to migrate from
     * @param targetNode the target node ID to migrate to
     * @return a Mono that emits true when the operation sequence is completed
     */
    public Mono<Boolean> triggerMovingNotification(String bdbId, String endpointId, String policy, String sourceNode,
            String targetNode) {
        // Enhanced parameter validation
        if (endpointId == null || endpointId.trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("Endpoint ID cannot be null or empty"));
        }
        if (policy == null || policy.trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("Policy cannot be null or empty"));
        }
        if (sourceNode == null || sourceNode.trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("Source node cannot be null or empty"));
        }
        if (targetNode == null || targetNode.trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("Target node cannot be null or empty"));
        }

        log.info("Triggering MOVING notification for endpoint {} with policy {} on BDB {} (migrate from node {} to node {})",
                endpointId, policy, bdbId, sourceNode, targetNode);

        // Step 1: Migrate all shards from source node to target node
        String migrateCommand = String.format("migrate node %s all_shards target_node %s", sourceNode, targetNode);

        return executeRladminCommand(bdbId, migrateCommand, CHECK_INTERVAL_LONG, MEDIUM_OPERATION_TIMEOUT)
                .doOnSuccess(success -> log.info("Successfully migrated all shards from node {} to node {} on BDB {}",
                        sourceNode, targetNode, bdbId))
                .doOnError(error -> log.error("Failed to migrate shards from node {} to node {} on BDB {}: {}", sourceNode,
                        targetNode, bdbId, error.getMessage()))
                .flatMap(migrationSuccess -> {
                    if (migrationSuccess) {
                        // Step 2: Now bind the endpoint to trigger MOVING notification
                        String bindCommand = String.format("bind endpoint %s policy %s", endpointId, policy);
                        log.info("Executing bind command after migration: {}", bindCommand);

                        return executeRladminCommand(bdbId, bindCommand, CHECK_INTERVAL_MEDIUM, ENDPOINT_OPERATION_TIMEOUT)
                                .doOnSuccess(bindSuccess -> log.info("Successfully bound endpoint {} after migration on BDB {}",
                                        endpointId, bdbId))
                                .doOnError(bindError -> log.error("Failed to bind endpoint {} after migration on BDB {}: {}",
                                        endpointId, bdbId, bindError.getMessage()));
                    } else {
                        return Mono.error(new RuntimeException("Migration failed, cannot proceed with endpoint bind"));
                    }
                });
    }

    /**
     * Ensures an empty target node by migrating all shards away from it first. This is required for migration operations that
     * need an empty target node.
     * 
     * @param bdbId the BDB ID
     * @param nodeToEmpty the node ID to empty (will become the target)
     * @param destinationNode the node to move shards to
     * @return a Mono that emits true when the node is empty
     */
    public Mono<Boolean> ensureEmptyTargetNode(String bdbId, String nodeToEmpty, String destinationNode) {
        log.info("Ensuring node {} is empty by migrating all shards to node {} on BDB {}", nodeToEmpty, destinationNode, bdbId);

        // First check if the node is already empty to avoid "nothing to do" errors
        return Mono.fromCallable(() -> RedisEnterpriseConfig.discover(this, bdbId)).flatMap(currentConfig -> {
            List<String> shardsOnNode = currentConfig.getShardsForNode(nodeToEmpty);

            if (shardsOnNode.isEmpty()) {
                log.info("Node {} is already empty on BDB {}, no migration needed", nodeToEmpty, bdbId);
                return Mono.just(true);
            }

            log.info("Node {} has {} shards on BDB {}, proceeding with migration to node {}", nodeToEmpty, shardsOnNode.size(),
                    bdbId, destinationNode);

            String emptyNodeCommand = String.format("migrate node %s all_shards target_node %s", nodeToEmpty, destinationNode);
            return executeRladminCommand(bdbId, emptyNodeCommand, CHECK_INTERVAL_LONG, MEDIUM_OPERATION_TIMEOUT);
        }).doOnSuccess(success -> log.info("Successfully ensured node {} is empty on BDB {}", nodeToEmpty, bdbId))
                .doOnError(error -> log.error("Failed to empty node {} on BDB {}: {}", nodeToEmpty, bdbId, error.getMessage()));
    }

    /**
     * Triggers shard migration with automatic empty target node preparation. This method first ensures the target node is
     * empty, then performs the migration.
     * 
     * @param bdbId the BDB ID
     * @param shardId the shard ID to migrate
     * @param sourceNode the source node ID to migrate from
     * @param targetNode the target node ID to migrate to (will be emptied first)
     * @param intermediateNode a third node to temporarily hold shards from target node
     * @return a Mono that emits true when the migration is completed
     */
    public Mono<Boolean> triggerShardMigrationWithEmptyTarget(String bdbId, String shardId, String sourceNode,
            String targetNode, String intermediateNode) {
        log.info("Starting migration with empty target preparation: source={}, target={}, intermediate={}", sourceNode,
                targetNode, intermediateNode);

        // Step 1: Empty the target node first (increased timeout)
        return ensureEmptyTargetNode(bdbId, targetNode, intermediateNode).flatMap(emptySuccess -> {
            if (emptySuccess) {
                log.info("Target node {} is now empty. Waiting before proceeding with migration...", targetNode);

                // Step 2: Add delay to let the cluster stabilize, then perform actual migration with longer timeout
                return Mono.delay(STABILIZATION_DELAY)
                        .then(executeRladminCommand(bdbId,
                                String.format("migrate node %s all_shards target_node %s", sourceNode, targetNode),
                                CHECK_INTERVAL_LONG, LONG_OPERATION_TIMEOUT)) // Increased timeout to 5 minutes
                        .doOnSuccess(success -> log.info("Migration from node {} to empty node {} completed on BDB {}",
                                sourceNode, targetNode, bdbId))
                        .doOnError(error -> log.error("Migration from node {} to empty node {} failed on BDB {}: {}",
                                sourceNode, targetNode, bdbId, error.getMessage()));
            } else {
                return Mono.error(new RuntimeException("Failed to empty target node " + targetNode));
            }
        });
    }

    /**
     * Executes an rladmin command and captures the output for parsing.
     *
     * @param bdbId the BDB ID to execute the command on
     * @param rladminCommand the rladmin command to execute
     * @param checkInterval interval between status checks
     * @param timeout maximum time to wait for completion
     * @return a Mono that emits the command output when completed
     */
    public Mono<String> executeRladminCommandAndCaptureOutput(String bdbId, String rladminCommand, Duration checkInterval,
            Duration timeout) {
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

        log.info("Executing rladmin command for output capture: {} on BDB: {}", rladminCommand, bdbId);

        return triggerAction("execute_rladmin_command", parameters)
                .flatMap(response -> waitForRladminCompletionAndCaptureOutput(response.getActionId(), rladminCommand,
                        checkInterval, timeout))
                .onErrorResume(e -> {
                    log.error("Failed to execute rladmin command '{}' on BDB '{}': {}", rladminCommand, bdbId, e.getMessage());
                    return Mono.error(new RuntimeException("Rladmin command execution failed: " + e.getMessage(), e));
                });
    }

    /**
     * Executes an rladmin command and captures output with default timing parameters.
     */
    public Mono<String> executeRladminCommandAndCaptureOutput(String bdbId, String rladminCommand) {
        return executeRladminCommandAndCaptureOutput(bdbId, rladminCommand, CHECK_INTERVAL_SHORT, SHORT_OPERATION_TIMEOUT);
    }

    /**
     * Enhanced status checking for rladmin operations with output capture.
     */
    private Mono<String> waitForRladminCompletionAndCaptureOutput(String actionId, String rladminCommand,
            Duration checkInterval, Duration timeout) {
        return Mono.defer(() -> checkRladminActionStatusAndCaptureOutput(actionId, rladminCommand)).flatMap(output -> {
            if (output != null) {
                log.info("Rladmin command '{}' completed successfully with output", rladminCommand);
                return Mono.just(output);
            }
            return Mono.empty(); // Trigger retry
        }).repeatWhenEmpty(repeat -> repeat.delayElements(checkInterval).timeout(timeout)
                .doOnError(e -> log.error("Timeout waiting for rladmin command '{}' to complete", rladminCommand)));
    }

    /**
     * Checks the status of an rladmin action and captures the output.
     */
    private Mono<String> checkRladminActionStatusAndCaptureOutput(String actionId, String rladminCommand) {
        return httpClient.get().uri(BASE_URL + "/action/" + actionId).responseSingle((response, body) -> body.asString())
                .flatMap(result -> {
                    log.debug("Rladmin action '{}' status response: {}", rladminCommand, result);

                    try {
                        // Properly parse JSON response using ObjectMapper
                        JsonNode statusResponse = objectMapper.readTree(result);
                        String status = statusResponse.has("status") ? statusResponse.get("status").asText() : null;
                        String error = statusResponse.has("error") && !statusResponse.get("error").isNull()
                                ? statusResponse.get("error").asText()
                                : null;

                        // Log available fields for debugging when needed
                        if (log.isDebugEnabled()) {
                            statusResponse.fieldNames().forEachRemaining(
                                    field -> log.debug("Response field '{}': {}", field, statusResponse.get(field)));
                        }

                        // Extract the actual command output from the nested JSON structure
                        String output = null;
                        if (statusResponse.has("output")) {
                            JsonNode outputNode = statusResponse.get("output");
                            if (outputNode.isNull()) {
                                // Output field is null - command likely still running
                                log.debug("Output field is null for command '{}'", rladminCommand);
                            } else if (outputNode.isTextual()) {
                                // Simple text output
                                output = outputNode.asText();
                                log.debug("Found simple text output in 'output' field");
                            } else if (outputNode.isObject() && outputNode.has("output")) {
                                // Nested JSON with output field (expected format)
                                output = outputNode.get("output").asText();
                                log.debug("Found nested output in 'output.output' field");
                            } else {
                                log.warn("Output field found but unexpected format: {}", outputNode);
                            }
                        } else {
                            log.debug("No output field in response for '{}'", rladminCommand);
                        }

                        log.debug("Parsed status: {}, error: {}, output present: {}", status, error, output != null);

                        if ("success".equals(status) || "completed".equals(status)) {
                            if (output != null && !output.trim().isEmpty()) {
                                log.debug("Captured output for command '{}': {}", rladminCommand, output);
                                return Mono.just(output);
                            } else {
                                log.debug("Command '{}' completed but no output captured", rladminCommand);
                                return Mono.just(""); // Empty output but successful
                            }
                        }

                        if ("failed".equals(status) || error != null) {
                            return Mono.error(
                                    new RuntimeException("Rladmin command failed: status=" + status + ", error=" + error));
                        }

                        if ("running".equals(status)) {
                            log.debug("Command '{}' still {}, will retry...", rladminCommand, status);
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
                .retryWhen(reactor.util.retry.Retry.backoff(5, Duration.ofMillis(500)).maxBackoff(RETRY_MAX_BACKOFF)
                        .filter(throwable -> !(throwable instanceof RuntimeException && throwable.getMessage() != null
                                && throwable.getMessage().contains("failed")))
                        .doBeforeRetry(retrySignal -> log.debug("Retrying rladmin output capture for '{}', attempt: {}",
                                rladminCommand, retrySignal.totalRetries() + 1)));
    }

}
