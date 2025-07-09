package io.lettuce.scenario;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
     * Get a source node ID for migration (first available node).
     */
    public String getSourceNodeId() {
        if (nodeIds.isEmpty()) {
            return "1"; // Default fallback to node 1
        }
        return extractNumericNodeId(nodeIds.get(0));
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
