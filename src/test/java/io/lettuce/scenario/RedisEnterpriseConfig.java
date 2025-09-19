package io.lettuce.scenario;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.fail;

/**
 * Configuration holder for dynamically discovered Redis Enterprise cluster information.
 */
public class RedisEnterpriseConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisEnterpriseConfig.class);

    // Timeout constants for Redis Enterprise discovery operations
    // Check interval for discovery commands
    private static final Duration DISCOVERY_CHECK_INTERVAL = Duration.ofSeconds(10);

    // Timeout for discovery commands
    private static final Duration DISCOVERY_TIMEOUT = Duration.ofSeconds(30);

    // 300 seconds - for migrations/failovers
    private static final Duration LONG_OPERATION_TIMEOUT = Duration.ofMinutes(5);

    private final List<String> masterShardIds = new ArrayList<>();

    private final List<String> slaveShardIds = new ArrayList<>();

    private final List<String> endpointIds = new ArrayList<>();

    private final List<String> nodeIds = new ArrayList<>();

    // Track which nodes have shards vs empty nodes
    private final Map<String, Integer> nodeShardCounts = new HashMap<>();

    // Track which shards are on which nodes
    private final Map<String, List<String>> nodeToShards = new HashMap<>();

    // Track which endpoints are bound to which nodes
    private final Map<String, String> endpointToNode = new HashMap<>();

    // Dynamic target configuration - captured during first discovery
    private Map<String, Integer> originalConfiguration = new HashMap<>();

    private boolean originalConfigurationCaptured = false;

    private final String bdbId;

    // Patterns to parse rladmin output - updated for real Redis Enterprise format
    private static final Pattern SHARD_PATTERN = Pattern
            .compile("db:(\\d+)\\s+\\S+\\s+(redis:\\d+)\\s+(node:\\d+)\\s+(master|slave)\\s+.*");

    private static final Pattern ENDPOINT_PATTERN = Pattern
            .compile("db:(\\d+)\\s+\\S+\\s+(endpoint:\\d+:\\d+)\\s+(node:\\d+)\\s+\\S+\\s+.*");

    public RedisEnterpriseConfig(String bdbId) {
        this.bdbId = bdbId;
    }

    /**
     * Discover the Redis Enterprise configuration for the given BDB using a FaultInjectionClient.
     */
    public static RedisEnterpriseConfig discover(FaultInjectionClient faultClient, String bdbId) {
        log.info("Starting Redis Enterprise configuration discovery for BDB {}", bdbId);

        RedisEnterpriseConfig config = new RedisEnterpriseConfig(bdbId);

        try {
            // Execute single discovery command to get all cluster information at once
            String statusOutput = executeCommandAndCaptureOutput(faultClient, bdbId, "status", "full cluster discovery");

            // Parse the comprehensive output to populate configuration
            if (statusOutput != null && !statusOutput.trim().isEmpty()) {
                config.parseFullStatus(statusOutput);
            }

            // Capture original configuration on first discovery for this BDB
            if (!config.originalConfigurationCaptured) {
                config.captureOriginalConfiguration();
            }

            log.info("Configuration discovery completed: {}", config.getSummary());

            // Validate the discovered configuration
            config.validateClusterConfiguration();

        } catch (Exception e) {
            log.error("Failed to discover configuration for BDB {}: {}", bdbId, e.getMessage());
            log.error("Discovery failure details: shardsOutput present={}, endpointsOutput present={}, nodesOutput present={}",
                    e.getMessage().contains("shards"), e.getMessage().contains("endpoints"), e.getMessage().contains("nodes"));
            throw new IllegalStateException("Redis Enterprise cluster discovery failed for BDB " + bdbId
                    + ". Cluster may be unreachable, malformed, or BDB may not exist. Original error: " + e.getMessage(), e);
        }

        return config;
    }

    /**
     * Execute a command using the fault injection client and capture the real output.
     */
    private static String executeCommandAndCaptureOutput(FaultInjectionClient faultClient, String bdbId, String command,
            String description) {
        log.info("Executing command for {}: rladmin {}", description, command);

        try {
            // Use the output capture method
            String output = faultClient
                    .executeRladminCommandAndCaptureOutput(bdbId, command, DISCOVERY_CHECK_INTERVAL, DISCOVERY_TIMEOUT)
                    .doOnNext(result -> log.info("{} command completed with output length: {}", description, result.length()))
                    .doOnError(e -> log.warn("{} command failed: {}", description, e.getMessage())).onErrorReturn("").block();

            if (output != null && !output.trim().isEmpty()) {
                log.info("{} command executed successfully, captured {} characters of output", description, output.length());
                log.debug("Raw output for {}: {}", description, output);
                return output;
            } else {
                log.warn("{} command completed but no output captured", description);
                return "";
            }

        } catch (Exception e) {
            log.warn("Error during {}: {}", description, e.getMessage());
            return "";
        }
    }

    /**
     * Parse comprehensive cluster information from rladmin status output. This replaces the need for separate status shards,
     * status endpoints, and status nodes calls.
     */
    public void parseFullStatus(String statusOutput) {
        log.info("Parsing full cluster status from single command output...");

        if (statusOutput == null || statusOutput.trim().isEmpty()) {
            log.warn("Empty status output received");
            return;
        }

        // Split the output into sections and add debug logging
        log.debug("Raw status output length: {}", statusOutput.length());
        String[] sections = statusOutput.split("(?=CLUSTER NODES:|DATABASES:|ENDPOINTS:|SHARDS:)");
        log.debug("Split into {} sections", sections.length);

        for (int i = 0; i < sections.length; i++) {
            String section = sections[i].trim();
            log.debug("Processing section {}: starts with '{}'", i, section.substring(0, Math.min(50, section.length())));

            if (section.startsWith("SHARDS:")) {
                log.debug("Parsing SHARDS section with {} characters", section.length());
                parseShards(section);
            } else if (section.startsWith("ENDPOINTS:")) {
                log.debug("Parsing ENDPOINTS section with {} characters", section.length());
                parseEndpoints(section);
            } else if (section.startsWith("CLUSTER NODES:")) {
                log.debug("Parsing CLUSTER NODES section with {} characters", section.length());
                parseNodes(section);
            } else {
                log.debug("Skipping section that starts with: {}", section.substring(0, Math.min(20, section.length())));
            }
            // We can ignore DATABASES: section for now as it's not used
        }
    }

    /**
     * Parse shard information from rladmin status shards output.
     */
    public void parseShards(String shardsOutput) {
        log.info("Parsing shards from output...");

        if (shardsOutput == null || shardsOutput.trim().isEmpty()) {
            log.warn("Empty shards output received");
            return;
        }

        // Clear previous shard counts and mappings
        nodeShardCounts.clear();
        nodeToShards.clear();

        // CRITICAL: Clear cached master/slave lists to get fresh data
        masterShardIds.clear();
        slaveShardIds.clear();

        String[] lines = shardsOutput.split("\\n");
        for (String line : lines) {
            line = line.trim();
            if (line.contains("db:" + bdbId) && (line.contains("master") || line.contains("slave"))) {
                Matcher matcher = SHARD_PATTERN.matcher(line);
                if (matcher.matches()) {
                    String shardId = matcher.group(2);
                    String nodeId = matcher.group(3);
                    String role = matcher.group(4);

                    // Track node IDs
                    if (!nodeIds.contains(nodeId)) {
                        nodeIds.add(nodeId);
                        log.info("Found node: {}", nodeId);
                    }

                    // Count shards per node
                    nodeShardCounts.merge(nodeId, 1, Integer::sum);
                    log.info("DEBUG: Added shard {} to node {}, new count: {}", shardId, nodeId, nodeShardCounts.get(nodeId));

                    // Track which shards are on which nodes
                    nodeToShards.computeIfAbsent(nodeId, k -> new ArrayList<>()).add(shardId);

                    if ("master".equals(role)) {
                        masterShardIds.add(shardId);
                        log.info("Found master shard: {} on {}", shardId, nodeId);
                    } else if ("slave".equals(role)) {
                        slaveShardIds.add(shardId);
                        log.info("Found slave shard: {} on {}", shardId, nodeId);
                    }
                }
            }
        }

        // Log shard distribution
        log.info("Node shard distribution: {}", nodeShardCounts);
        log.info("DEBUG: Final nodeShardCounts after parsing - details:");
        for (Map.Entry<String, Integer> entry : nodeShardCounts.entrySet()) {
            log.info("DEBUG:   {} -> {} shards", entry.getKey(), entry.getValue());
        }
    }

    /**
     * Parse cluster nodes output to discover all nodes (including empty ones).
     */
    public void parseNodes(String nodesOutput) {
        log.info("Parsing nodes from output...");
        log.info("DEBUG: parseNodes called - current nodeIds state: {}", nodeIds);

        if (nodesOutput == null || nodesOutput.trim().isEmpty()) {
            log.warn("Empty nodes output received");
            return;
        }

        // Clear previous node data to ensure fresh discovery
        nodeIds.clear();
        log.info("DEBUG: Cleared previous node data");

        String[] lines = nodesOutput.split("\\n");
        for (String line : lines) {
            line = line.trim();
            if (line.contains("node:")) {
                // Extract node ID from lines like "node:1 master..." or "*node:1 master..."
                String[] parts = line.split("\\s+");
                if (parts.length > 0) {
                    String firstPart = parts[0];
                    // Handle both "node:1" and "*node:1" formats
                    String nodeId = null;
                    if (firstPart.startsWith("node:")) {
                        nodeId = firstPart;
                    } else if (firstPart.startsWith("*node:")) {
                        nodeId = firstPart.substring(1); // Remove the "*" prefix
                    }

                    if (nodeId != null && !nodeIds.contains(nodeId)) {
                        nodeIds.add(nodeId);
                        log.info("Found node from nodes output: {}", nodeId);
                        // Initialize shard count if not already tracked
                        nodeShardCounts.putIfAbsent(nodeId, 0);
                    }
                }
            }
        }

        log.info("All discovered nodes: {}", nodeIds);
        log.info("Initial node shard distribution: {}", nodeShardCounts);
    }

    /**
     * Parse endpoint information from rladmin status endpoints output.
     */
    public void parseEndpoints(String endpointsOutput) {
        log.info("Parsing endpoints from output...");
        log.info("DEBUG: parseEndpoints called - current endpointToNode state: {}", endpointToNode);
        log.debug("Raw endpoints output: {}", endpointsOutput);

        if (endpointsOutput == null || endpointsOutput.trim().isEmpty()) {
            log.warn("Empty endpoints output received");
            return;
        }

        // Clear previous endpoint data to avoid stale mappings
        endpointIds.clear();
        endpointToNode.clear();
        log.info("DEBUG: Cleared previous endpoint data");

        String[] lines = endpointsOutput.split("\\n");
        for (String line : lines) {
            line = line.trim();
            log.debug("Processing endpoint line: '{}'", line);
            if (line.contains("db:" + bdbId)) {
                Matcher matcher = ENDPOINT_PATTERN.matcher(line);
                if (matcher.matches()) {
                    String endpointId = matcher.group(2);
                    String nodeId = matcher.group(3);

                    log.debug("Matched endpoint - raw endpointId: '{}', nodeId: '{}'", endpointId, nodeId);
                    endpointIds.add(endpointId);
                    String previousNode = endpointToNode.put(endpointId, nodeId);
                    log.info("Found endpoint: {} on {}", endpointId, nodeId);
                    log.info("DEBUG: Added endpoint mapping: '{}' -> '{}' (previous mapping was '{}')", endpointId, nodeId,
                            previousNode);

                    // Track node IDs in case they have appeared during endpoint discovery
                    if (!nodeIds.contains(nodeId)) {
                        nodeIds.add(nodeId);
                        log.info("Found node: {}", nodeId);
                    }
                } else {
                    log.debug("Line did not match endpoint pattern: '{}'", line);
                }
            }
        }
    }

    /**
     * Get the first available master shard ID for operations.
     */
    public String getFirstMasterShardId() {
        if (masterShardIds.isEmpty()) {
            throw new IllegalStateException("No master shards found for BDB " + bdbId + ". Cluster appears to be malformed.");
        }
        return extractNumericShardId(masterShardIds.get(0));
    }

    /**
     * Get the second master shard ID for operations (if available).
     */
    public String getSecondMasterShardId() {
        if (masterShardIds.isEmpty()) {
            throw new IllegalStateException("No master shards found for BDB " + bdbId + ". Cluster appears to be malformed.");
        }
        if (masterShardIds.size() < 2) {
            log.warn(
                    "Only {} master shard(s) found for BDB {}, expected at least 2. This may indicate a malformed cluster or incomplete sharding setup.",
                    masterShardIds.size(), bdbId);
            log.warn("Available master shards: {}", masterShardIds);
            throw new IllegalStateException(
                    "Insufficient master shards found. Expected at least 2, but found " + masterShardIds.size());
        }
        return extractNumericShardId(masterShardIds.get(1));
    }

    /**
     * Extract the numeric part of shard ID from full format "redis:X" -> "X"
     */
    private String extractNumericShardId(String fullShardId) {
        if (fullShardId == null) {
            return null;
        }
        if (fullShardId.contains(":")) {
            return fullShardId.split(":")[1];
        }
        return fullShardId;
    }

    /**
     * Extract the endpoint ID part from full format "endpoint:X:Y" -> "X:Y"
     */
    private static String extractEndpointId(String fullEndpointId) {
        if (fullEndpointId == null) {
            return null;
        }
        log.debug("Extracting endpoint ID from: '{}'", fullEndpointId);

        // If it already starts with "endpoint:", remove that prefix
        if (fullEndpointId.startsWith("endpoint:")) {
            String extracted = fullEndpointId.substring("endpoint:".length());
            log.debug("Extracted endpoint ID: '{}'", extracted);
            return extracted;
        }

        // If it doesn't start with "endpoint:", return as is (might be the raw format)
        log.debug("Using endpoint ID as-is: '{}'", fullEndpointId);
        return fullEndpointId;
    }

    /**
     * Get the first available endpoint ID.
     */
    public String getFirstEndpointId() {
        if (endpointIds.isEmpty()) {
            throw new IllegalStateException("No endpoints found for BDB " + bdbId + ". Cluster appears to be malformed.");
        }
        return extractEndpointId(endpointIds.get(0));
    }

    /**
     * Get the node where an endpoint is bound.
     */
    public String getEndpointNode(String endpointId) {
        String result = endpointToNode.get(endpointId);
        log.info("DEBUG: getEndpointNode('{}') -> '{}' from endpointToNode={}", endpointId, result, endpointToNode);
        return result;
    }

    /**
     * Check if the configuration has been properly discovered.
     */
    public boolean isValid() {
        return !masterShardIds.isEmpty() && !endpointIds.isEmpty();
    }

    /**
     * Get summary of discovered configuration.
     */
    public String getSummary() {
        return String.format("Redis Enterprise Config for BDB %s: Masters=%s, Slaves=%s, Endpoints=%s", bdbId, masterShardIds,
                slaveShardIds, endpointIds);
    }

    /**
     * Capture the original configuration for this BDB to use as target state for restoration.
     */
    private void captureOriginalConfiguration() {
        log.info("Capturing original configuration for BDB {} as target state", bdbId);

        // Create a snapshot of the current node shard distribution
        originalConfiguration.clear();
        for (String nodeId : nodeIds) {
            List<String> shards = nodeToShards.get(nodeId);
            int shardCount = shards != null ? shards.size() : 0;
            originalConfiguration.put(nodeId, shardCount);
            log.info("Original config - {}: {} shards", nodeId, shardCount);
        }

        originalConfigurationCaptured = true;
        log.info("Original configuration captured for BDB {}: {}", bdbId, originalConfiguration);
    }

    /**
     * Get the target configuration for this BDB (captured from first discovery).
     */
    public Map<String, Integer> getTargetConfiguration() {
        if (!originalConfigurationCaptured) {
            throw new IllegalStateException("Original configuration not yet captured for BDB " + bdbId);
        }
        return new HashMap<>(originalConfiguration);
    }

    // Getters
    public List<String> getMasterShardIds() {
        return new ArrayList<>(masterShardIds);
    }

    public List<String> getSlaveShardIds() {
        return new ArrayList<>(slaveShardIds);
    }

    public List<String> getEndpointIds() {
        return new ArrayList<>(endpointIds);
    }

    public String getBdbId() {
        return bdbId;
    }

    public List<String> getNodeIds() {
        return new ArrayList<>(nodeIds);
    }

    /**
     * Get the shard IDs (numeric format) for a specific node.
     */
    public List<String> getShardsForNode(String nodeId) {
        log.info("*** getShardsForNode DEBUG: nodeId='{}', full nodeToShards mapping: {}", nodeId, nodeToShards);

        List<String> shards = nodeToShards.get(nodeId);
        if (shards == null || shards.isEmpty()) {
            log.warn("No shards found for node: {}", nodeId);
            log.warn("*** Available nodes in mapping: {}", nodeToShards.keySet());
            return new ArrayList<>();
        }

        // Convert to numeric format (redis:1 -> 1)
        List<String> numericShards = shards.stream().map(this::extractNumericShardId).collect(Collectors.toList());
        log.info("*** getShardsForNode DEBUG: raw shards={}, converted to numeric={}", shards, numericShards);
        return numericShards;
    }

    /**
     * Currently it only works for 3 nodes environment, and even has hardcoded node:1, node:2, node:3 This is a temporary
     * solution to get the tests running, and should be replaced with a dynamic class that can work in more than 3 nodes
     * environment.
     * 
     * Get an empty node (0 shards) - perfect for migration target. Returns null if no empty node is found (which is a valid
     * state).
     */
    public String getEmptyNode() {
        // Check all discovered nodes, not just those in nodeShardCounts
        for (String nodeId : nodeIds) {
            Integer shardCount = nodeShardCounts.get(nodeId);
            if (shardCount == null || shardCount == 0) {
                log.debug("Found empty node: {} (shard count: {})", nodeId, shardCount);
                return extractNumericNodeId(nodeId);
            }
        }

        log.debug("No empty nodes found. Node shard distribution: {}", nodeShardCounts);
        log.debug("All discovered nodes: {}", nodeIds);
        return null;
    }

    /**
     * Get a node with shards - good for migration source. Returns null if no node with shards is found (which indicates a
     * malformed cluster).
     */
    public String getNodeWithShards() {
        String nodeWithShards = nodeShardCounts.entrySet().stream().filter(entry -> entry.getValue() > 0).map(Map.Entry::getKey)
                .findFirst().map(this::extractNumericNodeId).orElse(null);

        if (nodeWithShards == null) {
            log.warn("No nodes with shards found. This indicates an empty or malformed cluster. Node shard distribution: {}",
                    nodeShardCounts);
        }

        return nodeWithShards;
    }

    /**
     * Get a second node with shards - good for intermediate storage. Returns null if fewer than 2 nodes with shards exist.
     */
    public String getSecondNodeWithShards() {
        String secondNodeWithShards = nodeShardCounts.entrySet().stream().filter(entry -> entry.getValue() > 0)
                .map(Map.Entry::getKey).skip(1) // Skip the first one
                .findFirst().map(this::extractNumericNodeId).orElse(null);

        if (secondNodeWithShards == null) {
            long nodesWithShards = nodeShardCounts.entrySet().stream().filter(entry -> entry.getValue() > 0).count();
            log.debug("Only {} node(s) with shards found, cannot get second node. Node shard distribution: {}", nodesWithShards,
                    nodeShardCounts);
        }

        return secondNodeWithShards;
    }

    /**
     * Get the node ID that contains master shards (for failover operations).
     */
    public String getNodeWithMasterShards() {
        if (masterShardIds.isEmpty()) {
            log.error("No master shards found for BDB {}. Cannot determine node with master shards.", bdbId);
            throw new IllegalStateException("No master shards found for BDB " + bdbId + ". Cluster appears to be malformed.");
        }

        // Find which node contains the first master shard
        String firstMasterShard = masterShardIds.get(0);
        log.info("DEBUG: getNodeWithMasterShards DEBUG: shard='{}'", firstMasterShard);
        for (Map.Entry<String, List<String>> entry : nodeToShards.entrySet()) {
            String nodeId = entry.getKey();
            List<String> shards = entry.getValue();
            if (shards.contains(firstMasterShard)) {
                log.info("DEBUG: getNodeWithMasterShards DEBUG: nodeId='{}'", nodeId);
                return extractNumericNodeId(nodeId);
            }
        }

        log.error("Master shard {} not found on any discovered node. Node-to-shard mapping: {}", firstMasterShard,
                nodeToShards);
        throw new IllegalStateException(
                "Master shard " + firstMasterShard + " not found on any node. Cluster mapping is inconsistent.");
    }

    /**
     * Get a third node ID for migration operations (different from source and target).
     */
    public String getIntermediateNodeId() {
        if (nodeIds.size() < 3) {
            log.error(
                    "Insufficient nodes discovered for BDB {}. Found {} nodes, need at least 3 for intermediate migration operations.",
                    bdbId, nodeIds.size());
            log.error("Available nodes: {}", nodeIds);
            throw new IllegalStateException(
                    "Insufficient nodes for intermediate migration. Found " + nodeIds.size() + " nodes, need at least 3.");
        }
        return extractNumericNodeId(nodeIds.get(2));
    }

    /**
     * Extract the numeric part of node ID from full format "node:X" -> "X"
     */
    private String extractNumericNodeId(String fullNodeId) {
        if (fullNodeId == null) {
            return null;
        }
        if (fullNodeId.contains(":")) {
            return fullNodeId.split(":")[1];
        }
        return fullNodeId;
    }

    /**
     * Check if current cluster state matches the target configuration.
     */
    public boolean isInTargetConfiguration() {
        if (nodeShardCounts.isEmpty()) {
            return false;
        }

        if (!originalConfigurationCaptured) {
            log.warn("Cannot check target configuration - original state not captured yet for BDB {}", bdbId);
            return false;
        }

        for (Map.Entry<String, Integer> targetEntry : originalConfiguration.entrySet()) {
            String nodeId = targetEntry.getKey();
            Integer expectedShards = targetEntry.getValue();
            Integer actualShards = nodeShardCounts.get(nodeId);

            if (!expectedShards.equals(actualShards)) {
                log.info("Configuration mismatch: {} expected={}, actual={}", nodeId, expectedShards, actualShards);
                return false;
            }
        }

        log.info("Cluster is in target configuration: {}", originalConfiguration);
        return true;
    }

    /**
     * Get the migration needed to reach target configuration.
     */
    public MigrationPlan getMigrationPlan() {
        if (isInTargetConfiguration()) {
            return new MigrationPlan(false, null, null, "Already in target configuration");
        }

        // Find which nodes need to be adjusted
        String emptyNode = null;
        String nodeWithShards = null;
        String emptyNodeThatShouldHaveShards = null;

        if (!originalConfigurationCaptured) {
            return new MigrationPlan(false, null, null, "Original configuration not captured yet");
        }

        for (Map.Entry<String, Integer> entry : nodeShardCounts.entrySet()) {
            String nodeId = entry.getKey();
            Integer actualShards = entry.getValue();
            Integer expectedShards = originalConfiguration.get(nodeId);

            if (expectedShards != null) {
                if (expectedShards == 0 && actualShards > 0) {
                    // This node should be empty but has shards
                    emptyNode = nodeId;
                } else if (expectedShards > 0 && actualShards == 0) {
                    // This node should have shards but is empty
                    emptyNodeThatShouldHaveShards = nodeId;
                    nodeWithShards = findNodeWithShards();
                    break;
                }
            }
        }

        if (emptyNode != null) {
            // Need to empty a node that should be empty
            String targetNode = findTargetForEmptying(emptyNode);
            return new MigrationPlan(true, emptyNode, targetNode,
                    String.format("Empty %s by moving shards to %s", emptyNode, targetNode));
        }

        if (emptyNodeThatShouldHaveShards != null && nodeWithShards != null) {
            // Need to populate a node that should have shards but is empty
            return new MigrationPlan(true, nodeWithShards, emptyNodeThatShouldHaveShards,
                    String.format("Populate %s by moving shards from %s", emptyNodeThatShouldHaveShards, nodeWithShards));
        }

        return new MigrationPlan(false, null, null, "No migration plan determined");
    }

    /**
     * Find a node that currently has shards. This is used internally for migration planning and can return null safely.
     */
    private String findNodeWithShards() {
        String nodeWithShards = nodeShardCounts.entrySet().stream().filter(entry -> entry.getValue() > 0).map(Map.Entry::getKey)
                .findFirst().orElse(null);

        if (nodeWithShards == null) {
            log.debug("No nodes with shards found during migration planning");
        }

        return nodeWithShards;
    }

    /**
     * Find a suitable target node for emptying operations. This is used internally for migration planning and can return null
     * safely.
     */
    private String findTargetForEmptying(String nodeToEmpty) {
        if (!originalConfigurationCaptured) {
            // Fallback to any node with shards if original config not available
            return findNodeWithShards();
        }

        // Find a node that should have shards in target config and can accept more
        String targetNode = originalConfiguration.entrySet().stream().filter(entry -> entry.getValue() > 0) // Should have
                                                                                                            // shards
                .map(Map.Entry::getKey).filter(nodeId -> !nodeId.equals(nodeToEmpty)) // Not the node we're emptying
                .findFirst().orElse(null);

        if (targetNode == null) {
            log.debug("No suitable target node found for emptying {} based on target configuration", nodeToEmpty);
        }

        return targetNode;
    }

    /**
     * Get optimal source node based on target configuration.
     */
    public String getOptimalSourceNode() {

        // Find any node with shards
        String nodeWithShards = getNodeWithShards();
        if (nodeWithShards == null) {
            log.error("No nodes with shards found for BDB {}. Cannot determine optimal source node.", bdbId);
            log.error("Node shard distribution: {}", nodeShardCounts);
            throw new IllegalStateException("No nodes with shards found. Cluster appears to be empty or malformed.");
        }

        return nodeWithShards;
    }

    /**
     * Get optimal source node for endpoint-based operations. This method considers which node the endpoint is currently bound
     * to and selects that node as the migration source. This ensures that after migration, the endpoint will need to be
     * rebound, triggering the desired MOVING notification.
     */
    public String getOptimalSourceNodeForEndpoint(String endpointId) {
        if (endpointId == null || endpointId.trim().isEmpty()) {
            log.warn("Endpoint ID is null or empty, falling back to general source node selection");
            return getOptimalSourceNode();
        }

        // Find which node the endpoint is currently bound to
        // Try both formats: raw endpointId and full "endpoint:X:Y" format
        String endpointNode = getEndpointNode(endpointId);
        if (endpointNode == null) {
            // Try with "endpoint:" prefix
            String fullEndpointId = "endpoint:" + endpointId;
            endpointNode = getEndpointNode(fullEndpointId);
        }

        if (endpointNode == null) {
            log.warn(
                    "Could not determine which node endpoint {} is bound to (tried both '{}' and 'endpoint:{}'), falling back to general source node selection",
                    endpointId, endpointId, endpointId);
            log.warn("Available endpoint mappings: {}", endpointToNode);
            return getOptimalSourceNode();
        }

        // Check if the endpoint's node has shards to migrate
        // endpointNode is already in "node:X" format, so use it directly
        Integer shardCount = nodeShardCounts.get(endpointNode);
        if (shardCount == null || shardCount == 0) {
            log.warn("Endpoint {} is bound to node {} which has no shards, falling back to general source node selection",
                    endpointId, endpointNode);
            return getOptimalSourceNode();
        }

        // Extract numeric node ID for return value
        String numericNodeId = extractNumericNodeId(endpointNode);
        log.info("Selected endpoint-bound node {} as migration source (has {} shards)", numericNodeId, shardCount);
        return numericNodeId;
    }

    /**
     * Get optimal target node based on target configuration.
     */
    public String getOptimalTargetNode() {

        // Find any empty node
        String emptyNode = getEmptyNode();
        if (emptyNode == null) {
            log.error("No empty nodes found for BDB {}. Cannot determine optimal target node.", bdbId);
            log.error("Node shard distribution: {}", nodeShardCounts);
            throw new IllegalStateException("No empty nodes found. All nodes have shards, cannot perform migration.");
        }

        return emptyNode;
    }

    /**
     * Get optimal intermediate node based on target configuration.
     */
    public String getOptimalIntermediateNode() {

        // Find any node with shards (not source)
        String secondNodeWithShards = getSecondNodeWithShards();
        if (secondNodeWithShards == null) {
            log.error("No second node with shards found for BDB {}. Cannot determine optimal intermediate node.", bdbId);
            log.error("Node shard distribution: {}", nodeShardCounts);
            throw new IllegalStateException(
                    "Insufficient nodes with shards for intermediate migration. Need at least 2 nodes with shards.");
        }

        return secondNodeWithShards;
    }

    /**
     * Get migration strategy based on target configuration.
     */
    public String getMigrationStrategy() {
        if (isInTargetConfiguration()) {
            return "TARGET: Already in target configuration - tests can run directly";
        }

        MigrationPlan plan = getMigrationPlan();
        if (plan.isRequired()) {
            return String.format("SETUP: %s", plan.getDescription());
        }

        return "UNKNOWN: Cannot determine migration strategy";
    }

    /**
     * Check if we can do a direct migration based on target configuration.
     */
    public boolean canMigrateDirectly() {
        if (isInTargetConfiguration()) {
            return true;
        }

        // Check if target node is actually empty
        String targetNode = getOptimalTargetNode();
        if (targetNode != null) {
            String targetNodeKey = "node:" + targetNode;
            Integer shardCount = nodeShardCounts.get(targetNodeKey);
            return shardCount != null && shardCount == 0;
        }

        return false;
    }

    /**
     * Validate that the cluster configuration is suitable for testing. This should be called after discovery to ensure we have
     * a valid cluster.
     */
    public void validateClusterConfiguration() {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // Check basic requirements
        if (nodeIds.isEmpty()) {
            errors.add("No nodes discovered");
        } else if (nodeIds.size() < 3) {
            warnings.add(String.format("Only %d nodes discovered, expected at least 3 for full testing", nodeIds.size()));
        }

        if (masterShardIds.isEmpty()) {
            errors.add("No master shards found");
        } else if (masterShardIds.size() < 2) {
            warnings.add(String.format("Only %d master shard(s) found, some tests may require 2+", masterShardIds.size()));
        }

        if (endpointIds.isEmpty()) {
            errors.add("No endpoints found");
        }

        // Check node-to-shard mapping consistency
        if (!nodeToShards.isEmpty()) {
            for (String masterId : masterShardIds) {
                boolean foundOnNode = false;
                for (List<String> nodeShards : nodeToShards.values()) {
                    if (nodeShards.contains(masterId)) {
                        foundOnNode = true;
                        break;
                    }
                }
                if (!foundOnNode) {
                    errors.add(String.format("Master shard %s not found on any node", masterId));
                }
            }
        }

        // Check if we have the expected nodes from original configuration (if captured)
        if (originalConfigurationCaptured) {
            for (String expectedNode : originalConfiguration.keySet()) {
                if (!nodeIds.contains(expectedNode)) {
                    warnings.add(String.format("Expected node %s not found in cluster", expectedNode));
                }
            }
        }

        // Log results
        if (!errors.isEmpty()) {
            log.error("Cluster validation FAILED for BDB {}:", bdbId);
            for (String error : errors) {
                log.error("  ERROR: {}", error);
            }
            log.error("Current state: nodes={}, masters={}, endpoints={}", nodeIds, masterShardIds, endpointIds);
            throw new IllegalStateException("Cluster validation failed: " + String.join(", ", errors));
        }

        if (!warnings.isEmpty()) {
            log.warn("Cluster validation found issues for BDB {}:", bdbId);
            for (String warning : warnings) {
                log.warn("  WARNING: {}", warning);
            }
        } else {
            log.info("Cluster validation passed for BDB {}", bdbId);
        }
    }

    /**
     * Migration plan for reaching target configuration.
     */
    public static class MigrationPlan {

        private final boolean required;

        private final String sourceNode;

        private final String targetNode;

        private final String description;

        public MigrationPlan(boolean required, String sourceNode, String targetNode, String description) {
            this.required = required;
            this.sourceNode = sourceNode;
            this.targetNode = targetNode;
            this.description = description;
        }

        public boolean isRequired() {
            return required;
        }

        public String getSourceNode() {
            return sourceNode;
        }

        public String getTargetNode() {
            return targetNode;
        }

        public String getDescription() {
            return description;
        }

    }

    // Track original cluster state for proper cleanup
    private static Map<String, String> originalShardRoles = new HashMap<>();

    private static Map<String, List<String>> originalNodeToShards = new HashMap<>();

    private static Map<String, String> originalEndpointToNode = new HashMap<>();

    private static boolean originalStateRecorded = false;

    /**
     * Refresh the Redis Enterprise cluster configuration before test.
     */
    public static RedisEnterpriseConfig refreshClusterConfig(FaultInjectionClient faultClient, String bdbId) {
        log.info("Refreshing Redis Enterprise cluster configuration before test...");
        // Use the discovery service to get real-time cluster state
        RedisEnterpriseConfig clusterConfig = RedisEnterpriseConfig.discover(faultClient, bdbId);
        log.info("Cluster configuration refreshed: {}", clusterConfig.getSummary());

        // Record original state for proper cleanup (only once)
        // if (originalStateRecorded) {
        // restoreOriginalClusterState(faultClient, bdbId);
        // } else {
        recordOriginalClusterState(faultClient, bdbId);
        // originalStateRecorded = true;
        // }

        return clusterConfig;
    }

    /**
     * Record the original cluster state (both shard distribution and roles) for later restoration.
     */
    private static void recordOriginalClusterState(FaultInjectionClient faultClient, String bdbId) {
        log.info("Recording original cluster state for cleanup...");

        try {
            // Get the complete current configuration
            RedisEnterpriseConfig currentConfig = RedisEnterpriseConfig.discover(faultClient, bdbId);

            // Record shard roles (getMasterShardIds already returns "redis:X" format)
            originalShardRoles.clear();
            for (String masterShard : currentConfig.getMasterShardIds()) {
                originalShardRoles.put(masterShard, "master");
            }
            for (String slaveShard : currentConfig.getSlaveShardIds()) {
                originalShardRoles.put(slaveShard, "slave");
            }

            // Record shard distribution across nodes (getShardsForNode already returns "redis:X" format)
            originalNodeToShards.clear();
            for (String nodeId : currentConfig.getNodeIds()) {
                List<String> shards = currentConfig.getShardsForNode(nodeId);
                originalNodeToShards.put(nodeId, new ArrayList<>(shards));
            }

            // Record endpoint bindings
            originalEndpointToNode.clear();
            for (String endpointId : currentConfig.getEndpointIds()) {
                String nodeId = currentConfig.getEndpointNode(endpointId);
                originalEndpointToNode.put(endpointId, nodeId);
            }

            log.info("Original cluster state recorded:");
            log.info("  Shard roles: {}", originalShardRoles);
            log.info("  Node distribution: {}", originalNodeToShards);
            log.info("  Endpoint bindings: {}", originalEndpointToNode);

        } catch (Exception e) {
            log.warn("Failed to record original cluster state: {}", e.getMessage());
        }
    }

    /**
     * Restore the original cluster state (both shard distribution and roles) recorded at startup. This ensures all tests start
     * with the exact same cluster state.
     */
    private static void restoreOriginalClusterState(FaultInjectionClient faultClient, String bdbId) {
        log.info("Restoring original cluster state...");

        try {
            // Get current state
            RedisEnterpriseConfig currentConfig = RedisEnterpriseConfig.discover(faultClient, bdbId);

            // Log current state
            log.info("Current cluster state before restoration:");
            for (String nodeId : currentConfig.getNodeIds()) {
                List<String> shards = currentConfig.getShardsForNode(nodeId);
                log.info("  {}: {} shards {}", nodeId, shards.size(), shards);
            }

            // Step 1: Restore shard distribution across nodes
            boolean needsMigration = false;
            for (Map.Entry<String, List<String>> entry : originalNodeToShards.entrySet()) {
                String nodeId = entry.getKey();
                List<String> expectedShards = entry.getValue();
                List<String> currentShards = new ArrayList<>();

                // Get current shards (already in "redis:X" format)
                currentShards.addAll(currentConfig.getShardsForNode(nodeId));

                if (!expectedShards.equals(currentShards)) {
                    needsMigration = true;
                    log.info("Node {} has wrong shards. Expected: {}, Current: {}", nodeId, expectedShards, currentShards);
                }
            }

            if (needsMigration) {
                log.info("Need to restore shard distribution. Performing migrations...");

                // Strategy: Find misplaced shards and migrate them to their correct nodes
                // First, find nodes that have shards but should be empty
                for (Map.Entry<String, List<String>> entry : originalNodeToShards.entrySet()) {
                    String nodeId = entry.getKey();
                    List<String> expectedShards = entry.getValue();
                    List<String> currentShards = new ArrayList<>(currentConfig.getShardsForNode(nodeId));

                    if (expectedShards.isEmpty() && !currentShards.isEmpty()) {
                        // This node should be empty but has shards - migrate them away
                        log.info("Node {} should be empty but has {} shards - migrating away", nodeId, currentShards.size());

                        // Find the node that should have these shards
                        String sourceNodeNum = nodeId.replace("node:", "");
                        String targetNodeNum = null;

                        for (Map.Entry<String, List<String>> targetEntry : originalNodeToShards.entrySet()) {
                            String potentialTarget = targetEntry.getKey();
                            List<String> potentialTargetExpected = targetEntry.getValue();
                            List<String> potentialTargetCurrent = currentConfig.getShardsForNode(potentialTarget);

                            // Find a node that should have shards but currently doesn't have enough
                            if (!potentialTargetExpected.isEmpty() && !potentialTarget.equals(nodeId)
                                    && potentialTargetCurrent.size() < potentialTargetExpected.size()) {
                                targetNodeNum = potentialTarget.replace("node:", "");
                                break;
                            }
                        }

                        if (targetNodeNum != null) {
                            String migrateCommand = "migrate node " + sourceNodeNum + " all_shards target_node "
                                    + targetNodeNum;
                            log.info("Executing restoration migration: {}", migrateCommand);

                            StepVerifier
                                    .create(faultClient.executeRladminCommand(bdbId, migrateCommand, DISCOVERY_CHECK_INTERVAL,
                                            LONG_OPERATION_TIMEOUT))
                                    .expectNext(true).expectComplete().verify(LONG_OPERATION_TIMEOUT);

                            // Refresh config after migration
                            currentConfig = RedisEnterpriseConfig.discover(faultClient, bdbId);
                            break; // Only one migration at a time to avoid conflicts
                        }
                    }
                }

                log.info("Shard distribution restored");
            }

            // Step 2: Restore master/slave roles
            // Only failover shards that are currently MASTERS but should be SLAVES
            List<String> mastersToFailover = new ArrayList<>();
            for (Map.Entry<String, String> entry : originalShardRoles.entrySet()) {
                String shardId = entry.getKey();
                String originalRole = entry.getValue();

                // Only failover shards that are currently masters but should be slaves
                if ("slave".equals(originalRole) && currentConfig.getMasterShardIds().contains(shardId)) {
                    // Should be slave but is currently master - failover this master
                    mastersToFailover.add(shardId.replace("redis:", ""));
                    log.info("Shard {} should be slave but is currently master - will failover", shardId);
                }
            }

            if (!mastersToFailover.isEmpty()) {
                log.info("Found {} master shards that should be slaves, failing them over: {}", mastersToFailover.size(),
                        mastersToFailover);

                // Build failover command (only failover current masters)
                String failoverCommand = "failover shard " + String.join(" ", mastersToFailover);
                log.info("Executing restoration failover: {}", failoverCommand);

                // Execute the failover
                StepVerifier.create(faultClient.executeRladminCommand(bdbId, failoverCommand, DISCOVERY_CHECK_INTERVAL,
                        LONG_OPERATION_TIMEOUT)).expectNext(true).expectComplete().verify(LONG_OPERATION_TIMEOUT);

                log.info("Role restoration failover completed");
            } else {
                log.info("No role restoration needed - all shards are in correct roles");
            }

            // Step 3: Restore endpoint bindings
            for (Map.Entry<String, String> entry : originalEndpointToNode.entrySet()) {
                String endpointId = entry.getKey();
                String originalNodeId = entry.getValue();
                String currentNodeId = currentConfig.getEndpointNode(endpointId);

                log.info("Checking endpoint binding: endpointId='{}', originalNodeId='{}', currentNodeId='{}'", endpointId,
                        originalNodeId, currentNodeId);

                if (!originalNodeId.equals(currentNodeId)) {
                    log.info("Endpoint {} is bound to node {}, but should be bound to {}. Rebinding...", endpointId,
                            currentNodeId, originalNodeId);
                    // Extract the endpoint ID without the "endpoint:" prefix for the bind command
                    String extractedEndpointId = extractEndpointId(endpointId);
                    String rebindCommand = "bind endpoint " + extractedEndpointId + " policy single";
                    log.info("Executing rebind command: '{}'", rebindCommand);
                    StepVerifier.create(faultClient.executeRladminCommand(bdbId, rebindCommand, DISCOVERY_CHECK_INTERVAL,
                            LONG_OPERATION_TIMEOUT)).expectNext(true).expectComplete().verify(LONG_OPERATION_TIMEOUT);
                    log.info("Endpoint {} rebinded to {}", endpointId, originalNodeId);
                } else {
                    log.info("Endpoint {} is already correctly bound to {}", endpointId, originalNodeId);
                }
            }

            // Step 4: Verify final state matches original
            currentConfig = RedisEnterpriseConfig.discover(faultClient, bdbId);
            log.info("Final cluster state after restoration:");
            for (String nodeId : currentConfig.getNodeIds()) {
                List<String> shards = currentConfig.getShardsForNode(nodeId);
                log.info("  {}: {} shards {}", nodeId, shards.size(), shards);
            }
            log.info("Original cluster state restored successfully");

        } catch (Exception e) {
            fail("Failed to restore original cluster state - test should fail if we reach this line: " + e.getMessage());
            log.warn("Failed to restore original cluster state: {}", e.getMessage());
        }
    }
}
