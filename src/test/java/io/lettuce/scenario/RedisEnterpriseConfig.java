package io.lettuce.scenario;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration holder for dynamically discovered Redis Enterprise cluster information.
 */
public class RedisEnterpriseConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisEnterpriseConfig.class);

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

                    // Track node IDs
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
            throw new IllegalStateException("No master shards found for BDB " + bdbId);
        }
        return extractNumericShardId(masterShardIds.get(0));
    }

    /**
     * Get the second master shard ID for operations (if available).
     */
    public String getSecondMasterShardId() {
        if (masterShardIds.size() < 2) {
            // Fall back to first master if only one exists
            return getFirstMasterShardId();
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
            throw new IllegalStateException("No endpoints found for BDB " + bdbId);
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

            // Ensure node IDs are tracked
            if (!nodeIds.contains(nodeId)) {
                nodeIds.add(nodeId);
            }
        }

        log.info("Node-to-shard mapping updated: {}", nodeToShards);
        log.info("Node shard counts updated: {}", nodeShardCounts);
    }

    /**
     * Get an empty node (0 shards) - perfect for migration target.
     */
    public String getEmptyNode() {
        return nodeShardCounts.entrySet().stream().filter(entry -> entry.getValue() == 0).map(Map.Entry::getKey).findFirst()
                .map(this::extractNumericNodeId).orElse(null);
    }

    /**
     * Get a node with shards - good for migration source.
     */
    public String getNodeWithShards() {
        return nodeShardCounts.entrySet().stream().filter(entry -> entry.getValue() > 0).map(Map.Entry::getKey).findFirst()
                .map(this::extractNumericNodeId).orElse(null);
    }

    /**
     * Get a second node with shards - good for intermediate storage.
     */
    public String getSecondNodeWithShards() {
        return nodeShardCounts.entrySet().stream().filter(entry -> entry.getValue() > 0).map(Map.Entry::getKey).skip(1) // Skip
                                                                                                                        // the
                                                                                                                        // first
                                                                                                                        // one
                .findFirst().map(this::extractNumericNodeId).orElse(null);
    }

    /**
     * Get a source node ID for migration (first available node).
     */
    public String getSourceNodeId() {
        if (nodeIds.isEmpty()) {
            return "1"; // Default fallback to node 1
        }
        return extractNumericNodeId(nodeIds.get(0));
    }

    /**
     * Get the node ID that contains master shards (for failover operations).
     */
    public String getNodeWithMasterShards() {
        if (masterShardIds.isEmpty()) {
            return "3"; // Fallback to node 3
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

        return "3"; // Fallback to node 3
    }

    /**
     * Get a target node ID for migration (different from source).
     */
    public String getTargetNodeId() {
        if (nodeIds.size() < 2) {
            return "2"; // Default fallback to node 2
        }
        return extractNumericNodeId(nodeIds.get(1));
    }

    /**
     * Get a third node ID for migration operations (different from source and target).
     */
    public String getIntermediateNodeId() {
        if (nodeIds.size() < 3) {
            return "3"; // Default fallback to node 3
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

        return new MigrationPlan(false, null, null, "No migration plan determined");
    }

    /**
     * Find a node that currently has shards.
     */
    private String findNodeWithShards() {
        return nodeShardCounts.entrySet().stream().filter(entry -> entry.getValue() > 0).map(Map.Entry::getKey).findFirst()
                .orElse(null);
    }

    /**
     * Find a suitable target node for emptying operations.
     */
    private String findTargetForEmptying(String nodeToEmpty) {
        // Find a node that should have shards in target config and can accept more
        return TARGET_CONFIGURATION.entrySet().stream().filter(entry -> entry.getValue() > 0) // Should have shards
                .map(Map.Entry::getKey).filter(nodeId -> !nodeId.equals(nodeToEmpty)) // Not the node we're emptying
                .findFirst().orElse(null);
    }

    /**
     * Get optimal source node based on target configuration.
     */
    public String getOptimalSourceNode() {
        // In target config, node:1 should have shards
        if (TARGET_CONFIGURATION.containsKey("node:1") && TARGET_CONFIGURATION.get("node:1") > 0) {
            return "1";
        }
        // Fallback to any node with shards
        String nodeWithShards = getNodeWithShards();
        return nodeWithShards != null ? nodeWithShards : "1";
    }

    /**
     * Get optimal target node based on target configuration.
     */
    public String getOptimalTargetNode() {
        // In target config, node:2 should be empty
        if (TARGET_CONFIGURATION.containsKey("node:2") && TARGET_CONFIGURATION.get("node:2") == 0) {
            return "2";
        }
        // Fallback to any empty node
        String emptyNode = getEmptyNode();
        return emptyNode != null ? emptyNode : "2";
    }

    /**
     * Get optimal intermediate node based on target configuration.
     */
    public String getOptimalIntermediateNode() {
        // In target config, node:3 should have shards
        if (TARGET_CONFIGURATION.containsKey("node:3") && TARGET_CONFIGURATION.get("node:3") > 0) {
            return "3";
        }
        // Fallback to any node with shards (not source)
        String secondNodeWithShards = getSecondNodeWithShards();
        return secondNodeWithShards != null ? secondNodeWithShards : "3";
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

    /**
     * Populate configuration with target defaults for consistent testing.
     */
    public void populateWithTargetDefaults() {
        log.info("Populating Redis Enterprise config with target defaults for BDB {}", bdbId);

        // Add common node IDs
        if (nodeIds.isEmpty()) {
            nodeIds.add("node:1");
            nodeIds.add("node:2");
            nodeIds.add("node:3");
            log.info("Added default node IDs: {}", nodeIds);
        }

        // Set target configuration
        setToTargetConfiguration();

        // Add typical shard configuration
        if (masterShardIds.isEmpty()) {
            masterShardIds.add("redis:1");
            masterShardIds.add("redis:2");
            log.info("Added default master shard IDs: {}", masterShardIds);
        }

        // Add typical endpoint configuration
        if (endpointIds.isEmpty()) {
            endpointIds.add("endpoint:" + bdbId + ":1");
            log.info("Added default endpoint ID: {}", endpointIds.get(0));
        }
    }

    /**
     * Populate configuration with reasonable defaults based on typical Redis Enterprise setups. This method uses common node
     * IDs and configurations that are likely to exist in real clusters.
     */
    public void populateWithReasonableDefaults() {
        log.info("Populating Redis Enterprise config with reasonable defaults for BDB {}", bdbId);

        // Add common node IDs that are typically found in Redis Enterprise clusters
        if (nodeIds.isEmpty()) {
            nodeIds.add("node:1");
            nodeIds.add("node:2");
            nodeIds.add("node:3");
            log.info("Added default node IDs: {}", nodeIds);
        }

        // Use target configuration for consistency
        setToTargetConfiguration();

        // Add typical shard configuration
        if (masterShardIds.isEmpty()) {
            masterShardIds.add("redis:1");
            masterShardIds.add("redis:2");
            log.info("Added default master shard IDs: {}", masterShardIds);
        }

        // Note: shard-to-node mapping should be populated by dynamic discovery
        // No longer using hardcoded mappings - let the actual parsing populate this data

        // Add typical endpoint configuration
        if (endpointIds.isEmpty()) {
            endpointIds.add("endpoint:" + bdbId + ":1");
            log.info("Added default endpoint ID: {}", endpointIds.get(0));
        }
    }

    /**
     * Populate configuration with fallback defaults when discovery fails completely.
     */
    public void populateWithFallbackDefaults() {
        log.warn("Populating Redis Enterprise config with fallback defaults for BDB {}", bdbId);

        // Ensure we have at least minimal configuration
        if (nodeIds.isEmpty()) {
            nodeIds.add("node:1");
            nodeIds.add("node:2");
        }

        if (masterShardIds.isEmpty()) {
            masterShardIds.add("redis:1");
        }

        if (endpointIds.isEmpty()) {
            endpointIds.add("endpoint:" + bdbId + ":1");
        }

        log.warn("Fallback configuration: nodes={}, masters={}, endpoints={}", nodeIds, masterShardIds, endpointIds);
    }

}
