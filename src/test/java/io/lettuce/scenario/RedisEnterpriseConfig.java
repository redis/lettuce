package io.lettuce.scenario;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.stream.Collectors;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.test.StepVerifier;

/**
 * Configuration holder for dynamically discovered Redis Enterprise cluster information.
 */
public class RedisEnterpriseConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisEnterpriseConfig.class);

    // Timeout constants for Redis Enterprise discovery operations
    private static final Duration DISCOVERY_CHECK_INTERVAL = Duration.ofSeconds(2); // Check interval for discovery commands

    private static final Duration DISCOVERY_TIMEOUT = Duration.ofSeconds(30); // Timeout for discovery commands

    private static final Duration LONG_OPERATION_TIMEOUT = Duration.ofMinutes(5); // 300 seconds - for migrations/failovers

    private final List<String> masterShardIds = new ArrayList<>();

    private final List<String> slaveShardIds = new ArrayList<>();

    private final List<String> endpointIds = new ArrayList<>();

    private final List<String> nodeIds = new ArrayList<>();

    // Track which nodes have shards vs empty nodes
    private final Map<String, Integer> nodeShardCounts = new HashMap<>();

    // Track which shards are on which nodes
    private final Map<String, List<String>> nodeToShards = new HashMap<>();

    // Define target configuration for tests
    private static final Map<String, Integer> TARGET_CONFIGURATION;
    static {
        Map<String, Integer> config = new HashMap<>();
        config.put("node:1", 2); // node:1 has 2 shards - good source
        config.put("node:2", 0); // node:2 is empty - perfect target
        config.put("node:3", 2); // node:3 has 2 shards - good intermediate
        TARGET_CONFIGURATION = Collections.unmodifiableMap(config);
    }

    private final String bdbId;

    // Patterns to parse rladmin output
    private static final Pattern SHARD_PATTERN = Pattern
            .compile("db:(\\d+)\\s+\\S+\\s+(\\S+)\\s+(node:\\d+)\\s+(master|slave)\\s+.*");

    private static final Pattern ENDPOINT_PATTERN = Pattern.compile("db:(\\d+)\\s+\\S+\\s+(\\S+)\\s+(node:\\d+)\\s+\\S+\\s+.*");

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
            // Execute discovery commands to get actual cluster information
            String shardsOutput = executeCommandAndCaptureOutput(faultClient, bdbId, "status shards", "shards discovery");
            String endpointsOutput = executeCommandAndCaptureOutput(faultClient, bdbId, "status endpoints",
                    "endpoints discovery");
            String nodesOutput = executeCommandAndCaptureOutput(faultClient, bdbId, "status nodes", "nodes discovery");

            // Parse the actual output to populate configuration using existing methods
            if (shardsOutput != null && !shardsOutput.trim().isEmpty()) {
                config.parseShards(shardsOutput);
            }

            if (endpointsOutput != null && !endpointsOutput.trim().isEmpty()) {
                config.parseEndpoints(endpointsOutput);
            }

            if (nodesOutput != null && !nodesOutput.trim().isEmpty()) {
                config.parseNodes(nodesOutput);
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
    }

    /**
     * Parse cluster nodes output to discover all nodes (including empty ones).
     */
    public void parseNodes(String nodesOutput) {
        log.info("Parsing nodes from output...");

        if (nodesOutput == null || nodesOutput.trim().isEmpty()) {
            log.warn("Empty nodes output received");
            return;
        }

        String[] lines = nodesOutput.split("\\n");
        for (String line : lines) {
            line = line.trim();
            if (line.contains("node:")) {
                // Extract node ID from lines like "node:1 master 10.0.101.47..."
                String[] parts = line.split("\\s+");
                if (parts.length > 0 && parts[0].startsWith("node:")) {
                    String nodeId = parts[0];
                    if (!nodeIds.contains(nodeId)) {
                        nodeIds.add(nodeId);
                        log.info("Found node from nodes output: {}", nodeId);
                    }
                    // Initialize shard count if not already tracked
                    nodeShardCounts.putIfAbsent(nodeId, 0);
                }
            }
        }

        log.info("All discovered nodes: {}", nodeIds);
        log.info("Final node shard distribution: {}", nodeShardCounts);
    }

    /**
     * Parse endpoint information from rladmin status endpoints output.
     */
    public void parseEndpoints(String endpointsOutput) {
        log.info("Parsing endpoints from output...");

        if (endpointsOutput == null || endpointsOutput.trim().isEmpty()) {
            log.warn("Empty endpoints output received");
            return;
        }

        String[] lines = endpointsOutput.split("\\n");
        for (String line : lines) {
            line = line.trim();
            if (line.contains("db:" + bdbId)) {
                Matcher matcher = ENDPOINT_PATTERN.matcher(line);
                if (matcher.matches()) {
                    String endpointId = matcher.group(2);
                    String nodeId = matcher.group(3);

                    endpointIds.add(endpointId);
                    log.info("Found endpoint: {} on {}", endpointId, nodeId);

                    // Track node IDs in case they have appeared during endpoint discovery
                    if (!nodeIds.contains(nodeId)) {
                        nodeIds.add(nodeId);
                        log.info("Found node: {}", nodeId);
                    }
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
    private String extractEndpointId(String fullEndpointId) {
        if (fullEndpointId == null) {
            return null;
        }
        if (fullEndpointId.startsWith("endpoint:")) {
            return fullEndpointId.substring("endpoint:".length());
        }
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
     * Set the node-to-shard mapping (used by dynamic discovery).
     */
    public void setNodeToShards(Map<String, List<String>> nodeToShards) {
        this.nodeToShards.clear();
        this.nodeToShards.putAll(nodeToShards);

        // Also populate the node shard counts for consistency
        this.nodeShardCounts.clear();
        for (Map.Entry<String, List<String>> entry : nodeToShards.entrySet()) {
            String nodeId = entry.getKey();
            int shardCount = entry.getValue().size();
            this.nodeShardCounts.put(nodeId, shardCount);

            // Ensure node IDs are tracked in case they have appeared during shards discovery
            if (!nodeIds.contains(nodeId)) {
                nodeIds.add(nodeId);
            }
        }

        log.info("Node-to-shard mapping updated: {}", nodeToShards);
        log.info("Node shard counts updated: {}", nodeShardCounts);
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
        String emptyNode = nodeShardCounts.entrySet().stream().filter(entry -> entry.getValue() == 0).map(Map.Entry::getKey)
                .findFirst().map(this::extractNumericNodeId).orElse(null);

        if (emptyNode == null) {
            log.debug("No empty nodes found. Node shard distribution: {}", nodeShardCounts);
        }

        return emptyNode;
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

        for (Map.Entry<String, List<String>> entry : nodeToShards.entrySet()) {
            String nodeId = entry.getKey();
            List<String> shards = entry.getValue();
            if (shards.contains(firstMasterShard)) {
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

        for (Map.Entry<String, Integer> targetEntry : TARGET_CONFIGURATION.entrySet()) {
            String nodeId = targetEntry.getKey();
            Integer expectedShards = targetEntry.getValue();
            Integer actualShards = nodeShardCounts.get(nodeId);

            if (!expectedShards.equals(actualShards)) {
                log.info("Configuration mismatch: {} expected={}, actual={}", nodeId, expectedShards, actualShards);
                return false;
            }
        }

        log.info("Cluster is in target configuration: {}", TARGET_CONFIGURATION);
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

        for (Map.Entry<String, Integer> entry : nodeShardCounts.entrySet()) {
            String nodeId = entry.getKey();
            Integer actualShards = entry.getValue();
            Integer expectedShards = TARGET_CONFIGURATION.get(nodeId);

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
        // Find a node that should have shards in target config and can accept more
        String targetNode = TARGET_CONFIGURATION.entrySet().stream().filter(entry -> entry.getValue() > 0) // Should have shards
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
        // In target config, node:1 should have shards
        if (TARGET_CONFIGURATION.containsKey("node:1") && TARGET_CONFIGURATION.get("node:1") > 0) {
            // Verify this node actually exists and has shards
            String expectedSourceNode = "1";
            if (!nodeIds.contains("node:1")) {
                log.warn("Target configuration expects node:1 to exist, but it was not discovered. Available nodes: {}",
                        nodeIds);
            }
            Integer actualShards = nodeShardCounts.get("node:1");
            if (actualShards == null || actualShards == 0) {
                log.warn("Target configuration expects node:1 to have shards, but it has {} shards. Shard distribution: {}",
                        actualShards, nodeShardCounts);
            }
            return expectedSourceNode;
        }

        // Find any node with shards
        String nodeWithShards = getNodeWithShards();
        if (nodeWithShards == null) {
            log.error("No nodes with shards found for BDB {}. Cannot determine optimal source node.", bdbId);
            log.error("Node shard distribution: {}", nodeShardCounts);
            throw new IllegalStateException("No nodes with shards found. Cluster appears to be empty or malformed.");
        }

        log.warn("Using fallback source node {} instead of optimal node:1", nodeWithShards);
        return nodeWithShards;
    }

    /**
     * Get optimal target node based on target configuration.
     */
    public String getOptimalTargetNode() {
        // In target config, node:2 should be empty
        if (TARGET_CONFIGURATION.containsKey("node:2") && TARGET_CONFIGURATION.get("node:2") == 0) {
            // Verify this node actually exists
            if (!nodeIds.contains("node:2")) {
                log.warn("Target configuration expects node:2 to exist, but it was not discovered. Available nodes: {}",
                        nodeIds);
            }
            return "2";
        }

        // Find any empty node
        String emptyNode = getEmptyNode();
        if (emptyNode == null) {
            log.error("No empty nodes found for BDB {}. Cannot determine optimal target node.", bdbId);
            log.error("Node shard distribution: {}", nodeShardCounts);
            throw new IllegalStateException("No empty nodes found. All nodes have shards, cannot perform migration.");
        }

        log.warn("Using fallback target node {} instead of optimal node:2", emptyNode);
        return emptyNode;
    }

    /**
     * Get optimal intermediate node based on target configuration.
     */
    public String getOptimalIntermediateNode() {
        // In target config, node:3 should have shards
        if (TARGET_CONFIGURATION.containsKey("node:3") && TARGET_CONFIGURATION.get("node:3") > 0) {
            // Verify this node actually exists and has shards
            if (!nodeIds.contains("node:3")) {
                log.warn("Target configuration expects node:3 to exist, but it was not discovered. Available nodes: {}",
                        nodeIds);
            }
            Integer actualShards = nodeShardCounts.get("node:3");
            if (actualShards == null || actualShards == 0) {
                log.warn("Target configuration expects node:3 to have shards, but it has {} shards. Shard distribution: {}",
                        actualShards, nodeShardCounts);
            }
            return "3";
        }

        // Find any node with shards (not source)
        String secondNodeWithShards = getSecondNodeWithShards();
        if (secondNodeWithShards == null) {
            log.error("No second node with shards found for BDB {}. Cannot determine optimal intermediate node.", bdbId);
            log.error("Node shard distribution: {}", nodeShardCounts);
            throw new IllegalStateException(
                    "Insufficient nodes with shards for intermediate migration. Need at least 2 nodes with shards.");
        }

        log.warn("Using fallback intermediate node {} instead of optimal node:3", secondNodeWithShards);
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
        return isInTargetConfiguration() || (getOptimalTargetNode().equals("2") && getOptimalSourceNode().equals("1"));
    }

    /**
     * Update shard distribution to match target configuration.
     */
    public void setToTargetConfiguration() {
        nodeShardCounts.clear();
        nodeShardCounts.putAll(TARGET_CONFIGURATION);
        log.info("Set to target configuration: {}", TARGET_CONFIGURATION);
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

        // Check if we have the expected target configuration nodes
        for (String expectedNode : TARGET_CONFIGURATION.keySet()) {
            if (!nodeIds.contains(expectedNode)) {
                warnings.add(String.format("Expected node %s not found in cluster", expectedNode));
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
        if (!originalStateRecorded) {
            recordOriginalClusterState(faultClient, bdbId);
            originalStateRecorded = true;
        } else {
            // For subsequent tests, restore original state first
            restoreOriginalClusterState(faultClient, bdbId);
        }

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

            log.info("Original cluster state recorded:");
            log.info("  Shard roles: {}", originalShardRoles);
            log.info("  Node distribution: {}", originalNodeToShards);

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

                            Thread.sleep(20000);

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

                // Wait for completion
                Thread.sleep(15000);
                log.info("Role restoration failover completed");
            } else {
                log.info("No role restoration needed - all shards are in correct roles");
            }

            // Step 3: Verify final state matches original
            currentConfig = RedisEnterpriseConfig.discover(faultClient, bdbId);
            log.info("Final cluster state after restoration:");
            for (String nodeId : currentConfig.getNodeIds()) {
                List<String> shards = currentConfig.getShardsForNode(nodeId);
                log.info("  {}: {} shards {}", nodeId, shards.size(), shards);
            }
            log.info("Original cluster state restored successfully");

        } catch (Exception e) {
            log.warn("Failed to restore original cluster state: {}", e.getMessage());
        }
    }

}
